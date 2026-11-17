import { realpathSync, statSync } from 'node:fs';
import { isAbsolute } from 'node:path';
import pg from 'pg';

type Role = 'vehicle_app' | 'vehicle_migrator' | 'vehicle_invites';

export function localDatabase(role: Role, env = process.env) {
    const host = env.VEHICLE_PG_SOCKET;
    const database = env.VEHICLE_PG_DATABASE;
    if (!host || !isAbsolute(host) || !database || !/^vehicle_m51_[a-z0-9_]{1,32}$/.test(database)) {
        throw new Error('invalid_local_database_configuration');
    }
    const stat = statSync(host);
    if (!stat.isDirectory() || stat.uid !== process.getuid?.() || (stat.mode & 0o077) !== 0 ||
        realpathSync(host) !== host || process.getuid?.() === 0) {
        throw new Error('invalid_local_database_configuration');
    }
    const pool = new pg.Pool({
        host, database, user: role, port: 5432, ssl: false,
        max: 8, connectionTimeoutMillis: 2500, statement_timeout: 5000,
        lock_timeout: 4000, idle_in_transaction_session_timeout: 5000,
    });
    // A socket failure on an idle connection must not emit a raw driver error.
    pool.on('error', () => process.stderr.write('local_database_connection_lost\n'));
    return pool;
}

export async function transaction<T>(pool: pg.Pool, work: (client: pg.PoolClient) => Promise<T>): Promise<T> {
    const client = await pool.connect();
    let discard = false;
    try {
        await client.query('BEGIN');
        const result = await work(client);
        await client.query('COMMIT');
        return result;
    } catch (error) {
        try {
            await client.query('ROLLBACK');
        } catch {
            discard = true;
        }
        throw error;
    } finally {
        client.release(discard);
    }
}
