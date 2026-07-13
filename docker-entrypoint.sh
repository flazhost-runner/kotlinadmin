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

# ── 3. Database ──────────────────────────────────────────────────────────────
# App (AppConfig.resolveDb) sudah pintar: DB_URL eksplisit = override penuh; kalau
# tidak ada, ia MENYUSUN SENDIRI url dari DB_TYPE + DB_HOST/DB_PORT/DB_DATABASE.
#
# Entrypoint versi lama SELALU meng-export DB_URL=jdbc:sqlite:... Karena DB_URL
# eksplisit menang, app dipaksa ke SQLite dan DB_HOST/DB_TYPE dari platform dibuang —
# database MySQL yang sudah dibeli user menganggur, dan (setelah driver sqlite dilepas
# dari jar) app langsung crash: "No database found to handle jdbc:sqlite".
#
# Aturan sekarang: DB_URL HANYA di-set kalau memang tidak ada DB terpasang. Begitu
# platform mengirim DB_TYPE (mysql/postgres), diam saja — biar app yang menyusun.
if [ -z "${DB_URL:-}" ]; then
    case "${DB_TYPE:-}" in
        mysql|mariadb|postgres|postgresql)
            # Jangan set DB_URL/DB_DRIVER. App menyusunnya dari DB_TYPE + DB_HOST/DB_PORT/DB_DATABASE.
            echo "[entrypoint] Database: ${DB_TYPE} @ ${DB_HOST:-?}:${DB_PORT:-?}/${DB_DATABASE:-${DB_NAME:-?}}"
            ;;
        *)
            export DB_URL="jdbc:sqlite:$DATA_DIR/kotlinadmin.db"
            export DB_DRIVER="org.sqlite.JDBC"
            mkdir -p "$DATA_DIR" 2>/dev/null || true
            echo "[entrypoint] Database: sqlite @ $DATA_DIR/kotlinadmin.db (tidak ada DB_TYPE)"
            ;;
    esac
fi

# App membaca DB_USERNAME; platform mengirim DB_USER. Samakan supaya kredensial tidak
# hilang di tengah jalan.
export DB_USERNAME="${DB_USERNAME:-${DB_USER:-}}"
# Idem: sebagian template memakai DB_DATABASE, platform mengirim DB_NAME.
export DB_DATABASE="${DB_DATABASE:-${DB_NAME:-}}"

# ── 3b. TLS ke managed MySQL/PostgreSQL (Alibaba RDS) ────────────────────────
# Server RDS Alibaba HANYA menawarkan pertukaran kunci RSA statis
# (TLS_RSA_WITH_AES_256_GCM_SHA384); ECDHE ditolak. Sementara JDK 17+ memblokir
# seluruh TLS_RSA_* lewat jdk.tls.disabledAlgorithms (tidak punya forward secrecy).
# Hasilnya: JVM gagal handshake ("No appropriate protocol"), padahal OpenSSL
# (Rust/.NET/Node) menerimanya — makanya hanya app JVM yang terdampak.
#
# Menyebut cipher-nya di JDBC URL TIDAK cukup: JSSE menolaknya di level kebijakan.
# Satu-satunya jalan adalah menimpa security property. Yang dilonggarkan HANYA
# TLS_RSA_*; TLSv1/1.1, RC4, DES, 3DES, anon, NULL tetap terkunci.
#
# Alternatifnya adalah mematikan TLS sama sekali (sslMode=DISABLED) — kredensial dan
# data melintas polos ke endpoint publik RDS. Kehilangan forward secrecy jauh lebih
# ringan daripada itu.
if [ -n "${DB_TYPE:-}" ] && [ "${DB_TYPE}" != "sqlite" ]; then
    TLS_PROPS="$DATA_DIR/tls-override.properties"
    mkdir -p "$DATA_DIR" 2>/dev/null || true
    cat > "$TLS_PROPS" <<'EOF'
jdk.tls.disabledAlgorithms=SSLv3, TLSv1, TLSv1.1, DTLSv1.0, RC4, DES, \
    MD5withRSA, DH keySize < 1024, EC keySize < 224, 3DES_EDE_CBC, anon, NULL, \
    rsa_pkcs1_sha1 usage HandshakeSignature, ecdsa_sha1 usage HandshakeSignature, \
    dsa_sha1 usage HandshakeSignature
EOF
    export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Djava.security.properties=$TLS_PROPS"
    echo "[entrypoint] TLS: TLS_RSA_* diaktifkan utk RDS Alibaba (sisanya tetap terkunci)"
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
echo "[entrypoint] Starting KotlinAdmin on 0.0.0.0:${APP_PORT} (DB=${DB_URL:-${DB_TYPE:-sqlite} via DB_TYPE})"
exec java ${JAVA_OPTS:-} -jar /app/app.jar
