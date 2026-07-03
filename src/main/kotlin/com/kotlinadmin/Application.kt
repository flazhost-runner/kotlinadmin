package com.kotlinadmin

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.kotlinadmin.config.AppConfig
import com.kotlinadmin.config.DatabaseConfig
import com.kotlinadmin.config.RedisManager
import com.kotlinadmin.core.errors.AppException
import com.kotlinadmin.core.errors.ForbiddenError
import com.kotlinadmin.core.errors.UnauthorizedError
import com.kotlinadmin.core.errors.ValidationError
import com.kotlinadmin.core.helpers.anyToJsonElement
import com.kotlinadmin.core.helpers.respondError
import com.kotlinadmin.core.plugins.CsrfPlugin
import com.kotlinadmin.core.plugins.MethodOverridePlugin
import com.kotlinadmin.core.plugins.SecurityHeadersPlugin
import com.kotlinadmin.core.session.DatabaseSessionStorage
import com.kotlinadmin.core.session.RedisSessionStorage
import com.kotlinadmin.core.session.UserSession
import com.kotlinadmin.core.session.withErrors
import com.kotlinadmin.core.session.withFlash
import com.kotlinadmin.di.appModule
import com.kotlinadmin.modules.access.routes.accessApiModule
import com.kotlinadmin.modules.access.routes.accessWebModule
import com.kotlinadmin.modules.auth.routes.authModule
import com.kotlinadmin.modules.components.routes.componentsModule
import com.kotlinadmin.modules.dashboard.routes.dashboardModule
import com.kotlinadmin.modules.home.routes.homeModule
import com.kotlinadmin.modules.media.routes.mediaModule
import com.kotlinadmin.modules.profile.routes.profileApiModule
import com.kotlinadmin.modules.profile.routes.profileModule
import com.kotlinadmin.modules.setting.routes.settingApiModule
import com.kotlinadmin.modules.setting.routes.settingModule
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.auth.session
import io.ktor.server.freemarker.FreeMarker
import io.ktor.server.http.content.staticResources
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.compression.Compression
import io.ktor.server.plugins.compression.gzip
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.plugins.ratelimit.RateLimitName
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respondText
import io.ktor.server.routing.routing
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.cookie
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import kotlin.time.Duration.Companion.seconds

