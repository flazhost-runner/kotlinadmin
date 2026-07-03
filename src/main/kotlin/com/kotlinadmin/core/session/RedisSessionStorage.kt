package com.kotlinadmin.core.session

import com.kotlinadmin.config.RedisManager
import io.ktor.server.sessions.SessionStorage

private const val DEFAULT_SESSION_TTL_SECONDS = 7 * 24 * 3600L

class RedisSessionStorage(private val ttlSeconds: Long = DEFAULT_SESSION_TTL_SECONDS) : SessionStorage {

    override suspend fun write(id: String, value: String) {
        RedisManager.setSession(id, value, ttlSeconds)
    }

    override suspend fun read(id: String): String {
        return RedisManager.getSession(id)
            ?: throw NoSuchElementException("Session $id not found")
    }

    override suspend fun invalidate(id: String) {
        RedisManager.deleteSession(id)
    }
}
