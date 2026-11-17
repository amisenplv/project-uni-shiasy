import assert from 'node:assert/strict';
import { createHash, randomBytes, randomUUID } from 'node:crypto';
import { readFile } from 'node:fs/promises';
import { request as httpRequest } from 'node:http';
import { after, before, beforeEach, test } from 'node:test';
import { setTimeout as delay } from 'node:timers/promises';
import { localDatabase } from '../src/database.ts';
import { createApp } from '../src/http.ts';
import { migrate } from '../src/migrate.ts';
import { day, verifier } from '../src/security.ts';
import { snapshotMaxBytes, snapshotProblem } from '../src/snapshot-codec.ts';
import { localSnapshotTtl } from '../src/snapshots.ts';

assert.equal(process.env.VEHICLE_PG_DATABASE, 'vehicle_m51_test');
const pool = localDatabase('vehicle_app');
const operator = localDatabase('vehicle_invites');
const migrator = localDatabase('vehicle_migrator');
const key = randomBytes(32);
let now = Date.parse('2026-11-15T00:00:00Z');
const clock = () => now;
const ttl = 3_600_000;
const app = createApp(pool, key, clock, undefined, ttl);
const signedExport = await readFile(new URL('./fixtures/m4-signed-export.json', import.meta.url));
const fixture = signedExport.toString();
const empty = '{"formatVersion":1,"exportedAt":"2026-11-15T09:09:16Z","currencyUnit":"legacy_irr","vehicles":[],"entries":[],"readings":[],"reminders":[]}';
const exact = Buffer.from(fixture
    .replace('"M46-Ledger"', '"خودرو 🚘 \\u0645"')
    .replace('"amountRials":12345', '"amountRials":9223372036854775807')
    .replace('"createdAtEpochMillis":1794733505921', '"createdAtEpochMillis":-9223372036854775808') + '\n ');
const auth = (token: string) => ({ authorization: 'Bearer ' + token });
const quote = (revision: string) => '"' + revision + '"';

before(() => migrate(migrator));
beforeEach(() => { now += day; });
after(async () => {
    await app.close();
    await Promise.all([pool.end(), operator.end(), migrator.end()]);
});

async function account(username: string) {
    const code = randomBytes(16).toString('hex');
    await operator.query('INSERT INTO pilot_invites (verifier, expires_at) VALUES ($1, $2)',
        [verifier(code), new Date(now + day)]);
    const response = await app.inject({
        method: 'POST', url: '/v1/auth/register',
        payload: { username, password: 'synthetic snapshot test password', inviteCode: code },
    });
    assert.equal(response.statusCode, 201, response.body);
    return response.json() as { accountId: string; sessionToken: string; sessionExpiresAt: string };
}

const metadata = (token: string, instance = app) => instance.inject({
    method: 'GET', url: '/v1/backup/metadata', headers: auth(token),
});
const download = (token: string, revision: string) => app.inject({
    method: 'GET', url: '/v1/backup', headers: { ...auth(token), 'if-match': quote(revision) },
});
function uploadHeaders(token: string, revision: string, attempt: string) {
    return { ...auth(token), 'if-match': quote(revision), 'idempotency-key': attempt,
        'snapshot-format': 'snapshot-v1', 'content-type': 'application/json' };
}
function upload(token: string, revision: string, payload: Buffer | string, attempt = randomUUID(), instance = app) {
    return instance.inject({
        method: 'PUT', url: '/v1/backup', headers: uploadHeaders(token, revision, attempt), payload,
    });
}

async function slot(token: string) {
    const response = await metadata(token);
    assert.equal(response.statusCode, 200, response.body);
    const data = response.json();
    assert.equal(response.headers.etag, quote(data.slotRevision));
    assert.equal(response.headers['cache-control'], 'no-store');
    return data;
}

function error(response: { statusCode: number; json: () => any; headers: object; body: string }, status: number, code: string) {
    assert.equal(response.statusCode, status, response.body);
    const { error: result } = response.json();
    assert.equal(result.code, code);
    assert.match(result.requestId, /^[a-f0-9-]{36}$/);
    assert.deepEqual(Object.keys(result).sort(), ['code', 'requestId']);
    assert.equal(response.headers['cache-control'], 'no-store');
}

