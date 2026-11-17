import { localDatabase } from './database.ts';
import { createApp } from './http.ts';
import { throttleKey } from './security.ts';
import { localSnapshotTtl } from './snapshots.ts';

async function main() {
    const portSetting = process.env.VEHICLE_HTTP_PORT ?? '8765';
    const port = Number(portSetting);
    if (!/^\d{4,5}$/.test(portSetting) || port < 1024 || port > 65535) throw new Error('invalid_port');
    const key = throttleKey(process.env.VEHICLE_THROTTLE_KEY);
    const snapshotTtl = localSnapshotTtl(process.env.VEHICLE_LOCAL_SNAPSHOT_TTL_SECONDS);
    const pool = localDatabase('vehicle_app');
    const app = createApp(pool, key, Date.now, event => process.stderr.write(JSON.stringify(event) + '\n'), snapshotTtl);
    app.addHook('onClose', () => pool.end());
    for (const signal of ['SIGINT', 'SIGTERM']) {
        process.once(signal, () => { void app.close(); });
    }
    try {
        const settings = await pool.query('SHOW listen_addresses');
        if (settings.rows[0]?.listen_addresses !== '') throw new Error('database_tcp_enabled');
        await pool.query('SELECT 1 FROM snapshot_slots JOIN accounts ON accounts.id = snapshot_slots.account_id LIMIT 0');
        await app.listen({ host: '127.0.0.1', port });
        process.stdout.write('m51_local_ready\n');
    } catch {
        await app.close();
        throw new Error('local_startup_failed');
    }
}

await main().catch(() => {
    process.stderr.write('m51_local_startup_failed\n');
    process.exitCode = 1;
});
