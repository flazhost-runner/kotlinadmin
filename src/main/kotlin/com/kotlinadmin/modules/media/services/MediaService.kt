package com.kotlinadmin.modules.media.services

import java.io.File

class MediaService(private val uploadDir: String = "uploads/media") : IMediaService {

    init {
        File(uploadDir).mkdirs()
    }

    override suspend fun list(): List<MediaFileInfo> {
        val dir = File(uploadDir)
        if (!dir.exists()) return emptyList()
        return dir.listFiles()
            ?.filter { it.isFile }
            ?.map { file ->
                MediaFileInfo(
                    key = "media/${file.name}",
                    url = "/admin/v1/media/file/${file.name}",
                    name = file.name,
                    size = file.length()
                )
            } ?: emptyList()
    }

    override suspend fun upload(fileName: String, contentType: String, bytes: ByteArray): MediaFileInfo {
        val safeFileName = fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val uniqueName = "${System.currentTimeMillis()}_$safeFileName"
        val dest = File("$uploadDir/$uniqueName")
        dest.writeBytes(bytes)
        return MediaFileInfo(
            key = "media/$uniqueName",
            url = "/admin/v1/media/file/$uniqueName",
            name = uniqueName,
            size = bytes.size.toLong()
        )
    }

    override suspend fun delete(key: String) {
        val fileName = key.removePrefix("media/")
        val file = File("$uploadDir/$fileName")
        if (file.exists() && file.canonicalPath.startsWith(File(uploadDir).canonicalPath)) {
            file.delete()
        }
    }
}
