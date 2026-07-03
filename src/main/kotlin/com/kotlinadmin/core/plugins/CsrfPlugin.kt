package com.kotlinadmin.core.plugins

import com.kotlinadmin.core.errors.ForbiddenError
import com.kotlinadmin.core.session.UserSession
import io.ktor.http.HttpMethod
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import java.security.MessageDigest
import java.security.SecureRandom

val CsrfPlugin = createApplicationPlugin("CSRF") {
    onCall { call ->
        val path = call.request.local.uri
        if (path.startsWith("/api/")) return@onCall

        val method = call.request.local.method
        val safeMethods = setOf(HttpMethod.Get, HttpMethod.Head, HttpMethod.Options)
        if (method in safeMethods) return@onCall

        val session = call.sessions.get<UserSession>() ?: return@onCall
        val sessionToken = session.csrfToken
        if (sessionToken.isNullOrBlank()) return@onCall

        // 3 sources: query param, header, body field
        // Note: templates embed _csrf in the action URL query string (e.g. ?_csrf=...) so
        // tokenFromQuery covers all standard form POSTs. Body reading via receiveParameters()
        // is intentionally omitted here because Ktor allows the body to be consumed only
        // once — route handlers must not be deprived of it.
        val tokenFromQuery = call.request.queryParameters["_csrf"]
        val tokenFromHeader = call.request.headers["x-csrf-token"]
            ?: call.request.headers["X-CSRF-Token"]

        val submittedToken = tokenFromQuery ?: tokenFromHeader

        if (!timingSafeEquals(sessionToken, submittedToken ?: "")) {
            throw ForbiddenError("CSRF token invalid")
        }
    }
}

/** Constant-time string comparison to prevent timing attacks */
private fun timingSafeEquals(a: String, b: String): Boolean {
    val aBytes = a.toByteArray()
    val bBytes = b.toByteArray()
    return MessageDigest.isEqual(aBytes, bBytes)
}

private const val CSRF_TOKEN_BYTES = 32

fun generateCsrfToken(): String {
    val bytes = ByteArray(CSRF_TOKEN_BYTES)
    SecureRandom().nextBytes(bytes)
    return bytes.joinToString("") { "%02x".format(it) }
}