async function waitForAccountLocks(count: number) {
    for (let tries = 0; tries < 120; tries++) {
        const { rows: [row] } = await pool.query(`SELECT count(*)::integer AS count FROM pg_stat_activity
            WHERE usename = current_user AND query = $1 AND wait_event_type = 'Lock'`,
        ['SELECT id FROM accounts WHERE id = $1 FOR UPDATE']);
        if (row.count >= count) return;
        await delay(15);
    }
    assert.fail('upload did not reach the PostgreSQL account lock');
}

async function holdAccount(owner: string) {
    const connection = await pool.connect();
    await connection.query('BEGIN');
    await connection.query('SELECT id FROM accounts WHERE id = $1 FOR UPDATE', [owner]);
    return connection;
}

test('serves a signed M4 export and exact signed 64-bit values without rewriting UTF-8 bytes', async () => {
    assert.equal(createHash('sha256').update(signedExport).digest('hex'),
        '75b71e8e1173e481c9c3f78a16c27b6ea3360de061ad8300daafccc7662d9d86');
    const { sessionToken, accountId } = await account('snap_exact');
    for (const payload of [signedExport, exact]) {
        const attempt = randomUUID();
        const base = (await slot(sessionToken)).slotRevision;
        const response = await upload(sessionToken, base, payload, attempt);
        assert.equal(response.statusCode, 200, response.body);
        const current = response.json();
        assert.notEqual(current.slotRevision, base);
        assert.equal(response.headers.etag, quote(current.slotRevision));
        assert.deepEqual(current.snapshot, {
            attemptId: attempt, acknowledgedAt: new Date(now).toISOString(),
            expiresAt: new Date(now + ttl).toISOString(), byteLength: payload.length,
            sha256: createHash('sha256').update(payload).digest('hex'), payloadFormat: 'snapshot-v1',
        });
        assert.deepEqual(await slot(sessionToken), current);
        const { rows: [row] } = await pool.query('SELECT payload FROM snapshot_slots WHERE account_id = $1', [accountId]);
        assert.deepEqual(row.payload, payload);
        const file = await download(sessionToken, current.slotRevision);
        assert.equal(file.statusCode, 200, file.body);
        assert.equal(file.headers['snapshot-format'], 'snapshot-v1');
        assert.equal(file.headers['content-length'], String(payload.length));
        assert.equal(file.headers.etag, quote(current.slotRevision));
        assert.equal(file.headers['cache-control'], 'no-store');
        assert.deepEqual(file.rawPayload, payload);
    }
});

test('account isolation applies to upload, receipts, metadata and downloads', async () => {
    const a = await account('snap_owner_a');
    const b = await account('snap_owner_b');
    const aEmpty = await slot(a.sessionToken);
    const bEmpty = await slot(b.sessionToken);
    assert.notEqual(aEmpty.slotRevision, bEmpty.slotRevision);
    const attempt = randomUUID();
    const aResult = (await upload(a.sessionToken, aEmpty.slotRevision, signedExport, attempt)).json();
    error(await upload(b.sessionToken, aResult.slotRevision, empty, attempt), 412, 'revision_conflict');
    assert.deepEqual(await slot(b.sessionToken), bEmpty);
    error(await download(b.sessionToken, aResult.slotRevision), 404, 'snapshot_missing');
    assert.equal((await upload(b.sessionToken, bEmpty.slotRevision, empty, attempt)).statusCode, 200);
    error(await download(b.sessionToken, aResult.slotRevision), 412, 'revision_conflict');
    assert.deepEqual(await slot(a.sessionToken), aResult);
    assert.deepEqual((await download(a.sessionToken, aResult.slotRevision)).rawPayload, signedExport);
    for (const response of [await metadata(''), await download('', aResult.slotRevision),
        await upload('', aResult.slotRevision, signedExport)]) error(response, 401, 'invalid_session');
});

