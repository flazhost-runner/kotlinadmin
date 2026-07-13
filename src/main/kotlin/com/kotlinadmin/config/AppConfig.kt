package com.kotlinadmin.config

import io.ktor.server.config.ApplicationConfig

data class AppConfig(
    val appName: String,
    val appMode: String,
    val port: Int,
    val jwtSecret: String,
    val jwtExpiresIn: String,
    val sessionSecret: String,
    val sessionTtlHours: Long,
    val sessionDriver: String,
    val bcryptRounds: Int,
    val otpExpiryMinutes: Long,
    val defaultPageSize: Int,
    val feTemplateRemote: Boolean,
    val feTemplateCacheDir: String,
    val storage: StorageConfig,
    val mail: MailConfig,
    val db: DbConfig,
    val redis: RedisConfig
) {
    val isFullMode: Boolean get() = appMode == "full"

    /** Parse JWT_EXPIRES_IN string like '1h','30m','24h' into milliseconds */
    val jwtExpireMs: Long get() = parseExpiresIn(jwtExpiresIn)

    companion object {
        private const val MS_PER_SECOND = 1_000L
        private const val MS_PER_MINUTE = 60_000L
        private const val MS_PER_HOUR = 3_600_000L
        private const val MS_PER_DAY = 86_400_000L
        private const val DEFAULT_MINUTES = 60L
        private const val DEFAULT_SECONDS = 3_600L

        private const val DEFAULT_JWT_SECRET = "change-me-in-production-min-32-chars"
        private const val DEFAULT_SESSION_SECRET = "change-me-session-secret-min-32xx"

        private const val MYSQL_DRIVER = "com.mysql.cj.jdbc.Driver"
        private const val POSTGRES_DRIVER = "org.postgresql.Driver"
        private const val SQLITE_DRIVER = "org.sqlite.JDBC"
        private const val DEFAULT_SQLITE_URL = "jdbc:sqlite:./kotlinadmin.db"
        private const val MYSQL_PORT = 3306
        private const val POSTGRES_PORT = 5432

        fun fromEnv(config: ApplicationConfig): AppConfig {
            val appMode = config.propertyOrNull("app.mode")?.getString() ?: "full"
            val jwtSecret = config.propertyOrNull("app.jwtSecret")?.getString() ?: DEFAULT_JWT_SECRET
            val sessionSecret = config.propertyOrNull("app.sessionSecret")?.getString() ?: DEFAULT_SESSION_SECRET

            if (appMode == "production") {
                require(jwtSecret != DEFAULT_JWT_SECRET) { "JWT_SECRET must be changed from default in production" }
                require(
                    sessionSecret != DEFAULT_SESSION_SECRET
                ) { "SESSION_SECRET must be changed from default in production" }
            }

            return AppConfig(
                appName = config.propertyOrNull("app.name")?.getString() ?: "KotlinAdmin",
                appMode = appMode,
                port = config.propertyOrNull("ktor.deployment.port")?.getString()?.toIntOrNull() ?: 3000,
                jwtSecret = jwtSecret,
                jwtExpiresIn = config.propertyOrNull("app.jwtExpiresIn")?.getString() ?: "1h",
                sessionSecret = sessionSecret,
                sessionTtlHours = config.propertyOrNull("app.sessionTtlHours")?.getString()?.toLongOrNull() ?: 6L,
                sessionDriver = config.propertyOrNull("app.sessionDriver")?.getString() ?: "database",
                bcryptRounds = config.propertyOrNull("app.bcryptRounds")?.getString()?.toIntOrNull() ?: 10,
                otpExpiryMinutes = config.propertyOrNull("app.otpExpiryMinutes")?.getString()?.toLongOrNull() ?: 10L,
                defaultPageSize = config.propertyOrNull("app.defaultPageSize")?.getString()?.toIntOrNull() ?: 10,
                // Katalog frontend template: fetch live dari GitHub (matikan di test/offline).
                feTemplateRemote = config.propertyOrNull("app.feTemplateRemote")?.getString()
                    ?.toBooleanStrictOrNull() ?: true,
                feTemplateCacheDir = config.propertyOrNull("app.feTemplateCacheDir")?.getString()
                    ?: "storage/fe/templates",
                storage = StorageConfig(
                    driver = config.propertyOrNull("storage.driver")?.getString() ?: "local",
                    basePath = config.propertyOrNull("storage.basePath")?.getString() ?: "uploads",
                    accessKeyId = config.propertyOrNull("storage.accessKeyId")?.getString() ?: "",
                    secretAccessKey = config.propertyOrNull("storage.secretAccessKey")?.getString() ?: "",
                    endpoint = config.propertyOrNull("storage.endpoint")?.getString() ?: "",
                    bucket = config.propertyOrNull("storage.bucket")?.getString() ?: "",
                    region = config.propertyOrNull("storage.region")?.getString() ?: "",
                    ssl = config.propertyOrNull("storage.ssl")?.getString()?.toBooleanStrictOrNull() ?: true
                ),
                mail = MailConfig(
                    host = config.propertyOrNull("mail.host")?.getString() ?: "",
                    port = config.propertyOrNull("mail.port")?.getString()?.toIntOrNull() ?: 587,
                    secure = config.propertyOrNull("mail.secure")?.getString()?.toBooleanStrictOrNull() ?: false,
                    username = config.propertyOrNull("mail.username")?.getString() ?: "",
                    password = config.propertyOrNull("mail.password")?.getString() ?: "",
                    fromName = config.propertyOrNull("mail.fromName")?.getString() ?: "KotlinAdmin",
                    fromAddress = config.propertyOrNull("mail.fromAddress")?.getString() ?: ""
                ),
                db = resolveDb(config),
                redis = RedisConfig(
                    url = config.propertyOrNull("redis.url")?.getString() ?: "redis://127.0.0.1:6379",
                    host = config.propertyOrNull("redis.host")?.getString() ?: "localhost",
                    port = config.propertyOrNull("redis.port")?.getString()?.toIntOrNull() ?: 6379,
                    password = config.propertyOrNull("redis.password")?.getString()
                )
            )
        }

        /**
         * Susun DbConfig dari DB_TYPE + DB_HOST/DB_PORT/DB_DATABASE (paritas env NodeAdmin
         * dan port lain di fleet). DB_URL/DB_DRIVER tetap dihormati sebagai override penuh
         * bagi pemakai yang butuh parameter JDBC khusus (mis. sslmode, serverTimezone).
         */
        private fun resolveDb(config: ApplicationConfig): DbConfig {
            val explicitUrl = config.valueOf("database.url", "DB_URL")
            val type = config.valueOf("database.type", "DB_TYPE")?.lowercase()
            val host = config.valueOf("database.host", "DB_HOST") ?: "localhost"
            val name = config.valueOf("database.name", "DB_DATABASE")
                ?: config.valueOf("database.name", "DB_NAME")
                ?: "kotlinadmin"
            val port = config.valueOf("database.port", "DB_PORT")?.toIntOrNull()

            val (url, driver) = when {
                explicitUrl != null -> explicitUrl to (
                    config.valueOf("database.driver", "DB_DRIVER") ?: driverForUrl(explicitUrl)
                    )
                type == "mysql" || type == "mariadb" ->
                    "jdbc:mysql://$host:${port ?: MYSQL_PORT}/$name" to MYSQL_DRIVER
                type == "postgres" || type == "postgresql" ->
                    "jdbc:postgresql://$host:${port ?: POSTGRES_PORT}/$name" to POSTGRES_DRIVER
                // Catatan: SQLite in-memory sengaja tidak disediakan. Exposed membuka koneksi
                // baru per transaksi dan tiap koneksi ke ":memory:" mendapat DB kosong
                // sendiri — hasil migrasi Flyway tak akan terlihat oleh query aplikasi.
                else -> DEFAULT_SQLITE_URL to SQLITE_DRIVER
            }

            return DbConfig(
                url = url,
                driver = driver,
                user = config.valueOf("database.user", "DB_USERNAME")
                    ?: config.valueOf("database.user", "DB_USER")
                    ?: "",
                password = config.valueOf("database.password", "DB_PASSWORD") ?: ""
            )
        }

        /**
         * Ambil nilai dari HOCON, lalu jatuh ke environment variable bila kosong.
         *
         * Fallback env itu wajib: `testApplication` Ktor memasang ApplicationConfig kosong —
         * `application.conf` tidak ikut dimuat — sehingga tanpa ini setiap test (termasuk job
         * DB Compatibility di CI) selalu memakai default SQLite dan mengabaikan DB_* dari CI.
         */
        private fun ApplicationConfig.valueOf(key: String, envKey: String): String? =
            propertyOrNull(key)?.getString()?.takeIf { it.isNotBlank() }
                ?: System.getenv(envKey)?.takeIf { it.isNotBlank() }

        /** Tebak driver dari skema JDBC saat DB_URL diberikan tanpa DB_DRIVER. */
        private fun driverForUrl(url: String): String = when {
            url.startsWith("jdbc:mysql") || url.startsWith("jdbc:mariadb") -> MYSQL_DRIVER
            url.startsWith("jdbc:postgresql") -> POSTGRES_DRIVER
            else -> SQLITE_DRIVER
        }

        fun parseExpiresIn(s: String): Long {
            val trimmed = s.trim().lowercase()
            return when {
                trimmed.endsWith("h") -> (trimmed.dropLast(1).toLongOrNull() ?: 1L) * MS_PER_HOUR
                trimmed.endsWith("m") -> (trimmed.dropLast(1).toLongOrNull() ?: DEFAULT_MINUTES) * MS_PER_MINUTE
                trimmed.endsWith("d") -> (trimmed.dropLast(1).toLongOrNull() ?: 1L) * MS_PER_DAY
                else -> (trimmed.toLongOrNull() ?: DEFAULT_SECONDS) * MS_PER_SECOND
            }
        }
    }
}

data class StorageConfig(
    val driver: String,
    val basePath: String,
    val accessKeyId: String,
    val secretAccessKey: String,
    val endpoint: String,
    val bucket: String,
    val region: String,
    val ssl: Boolean
)

data class MailConfig(
    val host: String,
    val port: Int,
    val secure: Boolean,
    val username: String,
    val password: String,
    val fromName: String,
    val fromAddress: String
)

data class DbConfig(
    val url: String,
    val driver: String,
    val user: String,
    val password: String
)

data class RedisConfig(
    val url: String,
    val host: String,
    val port: Int,
    val password: String?
)
