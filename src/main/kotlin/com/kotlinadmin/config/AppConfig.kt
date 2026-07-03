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
                storage = StorageConfig(
                    driver = config.propertyOrNull("storage.driver")?.getString() ?: "local",
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
                db = DbConfig(
                    url = config.propertyOrNull("database.url")?.getString() ?: "jdbc:sqlite:./kotlinadmin.db",
                    driver = config.propertyOrNull("database.driver")?.getString() ?: "org.sqlite.JDBC",
                    user = config.propertyOrNull("database.user")?.getString() ?: "",
                    password = config.propertyOrNull("database.password")?.getString() ?: ""
                ),
                redis = RedisConfig(
                    url = config.propertyOrNull("redis.url")?.getString() ?: "redis://127.0.0.1:6379",
                    host = config.propertyOrNull("redis.host")?.getString() ?: "localhost",
                    port = config.propertyOrNull("redis.port")?.getString()?.toIntOrNull() ?: 6379,
                    password = config.propertyOrNull("redis.password")?.getString()
                )
            )
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