test('empty ledger content is distinct from a missing or expired snapshot', async () => {
    const { sessionToken } = await account('snap_empty');
    const first = await slot(sessionToken);
    assert.equal(first.snapshot, null);
    assert.deepEqual(await slot(sessionToken), first);
    error(await download(sessionToken, first.slotRevision), 404, 'snapshot_missing');
    const attempt = randomUUID();
    const result = (await upload(sessionToken, first.slotRevision, empty, attempt)).json();
    assert.equal((await download(sessionToken, result.slotRevision)).body, empty);
    now += ttl;
    error(await download(sessionToken, result.slotRevision), 404, 'snapshot_missing');
    const expired = await slot(sessionToken);
    assert.equal(expired.snapshot, null);
    assert.notEqual(expired.slotRevision, result.slotRevision);
    assert.notEqual(expired.slotRevision, first.slotRevision);
    error(await upload(sessionToken, first.slotRevision, empty, attempt), 409, 'attempt_superseded');
    error(await upload(sessionToken, result.slotRevision, empty), 412, 'revision_conflict');
    assert.deepEqual(await slot(sessionToken), expired);
});

test('rejects malformed, ambiguous, incompatible and invalid ledger data before storing it', async () => {
    const { sessionToken } = await account('snap_invalid');
    const base = (await slot(sessionToken)).slotRevision;
    const cases: [Buffer | string, string][] = [
        ['{', 'snapshot_invalid'],
        [Buffer.from([0xc0, 0xaf]), 'snapshot_invalid'],
        ['\uFEFF' + empty, 'snapshot_invalid'],
        [fixture.replace('"kind":"car"', '"kind":"car","ki\\u006ed":"car"'), 'snapshot_invalid'],
        [fixture.replace('"vehicleId":"630092eb-0ee6-46d8-af2e-ad42aa4c39ce"', '"vehicleId":"00000000-0000-0000-0000-000000000000"'), 'snapshot_invalid'],
        [fixture.replace('"formatVersion":1', '"formatVersion":2,"future":true'), 'snapshot_incompatible'],
        [fixture.replace('legacy_irr', 'toman'), 'snapshot_incompatible'],
        [fixture.replace('"amountRials":12345', '"amountRials":9223372036854775808'), 'snapshot_invalid'],
        [fixture.replace('"amountRials":12345', '"amountRials":12345e0'), 'snapshot_invalid'],
        [fixture.replace('"occurredOn":"2026-11-15"', '"occurredOn":"2026-02-29"'), 'snapshot_invalid'],
        [fixture.replace('"dueKilometers":null', '"dueKilometers":0'), 'snapshot_invalid'],
        [fixture.replace('"state":"active"', '"state":"pending"'), 'snapshot_invalid'],
        [fixture.replace('"M46Fuel"', '"\\ud800"'), 'snapshot_invalid'],
        [fixture.replace('"M46-Ledger"', '"\\u00a0\\u001c"'), 'snapshot_invalid'],
        [fixture.replace('"notes":null', '"unrecognized":null'), 'snapshot_invalid'],
    ];
    for (const [bytes, reason] of cases) error(await upload(sessionToken, base, bytes), 422, reason);
    assert.deepEqual(await slot(sessionToken), { slotRevision: base, snapshot: null });
});