private const val SECONDS_PER_HOUR = 3600L

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module() {
    val appConfig = AppConfig.fromEnv(environment.config)

    RedisManager.init(appConfig.redis)
    DatabaseConfig.setup(appConfig.db)

    install(Koin) {
        slf4jLogger()
        modules(appModule(appConfig))
    }

    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowHeader("X-CSRF-Token")
        allowHeader("x-csrf-token")
        anyHost()
    }

    install(Compression) { gzip() }

    install(SecurityHeadersPlugin)

    install(MethodOverridePlugin)

    install(RateLimit) {
        register(RateLimitName("login")) {
            rateLimiter(limit = 10, refillPeriod = 60.seconds)
        }
        register(RateLimitName("otp")) {
            rateLimiter(limit = 5, refillPeriod = 300.seconds)
        }
    }

    if (appConfig.isFullMode) {
        install(Sessions) {
            val sessionTtlSeconds = appConfig.sessionTtlHours * SECONDS_PER_HOUR
            val sessionStorage = if (appConfig.sessionDriver == "database") {
                DatabaseSessionStorage(sessionTtlSeconds)
            } else {
                RedisSessionStorage(sessionTtlSeconds)
            }
            cookie<UserSession>("KOTLINADMIN_SESSION", storage = sessionStorage) {
                cookie.httpOnly = true
                cookie.maxAgeInSeconds = sessionTtlSeconds
                cookie.path = "/"
                serializer = object : io.ktor.server.sessions.SessionSerializer<UserSession> {
                    override fun deserialize(text: String): UserSession =
                        Json.decodeFromString(UserSession.serializer(), text)
                    override fun serialize(session: UserSession): String =
                        Json.encodeToString(UserSession.serializer(), session)
                }
            }
        }
    }

    val jwtVerifier = JWT.require(Algorithm.HMAC256(appConfig.jwtSecret))
        .withIssuer("kotlinadmin")
        .build()

    install(Authentication) {
        jwt("api") {
            verifier(jwtVerifier)
            validate { credential ->
                val jti = credential.payload.id
                if (jti != null && RedisManager.isBlacklisted(jti)) {
                    null
                } else {
                    JWTPrincipal(credential.payload)
                }
            }
            challenge { _, _ ->
                call.respondError(HttpStatusCode.Unauthorized, "Invalid or expired token")
            }
        }
        if (appConfig.isFullMode) {
            session<UserSession>("web") {
                validate { session -> session }
                challenge {
                    call.respondRedirect("/auth/login")
                }
            }
        }
    }

    if (appConfig.isFullMode) {
        install(CsrfPlugin)
    }

    install(StatusPages) {
        exception<UnauthorizedError> { call, _ ->
            if (call.request.local.uri.startsWith("/api/")) {
                call.respondError(HttpStatusCode.Unauthorized, "Unauthorized")
            } else {
                call.respondRedirect("/auth/login")
            }
        }
        exception<ForbiddenError> { call, ex ->
            if (call.request.local.uri.startsWith("/api/")) {
                call.respondError(HttpStatusCode.Forbidden, ex.message ?: "Forbidden")
            } else {
                val session = call.sessions.get<UserSession>()
                if (session != null) {
                    call.sessions.set(session.withFlash("error", "Unauthorized."))
                }
                call.respondRedirect("/admin/v1/dashboard")
            }
        }
        exception<ValidationError> { call, ex ->
            if (call.request.local.uri.startsWith("/api/")) {
                val response = buildJsonObject {
                    put("status", false)
                    put("message", ex.message ?: "Validation Error")
                    put("errors", anyToJsonElement(ex.fieldErrors))
                }
                call.respondText(
                    Json.encodeToString(JsonObject.serializer(), response),
                    ContentType.Application.Json,
                    HttpStatusCode.UnprocessableEntity
                )
            } else {
                val session = call.sessions.get<UserSession>()
                if (session != null) {
                    call.sessions.set(session.withErrors(ex.fieldErrors, emptyMap()))
                }
                val referrer = call.request.headers["Referer"] ?: "/admin/v1/dashboard"
                call.respondRedirect(referrer)
            }
        }
        exception<AppException> { call, ex ->
            if (call.request.local.uri.startsWith("/api/")) {
                call.respondError(ex.statusCode, ex.message ?: "Error")
            } else {
                val session = call.sessions.get<UserSession>()
                if (session != null) {
                    call.sessions.set(session.withFlash("danger", ex.message ?: "An error occurred"))
                }
                val referrer = call.request.headers["Referer"] ?: "/admin/v1/dashboard"
                call.respondRedirect(referrer)
            }
        }
        exception<Exception> { call, ex ->
            call.application.environment.log.error("Unhandled exception", ex)
            call.respondError(HttpStatusCode.InternalServerError, "Internal server error")
        }
    }

    install(ContentNegotiation) {
        json(
            Json {
                prettyPrint = false
                isLenient = true
                ignoreUnknownKeys = true
            }
        )
    }

    if (appConfig.isFullMode) {
        install(FreeMarker) {
            templateLoader = freemarker.cache.ClassTemplateLoader(
                this::class.java.classLoader,
                "templates"
            )
            defaultEncoding = "UTF-8"
            templateExceptionHandler = freemarker.template.TemplateExceptionHandler.HTML_DEBUG_HANDLER
        }
    }

    configureRouting(appConfig)

    val shutdownLog = environment.log
    Runtime.getRuntime().addShutdownHook(
        Thread {
            shutdownLog.info("Shutting down KotlinAdmin...")
            RedisManager.close()
        }
    )
}

fun Application.configureRouting(config: AppConfig) {
    routing {
        staticResources("/assets", "static")
        staticResources("/be", "static/be")

        if (config.isFullMode) {
            authModule()
            accessWebModule()
            dashboardModule()
            settingModule()
            profileModule()
            mediaModule()
            componentsModule()
            homeModule()
        }

        accessApiModule()
        profileApiModule()
        settingApiModule()
    }
}
