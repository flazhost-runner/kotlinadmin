package com.kotlinadmin.modules.setting.routes

import com.kotlinadmin.core.helpers.respondJson
import com.kotlinadmin.core.helpers.respondView
import com.kotlinadmin.core.routing.*
import com.kotlinadmin.core.session.withFlash
import com.kotlinadmin.modules.setting.services.ISettingService
import com.kotlinadmin.modules.setting.services.UpdateSettingDto
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import org.koin.ktor.ext.get

fun Application.settingModule() {
    val settingService = get<ISettingService>()

    routing {
        namedGet("admin.v1.setting.index", "/admin/v1/setting") {
            val session = call.requireAuthenticated()
            call.checkAccess("admin.v1.setting.index", "GET")
            val setting = settingService.get()
            call.respondView(
                "setting/index.ftl",
                mapOf(
                    "setting_data" to setting,
                    "page_title" to "Setting"
                )
            )
        }

        namedPut("admin.v1.setting.update", "/admin/v1/setting/update") {
            val session = call.requireAuthenticated()
            call.checkAccess("admin.v1.setting.update", "PUT")
            val params = call.receiveParameters()

            val dto = UpdateSettingDto(
                initial = params["initial"],
                name = params["name"],
                description = params["description"],
                icon = params["icon"],
                logo = params["logo"],
                loginImage = params["login_image"],
                phone = params["phone"],
                address = params["address"],
                email = params["email"],
                copyright = params["copyright"],
                theme = params["theme"],
                feTemplate = params["fe_template"]
            )

            settingService.update(dto, session.userId)
            settingService.invalidateCache()
            call.sessions.set(session.withFlash("success", "Save Setting Success."))
            call.respondRedirect("/admin/v1/setting")
        }

        namedGet("admin.v1.setting.fe_preview", "/admin/v1/setting/fe-preview/{slug}") {
            val slug = call.parameters["slug"] ?: return@namedGet call.respond(HttpStatusCode.BadRequest)

            // Validate slug pattern (anti-SSRF)
            val slugPattern = Regex("^([a-z]+(?:-[a-z]+)*)-([0-9]{3})-([a-z0-9-]+)$")
            if (!slugPattern.matches(slug)) {
                call.respond(HttpStatusCode.BadRequest, "Invalid slug")
                return@namedGet
            }

            val html = settingService.previewTemplate(slug)
            call.respondText(html, ContentType.Text.Html)
        }
    }
}

fun Application.settingApiModule() {
    val settingService = get<ISettingService>()

    routing {
        authenticate("api") {
            namedGet("api.v1.setting.index", "/api/v1/setting") {
                val s = settingService.get()
                call.respondJson(
                    data = mapOf(
                        "id" to s.id,
                        "name" to (s.name ?: ""),
                        "theme" to s.theme,
                        "fe_template" to s.feTemplate
                    )
                )
            }
        }
    }
}