test('M4 canonical instants, domain rules, collection and parser limits', () => {
    for (const time of ['-1000000000-01-01T00:00:00Z', '+1000000000-12-31T23:59:59.999999999Z',
        '-0001-01-01T00:00:00Z', '0000-02-29T00:00:00Z', '2024-02-29T00:00:00.010Z']) {
        assert.equal(snapshotProblem(Buffer.from(empty.replace('2026-11-15T09:09:16Z', time))), undefined, time);
    }
    for (const time of ['2026-02-29T00:00:00Z', '2026-11-15T24:00:00Z', '2026-11-15T00:00:00.100000Z',
        '+1000000001-01-01T00:00:00Z', '-0000-01-01T00:00:00Z', '2026-11-15T00:00:00.1Z']) {
        assert.equal(snapshotProblem(Buffer.from(empty.replace('2026-11-15T09:09:16Z', time))), 'snapshot_invalid', time);
    }

    const data = JSON.parse(fixture);
    const originalReading = data.readings[0];
    data.readings.push({ ...originalReading, id: '00000000-0000-0000-0000-000000000009',
        kilometers: 0, entryId: null });
    assert.equal(snapshotProblem(Buffer.from(JSON.stringify(data))), undefined);
    data.readings[1].occurredOn = '2026-11-16';
    assert.equal(snapshotProblem(Buffer.from(JSON.stringify(data))), 'snapshot_invalid');
    data.readings[1] = { ...originalReading, id: '00000000-0000-0000-0000-000000000009' };
    assert.equal(snapshotProblem(Buffer.from(JSON.stringify(data))), 'snapshot_invalid');

    const vehicle = data.vehicles[0];
    const vehicles = Array.from({ length: 101 }, (_, index) => ({
        ...vehicle, id: '00000000-0000-0000-0000-' + index.toString(16).padStart(12, '0'),
    }));
    const withVehicles = (items: object[]) =>
        Buffer.from(empty.replace('"vehicles":[]', '"vehicles":' + JSON.stringify(items)));
    assert.equal(snapshotProblem(withVehicles(vehicles.slice(0, 100))), undefined);
    assert.equal(snapshotProblem(withVehicles(vehicles)), 'snapshot_invalid');
    assert.equal(snapshotProblem(Buffer.from(empty.replace('"vehicles":[]', '"vehicles":[[[[]]]]'))), 'snapshot_invalid');
    assert.equal(snapshotProblem(Buffer.from(empty.replace('"vehicles":[]', '"vehicles":[' +
        'null,'.repeat(350_000) + 'null]'))), 'snapshot_invalid');
});

test('size cap accepts exactly 4 MiB and rejects known and streamed oversize bodies', async () => {
    const { sessionToken } = await account('snap_size');
    const base = (await slot(sessionToken)).slotRevision;
    const atLimit = Buffer.alloc(snapshotMaxBytes, 32);
    Buffer.from(empty).copy(atLimit);
    const result = await upload(sessionToken, base, atLimit);
    assert.equal(result.statusCode, 200, result.body);
    const revision = result.json().slotRevision;
    const over = Buffer.concat([atLimit, Buffer.from(' ')]);
    error(await upload(sessionToken, revision, over), 413, 'body_too_large');
    const listener = createApp(pool, key, clock, undefined, ttl);
    try {
        const address = await listener.listen({ host: '127.0.0.1', port: 0 });
        const response = await new Promise<{ status?: number; body: string }>((resolve, reject) => {
            const req = httpRequest(address + '/v1/backup',
                { method: 'PUT', headers: { ...uploadHeaders(sessionToken, revision, randomUUID()), 'transfer-encoding': 'chunked' } },
                res => {
                    res.setEncoding('utf8');
                    let body = '';
                    res.on('data', chunk => { body += chunk; });
                    res.on('end', () => resolve({ status: res.statusCode, body }));
                    res.on('error', reject);
                });
            req.on('error', reject);
            req.write(over.subarray(0, 1024));
            req.end(over.subarray(1024));
        });
        assert.equal(response.status, 413, response.body);
        assert.equal(JSON.parse(response.body).error.code, 'body_too_large');
        assert.equal((await upload(sessionToken, revision, empty, randomUUID(), listener)).statusCode, 200);
    } finally {
        await listener.close();
    }
});

test('same-base writes race on real PostgreSQL and commit exactly one result', async () => {
    const { sessionToken, accountId } = await account('snap_race');
    const base = (await slot(sessionToken)).slotRevision;
    const lock = await holdAccount(accountId);
    const first = upload(sessionToken, base, signedExport);
    let second: ReturnType<typeof upload> | undefined;
    try {
        await waitForAccountLocks(1);
        second = upload(sessionToken, base, exact);
        await waitForAccountLocks(2);
    } finally {
        await lock.query('COMMIT');
        lock.release();
    }
    assert.ok(second);
    const results = await Promise.all([first, second]);
    assert.deepEqual(results.map(result => result.statusCode).sort(), [200, 412]);
    const accepted = results.find(result => result.statusCode === 200)!.json();
    assert.deepEqual(await slot(sessionToken), accepted);
    const { rows: [row] } = await pool.query('SELECT count(*)::integer AS count FROM snapshot_receipts WHERE account_id = $1', [accountId]);
    assert.equal(row.count, 1);
});

