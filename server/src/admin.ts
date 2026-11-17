import { localDatabase } from './database.ts';
import { migrate } from './migrate.ts';
import { day, randomCode, validCode, verifier } from './security.ts';

async function run() {
    const [action, ...args] = process.argv.slice(2);
    if (args.length || !['migrate', 'invite', 'revoke-invite'].includes(action)) throw new Error('invalid_command');
    const pool = localDatabase(action === 'migrate' ? 'vehicle_migrator' : 'vehicle_invites');
    try {
        if (action === 'migrate') {
            process.stdout.write(`applied_migrations=${await migrate(pool)}\n`);
        } else if (action === 'invite') {
            const code = randomCode();
            await pool.query('INSERT INTO pilot_invites (verifier, expires_at) VALUES ($1, $2)',
                [verifier(code), new Date(Date.now() + 7 * day)]);
            // Intentional one-time output direct to the operator.
            process.stdout.write(code + '\n');
        } else {
            // Read a public verifier from stdin so the original invite is not
            // passed as a process argument or printed.
            process.stdin.setEncoding('utf8');
            let digest = '';
            for await (const chunk of process.stdin) {
                digest += chunk;
                if (digest.length > 66) throw new Error('invalid_invite_verifier');
            }
            const hex = digest.trim();
            if (hex.length !== 64 || !validCode(hex.slice(0, 32)) || !validCode(hex.slice(32))) {
                throw new Error('invalid_invite_verifier');
            }
            const result = await pool.query(`UPDATE pilot_invites SET revoked_at = now()
                WHERE verifier = $1 AND redeemed_at IS NULL AND revoked_at IS NULL`, [Buffer.from(hex, 'hex')]);
            process.stdout.write(`revoked_invites=${result.rowCount}\n`);
        }
    } finally {
        await pool.end();
    }
}

await run().catch(() => {
    process.stderr.write('m51_admin_failed\n');
    process.exitCode = 1;
});
