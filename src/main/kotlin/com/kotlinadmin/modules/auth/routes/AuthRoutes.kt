package com.kotlinadmin.modules.auth.routes

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.kotlinadmin.config.AppConfig
import com.kotlinadmin.core.helpers.respondJson
import com.kotlinadmin.core.helpers.respondView
import com.kotlinadmin.core.routing.namedGet
import com.kotlinadmin.core.routing.namedPost
import com.kotlinadmin.core.session.UserSession
import com.kotlinadmin.core.session.withErrors
import com.kotlinadmin.core.session.withFlash
import com.kotlinadmin.modules.auth.dto.LoginDto
import com.kotlinadmin.modules.auth.dto.RegisterDto
import com.kotlinadmin.modules.auth.dto.ResetProcessDto
import com.kotlinadmin.modules.auth.dto.ResetRequestDto
import com.kotlinadmin.modules.auth.services.IAuthService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import org.koin.ktor.ext.get
import java.util.*

private const val MS_PER_SECOND = 1000L

fun Application.authModule() {
    val authService = get<IAuthService>()
    val config = get<AppConfig>()

    routing {
        // ── Public web routes ────────────────────────────────────────────────
        namedGet("web.home.root", "/") {
            call.respondView("home/index.ftl")
        }

        namedGet("web.home.index", "/home") {
            call.respondView("home/index.ftl")
        }

        namedGet("web.auth.login", "/auth/login") {
            val existing = call.sessions.get<UserSession>()
            if (existing != null) {
                call.respondRedirect("/admin/v1/dashboard")
                return@namedGet
            }
            call.respondView("auth/login.ftl")
        }

        namedPost("web.auth.login.post", "/auth/login") {
            val ip = call.request.local.remoteAddress
            authService.checkAuthRateLimit(ip)

            val params = call.receiveParameters()
            val email = params["email"]?.trim() ?: ""
            val password = params["password"] ?: ""

            val errors = mutableMapOf<String, String>()
            if (email.isBlank()) errors["email"] = "Email is required"
            if (password.isBlank()) errors["password"] = "Password is required"
            if (errors.isNotEmpty()) {
                val blankSession = UserSession("", "", "", emptyList())
                call.sessions.set(blankSession.withErrors(errors, mapOf("email" to email)))
                call.respondRedirect("/auth/login")
                return@namedPost
            }

            val user = authService.login(email, password)
            val roles = authService.getUserRoleNames(user.id.value)
            val csrfToken = UUID.randomUUID().toString()
            val session = UserSession(
                userId = user.id.value,
                userName = user.name,
                userEmail = user.email,
                roles = roles,
                csrfToken = csrfToken
            )
            call.sessions.set(session)
            call.respondRedirect("/admin/v1/dashboard")
        }

        namedGet("web.auth.register", "/auth/register") {
            val existing = call.sessions.get<UserSession>()
            if (existing != null) {
                call.respondRedirect("/admin/v1/dashboard")
                return@namedGet
            }
            call.respondView("auth/register.ftl")
        }

        namedPost("web.auth.register.post", "/auth/register") {
            val ip = call.request.local.remoteAddress
            authService.checkAuthRateLimit(ip)

            val params = call.receiveParameters()
            val name = params["name"]?.trim() ?: ""
            val email = params["email"]?.trim() ?: ""
            val password = params["password"] ?: ""
            val passwordConfirm = params["password_confirmation"] ?: ""

            val errors = mutableMapOf<String, String>()
            if (name.isBlank()) errors["name"] = "Name is required"
            if (email.isBlank()) errors["email"] = "Email is required"
            if (password.isBlank()) errors["password"] = "Password is required"
            if (password != passwordConfirm) errors["password_confirmation"] = "Passwords do not match"

            if (errors.isNotEmpty()) {
                val blankSession = UserSession("", "", "", emptyList())
                call.sessions.set(blankSession.withErrors(errors, mapOf("name" to name, "email" to email)))
                call.respondRedirect("/auth/register")
                return@namedPost
            }

            authService.register(RegisterDto(name = name, email = email, password = password))
            val blankSession = UserSession("", "", "", emptyList())
            call.sessions.set(blankSession.withFlash("success", "Register Success."))
            call.respondRedirect("/auth/login")
        }

        namedPost("web.auth.logout", "/auth/logout") {
            call.sessions.clear<UserSession>()
            call.respondRedirect("/auth/login")
        }

        // ── Password reset OTP (public) ──────────────────────────────────────
        namedGet("admin.v1.auth.reset.req", "/admin/v1/auth/reset/req") {
            call.respondView("auth/reset_req.ftl")
        }

        namedPost("admin.v1.auth.reset.request", "/admin/v1/auth/reset/request") {
            val ip = call.request.local.remoteAddress
            authService.checkAuthRateLimit(ip)

            val params = call.receiveParameters()
            val email = params["email"]?.trim() ?: ""

            if (email.isBlank()) {
                val s = UserSession("", "", "", emptyList())
                call.sessions.set(s.withErrors(mapOf("email" to "Email is required"), mapOf("email" to email)))
                call.respondRedirect("/admin/v1/auth/reset/req")
                return@namedPost
            }

            authService.requestOtp(ResetRequestDto(email = email))
            val s = UserSession("", "", "", emptyList())
            call.sessions.set(s.withFlash("success", "OTP Send Success."))
            call.respondRedirect("/admin/v1/auth/reset/proc")
        }

        namedGet("admin.v1.auth.reset.proc", "/admin/v1/auth/reset/proc") {
            call.respondView("auth/reset_proc.ftl")
        }

        namedPost("admin.v1.auth.reset.process", "/admin/v1/auth/reset/process") {
            val ip = call.request.local.remoteAddress
            authService.checkOtpRateLimit(ip)

            val params = call.receiveParameters()
            val email = params["email"]?.trim() ?: ""
            val otp = params["otp"]?.trim() ?: ""
            val password = params["password"] ?: ""
            val passwordConfirm = params["password_confirmation"] ?: ""

            val errors = mutableMapOf<String, String>()
            if (email.isBlank()) errors["email"] = "Email is required"
            if (otp.isBlank()) errors["otp"] = "OTP is required"
            if (password.isBlank()) errors["password"] = "New password is required"
            if (password != passwordConfirm) errors["password_confirmation"] = "Passwords do not match"

            if (errors.isNotEmpty()) {
                val s = UserSession("", "", "", emptyList())
                call.sessions.set(s.withErrors(errors, mapOf("email" to email, "otp" to otp)))
                call.respondRedirect("/admin/v1/auth/reset/proc")
                return@namedPost
            }

            authService.processOtp(ResetProcessDto(email = email, otp = otp, newPassword = password))
            val s = UserSession("", "", "", emptyList())
            call.sessions.set(s.withFlash("success", "Reset Password Success."))
            call.respondRedirect("/auth/login")
        }

        // ── API auth routes ──────────────────────────────────────────────────
        namedPost("api.v1.auth.login", "/api/v1/auth/login") {
            val dto = call.receive<LoginDto>()
            val user = authService.login(dto.email, dto.password)
            val roles = authService.getUserRoleNames(user.id.value)

            val jti = UUID.randomUUID().toString()
            val expMs = System.currentTimeMillis() + config.jwtExpireMs
            val token = JWT.create()
                .withIssuer("kotlinadmin")
                .withSubject(user.id.value)
                .withJWTId(jti)
                .withClaim("name", user.name)
                .withClaim("email", user.email)
                .withClaim("roles", roles)
                .withExpiresAt(Date(expMs))
                .sign(Algorithm.HMAC256(config.jwtSecret))

            call.respondJson(
                data = mapOf(
                    "token" to token,
                    "user" to mapOf("id" to user.id.value, "name" to user.name, "email" to user.email, "roles" to roles)
                )
            )
        }

        namedPost("api.v1.auth.register", "/api/v1/auth/register") {
            val dto = call.receive<RegisterDto>()
            val user = authService.register(dto)
            call.respondJson(
                HttpStatusCode.Created,
                message = "Register Success.",
                data = mapOf(
                    "id" to user.id.value,
                    "name" to user.name,
                    "email" to user.email
                )
            )
        }

        authenticate("api") {
            namedGet("api.v1.auth.me", "/api/v1/auth/me") {
                val principal = call.principal<JWTPrincipal>()!!
                val userId = principal.subject!!
                val user = authService.getById(userId)
                call.respondJson(
                    data = mapOf(
                        "id" to user.id.value,
                        "name" to user.name,
                        "email" to user.email,
                        "status" to user.status
                    )
                )
            }

            namedPost("api.v1.auth.logout", "/api/v1/auth/logout") {
                val principal = call.principal<JWTPrincipal>()!!
                val jti = principal.jwtId ?: ""
                val expMs = principal.expiresAt?.time ?: System.currentTimeMillis()
                val ttl = ((expMs - System.currentTimeMillis()) / MS_PER_SECOND).coerceAtLeast(1)
                authService.blacklistJwt(jti, ttl)
                call.respondJson(data = mapOf("message" to "Logged out successfully"))
            }

            namedPost("api.v1.auth.reset.request", "/api/v1/auth/reset/request") {
                val dto = call.receive<ResetRequestDto>()
                authService.requestOtp(dto)
                call.respondJson(data = mapOf("message" to "OTP Send Success."))
            }

            namedPost("api.v1.auth.reset.process", "/api/v1/auth/reset/process") {
                val dto = call.receive<ResetProcessDto>()
                authService.processOtp(dto)
                call.respondJson(data = mapOf("message" to "Reset Password Success."))
            }
        }
    }
}
