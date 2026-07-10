package com.kotlinadmin.core.helpers

import com.kotlinadmin.core.errors.THEMES
import com.kotlinadmin.core.errors.getTheme
import com.kotlinadmin.core.plugins.generateCsrfToken
import com.kotlinadmin.core.session.UserSession
import com.kotlinadmin.core.session.clearFlashAndErrors
import com.kotlinadmin.core.storage.IStorageService
import com.kotlinadmin.modules.access.models.Permissions
import com.kotlinadmin.modules.access.models.Roles
import com.kotlinadmin.modules.access.models.RolesPermissions
import com.kotlinadmin.modules.setting.models.SettingCache
import com.kotlinadmin.modules.setting.models.SettingData
import freemarker.template.TemplateMethodModelEx
import freemarker.template.TemplateScalarModel
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.freemarker.FreeMarkerContent
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.ktor.ext.getKoin

/**
 * URL render untuk key storage: local → `/storage/<key>`, oss/s3 → URL absolut.
 * URL `http(s)://` absolut (data lama) diteruskan apa adanya; kosong → "".
 */
private fun ApplicationCall.storageUrl(path: String): String =
    if (path.isBlank() || path.startsWith("http://") || path.startsWith("https://")) {
        path
    } else {
        application.getKoin().get<IStorageService>().url(path)
    }

suspend fun ApplicationCall.respondView(
    templatePath: String,
    extraModel: Map<String, Any?> = emptyMap()
) {
    val session = sessions.get<UserSession>()
    val setting = SettingCache.get()
    val theme = getTheme(setting.theme)

    val flash = session?.flash
    val errors = session?.errors ?: emptyMap()
    val old = session?.old ?: emptyMap()

    var csrfToken = session?.csrfToken
    if (csrfToken.isNullOrBlank()) {
        csrfToken = generateCsrfToken()
        session?.let { sessions.set(it.copy(csrfToken = csrfToken)) }
    }

    if (session != null && (flash != null || errors.isNotEmpty())) {
        sessions.set(session.clearFlashAndErrors().copy(csrfToken = csrfToken))
    }

    val userRoles = session?.roles ?: emptyList()

    val model = mutableMapOf<String, Any?>(
        "theme" to theme.toMap(),
        "themeName" to theme.name,
        "themes" to THEMES.map { it.toMap() },
        "setting" to setting.toViewMap(),
        "appName" to (setting.name ?: "KotlinAdmin").ifBlank { "KotlinAdmin" },
        "user" to session?.let {
            mapOf(
                "id" to it.userId,
                "name" to it.userName,
                "email" to it.userEmail,
                "roles" to it.roles
            )
        },
        "flash" to flash?.let { mapOf("key" to it.key, "message" to it.message) },
        "errors" to errors,
        "old" to old,
        "_csrf" to (csrfToken ?: ""),
        "hasRole" to TemplateMethodModelEx { args ->
            val roleName = (args?.getOrNull(0) as? TemplateScalarModel)?.asString
            roleName != null && userRoles.contains(roleName)
        },
        "hasAccess" to TemplateMethodModelEx { args ->
            val routeName = (args?.getOrNull(0) as? TemplateScalarModel)?.asString
            val method = (args?.getOrNull(1) as? TemplateScalarModel)?.asString?.uppercase() ?: "GET"
            when {
                routeName == null -> false
                userRoles.contains("Administrator") -> true
                else -> try {
                    transaction {
                        Roles.innerJoin(RolesPermissions).innerJoin(Permissions)
                            .selectAll()
                            .where {
                                (Roles.name inList userRoles) and
                                    (Permissions.name eq routeName) and
                                    (Permissions.method eq method)
                            }
                            .count() > 0
                    }
                } catch (_: Exception) { false }
            }
        },
        // getFile(key) → URL render sesuai driver storage aktif (lihat storageUrl).
        "getFile" to TemplateMethodModelEx { args ->
            storageUrl((args?.getOrNull(0) as? TemplateScalarModel)?.asString ?: "")
        }
    )
    model.putAll(extraModel)

    respond(FreeMarkerContent(templatePath, model))
}

suspend fun ApplicationCall.respondJson(
    status: HttpStatusCode = HttpStatusCode.OK,
    message: String = "Success",
    data: Any? = null
) {
    val response = buildJsonObject {
        put("status", true)
        put("message", message)
        put("data", anyToJsonElement(data))
    }
    respondText(Json.encodeToString(JsonObject.serializer(), response), ContentType.Application.Json, status)
}

suspend fun ApplicationCall.respondError(status: HttpStatusCode, message: String) {
    val response = buildJsonObject {
        put("status", false)
        put("message", message)
        put("data", JsonNull)
    }
    respondText(Json.encodeToString(JsonObject.serializer(), response), ContentType.Application.Json, status)
}

suspend fun ApplicationCall.respondRawHtml(html: String) {
    respondText(html, ContentType.Text.Html)
}

fun anyToJsonElement(value: Any?): JsonElement = when (value) {
    null -> JsonNull
    is JsonElement -> value
    is String -> JsonPrimitive(value)
    is Boolean -> JsonPrimitive(value)
    is Number -> JsonPrimitive(value)
    is Map<*, *> -> buildJsonObject {
        value.forEach { (k, v) -> put(k.toString(), anyToJsonElement(v)) }
    }
    is List<*> -> buildJsonArray { value.forEach { add(anyToJsonElement(it)) } }
    is Array<*> -> buildJsonArray { value.forEach { add(anyToJsonElement(it)) } }
    else -> JsonPrimitive(value.toString())
}

fun SettingData.toViewMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "initial" to initial,
    "name" to name,
    "description" to description,
    "icon" to icon,
    "favicon" to favicon,
    "logo" to logo,
    "login_image" to loginImage,
    "phone" to phone,
    "address" to address,
    "email" to email,
    "copyright" to copyright,
    "theme" to theme,
    "fe_template" to feTemplate
)
