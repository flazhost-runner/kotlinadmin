package com.kotlinadmin.modules.home.routes

import com.kotlinadmin.core.helpers.respondView
import com.kotlinadmin.core.routing.namedGet
import com.kotlinadmin.modules.home.services.IFeCatalogService
import com.kotlinadmin.modules.home.services.IHomeService
import io.ktor.http.ContentType
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respondText
import io.ktor.server.routing.routing
import org.koin.ktor.ext.get

// Landing publik. Slug default (bundled) → view FreeMarker native home/index.ftl
// (landing v6); slug lain → HTML mentah hasil unduhan katalog. Unduhan gagal →
// fallback ke view native agar landing tidak pernah error (paritas GoAdmin).
fun Application.homeModule() {
    val homeService = get<IHomeService>()
    val feCatalog = get<IFeCatalogService>()

    suspend fun ApplicationCall.respondLanding() {
        val slug = homeService.activeSlug()
        val html = runCatching { feCatalog.activeHtml(slug) }.getOrNull()
        if (html != null) {
            respondText(html, ContentType.Text.Html)
        } else {
            respondView("home/index.ftl", mapOf("landing" to homeService.landing()))
        }
    }

    routing {
        namedGet("web.home.root", "/") { call.respondLanding() }
        namedGet("web.home.index", "/home") { call.respondLanding() }
    }
}
