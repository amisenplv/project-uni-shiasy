CREATE TABLE accounts (
    id uuid PRIMARY KEY,
    username text COLLATE "C" NOT NULL UNIQUE CHECK (username ~ '^[a-z0-9._-]{3,32}$'),
    password_salt bytea NOT NULL CHECK (octet_length(password_salt) = 16),
    password_tag bytea NOT NULL CHECK (octet_length(password_tag) = 32),
    credential_version bigint NOT NULL DEFAULT 1 CHECK (credential_version > 0)
);

CREATE TABLE pilot_invites (
    verifier bytea PRIMARY KEY CHECK (octet_length(verifier) = 32),
    expires_at timestamptz NOT NULL,
    revoked_at timestamptz,
    redeemed_at timestamptz,
    account_id uuid UNIQUE REFERENCES accounts(id),
    CHECK ((account_id IS NULL) = (redeemed_at IS NULL)),
    CHECK (revoked_at IS NULL OR redeemed_at IS NULL)
);

CREATE TABLE sessions (
    verifier bytea PRIMARY KEY CHECK (octet_length(verifier) = 32),
    account_id uuid NOT NULL REFERENCES accounts(id),
    expires_at timestamptz NOT NULL,
    revoked_at timestamptz
);
CREATE INDEX sessions_account ON sessions(account_id);

CREATE TABLE recovery_codes (
    account_id uuid PRIMARY KEY REFERENCES accounts(id),
    verifier bytea NOT NULL UNIQUE CHECK (octet_length(verifier) = 32),
    expires_at timestamptz NOT NULL
);

CREATE TABLE auth_limits (
    bucket bytea PRIMARY KEY CHECK (octet_length(bucket) = 32),
    expires_at timestamptz NOT NULL,
    attempts integer NOT NULL CHECK (attempts > 0)
);
CREATE INDEX auth_limits_expiry ON auth_limits(expires_at);

GRANT SELECT, INSERT ON accounts TO vehicle_app;
GRANT UPDATE (password_salt, password_tag, credential_version) ON accounts TO vehicle_app;
GRANT SELECT ON pilot_invites TO vehicle_app, vehicle_invites;
GRANT UPDATE (redeemed_at, account_id) ON pilot_invites TO vehicle_app;
GRANT INSERT (verifier, expires_at), UPDATE (revoked_at) ON pilot_invites TO vehicle_invites;
GRANT SELECT, INSERT ON sessions TO vehicle_app;
GRANT UPDATE (revoked_at) ON sessions TO vehicle_app;
GRANT SELECT, INSERT, UPDATE (verifier, expires_at) ON recovery_codes TO vehicle_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON auth_limits TO vehicle_app;
