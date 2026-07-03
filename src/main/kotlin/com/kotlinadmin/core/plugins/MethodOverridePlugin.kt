package com.kotlinadmin.core.plugins

import io.ktor.http.HttpMethod
import io.ktor.server.application.createApplicationPlugin
import io.ktor.util.AttributeKey

val EffectiveMethodKey = AttributeKey<HttpMethod>("EffectiveMethod")

val MethodOverridePlugin = createApplicationPlugin("MethodOverride") {
    onCall { call ->
        if (call.request.local.method == HttpMethod.Post) {
            val override = call.request.queryParameters["_method"]?.uppercase()
            if (override in setOf("PUT", "PATCH", "DELETE")) {
                call.attributes.put(EffectiveMethodKey, HttpMethod.parse(override!!))
            }
        }
    }
}
