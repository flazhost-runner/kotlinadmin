package com.kotlinadmin.modules.dashboard.routes

import com.kotlinadmin.core.helpers.respondView
import com.kotlinadmin.core.routing.namedGet
import com.kotlinadmin.core.routing.requireAuthenticated
import com.kotlinadmin.modules.dashboard.services.IDashboardService
import io.ktor.server.application.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.get

fun Application.dashboardModule() {
    val dashboardService = get<IDashboardService>()

    routing {
        namedGet("admin.v1.dashboard.index", "/admin/v1/dashboard") {
            call.requireAuthenticated()
            val stats = dashboardService.getStats()
            call.respondView(
                "dashboard/index.ftl",
                mapOf(
                    "stats" to stats,
                    "page_title" to "Dashboard"
                )
            )
        }
    }
}
