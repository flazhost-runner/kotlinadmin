package com.kotlinadmin.modules.profile.routes

import com.kotlinadmin.core.helpers.respondJson
import com.kotlinadmin.core.helpers.respondView
import com.kotlinadmin.core.routing.namedGet
import com.kotlinadmin.core.routing.namedPost
import com.kotlinadmin.core.routing.namedPut
import com.kotlinadmin.core.routing.requireAuthenticated
import com.kotlinadmin.core.session.withErrors
import com.kotlinadmin.core.session.withFlash
import com.kotlinadmin.modules.access.models.toMap
import com.kotlinadmin.modules.profile.services.IProfileService
import com.kotlinadmin.modules.profile.services.UpdateProfileDto
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.routing
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import org.koin.ktor.ext.get

fun Application.profileModule() {
    val profileService = get<IProfileService>()

    routing {
        namedGet("admin.v1.profile.index", "/admin/v1/profile") {
            val session = call.requireAuthenticated()
            val user = profileService.get(session.userId)
            call.respondView(
                "profile/profile.ftl",
                mapOf(
                    "user" to user.toMap(),
                    "page_title" to "Profile"
                )
            )
        }

        namedPut("admin.v1.profile.update", "/admin/v1/profile/update") {
            val session = call.requireAuthenticated()
            val params = call.receiveParameters()

            val name = params["name"]?.trim() ?: ""
            val email = params["email"]?.trim() ?: ""
            val code = params["code"]?.trim() ?: ""
            val phone = params["phone"]?.trim() ?: ""
            val timezone = params["timezone"]?.trim() ?: "UTC"
            val status = params["status"]?.trim() ?: "Active"

            val errors = mutableMapOf<String, String>()
            if (name.isBlank()) errors["name"] = "Name is required"
            if (email.isBlank()) errors["email"] = "Email is required"

            if (errors.isNotEmpty()) {
                call.sessions.set(
                    session.withErrors(
                        errors,
                        mapOf(
                            "name" to name,
                            "email" to email,
                            "code" to code,
                            "phone" to phone,
                            "timezone" to timezone
                        )
                    )
                )
                call.respondRedirect("/admin/v1/profile")
                return@namedPut
            }

            val dto = UpdateProfileDto(
                name = name,
                email = email,
                code = code,
                phone = phone.ifBlank { null },
                timezone = timezone,
                status = status
            )
            val updated = profileService.update(session.userId, dto)

            val newSession = session.copy(userName = updated.name, userEmail = updated.email)
                .withFlash("success", "Update Profile Success.")
            call.sessions.set(newSession)
            call.respondRedirect("/admin/v1/profile")
        }

        namedPost("admin.v1.profile.password", "/admin/v1/profile/password") {
            val session = call.requireAuthenticated()
            val params = call.receiveParameters()

            val currentPassword = params["currentPassword"] ?: ""
            val newPassword = params["newPassword"] ?: ""
            val newPasswordConfirm = params["newPasswordConfirm"] ?: ""

            val errors = mutableMapOf<String, String>()
            if (currentPassword.isBlank()) errors["currentPassword"] = "Current password is required"
            if (newPassword.isBlank()) errors["newPassword"] = "New password is required"
            if (newPassword != newPasswordConfirm) errors["newPasswordConfirm"] = "Passwords do not match"

            if (errors.isNotEmpty()) {
                call.sessions.set(session.withErrors(errors, emptyMap()))
                call.respondRedirect("/admin/v1/profile")
                return@namedPost
            }

            val dto = UpdateProfileDto(password = newPassword, passwordConfirm = newPasswordConfirm)
            profileService.update(session.userId, dto)
            call.sessions.set(session.withFlash("success", "Update Profile Success."))
            call.respondRedirect("/admin/v1/profile")
        }

        namedPost("admin.v1.profile.picture", "/admin/v1/profile/picture") {
            val session = call.requireAuthenticated()
            // Picture upload: handled via multipart in real implementation
            // For now, just redirect back
            call.sessions.set(session.withFlash("info", "Picture upload not yet implemented"))
            call.respondRedirect("/admin/v1/profile")
        }
    }
}

fun Application.profileApiModule() {
    val profileService = get<IProfileService>()

    routing {
        authenticate("api") {
            namedGet("api.v1.profile.index", "/api/v1/profile") {
                val principal = call.principal<JWTPrincipal>()!!
                val userId = principal.subject!!
                val user = profileService.get(userId)
                call.respondJson(
                    data = mapOf(
                        "id" to user.id.value,
                        "name" to user.name,
                        "email" to user.email,
                        "timezone" to (user.timezone ?: "UTC"),
                        "picture" to (user.picture ?: ""),
                        "status" to user.status
                    )
                )
            }
        }
    }
}
