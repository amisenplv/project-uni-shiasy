import assert from 'node:assert/strict';
import { randomBytes } from 'node:crypto';
import { spawnSync } from 'node:child_process';
import { after, before, test } from 'node:test';
import { fileURLToPath } from 'node:url';
import { sessionAccount, withAuthorizedAccount } from '../src/accounts.ts';
import { localDatabase } from '../src/database.ts';
import { createApp } from '../src/http.ts';
import { migrate } from '../src/migrate.ts';
import { day, verifier } from '../src/security.ts';

assert.equal(process.env.VEHICLE_PG_DATABASE, 'vehicle_m51_test');
const pool = localDatabase('vehicle_app');
const operator = localDatabase('vehicle_invites');
const migrator = localDatabase('vehicle_migrator');
let now = Date.parse('2026-11-15T00:00:00Z');
const clock = () => now;
const key = randomBytes(32);
const log: unknown[] = [];
const app = createApp(pool, key, clock, event => log.push(event));
const firstPassword = 'synthetic eligibility fixture';
const nextPassword = 'synthetic replacement fixture';

before(async () => {
    assert.deepEqual((await pool.query('SHOW listen_addresses')).rows, [{ listen_addresses: '' }]);
    assert.match((await pool.query('SHOW server_version')).rows[0].server_version, /^18\.(?:[7-9]|\d{2,})\b/);
    const counts = await Promise.all([migrate(migrator), migrate(migrator)]);
    assert.deepEqual(counts.sort(), [0, 2]);
    assert.equal(await migrate(migrator), 0);
});

after(async () => {
    await app.close();
    await Promise.all([pool.end(), operator.end(), migrator.end()]);
});

function post(route: string, payload?: object | string | Buffer, token?: string, url = '/v1/auth/' + route) {
    return app.inject({
        method: 'POST', url, payload,
        headers: {
            ...(payload !== undefined && { 'content-type': 'application/json' }),
            ...(token !== undefined && { authorization: 'Bearer ' + token }),
        },
    });
}

async function invite(expires = now + 7 * day, revoked?: number) {
    const code = randomBytes(16).toString('hex');
    await operator.query('INSERT INTO pilot_invites (verifier, expires_at) VALUES ($1, $2)',
        [verifier(code), new Date(expires)]);
    if (revoked !== undefined) {
        await operator.query('UPDATE pilot_invites SET revoked_at = $1 WHERE verifier = $2',
            [new Date(revoked), verifier(code)]);
    }
    return code;
}

async function register(username: string, inviteCode?: string) {
    return post('register', { username, inviteCode: inviteCode ?? await invite(), password: firstPassword });
}

function error(response: Awaited<ReturnType<typeof post>>, status: number, code: string) {
    assert.equal(response.statusCode, status, response.body);
    const payload = response.json();
    assert.deepEqual(Object.keys(payload), ['error']);
    assert.deepEqual(Object.keys(payload.error).sort(), ['code', 'requestId']);
    assert.equal(payload.error.code, code);
    assert.match(payload.error.requestId, /^[a-f0-9-]{36}$/);
    assert.equal(response.headers['cache-control'], 'no-store');
}

test('migrations serialize and are repeatable, with limited database roles', async () => {
    const { rows } = await migrator.query('SELECT filename FROM schema_migrations');
    assert.deepEqual(rows, [{ filename: '001_accounts.sql' }, { filename: '002_snapshots.sql' }]);
    await assert.rejects(pool.query('CREATE TABLE role_probe (id int)'), { code: '42501' });
    await assert.rejects(operator.query('SELECT password_tag FROM accounts'), { code: '42501' });
    await assert.rejects(pool.query('INSERT INTO pilot_invites (verifier, expires_at) VALUES ($1, now())', [randomBytes(32)]),
        { code: '42501' });
});

test('registration consumes an invite once and stores only verifiers', async () => {
    now += day;
    const code = await invite();
    const response = await register('register_once', code);
    assert.equal(response.statusCode, 201, response.body);
    const data = response.json();
    assert.equal(Date.parse(data.sessionExpiresAt), now + 30 * day);
    assert.equal(Date.parse(data.recoveryExpiresAt), now + 365 * day);
    const { rows: [row] } = await pool.query(`SELECT a.password_salt, a.password_tag, i.verifier AS invite, i.redeemed_at,
        s.verifier AS session, c.verifier AS recovery
        FROM accounts a JOIN pilot_invites i ON i.account_id = a.id
        JOIN sessions s ON s.account_id = a.id JOIN recovery_codes c ON c.account_id = a.id WHERE a.id = $1`,
    [data.accountId]);
    assert.equal(row.password_salt.length, 16);
    assert.equal(row.password_tag.length, 32);
    assert.equal(row.redeemed_at.getTime(), now);
    assert.deepEqual(row.invite, verifier(code));
    assert.deepEqual(row.session, verifier(data.sessionToken));
    assert.deepEqual(row.recovery, verifier(data.recoveryCode));
    error(await register('register_replay', code), 401, 'invalid_credentials');
});