test('lost acknowledgement retries across app instances without a new revision', async () => {
    const { sessionToken, accountId } = await account('snap_retry');
    const base = (await slot(sessionToken)).slotRevision;
    const attempt = randomUUID();
    const listener = createApp(pool, key, clock, undefined, ttl);
    const address = await listener.listen({ host: '127.0.0.1', port: 0 });
    const lock = await holdAccount(accountId);
    let responseStarted = false;
    const wire = httpRequest(address + '/v1/backup', {
        method: 'PUT', headers: { ...uploadHeaders(sessionToken, base, attempt), 'content-length': signedExport.length },
    }, response => { responseStarted = true; response.resume(); });
    wire.on('error', () => {});
    try {
        wire.end(signedExport);
        await waitForAccountLocks(1);
        wire.destroy();
        assert.equal(responseStarted, false);
    } finally {
        await lock.query('COMMIT');
        lock.release();
        await listener.close();
    }

    const accepted = await slot(sessionToken);
    assert.equal(accepted.snapshot.attemptId, attempt);
    now += 15_000;
    const retry = await upload(sessionToken, base, signedExport, attempt);
    assert.equal(retry.statusCode, 200, retry.body);
    assert.deepEqual(retry.json(), accepted);
    error(await upload(sessionToken, base, exact, attempt), 409, 'attempt_mismatch');
    error(await upload(sessionToken, base, '{', attempt), 409, 'attempt_mismatch');
    const replaced = await upload(sessionToken, accepted.slotRevision, exact);
    assert.equal(replaced.statusCode, 200);
    error(await upload(sessionToken, base, signedExport, attempt), 409, 'attempt_superseded');
    error(await download(sessionToken, accepted.slotRevision), 412, 'revision_conflict');
});

test('receipt expiry does not allow a stale exact retry to create a new revision', async () => {
    const { sessionToken, accountId } = await account('snap_receipt_expiry');
    const base = (await slot(sessionToken)).slotRevision;
    const attempt = randomUUID();
    assert.equal((await upload(sessionToken, base, empty, attempt)).statusCode, 200);
    now += 7 * day;
    error(await upload(sessionToken, base, empty, attempt), 412, 'revision_conflict');
    const expired = await slot(sessionToken);
    assert.equal(expired.snapshot, null);
    assert.notEqual(expired.slotRevision, base);
    assert.equal((await pool.query('SELECT 1 FROM snapshot_receipts WHERE account_id = $1', [accountId])).rowCount, 0);
});

test('upload rate budget is scoped to the account and resets at the window boundary', async () => {
    const a = await account('snap_rate_a');
    const b = await account('snap_rate_b');
    const aBase = (await slot(a.sessionToken)).slotRevision;
    const bBase = (await slot(b.sessionToken)).slotRevision;
    for (let i = 0; i < 20; i++) error(await upload(a.sessionToken, aBase, '{'), 422, 'snapshot_invalid');
    const limited = await upload(a.sessionToken, aBase, empty);
    error(limited, 429, 'rate_limited');
    assert.equal(limited.headers['retry-after'], '3600');
    assert.equal((await upload(b.sessionToken, bBase, empty)).statusCode, 200);
    now += ttl;
    assert.equal((await upload(a.sessionToken, aBase, empty)).statusCode, 200);
});

test('logout and expiry winning the account lock prevent an in-flight upload', async () => {
    for (const revoke of [true, false]) {
        const { sessionToken, accountId, sessionExpiresAt } = await account(revoke ? 'snap_revoked' : 'snap_elapsed');
        const base = (await slot(sessionToken)).slotRevision;
        const lock = await holdAccount(accountId);
        let logout: ReturnType<typeof app.inject> | undefined;
        let write: ReturnType<typeof upload> | undefined;
        try {
            if (revoke) {
                logout = app.inject({ method: 'POST', url: '/v1/auth/logout', headers: auth(sessionToken) });
                await waitForAccountLocks(1);
            }
            write = upload(sessionToken, base, signedExport);
            await waitForAccountLocks(revoke ? 2 : 1);
            if (!revoke) now = Date.parse(sessionExpiresAt);
        } finally {
            await lock.query('COMMIT');
            lock.release();
        }
        if (logout) assert.equal((await logout).statusCode, 204);
        assert.ok(write);
        error(await write, 401, 'invalid_session');
        error(await metadata(sessionToken), 401, 'invalid_session');
        error(await download(sessionToken, base), 401, 'invalid_session');
        const { rows: [row] } = await pool.query('SELECT revision, payload FROM snapshot_slots WHERE account_id = $1', [accountId]);
        assert.deepEqual(row, { revision: base, payload: null });
    }
});

