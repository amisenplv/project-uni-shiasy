#!/usr/bin/env bash
set -euo pipefail
umask 077
cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.."

: "${VEHICLE_PG_TOOLS:?Set VEHICLE_PG_TOOLS to the extracted Debian package root}"
pg_bin="$VEHICLE_PG_TOOLS/usr/lib/postgresql/18/bin"
pg_share="$VEHICLE_PG_TOOLS/usr/share/postgresql/18"
export LD_LIBRARY_PATH="$VEHICLE_PG_TOOLS/usr/lib/x86_64-linux-gnu"
[[ $(id -u) != 0 ]]
version=$("$pg_bin/postgres" --version)
[[ $version =~ ^postgres\ \(PostgreSQL\)\ 18\.([0-9]+)\  && ${BASH_REMATCH[1]} -ge 7 ]]

if [[ $# != 1 || ( $1 != test && $1 != dev ) ]]; then
    printf '%s\n' 'usage: local-postgres.sh test|dev' >&2
    exit 2
fi

fixture=$(mktemp -d /tmp/vehicle-m51.XXXXXX)
export VEHICLE_PG_SOCKET="$fixture/socket" VEHICLE_PG_DATABASE="vehicle_m51_$1"
mkdir -m 700 "$VEHICLE_PG_SOCKET"
started=
cleanup() {
    if [[ -n $started ]]; then
        "$pg_bin/pg_ctl" -D "$fixture/data" -m fast -w stop
    fi
    printf 'Stopped disposable cluster. Fixture retained at %s\n' "$fixture"
}
trap cleanup EXIT
"$pg_bin/initdb" -D "$fixture/data" -L "$pg_share" --username=vehicle_admin \
    --auth-local=peer --auth-host=reject --encoding=UTF8 --no-locale >/dev/null
cat > "$fixture/data/pg_hba.conf" <<'SQL'
local all all peer map=vehicle_local
host all all 0.0.0.0/0 reject
host all all ::/0 reject
SQL
for role in vehicle_admin vehicle_app vehicle_migrator vehicle_invites; do
    printf 'vehicle_local "%s" %s\n' "$(id -un)" "$role"
done > "$fixture/data/pg_ident.conf"
"$pg_bin/pg_ctl" -D "$fixture/data" -l "$fixture/postgres.log" \
    -o "-c listen_addresses='' -k $VEHICLE_PG_SOCKET -c unix_socket_permissions=0700 -c log_statement=none -c log_min_error_statement=panic -c log_parameter_max_length=0 -c log_parameter_max_length_on_error=0" -w start
started=yes
psql_local() {
    "$pg_bin/psql" -X -h "$VEHICLE_PG_SOCKET" -U vehicle_admin -v ON_ERROR_STOP=1 "$@"
}
psql_local -d postgres -v db="$VEHICLE_PG_DATABASE" <<'SQL'
CREATE ROLE vehicle_app LOGIN;
CREATE ROLE vehicle_invites LOGIN;
CREATE ROLE vehicle_migrator LOGIN;
CREATE DATABASE :"db" OWNER vehicle_migrator;
REVOKE ALL ON DATABASE :"db" FROM PUBLIC;
GRANT CONNECT ON DATABASE :"db" TO vehicle_app, vehicle_invites;
SQL
psql_local -d "$VEHICLE_PG_DATABASE" <<'SQL'
REVOKE ALL ON SCHEMA public FROM PUBLIC;
GRANT USAGE ON SCHEMA public TO vehicle_app, vehicle_invites;
SQL
if [[ $1 == test ]]; then
    npm test
else
    npm run migrate
    export VEHICLE_THROTTLE_KEY
    VEHICLE_THROTTLE_KEY=$(node -p "require('node:crypto').randomBytes(32).toString('hex')")
    printf 'For a synthetic invite from another terminal with Node 26 on PATH, run:\n'
    printf 'VEHICLE_PG_SOCKET=%q VEHICLE_PG_DATABASE=%q node src/admin.ts invite\n' "$VEHICLE_PG_SOCKET" "$VEHICLE_PG_DATABASE"
    export VEHICLE_LOCAL_SNAPSHOT_TTL_SECONDS="${VEHICLE_LOCAL_SNAPSHOT_TTL_SECONDS:-3600}"
    npm start
fi