test('competing invite redemptions allow one commit and one linked account', async () => {
    now += day;
    const code = await invite();
    const responses = await Promise.all([register('invite_race_a', code), register('invite_race_b', code)]);
    assert.deepEqual(responses.map(item => item.statusCode).sort(), [201, 401]);
    const winner = responses.find(item => item.statusCode === 201)!.json();
    const { rows } = await pool.query('SELECT account_id FROM pilot_invites WHERE verifier = $1', [verifier(code)]);
    assert.deepEqual(rows, [{ account_id: winner.accountId }]);
    assert.equal((await pool.query("SELECT 1 FROM accounts WHERE username IN ('invite_race_a', 'invite_race_b')")).rowCount, 1);
});

test('expired and revoked invites fail, and username conflict leaves a valid invite usable', async () => {
    now += day;
    error(await register('invite_expired', await invite(now)), 401, 'invalid_credentials');
    error(await register('invite_revoked', await invite(now + day, now)), 401, 'invalid_credentials');
    error(await register('register_once', 'e'.repeat(32)), 401, 'invalid_credentials');
    const code = await invite();
    error(await register('register_once', code), 409, 'registration_conflict');
    assert.equal((await register('invite_reassigned', code)).statusCode, 201);
});

test('sign-in scopes sessions to the bearer account and logout revokes only its session', async () => {
    now += day;
    const a = (await register('owner_a')).json();
    const b = (await register('owner_b')).json();
    const result = await post('login', { username: 'owner_a', password: firstPassword });
    assert.equal(result.statusCode, 200, result.body);
    const login = result.json();
    assert.equal(login.accountId, a.accountId);
    assert.notEqual(login.sessionToken, a.sessionToken);
    assert.equal(await sessionAccount(pool, verifier(b.sessionToken), now), b.accountId);
    error(await post('recovery-code', { password: firstPassword, accountId: b.accountId }, a.sessionToken),
        400, 'invalid_request');
    assert.equal(await withAuthorizedAccount(pool, verifier(a.sessionToken), clock, async (client, owner) => {
        const result = await client.query('SELECT 1 FROM accounts WHERE id = $1 AND id = $2', [owner, b.accountId]);
        return result.rowCount;
    }), 0);
    error(await post('logout', { unexpected: true }, a.sessionToken), 400, 'invalid_request');
    const logout = await app.inject({
        method: 'POST', url: '/v1/auth/logout',
        headers: { authorization: 'Bearer ' + a.sessionToken,
            'content-type': 'application/x-www-form-urlencoded', 'content-length': '0' },
    });
    assert.equal(logout.statusCode, 204, logout.body);
    error(await post('logout', undefined, a.sessionToken), 401, 'invalid_session');
    assert.equal(await sessionAccount(pool, verifier(login.sessionToken), now), a.accountId);
    assert.equal(await sessionAccount(pool, verifier(b.sessionToken), now), b.accountId);
    now = Date.parse(login.sessionExpiresAt);
    error(await post('logout', undefined, login.sessionToken), 401, 'invalid_session');
});

test('wrong password and unknown username receive equivalent safe errors', async () => {
    now += day;
    for (const username of ['owner_b', 'does_not_exist']) {
        error(await post('login', { username, password: 'synthetic wrong password' }), 401, 'invalid_credentials');
    }
    error(await post('recovery-code', { password: firstPassword }), 401, 'invalid_session');
});

test('concurrent recovery consumes the code once, rotates password and revokes old sessions', async () => {
    now += day;
    const original = (await register('recover_race')).json();
    const login = (await post('login', { username: 'recover_race', password: firstPassword })).json();
    const body = { username: 'recover_race', recoveryCode: original.recoveryCode, newPassword: nextPassword };
    const responses = await Promise.all([post('recover', body), post('recover', body)]);
    assert.deepEqual(responses.map(response => response.statusCode).sort(), [200, 401]);
    const winner = responses.find(response => response.statusCode === 200)!.json();
    assert.equal(winner.accountId, original.accountId);
    assert.notEqual(winner.recoveryCode, original.recoveryCode);
    error(await post('recover', body), 401, 'invalid_credentials');
    error(await post('login', { username: 'recover_race', password: firstPassword }), 401, 'invalid_credentials');
    for (const token of [login.sessionToken, original.sessionToken]) {
        error(await post('logout', undefined, token), 401, 'invalid_session');
    }
    assert.equal(await sessionAccount(pool, verifier(winner.sessionToken), now), original.accountId);
    now += 900_000;
    assert.equal((await post('login', { username: 'recover_race', password: nextPassword })).statusCode, 200);
});

