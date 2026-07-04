#!/bin/sh
# KotlinAdmin container entrypoint (FlazHost / CapRover).
#   1) Map CapRover's $PORT → APP_PORT (Ktor reads ktor.deployment.port = ${?APP_PORT}).
#   2) Ensure mandatory secrets exist (generate + persist if not provided).
#   3) Resolve the database URL (default: SQLite under /app/data — the only
#      JDBC driver bundled is sqlite-jdbc; Flyway migrations are SQLite dialect).
#   4) Start the bundled redis-server when Redis points at localhost
#      (RedisManager connects EAGERLY at boot — the app cannot start without it).
#   5) Exec the fat jar. Flyway migrate+seed (admin@admin.com / 12345678) runs
#      inside the app at startup (DatabaseConfig.setup → flyway.migrate()).
set -eu

DATA_DIR=/app/data
mkdir -p "$DATA_DIR"

# ── 1. Port: CapRover injects $PORT (default 80). Ktor listens on APP_PORT. ──
: "${PORT:=80}"
export APP_PORT="${APP_PORT:-$PORT}"

# ── 2. Secrets (SESSION_SECRET / JWT_SECRET) ─────────────────────────────────
# Honour values supplied via the environment. Otherwise generate strong random
# secrets once and persist them so sessions/JWTs survive container restarts
# (persists across restarts when /app/data is a mounted volume).
SECRETS_FILE="$DATA_DIR/.runtime-secrets"
[ -f "$SECRETS_FILE" ] && . "$SECRETS_FILE"

gen_secret() {
    head -c 32 /dev/urandom | od -An -tx1 | tr -d ' \n'
}

if [ -z "${SESSION_SECRET:-}" ]; then
    SESSION_SECRET="$(gen_secret)"
    echo "SESSION_SECRET=$SESSION_SECRET" >> "$SECRETS_FILE"
    echo "[entrypoint] Generated SESSION_SECRET (persisted in $SECRETS_FILE)"
fi
if [ -z "${JWT_SECRET:-}" ]; then
    JWT_SECRET="$(gen_secret)"
    echo "JWT_SECRET=$JWT_SECRET" >> "$SECRETS_FILE"
    echo "[entrypoint] Generated JWT_SECRET (persisted in $SECRETS_FILE)"
fi
export SESSION_SECRET JWT_SECRET

# ── 3. Database (app reads DB_URL / DB_DRIVER / DB_USERNAME / DB_PASSWORD) ───
# Only the SQLite driver ships in the fat jar, and the Flyway migrations use
# SQLite syntax — an external Postgres/MySQL cannot work without adding a
# driver dependency. Warn loudly if someone points DB_URL elsewhere.
export DB_URL="${DB_URL:-jdbc:sqlite:$DATA_DIR/kotlinadmin.db}"
export DB_DRIVER="${DB_DRIVER:-org.sqlite.JDBC}"
case "$DB_URL" in
    jdbc:sqlite:*)
        db_file="${DB_URL#jdbc:sqlite:}"
        case "$db_file" in
            /*) mkdir -p "$(dirname "$db_file")" 2>/dev/null || true ;;
        esac
        ;;
    *)
        echo "[entrypoint] WARN: DB_URL=$DB_URL is not SQLite — this image only bundles" >&2
        echo "[entrypoint] WARN: the sqlite-jdbc driver and SQLite-dialect migrations;" >&2
        echo "[entrypoint] WARN: boot will likely fail unless a matching driver was added." >&2
        ;;
esac
if [ -n "${DB_HOST:-}" ]; then
    echo "[entrypoint] WARN: DB_HOST is set but ignored — the app resolves the database" >&2
    echo "[entrypoint] WARN: via DB_URL only (currently: $DB_URL)." >&2
fi

# ── 4. Redis (RedisManager uses REDIS_HOST/REDIS_PORT, not REDIS_URL) ────────
# Derive host/port from REDIS_URL when explicit host/port are not given, so a
# managed Redis works by setting either REDIS_URL or REDIS_HOST/REDIS_PORT.
if [ -z "${REDIS_HOST:-}" ] && [ -n "${REDIS_URL:-}" ]; then
    _hp="${REDIS_URL#rediss://}"; _hp="${_hp#redis://}"
    _hp="${_hp##*@}"; _hp="${_hp%%/*}"
    REDIS_HOST="${_hp%%:*}"
    case "$_hp" in
        *:*) REDIS_PORT="${_hp##*:}" ;;
    esac
fi
export REDIS_HOST="${REDIS_HOST:-127.0.0.1}"
export REDIS_PORT="${REDIS_PORT:-6379}"

case "$REDIS_HOST" in
    127.0.0.1|localhost|::1)
        echo "[entrypoint] Starting bundled redis-server on 127.0.0.1:$REDIS_PORT"
        redis-server --daemonize yes --port "$REDIS_PORT" --bind 127.0.0.1 \
            --save "" --appendonly no >/dev/null 2>&1 || \
            echo "[entrypoint] WARN: could not start bundled redis-server" >&2
        ;;
    *)
        echo "[entrypoint] Using external Redis at $REDIS_HOST:$REDIS_PORT"
        ;;
esac

# ── 5. Start the server (PID 1 → clean SIGTERM/graceful shutdown) ────────────
# Flyway migrations + admin seed run inside the app during module init; a DB
# failure there is fatal by design (schema is required for sessions/auth).
echo "[entrypoint] Starting KotlinAdmin on 0.0.0.0:${APP_PORT} (DB_URL=$DB_URL)"
exec java ${JAVA_OPTS:-} -jar /app/app.jar
