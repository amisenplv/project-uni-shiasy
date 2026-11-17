import { randomUUID } from 'node:crypto';
import Fastify from 'fastify';
import type { FastifyRequest } from 'fastify';
import type pg from 'pg';
import { accounts } from './accounts.ts';
import { registerSnapshotRoutes } from './snapshot-http.ts';
import { AuthError, invalidCredentials, invalidRequest, invalidSession, limitRequest, validCode, validToken, verifier } from './security.ts';

// All current control objects have only string values. Decode each key before
// checking duplicates so escaped spellings cannot hide a second key.
function parseControl(buffer: Buffer) {
    const source = new TextDecoder('utf-8', { fatal: true, ignoreBOM: true }).decode(buffer);
    const space = /[ \t\r\n]*/y;
    const string = /"(?:[^"\\\u0000-\u001f]|\\(?:["\\/bfnrt]|u[0-9a-fA-F]{4}))*"/y;
    let offset = 0;
    const skip = () => {
        space.lastIndex = offset;
        space.exec(source);
        offset = space.lastIndex;
    };
    const take = (char: string) => {
        skip();
        if (source[offset] !== char) return false;
        offset++;
        return true;
    };
    const token = () => {
        skip();
        string.lastIndex = offset;
        const match = string.exec(source);
        if (!match) throw invalidRequest();
        offset = string.lastIndex;
        const value = JSON.parse(match[0]) as string;
        if (!value.isWellFormed()) throw invalidRequest();
        return value;
    };
    if (!take('{')) throw invalidRequest();
    const result = new Map<string, string>();
    if (!take('}')) {
        do {
            const key = token();
            if (result.has(key) || result.size >= 3 || !take(':')) throw invalidRequest();
            result.set(key, token());
        } while (take(','));
        if (!take('}')) throw invalidRequest();
    }
    skip();
    if (offset !== source.length) throw invalidRequest();
    return result;
}

function fields(request: FastifyRequest, ...expected: string[]) {
    const body = request.body;
    if (!(body instanceof Map) || body.size !== expected.length || expected.some(key => !body.has(key))) {
        throw invalidRequest();
    }
    return expected.map(key => body.get(key) as string);
}

function validateUsername(value: string) {
    if (!/^[a-z0-9._-]{3,32}$/.test(value)) throw invalidRequest();
}

function validatePassword(value: string) {
    const count = [...value].length;
    if (count < 15 || count > 128) throw invalidRequest();
}

function bearer(request: FastifyRequest) {
    const occurrences = request.raw.rawHeaders.filter((header, index) =>
        index % 2 === 0 && header.toLowerCase() === 'authorization').length;
    const value = request.headers.authorization?.match(/^Bearer ([a-f0-9]{64})$/i)?.[1];
    if (occurrences > 1 || !value || !validToken(value)) throw invalidSession();
    return verifier(value);
}

type SafeLog = (data: { requestId: string; code: string }) => void;

export function createApp(pool: pg.Pool, key: Buffer, clock = Date.now, log: SafeLog = () => {}, snapshotTtl?: number) {
    if (key.length !== 32) throw new Error('invalid_throttle_configuration');
    const auth = accounts(pool, key, clock);
    const app = Fastify({
        logger: false, trustProxy: false, genReqId: () => randomUUID(), requestIdHeader: false,
        bodyLimit: 8192, requestTimeout: 15_000, connectionTimeout: 10_000,
    });

    app.removeAllContentTypeParsers();
    app.addContentTypeParser('application/json', { parseAs: 'buffer' }, (_request, body, done) => {
        try {
            done(null, parseControl(body as Buffer));
        } catch {
            done(invalidRequest());
        }
    });
    app.addHook('onRequest', async (request, reply) => {
        reply.header('Cache-Control', 'no-store');
        if (request.url.includes('?')) throw invalidRequest();
    });
    app.setErrorHandler((error, request, reply) => {
        let failure: AuthError;
        if (error instanceof AuthError) {
            failure = error;
        } else if (error.code === 'FST_ERR_CTP_BODY_TOO_LARGE') {
            failure = new AuthError(413, 'body_too_large');
        } else if (error.code === 'FST_ERR_CTP_INVALID_MEDIA_TYPE') {
            failure = new AuthError(415, 'unsupported_media_type');
        } else if (error.statusCode === 400) {
            failure = invalidRequest();
        } else {
            failure = new AuthError(500, 'internal_error');
        }
        if (failure.status >= 500) log({ requestId: request.id, code: failure.code });
        if (failure.retryAfter !== undefined) reply.header('Retry-After', String(failure.retryAfter));
        reply.code(failure.status).send({ error: { code: failure.code, requestId: request.id } });
    });
    app.setNotFoundHandler((request, reply) => {
        reply.code(404).send({ error: { code: 'route_missing', requestId: request.id } });
    });

    app.post('/v1/auth/register', async (request, reply) => {
        const [code, username, password] = fields(request, 'inviteCode', 'username', 'password');
        validateUsername(username);
        validatePassword(password);
        await limitRequest(pool, key, clock, 'register', request.ip);
        if (!validCode(code)) throw invalidCredentials();
        return reply.code(201).send(await auth.register(username, password, code));
    });
    app.post('/v1/auth/login', async request => {
        const [username, password] = fields(request, 'username', 'password');
        validateUsername(username);
        validatePassword(password);
        await limitRequest(pool, key, clock, 'login', request.ip, username);
        return auth.login(username, password);
    });
    app.post('/v1/auth/recover', async request => {
        const [username, code, password] = fields(request, 'username', 'recoveryCode', 'newPassword');
        validateUsername(username);
        validatePassword(password);
        await limitRequest(pool, key, clock, 'recover', request.ip, username);
        if (!validCode(code)) throw invalidCredentials();
        return auth.recover(username, code, password);
    });
    app.post('/v1/auth/recovery-code', async request => {
        const [password] = fields(request, 'password');
        validatePassword(password);
        await limitRequest(pool, key, clock, 'rotate-source', request.ip);
        return auth.rotate(bearer(request), password);
    });
    app.post('/v1/auth/logout', {
        onRequest: async request => {
            // Android HttpURLConnection adds a media type even to an empty POST.
            // Only an explicitly zero-length, unchunked logout can skip parsing.
            if (request.headers['content-length'] === '0' && request.headers['transfer-encoding'] === undefined) {
                delete request.headers['content-type'];
            }
        },
    }, async (request, reply) => {
        if (request.body !== undefined) throw invalidRequest();
        // Revocation is cheap and must remain available even if the account's
        // password-verification budget has been exhausted.
        await limitRequest(pool, key, clock, 'logout-source', request.ip);
        await auth.logout(bearer(request));
        return reply.code(204).send();
    });
    if (snapshotTtl !== undefined) registerSnapshotRoutes(app, pool, key, clock, snapshotTtl, bearer);
    return app;
}
