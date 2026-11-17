import { randomBytes, randomUUID } from 'node:crypto';
import type pg from 'pg';
import { transaction } from './database.ts';
import { AuthError, Passwords, consumeLimits, day, invalidCredentials, invalidSession, randomCode, verifier } from './security.ts';

async function issueSession(client: pg.PoolClient, accountId: string, now: number) {
    const sessionToken = randomBytes(32).toString('hex');
    const expires = new Date(now + 30 * day);
    await client.query('INSERT INTO sessions (verifier, account_id, expires_at) VALUES ($1, $2, $3)',
        [verifier(sessionToken), accountId, expires]);
    return { accountId, sessionToken, sessionExpiresAt: expires.toISOString() };
}

async function issueRecovery(client: pg.PoolClient, accountId: string, now: number) {
    const recoveryCode = randomCode();
    const expires = new Date(now + 365 * day);
    await client.query(`INSERT INTO recovery_codes (account_id, verifier, expires_at) VALUES ($1, $2, $3)
        ON CONFLICT (account_id) DO UPDATE SET verifier = EXCLUDED.verifier, expires_at = EXCLUDED.expires_at`,
    [accountId, verifier(recoveryCode), expires]);
    return { recoveryCode, recoveryExpiresAt: expires.toISOString() };
}

export async function sessionAccount(client: pg.Pool | pg.PoolClient, digest: Buffer, now: number) {
    const { rows: [session] } = await client.query(`SELECT account_id FROM sessions
        WHERE verifier = $1 AND revoked_at IS NULL AND expires_at > $2`, [digest, new Date(now)]);
    if (!session) throw invalidSession();
    return session.account_id as string;
}

export async function withAuthorizedAccount<T>(
    pool: pg.Pool, digest: Buffer, clock: () => number,
    work: (client: pg.PoolClient, accountId: string, now: number) => Promise<T>,
) {
    return transaction(pool, async client => {
        const accountId = await sessionAccount(client, digest, clock());
        await client.query('SELECT id FROM accounts WHERE id = $1 FOR UPDATE', [accountId]);
        const now = clock();
        if (await sessionAccount(client, digest, now) !== accountId) throw invalidSession();
        return work(client, accountId, now);
    });
}

export function accounts(pool: pg.Pool, key: Buffer, clock: () => number) {
    const passwords = new Passwords();

    return {
        async register(username: string, password: string, code: string) {
            const digest = verifier(code);
            const credentials = await passwords.create(password);
            return transaction(pool, async client => {
                const { rows } = await client.query(`SELECT expires_at, revoked_at, redeemed_at FROM pilot_invites
                    WHERE verifier = $1 FOR UPDATE`, [digest]);
                const now = clock();
                const invite = rows[0];
                if (!invite || invite.revoked_at || invite.redeemed_at || invite.expires_at.getTime() <= now) {
                    throw invalidCredentials();
                }
                const id = randomUUID();
                const inserted = await client.query(`INSERT INTO accounts (id, username, password_salt, password_tag)
                    VALUES ($1, $2, $3, $4) ON CONFLICT (username) DO NOTHING`,
                [id, username, credentials.salt, credentials.tag]);
                if (inserted.rowCount !== 1) throw new AuthError(409, 'registration_conflict');
                await client.query('UPDATE pilot_invites SET account_id = $1, redeemed_at = $2 WHERE verifier = $3',
                    [id, new Date(now), digest]);
                return { ...await issueSession(client, id, now), ...await issueRecovery(client, id, now) };
            });
        },

        async login(username: string, password: string) {
            const { rows: [account] } = await pool.query(`SELECT id, password_salt, password_tag, credential_version
                FROM accounts WHERE username = $1`, [username]);
            if (!await passwords.verify(password, account)) throw invalidCredentials();
            return transaction(pool, async client => {
                const { rows: [current] } = await client.query(
                    'SELECT credential_version FROM accounts WHERE id = $1 FOR UPDATE', [account.id]);
                if (!current || current.credential_version !== account.credential_version) throw invalidCredentials();
                return issueSession(client, account.id, clock());
            });
        },

        async recover(username: string, code: string, password: string) {
            const credentials = await passwords.create(password);
            return transaction(pool, async client => {
                const { rows: [account] } = await client.query(
                    'SELECT id FROM accounts WHERE username = $1 FOR UPDATE', [username]);
                const now = clock();
                if (!account) throw invalidCredentials();
                const { rows } = await client.query(`SELECT 1 FROM recovery_codes
                    WHERE account_id = $1 AND verifier = $2 AND expires_at > $3`,
                [account.id, verifier(code), new Date(now)]);
                if (!rows.length) throw invalidCredentials();
                await client.query(`UPDATE accounts SET password_salt = $1, password_tag = $2,
                    credential_version = credential_version + 1 WHERE id = $3`,
                [credentials.salt, credentials.tag, account.id]);
                await client.query('UPDATE sessions SET revoked_at = $1 WHERE account_id = $2 AND revoked_at IS NULL',
                    [new Date(now), account.id]);
                return { ...await issueSession(client, account.id, now), ...await issueRecovery(client, account.id, now) };
            });
        },

        async rotate(digest: Buffer, password: string) {
            const accountId = await sessionAccount(pool, digest, clock());
            await consumeLimits(pool, key, clock, [['rotate-account', accountId, 5, 900_000]]);
            const { rows: [account] } = await pool.query(`SELECT password_salt, password_tag, credential_version
                FROM accounts WHERE id = $1`, [accountId]);
            if (!await passwords.verify(password, account)) throw invalidCredentials();
            return withAuthorizedAccount(pool, digest, clock, async (client, id, now) => {
                const { rows: [current] } = await client.query(
                    'SELECT credential_version FROM accounts WHERE id = $1', [id]);
                if (id !== accountId || current.credential_version !== account.credential_version) throw invalidSession();
                return issueRecovery(client, id, now);
            });
        },

        async logout(digest: Buffer) {
            await withAuthorizedAccount(pool, digest, clock, async (client, id, now) => {
                await client.query('UPDATE sessions SET revoked_at = $1 WHERE account_id = $2 AND verifier = $3',
                    [new Date(now), id, digest]);
            });
        },
    };
}
