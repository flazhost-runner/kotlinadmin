package com.kotlinadmin.modules.setting.routes

import com.kotlinadmin.core.helpers.respondJson
import com.kotlinadmin.core.helpers.respondView
import com.kotlinadmin.core.routing.*
import com.kotlinadmin.core.session.withFlash
import com.kotlinadmin.modules.home.services.DEFAULT_FE_TEMPLATE
import com.kotlinadmin.modules.home.services.IFeCatalogService
import com.kotlinadmin.modules.home.services.isValidFeSlug
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
    val feCatalog = get<IFeCatalogService>()

    routing {
        namedGet("admin.v1.setting.index", "/admin/v1/setting") {
            val session = call.requireAuthenticated()
            call.checkAccess("admin.v1.setting.index", "GET")
            val setting = settingService.get()
            // Katalog switcher: filter nama/kategori + pagination 12/halaman,
            // slug aktif di-pin ke halaman 1 (paritas NodeAdmin/GoAdmin).
            val feActive = setting.feTemplate.ifBlank { DEFAULT_FE_TEMPLATE }
            val fePage = call.request.queryParameters["fe_page"]?.toIntOrNull() ?: 1
            val feSearch = call.request.queryParameters["fe_search"]
            val feCategory = call.request.queryParameters["fe_category"]
            val catalogPage = feCatalog.paginate(feSearch, feCategory, fePage, feActive)
            call.respondView(
                "setting/index.ftl",
                mapOf(
                    "setting_data" to setting,
                    "page_title" to "Setting",
                    "fe_catalog" to catalogPage.items.map { it.toMap() },
                    "fe_total" to catalogPage.total,
                    "fe_page" to catalogPage.page,
                    "fe_last_page" to catalogPage.lastPage,
                    "fe_active" to feActive,
                    "fe_categories" to feCatalog.categories(),
                    "fe_search" to (feSearch ?: ""),
                    "fe_category" to (feCategory ?: "")
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
            // Unduh + cache template terpilih saat Save (best-effort — kegagalan
            // jaringan tidak menggagalkan penyimpanan; landing punya fallback).
            dto.feTemplate?.let { slug -> runCatching { feCatalog.ensure(slug) } }
            call.sessions.set(session.withFlash("success", "Save Setting Success."))
            call.respondRedirect("/admin/v1/setting")
        }

        namedGet("admin.v1.setting.fe_preview", "/admin/v1/setting/fe-preview/{slug}") {
            call.requireAuthenticated()
            val slug = call.parameters["slug"] ?: return@namedGet call.respond(HttpStatusCode.BadRequest)

            // Validasi pola slug (anti-SSRF) sebelum menyentuh katalog.
            if (!isValidFeSlug(slug)) {
                call.respond(HttpStatusCode.BadRequest, "Invalid slug")
                return@namedGet
            }

            val html = feCatalog.previewHtml(slug)
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
