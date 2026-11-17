import type { FastifyInstance, FastifyRequest } from 'fastify';
import type pg from 'pg';
import { sessionAccount } from './accounts.ts';
import { AuthError, consumeLimits, invalidRequest } from './security.ts';
import { snapshotFormat, snapshotMaxBytes, uuidPattern } from './snapshot-codec.ts';
import { snapshots } from './snapshots.ts';

function singleHeader(request: FastifyRequest, header: string) {
    const count = request.raw.rawHeaders.filter((key, index) => index % 2 === 0 && key.toLowerCase() === header).length;
    const value = request.headers[header];
    if (count > 1 || Array.isArray(value)) throw invalidRequest();
    return value;
}

function revisionFrom(request: FastifyRequest) {
    const value = singleHeader(request, 'if-match');
    if (value === undefined) throw new AuthError(428, 'precondition_required');
    if (value.length !== 38 || !value.startsWith('"') || !value.endsWith('"') || !uuidPattern.test(value.slice(1, -1))) {
        throw invalidRequest();
    }
    return value.slice(1, -1);
}

const etag = (revision: string) => '"' + revision + '"';

export function registerSnapshotRoutes(
    app: FastifyInstance, pool: pg.Pool, key: Buffer, clock: () => number, ttl: number,
    bearer: (request: FastifyRequest) => Buffer,
) {
    const storage = snapshots(pool, clock, ttl);
    // Fastify is a single loopback process in M5.2. PostgreSQL serializes writes
    // and rate limits independently of the process-local ingress budget.
    const receiving = new Set<string>();
    const uploads = new Map<FastifyRequest, {
        owner: string; session: Buffer; revision: string; attempt: string; format: string; handling: boolean;
    }>();
    function release(request: FastifyRequest) {
        const context = uploads.get(request);
        if (!context) return;
        if (!context.handling) receiving.delete(context.owner);
        uploads.delete(request);
    }
    function releaseBeforeHandling(request: FastifyRequest) {
        if (!uploads.get(request)?.handling) release(request);
    }

    app.register(async routes => {
        routes.removeContentTypeParser('application/json');
        routes.addContentTypeParser('application/json', { parseAs: 'buffer' }, (_request, body, done) => done(null, body));
        routes.addHook('onError', async request => releaseBeforeHandling(request));
        routes.addHook('onTimeout', async request => releaseBeforeHandling(request));
        routes.addHook('onRequestAbort', async request => releaseBeforeHandling(request));
        routes.addHook('onResponse', async request => release(request));

        routes.get('/v1/backup/metadata', async (request, reply) => {
            const result = await storage.metadata(bearer(request));
            return reply.header('ETag', etag(result.slotRevision)).send(result);
        });

        routes.get('/v1/backup', {
            onRequest: async request => { await sessionAccount(pool, bearer(request), clock()); },
        }, async (request, reply) => {
            const result = await storage.download(bearer(request), revisionFrom(request));
            return reply
                .header('ETag', etag(result.revision))
                .header('Snapshot-Format', snapshotFormat)
                .header('Content-Length', result.payload.length)
                .type('application/json; charset=utf-8')
                .send(result.payload);
        });

        routes.put('/v1/backup', {
            bodyLimit: snapshotMaxBytes,
            onRequest: async request => {
                const session = bearer(request);
                const owner = await sessionAccount(pool, session, clock());
                const revision = revisionFrom(request);
                const attempt = singleHeader(request, 'idempotency-key');
                const format = singleHeader(request, 'snapshot-format');
                if (!attempt || !uuidPattern.test(attempt) || !format || !/^[a-z0-9-]{1,64}$/.test(format)) {
                    throw invalidRequest();
                }

                const encoding = singleHeader(request, 'content-encoding');
                const type = singleHeader(request, 'content-type');
                if (!type || !/^application\/json(?:\s*;\s*charset\s*=\s*(?:utf-8|"utf-8"))?\s*$/i.test(type) ||
                    (encoding !== undefined && encoding.toLowerCase() !== 'identity')) {
                    throw new AuthError(415, 'unsupported_media_type');
                }
                const length = singleHeader(request, 'content-length');
                if (length !== undefined && Number(length) > snapshotMaxBytes) throw new AuthError(413, 'body_too_large');

                await consumeLimits(pool, key, clock, [['snapshot-put', owner, 20, 3_600_000]]);
                if (request.raw.aborted || request.raw.socket.destroyed) return;
                if (receiving.has(owner) || uploads.size >= 4) throw new AuthError(429, 'rate_limited', 1);
                receiving.add(owner);
                uploads.set(request, { owner, session, revision, attempt, format, handling: false });
            },
        }, async (request, reply) => {
            const context = uploads.get(request);
            if (!context) throw invalidRequest();
            context.handling = true;
            receiving.delete(context.owner);
            try {
                if (!Buffer.isBuffer(request.body)) throw invalidRequest();
                const result = await storage.upload(context.session, context.revision, context.attempt, context.format, request.body);
                return reply.header('ETag', etag(result.slotRevision)).send(result);
            } finally {
                release(request);
            }
        });
    });
}
