import { argon2, createHash, createHmac, randomBytes, timingSafeEqual } from 'node:crypto';
import type pg from 'pg';
import { transaction } from './database.ts';

export class AuthError extends Error {
    status: number;
    code: string;
    retryAfter?: number;

    constructor(status: number, code: string, retryAfter?: number) {
        super(code);
        this.status = status;
        this.code = code;
        this.retryAfter = retryAfter;
    }
}

export const invalidCredentials = () => new AuthError(401, 'invalid_credentials');
export const invalidSession = () => new AuthError(401, 'invalid_session');
export const invalidRequest = () => new AuthError(400, 'invalid_request');

export const day = 86_400_000;
export const verifier = (secret: string) => createHash('sha256').update(secret, 'ascii').digest();
export const randomCode = () => randomBytes(16).toString('hex');
export const validCode = (value: string) => /^[a-f0-9]{32}$/.test(value);
export const validToken = (value: string) => /^[a-f0-9]{64}$/.test(value);

export function throttleKey(value: string | undefined) {
    if (!value || !/^[a-f0-9]{64}$/.test(value)) throw new Error('invalid_throttle_configuration');
    return Buffer.from(value, 'hex');
}

export class Passwords {
    private active = 0;
    readonly dummySalt = randomBytes(16);
    readonly dummyTag = randomBytes(32);

    async derive(password: string, salt: Buffer) {
        if (this.active >= 2) throw new AuthError(503, 'temporarily_unavailable');
        this.active++;
        try {
            return await new Promise<Buffer>((resolve, reject) => {
                argon2('argon2id', {
                    message: password, nonce: salt, parallelism: 4, tagLength: 32,
                    memory: 65_536, passes: 3,
                }, (error, tag) => error ? reject(error) : resolve(tag));
            });
        } finally {
            this.active--;
        }
    }

    async create(password: string) {
        const salt = randomBytes(16);
        return { salt, tag: await this.derive(password, salt) };
    }

    async verify(password: string, row?: { password_salt: Buffer; password_tag: Buffer }) {
        const tag = await this.derive(password, row?.password_salt ?? this.dummySalt);
        return timingSafeEqual(tag, row?.password_tag ?? this.dummyTag) && row !== undefined;
    }
}

type Limit = [scope: string, value: string, max: number, window: number];

export async function consumeLimits(pool: pg.Pool, key: Buffer, clock: () => number, limits: Limit[]) {
    const retry = await transaction(pool, async client => {
        // One short database lock makes the decision across multiple buckets atomic.
        await client.query('SELECT pg_advisory_xact_lock(51002)');
        const now = clock();
        const timestamp = new Date(now);
        await client.query('DELETE FROM auth_limits WHERE expires_at <= $1', [timestamp]);
        const buckets = limits.map(([scope, value, max, window]) => ({
            digest: createHmac('sha256', key).update(scope).update('\0').update(value).digest(),
            max, window,
        }));
        let wait = 0;
        for (const bucket of buckets) {
            const { rows: [row] } = await client.query(
                'SELECT attempts, expires_at FROM auth_limits WHERE bucket = $1', [bucket.digest]);
            if (row && row.attempts >= bucket.max) {
                wait = Math.max(wait, Math.ceil((row.expires_at.getTime() - now) / 1000));
            }
        }
        if (wait) return wait;
        for (const bucket of buckets) {
            await client.query(`INSERT INTO auth_limits (bucket, expires_at, attempts) VALUES ($1, $2, 1)
                ON CONFLICT (bucket) DO UPDATE SET attempts = auth_limits.attempts + 1`,
            [bucket.digest, new Date(now + bucket.window)]);
        }
        return 0;
    });
    if (retry) throw new AuthError(429, 'rate_limited', retry);
}

export async function limitRequest(pool: pg.Pool, key: Buffer, clock: () => number, route: string, source: string, username?: string) {
    const limits: Limit[] = [['global', '', 60, 60_000], [route, source, 10, 60_000]];
    if (username !== undefined) limits.push(['username', username, 5, 900_000]);
    await consumeLimits(pool, key, clock, limits);
}
