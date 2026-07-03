package com.kotlinadmin.modules.media.services

interface IMediaService {
    suspend fun list(): List<MediaFileInfo>
    suspend fun upload(fileName: String, contentType: String, bytes: ByteArray): MediaFileInfo
    suspend fun delete(key: String)
}

data class MediaFileInfo(
    val key: String,
    val url: String,
    val name: String,
    val size: Long = 0L
)
