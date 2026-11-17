import { readFile, readdir } from 'node:fs/promises';
import { createHash } from 'node:crypto';
import type pg from 'pg';
import { transaction } from './database.ts';

const migrations = new URL('../migrations/', import.meta.url);

export async function migrate(pool: pg.Pool) {
    const files = (await readdir(migrations)).filter(file => /^\d{3}_[a-z_]+\.sql$/.test(file)).sort();
    if (!files.length || new Set(files.map(file => file.slice(0, 3))).size !== files.length) {
        throw new Error('invalid_migration_files');
    }
    return transaction(pool, async client => {
        await client.query('SELECT pg_advisory_xact_lock(51001)');
        await client.query(`CREATE TABLE IF NOT EXISTS schema_migrations (
            filename text PRIMARY KEY, checksum text NOT NULL, applied_at timestamptz NOT NULL DEFAULT now()
        )`);
        const { rows } = await client.query('SELECT filename, checksum FROM schema_migrations ORDER BY filename');
        if (rows.some((row, index) => row.filename !== files[index])) throw new Error('invalid_migration_history');
        for (const [index, file] of files.entries()) {
            const sql = await readFile(new URL(file, migrations), 'utf8');
            const checksum = createHash('sha256').update(sql).digest('hex');
            if (rows[index]) {
                if (rows[index].checksum !== checksum) throw new Error('migration_checksum_mismatch');
                continue;
            }
            await client.query(sql);
            await client.query('INSERT INTO schema_migrations (filename, checksum) VALUES ($1, $2)', [file, checksum]);
        }
        return files.length - rows.length;
    });
}
