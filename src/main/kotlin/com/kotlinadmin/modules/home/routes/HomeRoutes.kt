package com.kotlinadmin.modules.home.routes

import com.kotlinadmin.modules.home.services.IHomeService
import io.ktor.server.application.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.get

// Public home routes are registered in AuthRoutes (web.home.root + web.home.index).
// This module wires additional home services if needed.
fun Application.homeModule() {
    @Suppress("UNUSED_VARIABLE")
    val homeService = get<IHomeService>()
    // Additional routes can be added here
    routing { }
}