test('receipt write failure rolls back the payload and preserves the previous acknowledgement', async () => {
    const { sessionToken, accountId } = await account('snap_rollback');
    const base = (await slot(sessionToken)).slotRevision;
    const seeded = await upload(sessionToken, base, signedExport);
    assert.equal(seeded.statusCode, 200);
    const previous = seeded.json();
    const attempt = randomUUID();
    await migrator.query(`CREATE FUNCTION fail_snapshot_receipt() RETURNS trigger LANGUAGE plpgsql AS
        $$ BEGIN RAISE EXCEPTION 'synthetic receipt failure'; END; $$;
        CREATE TRIGGER fail_snapshot_receipt BEFORE INSERT ON snapshot_receipts
        FOR EACH ROW EXECUTE FUNCTION fail_snapshot_receipt()`);
    try {
        // An isolated app logger verifies that PostgreSQL details are not exposed.
        const logs: object[] = [];
        const listener = createApp(pool, key, clock, entry => logs.push(entry), ttl);
        try {
            const fail = await upload(sessionToken, previous.slotRevision, exact, attempt, listener);
            error(fail, 500, 'internal_error');
            assert.deepEqual(logs, [{ requestId: fail.json().error.requestId, code: 'internal_error' }]);
        } finally {
            await listener.close();
        }
        assert.deepEqual(await slot(sessionToken), previous);
        assert.deepEqual((await download(sessionToken, previous.slotRevision)).rawPayload, signedExport);
        assert.equal((await pool.query('SELECT 1 FROM snapshot_receipts WHERE account_id = $1 AND attempt_id = $2',
            [accountId, attempt])).rowCount, 0);
    } finally {
        await migrator.query('DROP TRIGGER fail_snapshot_receipt ON snapshot_receipts; DROP FUNCTION fail_snapshot_receipt()');
    }
    assert.equal((await upload(sessionToken, previous.slotRevision, exact, attempt)).statusCode, 200);
});

test('explicit valid PUT is required and HTTP control headers fail closed', async () => {
    const { sessionToken, accountId } = await account('snap_explicit');
    const noUpload = () => pool.query('SELECT 1 FROM snapshot_slots WHERE account_id = $1 AND payload IS NOT NULL', [accountId]);
    assert.equal((await noUpload()).rowCount, 0);
    const base = (await slot(sessionToken)).slotRevision;
    error(await download(sessionToken, base), 404, 'snapshot_missing');
    error(await app.inject({ method: 'GET', url: '/v1/backup', headers: auth(sessionToken) }), 428, 'precondition_required');
    error(await download(sessionToken, '*'), 400, 'invalid_request');
    const headers = uploadHeaders(sessionToken, base, randomUUID());
    error(await app.inject({ method: 'PUT', url: '/v1/backup',
        headers: { ...headers, 'content-encoding': 'gzip' }, payload: empty }), 415, 'unsupported_media_type');
    error(await app.inject({ method: 'PUT', url: '/v1/backup',
        headers: { ...headers, 'snapshot-format': 'encrypted-snapshot-v1' }, payload: empty }), 422, 'snapshot_incompatible');
    error(await app.inject({ method: 'POST', url: '/v1/backup', headers: auth(sessionToken) }), 404, 'route_missing');
    assert.deepEqual(await slot(sessionToken), { slotRevision: base, snapshot: null });
    assert.equal((await noUpload()).rowCount, 0);
    assert.throws(() => localSnapshotTtl(undefined), /invalid_local_snapshot_ttl/);
    assert.throws(() => localSnapshotTtl('7776000'), /invalid_local_snapshot_ttl/);
});