test('recovery requires the matching account and a current code, including after credential rotation', async () => {
    now += day;
    const a = (await register('rotation_a')).json();
    const b = (await register('rotation_b')).json();
    error(await post('recover', { username: 'rotation_b', recoveryCode: a.recoveryCode, newPassword: nextPassword }),
        401, 'invalid_credentials');
    const rotated = await post('recovery-code', { password: firstPassword }, a.sessionToken);
    assert.equal(rotated.statusCode, 200, rotated.body);
    error(await post('recover', { username: 'rotation_a', recoveryCode: a.recoveryCode, newPassword: nextPassword }),
        401, 'invalid_credentials');
    assert.equal((await post('recover',
        { username: 'rotation_a', recoveryCode: rotated.json().recoveryCode, newPassword: nextPassword })).statusCode, 200);
    now = Date.parse(b.recoveryExpiresAt);
    error(await post('recover', { username: 'rotation_b', recoveryCode: b.recoveryCode, newPassword: nextPassword }),
        401, 'invalid_credentials');
});

test('failed sign-ins persist throttling across app instances and expire at the boundary', async () => {
    now += day;
    const payload = { username: 'rate_login', password: firstPassword };
    for (let i = 0; i < 5; i++) error(await post('login', payload), 401, 'invalid_credentials');
    const other = createApp(pool, key, clock);
    try {
        const limited = await other.inject({ method: 'POST', url: '/v1/auth/login', payload });
        error(limited, 429, 'rate_limited');
        assert.equal(limited.headers['retry-after'], '900');
    } finally {
        await other.close();
    }
    now += 900_000;
    error(await post('login', payload), 401, 'invalid_credentials');
});

test('concurrent throttle checks do not exceed the per-username or per-source window', async () => {
    now += day;
    const payload = { username: 'rate_concurrent', recoveryCode: 'x', newPassword: nextPassword };
    const run = (body: typeof payload) => post('recover', body);
    const results = await Promise.all(Array.from({ length: 9 }, () => run(payload)));
    assert.equal(results.filter(result => result.statusCode === 401).length, 5);
    assert.equal(results.filter(result => result.statusCode === 429).length, 4);
    now += day;
    const source = await Promise.all(Array.from({ length: 14 }, (_, index) =>
        run({ ...payload, username: 'rate_source_' + index })));
    assert.equal(source.filter(result => result.statusCode === 401).length, 10);
    assert.equal(source.filter(result => result.statusCode === 429).length, 4);
});

test('rejects duplicate, unknown, oversized and malformed input and exposes no snapshot routes', async () => {
    now += day;
    error(await post('login', '{"username":"bad","user\\u006eame":"other","password":"secret"}'), 400, 'invalid_request');
    error(await post('login', { username: 'bad', password: '\ud800' }), 400, 'invalid_request');
    error(await post('login', Buffer.from([0xc0, 0xaf])), 400, 'invalid_request');
    error(await post('login', { username: 'bad', password: 'x'.repeat(8192) }), 413, 'body_too_large');
    const response = await app.inject({ method: 'GET', url: '/v1/backup' });
    error(response, 404, 'route_missing');
});

test('loopback HTTP listener serves the same contract', async () => {
    now += day;
    const local = createApp(pool, key, clock);
    try {
        const address = await local.listen({ host: '127.0.0.1', port: 0 });
        assert.match(address, /^http:\/\/127\.0\.0\.1:\d+$/);
        const response = await fetch(address + '/v1/auth/login', {
            method: 'POST',
            headers: { 'content-type': 'application/json', 'x-forwarded-for': '203.0.113.1' },
            body: JSON.stringify({ username: 'loopback_check', password: firstPassword }),
        });
        assert.equal(response.status, 401);
        assert.equal(response.headers.get('cache-control'), 'no-store');
        assert.equal((await response.json()).error.code, 'invalid_credentials');
    } finally {
        await local.close();
    }
});

test('database failures redact raw errors and configuration fails closed', async () => {
    now += day;
    const privateText = 'synthetic secret that must stay private';
    const failure = Object.assign(Object.create(pool), {
        connect: async () => { throw new Error(privateText); },
    });
    const broken = createApp(failure, key, clock, event => log.push(event));
    try {
        const response = await broken.inject({ method: 'POST', url: '/v1/auth/login',
            payload: { username: 'safe_error', password: privateText } });
        error(response, 500, 'internal_error');
        assert.deepEqual(log.at(-1), { requestId: response.json().error.requestId, code: 'internal_error' });
        assert.equal(JSON.stringify(log).includes(privateText), false);
    } finally {
        await broken.close();
    }
    assert.throws(() => localDatabase('vehicle_app', {}), /invalid_local_database_configuration/);
    assert.throws(() => localDatabase('vehicle_app', {
        VEHICLE_PG_SOCKET: '/tmp', VEHICLE_PG_DATABASE: 'vehicle_m51_test',
    }), /invalid_local_database_configuration/);
    const child = spawnSync(process.execPath, [fileURLToPath(new URL('../src/main.ts', import.meta.url))], {
        env: {}, encoding: 'utf8',
    });
    assert.equal(child.status, 1);
    assert.equal(child.stdout, '');
    assert.equal(child.stderr, 'm51_local_startup_failed\n');
});
