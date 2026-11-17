CREATE TABLE snapshot_slots (
    account_id uuid PRIMARY KEY REFERENCES accounts(id),
    revision uuid NOT NULL UNIQUE,
    attempt_id uuid,
    acknowledged_at timestamptz,
    expires_at timestamptz,
    digest bytea,
    payload bytea,
    CHECK (
        (payload IS NULL AND attempt_id IS NULL AND acknowledged_at IS NULL AND expires_at IS NULL AND digest IS NULL)
        OR
        (payload IS NOT NULL AND attempt_id IS NOT NULL AND acknowledged_at IS NOT NULL AND expires_at IS NOT NULL
            AND expires_at > acknowledged_at AND digest IS NOT NULL AND octet_length(digest) = 32
            AND octet_length(payload) BETWEEN 1 AND 4194304)
    )
);

CREATE TABLE snapshot_receipts (
    account_id uuid NOT NULL REFERENCES snapshot_slots(account_id),
    attempt_id uuid NOT NULL,
    base_revision uuid NOT NULL,
    result_revision uuid NOT NULL,
    payload_format text NOT NULL CHECK (payload_format = 'snapshot-v1'),
    digest bytea NOT NULL CHECK (octet_length(digest) = 32),
    byte_length integer NOT NULL CHECK (byte_length BETWEEN 1 AND 4194304),
    acknowledged_at timestamptz NOT NULL,
    expires_at timestamptz NOT NULL CHECK (expires_at > acknowledged_at),
    PRIMARY KEY (account_id, attempt_id)
);
CREATE INDEX snapshot_receipts_expiry ON snapshot_receipts(account_id, expires_at);

GRANT SELECT, INSERT ON snapshot_slots TO vehicle_app;
GRANT UPDATE (revision, attempt_id, acknowledged_at, expires_at, digest, payload) ON snapshot_slots TO vehicle_app;
GRANT SELECT, INSERT, DELETE ON snapshot_receipts TO vehicle_app;
