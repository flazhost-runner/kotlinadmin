package com.kotlinadmin.modules.media.services

import com.kotlinadmin.core.storage.IStorageService

/**
 * Media library — menyimpan **key** object; URL render dibangun via
 * [IStorageService.url] sesuai driver aktif (local → `/storage/media/<name>`,
 * oss/s3 → URL absolut). Berpindah backend cukup lewat `.env`.
 */
class MediaService(private val storage: IStorageService) : IMediaService {

    override suspend fun list(): List<MediaFileInfo> =
        storage.list(PREFIX).map { obj ->
            val name = obj.key.removePrefix("$PREFIX/")
            MediaFileInfo(key = obj.key, url = storage.url(obj.key), name = name, size = obj.size)
        }

    override suspend fun upload(fileName: String, contentType: String, bytes: ByteArray): MediaFileInfo {
        val safeFileName = fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val uniqueName = "${System.currentTimeMillis()}_$safeFileName"
        val key = "$PREFIX/$uniqueName"
        storage.put(key, bytes)
        return MediaFileInfo(key = key, url = storage.url(key), name = uniqueName, size = bytes.size.toLong())
    }

    override suspend fun delete(key: String) {
        // Terima key penuh (`media/<name>`) maupun nama file saja (kompat lama).
        val normalized = if (key.startsWith("$PREFIX/")) key else "$PREFIX/${key.removePrefix("$PREFIX/")}"
        storage.delete(normalized)
    }

    private companion object {
        const val PREFIX = "media"
    }
}
