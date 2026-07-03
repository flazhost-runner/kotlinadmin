package com.kotlinadmin.config

import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.sync.RedisCommands
import org.slf4j.LoggerFactory

object RedisManager {
    private val logger = LoggerFactory.getLogger(RedisManager::class.java)
    private lateinit var client: RedisClient
    private lateinit var connection: StatefulRedisConnection<String, String>

    fun init(config: RedisConfig) {
        val uriBuilder = RedisURI.Builder
            .redis(config.host, config.port)
        config.password?.let { uriBuilder.withPassword(it.toCharArray()) }
        client = RedisClient.create(uriBuilder.build())
        connection = client.connect()
        logger.info("Redis connected to ${config.host}:${config.port}")
    }

    fun commands(): RedisCommands<String, String> = connection.sync()

    fun blacklistJwt(jti: String, ttlSeconds: Long) {
        commands().setex("blacklist:$jti", ttlSeconds, "1")
    }

    fun isBlacklisted(jti: String): Boolean {
        return commands().exists("blacklist:$jti") > 0
    }

    fun setSession(sessionId: String, value: String, ttlSeconds: Long) {
        commands().setex("session:$sessionId", ttlSeconds, value)
    }

    fun getSession(sessionId: String): String? {
        return commands().get("session:$sessionId")
    }

    fun deleteSession(sessionId: String) {
        commands().del("session:$sessionId")
    }

    fun incrementRateLimit(key: String, ttlSeconds: Long): Long {
        val count = commands().incr(key)
        if (count == 1L) commands().expire(key, ttlSeconds)
        return count
    }

    fun close() {
        if (::connection.isInitialized) connection.close()
        if (::client.isInitialized) client.shutdown()
    }
}
