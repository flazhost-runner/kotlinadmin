package com.kotlinadmin

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Semua respons API memakai amplop kanonik `{status, message, data}`.
 * Helper ini mengekstrak `data.token` dari respons login API.
 */
fun extractJwtToken(body: String): String? =
    Json.parseToJsonElement(body)
        .jsonObject["data"]
        ?.jsonObject?.get("token")
        ?.jsonPrimitive?.content
