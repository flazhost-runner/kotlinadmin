package com.kotlinadmin.modules.access.routes

import io.ktor.server.application.Application

fun Application.accessWebModule() {
    accessUserWebModule()
    accessRoleWebModule()
    accessPermissionWebModule()
}

fun Application.accessApiModule() {
    accessUserApiModule()
    accessRoleApiModule()
    accessPermissionApiModule()
}
