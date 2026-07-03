package com.kotlinadmin.core.session

import com.kotlinadmin.config.DatabaseConfig
import io.ktor.server.sessions.SessionStorage
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.upsert
import java.time.LocalDateTime

object Sessions : Table("sessions") {
    val id = varchar("id", 128)
    val data = text("data")
    val expiresAt = datetime("expires_at")
    override val primaryKey = PrimaryKey(id)
}

private const val DEFAULT_SESSION_TTL_SECONDS = 7 * 24 * 3600L

class DatabaseSessionStorage(private val ttlSeconds: Long = DEFAULT_SESSION_TTL_SECONDS) : SessionStorage {

    override suspend fun write(id: String, value: String) {
        val expiresAt = LocalDateTime.now().plusSeconds(ttlSeconds)
        DatabaseConfig.dbQuery {
            Sessions.upsert {
                it[Sessions.id] = id
                it[Sessions.data] = value
                it[Sessions.expiresAt] = expiresAt
            }
        }
    }

    override suspend fun read(id: String): String {
        return DatabaseConfig.dbQuery {
            Sessions.selectAll()
                .where { Sessions.id eq id }
                .firstOrNull()
                ?.let { row ->
                    if (row[Sessions.expiresAt].isBefore(LocalDateTime.now())) {
                        null
                    } else {
                        row[Sessions.data]
                    }
                }
        } ?: throw NoSuchElementException("Session $id not found or expired")
    }

    override suspend fun invalidate(id: String) {
        DatabaseConfig.dbQuery {
            Sessions.deleteWhere { Sessions.id eq id }
        }
    }
}
