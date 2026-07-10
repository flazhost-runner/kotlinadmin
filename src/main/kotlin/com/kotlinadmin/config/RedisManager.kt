package com.kotlinadmin.config

import io.lettuce.core.RedisClient
import io.lettuce.core.RedisException
import io.lettuce.core.RedisURI
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.sync.RedisCommands
import org.slf4j.LoggerFactory

object RedisManager {
    private val logger = LoggerFactory.getLogger(RedisManager::class.java)
    private var client: RedisClient? = null
    private var connection: StatefulRedisConnection<String, String>? = null

    /**
     * Bangun RedisURI dari REDIS_URL bila tersedia. Untuk skema `rediss://`
     * ini mengaktifkan TLS dan — karena Lettuce membuat SSLEngine dengan host
     * dari RedisURI (SslConnectionBuilder → SslContext.newEngine(alloc, host, port))
     * — Netty mengirim SNI servername = host REDIS_URL. Ini yang dibutuhkan oleh
     * HAProxy managed-Redis yang me-route berdasarkan SNI di satu port bersama.
     * Fallback ke host/port (plaintext) hanya bila URL kosong.
     */
    private fun buildUri(config: RedisConfig): RedisURI {
        val url = config.url.trim()
        if (url.isNotEmpty()) {
            val uri = RedisURI.create(url)
            // Terapkan REDIS_PASSWORD terpisah hanya bila URL tidak membawa password.
            if (!config.password.isNullOrEmpty() && uri.password?.isNotEmpty() != true) {
                uri.setPassword(config.password.toCharArray())
            }
            return uri
        }
        val uriBuilder = RedisURI.Builder.redis(config.host, config.port)
        config.password?.let { uriBuilder.withPassword(it.toCharArray()) }
        return uriBuilder.build()
    }

    fun init(config: RedisConfig) {
        val uri = buildUri(config)
        // Simpan uri lebih dulu agar reconnect lazy tetap mungkin walau connect awal gagal.
        pendingUri = uri
        try {
            val created = RedisClient.create(uri)
            client = created
            connection = created.connect()
            logger.info("Redis connected to ${uri.host}:${uri.port} (ssl=${uri.isSsl})")
        } catch (ex: RedisException) {
            // Kegagalan Redis tidak boleh memblokir startup HTTP. Sambungan akan
            // dicoba-ulang secara lazy saat command pertama dijalankan.
            logger.error("Redis connection failed at startup (ssl=${uri.isSsl}); continuing without Redis", ex)
        }
    }

    private var pendingUri: RedisURI? = null

    private fun conn(): StatefulRedisConnection<String, String>? {
        val active = connection?.takeIf { it.isOpen }
        return active ?: pendingUri?.let { connect(it) }
    }

    /** Sambungan lazy; `null` bila Redis sedang tak tersedia. */
    private fun connect(uri: RedisURI): StatefulRedisConnection<String, String>? =
        try {
            val active = client ?: RedisClient.create(uri).also { client = it }
            active.connect().also { connection = it }
        } catch (ex: RedisException) {
            logger.warn("Redis reconnect failed", ex)
            null
        }

    fun commands(): RedisCommands<String, String> =
        conn()?.sync() ?: error("Redis is not available")

    fun blacklistJwt(jti: String, ttlSeconds: Long) {
        conn()?.sync()?.setex("blacklist:$jti", ttlSeconds, "1")
    }

    fun isBlacklisted(jti: String): Boolean {
        // Fail-open: bila Redis tidak tersedia jangan gagalkan verifikasi token.
        return conn()?.sync()?.exists("blacklist:$jti")?.let { it > 0 } ?: false
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
        connection?.close()
        client?.shutdown()
    }
}
