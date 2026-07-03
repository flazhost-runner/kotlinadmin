package com.kotlinadmin.core.plugins

import io.ktor.server.application.createApplicationPlugin

val SecurityHeadersPlugin = createApplicationPlugin("SecurityHeaders") {
    onCall { call ->
        call.response.headers.apply {
            append("X-Frame-Options", "DENY")
            append("X-Content-Type-Options", "nosniff")
            append("Referrer-Policy", "strict-origin-when-cross-origin")
            append("X-XSS-Protection", "1; mode=block")
            append("Permissions-Policy", "camera=(), microphone=(), geolocation=()")
        }
    }
}
