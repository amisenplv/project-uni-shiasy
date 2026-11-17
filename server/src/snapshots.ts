import { createHash, randomUUID } from 'node:crypto';
import type pg from 'pg';
import { withAuthorizedAccount } from './accounts.ts';
import { AuthError, day } from './security.ts';
import { snapshotFormat, snapshotProblem } from './snapshot-codec.ts';

type Slot = {
    revision: string;
    attempt_id: string | null;
    acknowledged_at: Date | null;
    expires_at: Date | null;
    digest: Buffer | null;
    byte_length: number | null;
};

const metadataColumns = 'revision, attempt_id, acknowledged_at, expires_at, digest, octet_length(payload) AS byte_length';
const receiptTtl = 7 * day;
const receiptLimit = 256;

export function localSnapshotTtl(value: string | undefined) {
    // Required explicit synthetic-data lifetime. There is no production default.
    if (!value || !/^[1-9][0-9]{0,5}$/.test(value) || Number(value) > receiptTtl / 1000) {
        throw new Error('invalid_local_snapshot_ttl');
    }
    return Number(value) * 1000;
}

function metadata(slot: Slot) {
    return {
        slotRevision: slot.revision,
        snapshot: slot.attempt_id === null ? null : {
            attemptId: slot.attempt_id,
            acknowledgedAt: slot.acknowledged_at!.toISOString(),
            expiresAt: slot.expires_at!.toISOString(),
            byteLength: slot.byte_length,
            sha256: slot.digest!.toString('hex'),
            payloadFormat: snapshotFormat,
        },
    };
}

async function currentSlot(client: pg.PoolClient, owner: string, now: number): Promise<Slot> {
    const result = await client.query<Slot>(
        `SELECT ${metadataColumns} FROM snapshot_slots WHERE account_id = $1`, [owner]);
    const current = result.rows[0];
    if (current && (current.expires_at === null || current.expires_at.getTime() > now)) return current;

    const revision = randomUUID();
    if (current) {
        await client.query(`UPDATE snapshot_slots SET revision = $2, attempt_id = NULL, acknowledged_at = NULL,
            expires_at = NULL, digest = NULL, payload = NULL WHERE account_id = $1`, [owner, revision]);
    } else {
        await client.query('INSERT INTO snapshot_slots (account_id, revision) VALUES ($1, $2)', [owner, revision]);
    }
    return { revision, attempt_id: null, acknowledged_at: null, expires_at: null, digest: null, byte_length: null };
}

const conflict = () => new AuthError(412, 'revision_conflict');

export function snapshots(pool: pg.Pool, clock: () => number, ttl: number) {
    if (!Number.isSafeInteger(ttl) || ttl < 1000 || ttl > receiptTtl) throw new Error('invalid_local_snapshot_ttl');
    return {
        async metadata(session: Buffer) {
            return withAuthorizedAccount(pool, session, clock, async (client, owner, now) =>
                metadata(await currentSlot(client, owner, now)));
        },

        async download(session: Buffer, revision: string) {
            const result = await withAuthorizedAccount(pool, session, clock, async (client, owner, now) => {
                const slot = await currentSlot(client, owner, now);
                if (slot.attempt_id === null) return new AuthError(404, 'snapshot_missing');
                if (revision !== slot.revision) return conflict();
                const { rows: [row] } = await client.query<{ payload: Buffer }>(
                    'SELECT payload FROM snapshot_slots WHERE account_id = $1 AND revision = $2', [owner, revision]);
                return { revision, payload: row.payload };
            });
            if (result instanceof AuthError) throw result;
            return result;
        },

        async upload(session: Buffer, base: string, attempt: string, format: string, payload: Buffer) {
            const digest = createHash('sha256').update(payload).digest();
            const problem = format === snapshotFormat ? snapshotProblem(payload) : 'snapshot_incompatible';
            const result = await withAuthorizedAccount(pool, session, clock, async (client, owner, now) => {
                const slot = await currentSlot(client, owner, now);
                const timestamp = new Date(now);
                await client.query('DELETE FROM snapshot_receipts WHERE account_id = $1 AND expires_at <= $2',
                    [owner, timestamp]);
                const { rows: [receipt] } = await client.query(`SELECT base_revision, result_revision, payload_format,
                    digest, byte_length FROM snapshot_receipts WHERE account_id = $1 AND attempt_id = $2`, [owner, attempt]);

                if (receipt) {
                    const mismatch = () => new AuthError(409, 'attempt_mismatch');
                    if (receipt.base_revision !== base || receipt.payload_format !== format ||
                        receipt.byte_length !== payload.length || !receipt.digest.equals(digest)) return mismatch();
                    if (receipt.result_revision !== slot.revision) return new AuthError(409, 'attempt_superseded');
                    const exact = await client.query(`SELECT 1 FROM snapshot_slots
                        WHERE account_id = $1 AND revision = $2 AND payload = $3`, [owner, slot.revision, payload]);
                    return exact.rowCount === 1 ? metadata(slot) : mismatch();
                }

                if (base !== slot.revision) return conflict();
                if (problem) return new AuthError(422, problem);
                const { rows: [budget] } = await client.query(`SELECT count(*)::integer AS count, min(expires_at) AS next
                    FROM snapshot_receipts WHERE account_id = $1`, [owner]);
                if (budget.count >= receiptLimit) {
                    return new AuthError(429, 'rate_limited', Math.ceil((budget.next.getTime() - now) / 1000));
                }

                const revision = randomUUID();
                const expires = new Date(now + ttl);
                const { rows: [accepted] } = await client.query<Slot>(`UPDATE snapshot_slots SET revision = $3, attempt_id = $4,
                    acknowledged_at = $5, expires_at = $6, digest = $7, payload = $8 WHERE account_id = $1 AND revision = $2
                    RETURNING ${metadataColumns}`, [owner, base, revision, attempt, timestamp, expires, digest, payload]);
                if (!accepted) throw new Error('snapshot_revision_update_failed');
                await client.query(`INSERT INTO snapshot_receipts
                    (account_id, attempt_id, base_revision, result_revision, payload_format, digest, byte_length, acknowledged_at, expires_at)
                    VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)`,
                [owner, attempt, base, revision, format, digest, payload.length, timestamp, new Date(now + receiptTtl)]);
                return metadata(accepted);
            });
            // Commit independent expiry and pruning even when an operation fails a precondition.
            if (result instanceof AuthError) throw result;
            return result;
        },
    };
}
