package com.kotlinadmin.modules.media.routes

import com.kotlinadmin.core.helpers.respondError
import com.kotlinadmin.core.helpers.respondJson
import com.kotlinadmin.core.routing.namedGet
import com.kotlinadmin.core.routing.namedPost
import com.kotlinadmin.core.routing.requireAuthenticated
import com.kotlinadmin.modules.media.services.IMediaService
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.get

private const val MAX_SIZE = 2 * 1024 * 1024L // 2MB

fun Application.mediaModule() {
    val mediaService = get<IMediaService>()

    routing {
        // GET /admin/v1/media/list
        namedGet("admin.v1.media.list", "/admin/v1/media/list") {
            call.requireAuthenticated()
            val files = mediaService.list()
            call.respondJson(
                data = files.map { mapOf("key" to it.key, "url" to it.url, "name" to it.name, "size" to it.size) }
            )
        }

        // POST /admin/v1/media/upload — CSRF via x-csrf-token header or body
        namedPost("admin.v1.media.upload", "/admin/v1/media/upload") {
            call.requireAuthenticated()
            val multipart = call.receiveMultipart()
            var fileInfo: com.kotlinadmin.modules.media.services.MediaFileInfo? = null
            var errorMsg: String? = null

            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FileItem -> {
                        val contentType = part.contentType?.toString() ?: ""
                        val mimeBase = contentType.split(";").first().trim().lowercase()
                        if (!mimeBase.startsWith("image/")) {
                            errorMsg = "Only image files are allowed."
                        } else {
                            val bytes = part.streamProvider().readBytes()
                            if (bytes.size > MAX_SIZE) {
                                errorMsg = "File size exceeds 2MB limit."
                            } else {
                                val fileName = part.originalFileName ?: "upload"
                                fileInfo = mediaService.upload(fileName, contentType, bytes)
                            }
                        }
                    }
                    else -> {}
                }
                part.dispose()
            }

            if (errorMsg != null) {
                call.respondError(HttpStatusCode.UnprocessableEntity, errorMsg!!)
            } else if (fileInfo != null) {
                call.respondJson(
                    data = mapOf(
                        "key" to fileInfo!!.key,
                        "url" to fileInfo!!.url,
                        "name" to fileInfo!!.name
                    )
                )
            } else {
                call.respondError(HttpStatusCode.BadRequest, "No file uploaded.")
            }
        }

        // POST /admin/v1/media/delete — body: {key}
        namedPost("admin.v1.media.delete", "/admin/v1/media/delete") {
            call.requireAuthenticated()
            val params = call.receiveParameters()
            val key = params["key"]?.trim() ?: ""
            if (key.isBlank()) {
                call.respondError(HttpStatusCode.BadRequest, "Key is required.")
                return@namedPost
            }
            mediaService.delete(key)
            call.respondJson(message = "File deleted.")
        }
        // File disajikan oleh route static `/storage/<key>` (driver=local) atau
        // langsung dari URL absolut oss/s3 — lihat IStorageService.url().
    }
}
