package com.kotlinadmin.modules.profile.routes

import com.kotlinadmin.core.helpers.respondJson
import com.kotlinadmin.core.helpers.respondView
import com.kotlinadmin.core.routing.namedGet
import com.kotlinadmin.core.routing.namedPut
import com.kotlinadmin.core.routing.requireAuthenticated
import com.kotlinadmin.core.session.withErrors
import com.kotlinadmin.core.session.withFlash
import com.kotlinadmin.core.storage.IStorageService
import com.kotlinadmin.modules.access.models.toMap
import com.kotlinadmin.modules.profile.services.IProfileService
import com.kotlinadmin.modules.profile.services.UpdateProfileDto
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.routing
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import org.koin.ktor.ext.get
import java.time.ZoneId

private const val MAX_PICTURE_SIZE = 2 * 1024 * 1024L // 2MB — matches the fleet upload standard

fun Application.profileModule() {
    val profileService = get<IProfileService>()
    val storage = get<IStorageService>()

    routing {
        namedGet("admin.v1.profile.index", "/admin/v1/profile") {
            val session = call.requireAuthenticated()
            val user = profileService.get(session.userId)
            call.respondView(
                "profile/profile.ftl",
                mapOf(
                    "user" to user.toMap(),
                    // Full IANA zone list — mirrors NodeAdmin getTimezones()
                    // (Intl.supportedValuesOf('timeZone')).
                    "timezones" to ZoneId.getAvailableZoneIds().sorted(),
                    "page_title" to "Profile"
                )
            )
        }

        // Single multipart form (code/name/phone/email/timezone/password/status/picture),
        // 1:1 with NodeAdmin. CSRF (?_csrf) and method-override (?_method=PUT) are read from
        // the query string, so the multipart body is left intact for receiveMultipart().
        namedPut("admin.v1.profile.update", "/admin/v1/profile/update") {
            val session = call.requireAuthenticated()

            val fields = mutableMapOf<String, String>()
            var pictureBytes: ByteArray? = null
            var pictureFileName: String? = null
            var pictureContentType: String? = null

            call.receiveMultipart().forEachPart { part ->
                when (part) {
                    is PartData.FormItem -> fields[part.name ?: ""] = part.value
                    is PartData.FileItem -> if (part.name == "picture") {
                        val fileName = part.originalFileName ?: ""
                        if (fileName.isNotBlank()) {
                            pictureBytes = part.streamProvider().readBytes()
                            pictureFileName = fileName
                            pictureContentType = part.contentType?.toString()?.split(";")?.first()?.trim()?.lowercase()
                        }
                    }
                    else -> {}
                }
                part.dispose()
            }

            val name = fields["name"]?.trim() ?: ""
            val email = fields["email"]?.trim() ?: ""
            val code = fields["code"]?.trim() ?: ""
            val phone = fields["phone"]?.trim() ?: ""
            val timezone = fields["timezone"]?.trim() ?: "UTC"
            val status = fields["status"]?.trim() ?: "Active"
            val password = fields["password"]?.trim() ?: ""
            val passwordConfirmation = fields["password_confirmation"]?.trim() ?: ""

            val errors = mutableMapOf<String, String>()
            if (name.isBlank()) errors["name"] = "Name is required"
            if (email.isBlank()) errors["email"] = "Email is required"
            if (password.isNotBlank() && password != passwordConfirmation) {
                errors["password_confirmation"] = "Password confirmation does not match"
            }
            if (pictureBytes != null) {
                if (pictureContentType?.startsWith("image/") != true) {
                    errors["picture"] = "Only image files are allowed."
                } else if (pictureBytes!!.size > MAX_PICTURE_SIZE) {
                    errors["picture"] = "File size exceeds 2MB limit."
                }
            }

            if (errors.isNotEmpty()) {
                call.sessions.set(
                    session.withErrors(
                        errors,
                        mapOf(
                            "name" to name,
                            "email" to email,
                            "code" to code,
                            "phone" to phone,
                            "timezone" to timezone,
                            "status" to status
                        )
                    )
                )
                call.respondRedirect("/admin/v1/profile")
                return@namedPut
            }

            // Upload avatar (only after validation passes) — driver-agnostic: local writes to
            // STORAGE_BASE_PATH, oss/s3 uploads to the bucket. Key `user/<id>.<ext>` mirrors
            // NodeAdmin; render URL is derived from the key by getFile()/IStorageService.url().
            var pictureKey: String? = null
            if (pictureBytes != null) {
                val ext = pictureFileName!!.substringAfterLast('.', "").lowercase()
                    .ifBlank { pictureContentType!!.substringAfterLast('/') }
                    .let { if (it == "jpeg") "jpg" else it }
                pictureKey = "user/${session.userId}.$ext"
                storage.put(pictureKey, pictureBytes!!)
            }

            val dto = UpdateProfileDto(
                name = name,
                email = email,
                code = code,
                phone = phone.ifBlank { null },
                timezone = timezone,
                status = status,
                password = password.ifBlank { null },
                passwordConfirm = passwordConfirmation.ifBlank { null },
                picture = pictureKey
            )
            val updated = profileService.update(session.userId, dto)

            val newSession = session.copy(userName = updated.name, userEmail = updated.email)
                .withFlash("success", "Update Profile Success.")
            call.sessions.set(newSession)
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
