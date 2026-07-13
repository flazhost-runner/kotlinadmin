# syntax=docker/dockerfile:1
# ── KotlinAdmin starter kit · FlazHost PaaS (CapRover) ───────────────────────
# Multi-stage build:
#   1) eclipse-temurin:21-jdk  — gradle wrapper (8.7) builds the Ktor fat jar
#   2) eclipse-temurin:21-jre-alpine — runtime + bundled redis-server
#
# Zero-config boot: SQLite DB at /app/data/kotlinadmin.db (sqlite-jdbc is the
# only JDBC driver bundled by build.gradle.kts; Flyway migrations are written
# in SQLite dialect). Redis is REQUIRED at boot (RedisManager connects eagerly
# for the JWT blacklist / session store), so a local redis-server is bundled
# and started by the entrypoint whenever REDIS_HOST points at localhost.

# 1) Build stage ──────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src

# Gradle files first → the (expensive) wrapper + dependency download layers
# are cached across source-only changes.
COPY gradlew settings.gradle.kts build.gradle.kts detekt.yml ./
COPY gradle/ gradle/
RUN chmod +x gradlew \
 && ./gradlew --no-daemon dependencies --quiet > /dev/null 2>&1 || true

# App source → fat jar (io.ktor.plugin provides buildFatJar; skips tests).
COPY src/ src/
RUN ./gradlew --no-daemon buildFatJar \
 && ls -la build/libs/

# 2) Runtime stage ────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# redis: bundled local store for zero-config deploys (JWT blacklist/sessions).
RUN apk add --no-cache redis

COPY --from=build /src/build/libs/*-all.jar /app/app.jar
COPY docker-entrypoint.sh /app/docker-entrypoint.sh

# Writable location for the SQLite DB file + persisted runtime secrets.
RUN chmod +x /app/docker-entrypoint.sh && mkdir -p /app/data

# ── Zero-config defaults (all overridable via env) ──────────────────────────
# APP_PORT feeds ktor.deployment.port (application.conf: ${?APP_PORT});
# the entrypoint also maps CapRover's injected $PORT → APP_PORT.
# DB_URL / DB_DRIVER are what the app actually reads (see application.conf).
# JANGAN set DB_URL/DB_DRIVER di sini. ENV terisi sejak image dibangun, sehingga
# pemeriksaan `[ -z "${DB_URL:-}" ]` di entrypoint SELALU salah → blok pemilihan
# database dilewati → DB_TYPE/DB_HOST dari platform tidak pernah dilihat, dan app
# diam-diam memakai SQLite lokal per container (login rusak begitu replika > 1).
# Default SQLite kini ditentukan entrypoint, HANYA saat DB_TYPE kosong.
ENV APP_PORT=80 \
    APP_MODE=full \
    APP_NAME=KotlinAdmin \
    REDIS_URL=redis://127.0.0.1:6379 \
    JAVA_OPTS="-XX:MaxRAMPercentage=75.0"

EXPOSE 80

ENTRYPOINT ["/app/docker-entrypoint.sh"]
