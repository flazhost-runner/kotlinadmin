package com.kotlinadmin.modules.components.routes

import com.kotlinadmin.core.helpers.respondView
import com.kotlinadmin.core.routing.checkAccess
import com.kotlinadmin.core.routing.namedGet
import com.kotlinadmin.core.routing.requireAuthenticated
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.componentsModule() {
    routing {
        namedGet("admin.v1.components.index", "/admin/v1/components") {
            call.requireAuthenticated()
            call.checkAccess("admin.v1.components.index", "GET")
            call.respondView("components/index.ftl", mapOf("page_title" to "UI Components"))
        }
    }
}
