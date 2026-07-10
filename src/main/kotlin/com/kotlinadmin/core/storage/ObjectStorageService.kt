package com.kotlinadmin.core.storage

import com.kotlinadmin.config.StorageConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.MessageDigest
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

private const val HTTP_OK_MIN = 200
private const val HTTP_OK_MAX = 299
private fun Int.isHttpSuccess(): Boolean = this in HTTP_OK_MIN..HTTP_OK_MAX

private fun hmac(key: ByteArray, data: String): ByteArray =
    Mac.getInstance("HmacSHA256").apply { init(SecretKeySpec(key, "HmacSHA256")) }
        .doFinal(data.toByteArray())

private fun hmacSha1Base64(secret: String, data: String): String {
    val mac = Mac.getInstance("HmacSHA1").apply { init(SecretKeySpec(secret.toByteArray(), "HmacSHA1")) }
    return java.util.Base64.getEncoder().encodeToString(mac.doFinal(data.toByteArray()))
}

private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

private fun sha256Hex(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256").digest(bytes).toHex()

// RFC 3986: encode kecuali unreserved; '/' dipertahankan bila bukan [encodeSlash].
private fun uriEncode(input: String, encodeSlash: Boolean): String = buildString {
    for (b in input.toByteArray()) {
        val c = b.toInt().toChar()
        when {
            c in 'A'..'Z' || c in 'a'..'z' || c in '0'..'9' || c == '_' || c == '-' || c == '~' || c == '.' ->
                append(c)
            c == '/' && !encodeSlash -> append(c)
            else -> append("%%%02X".format(b))
        }
    }
}

/**
 * Driver object storage: AWS S3 / S3-compatible (MinIO, R2, B2) dan Alibaba OSS.
 *
 * Mirror `storageClient.ts` + `fileService.ts` NodeAdmin, tanpa SDK eksternal —
 * signing dilakukan manual (S3 SigV4, OSS V1) di atas `java.net.http.HttpClient`
 * bawaan JDK. [url] mengembalikan URL absolut:
 *  - **s3**: presigned URL (SigV4, TTL 6 jam) — bucket tetap privat.
 *  - **oss**: URL publik virtual-hosted `https://<bucket>.<endpoint>/<key>`.
 *
 * Catatan: jalur tulis (put/delete/list) diverifikasi statis (kompilasi); butuh
 * kredensial cloud untuk uji runtime.
 */
class ObjectStorageService(private val cfg: StorageConfig) : IStorageService {

    private val http: HttpClient = HttpClient.newHttpClient()
    private val isS3 = cfg.driver == "s3"
    private val region = cfg.region.ifBlank { "us-east-1" }
    private val protocol = if (cfg.ssl) "https" else "http"

    // Path-style (host = endpoint, path = /bucket/key) bila endpoint diisi
    // (MinIO/R2/OSS-S3-compat); virtual-hosted (bucket.s3.region.amazonaws.com) bila kosong.
    private val pathStyle = cfg.endpoint.isNotBlank()
    private val host: String = when {
        !pathStyle -> "${cfg.bucket}.s3.$region.amazonaws.com"
        else -> cfg.endpoint.replace(Regex("^https?://"), "")
    }

    override fun url(key: String): String {
        if (cfg.accessKeyId.isBlank() || cfg.secretAccessKey.isBlank()) {
            return if (key.startsWith("/")) key else "/$key"
        }
        return if (isS3) s3PresignedUrl(key, TTL_SECONDS) else ossPublicUrl(key)
    }

    override suspend fun put(key: String, bytes: ByteArray) = withContext(Dispatchers.IO) {
        val req = if (isS3) {
            signedS3Request("PUT", key, bytes)
        } else {
            signedOssRequest("PUT", key, bytes)
        }
        val res = http.send(req, HttpResponse.BodyHandlers.ofString())
        check(res.statusCode().isHttpSuccess()) { "Storage PUT $key gagal: HTTP ${res.statusCode()}" }
        Unit
    }

    override suspend fun delete(key: String) = withContext(Dispatchers.IO) {
        val req = if (isS3) {
            signedS3Request("DELETE", key, ByteArray(0))
        } else {
            signedOssRequest("DELETE", key, ByteArray(0))
        }
        http.send(req, HttpResponse.BodyHandlers.discarding())
        Unit
    }

    override suspend fun list(prefix: String): List<StorageObject> = withContext(Dispatchers.IO) {
        val query = "list-type=2&max-keys=100&prefix=${uriEncode(prefix, false)}"
        val req = if (isS3) {
            signedS3Request("GET", "", ByteArray(0), query = query)
        } else {
            signedOssRequest("GET", "", ByteArray(0), query = "prefix=$prefix&max-keys=100")
        }
        val res = http.send(req, HttpResponse.BodyHandlers.ofString())
        if (!res.statusCode().isHttpSuccess()) return@withContext emptyList()
        KEY_REGEX.findAll(res.body())
            .map { it.groupValues[1] }
            .filter { it.isNotBlank() && it != prefix && !it.endsWith("/") }
            .map { StorageObject(key = it) }
            .toList()
    }

    override fun localMount(): Pair<String, java.io.File>? = null

    // ── S3 (AWS Signature V4) ────────────────────────────────────────────────

    private fun canonicalUri(key: String): String {
        val path = if (pathStyle) "/${cfg.bucket}/$key" else "/$key"
        return path.split("/").joinToString("/") { uriEncode(it, false) }
    }

    private fun s3PresignedUrl(key: String, ttlSeconds: Long): String {
        val now = ZonedDateTime.now(ZoneOffset.UTC)
        val date = now.format(DATE_FMT)
        val datetime = now.format(DATETIME_FMT)
        val credScope = "$date/$region/s3/aws4_request"
        val canonUri = canonicalUri(key)

        val qp = listOf(
            "X-Amz-Algorithm" to "AWS4-HMAC-SHA256",
            "X-Amz-Credential" to "${cfg.accessKeyId}/$credScope",
            "X-Amz-Date" to datetime,
            "X-Amz-Expires" to ttlSeconds.toString(),
            "X-Amz-SignedHeaders" to "host"
        ).sortedBy { it.first }
        val canonQs = qp.joinToString("&") { "${uriEncode(it.first, true)}=${uriEncode(it.second, true)}" }

        val canonRequest = listOf(
            "GET",
            canonUri,
            canonQs,
            "host:$host\n",
            "host",
            "UNSIGNED-PAYLOAD"
        ).joinToString("\n")
        val signature = sign(date, credScope, datetime, canonRequest)
        return "$protocol://$host$canonUri?$canonQs&X-Amz-Signature=$signature"
    }

    private fun signedS3Request(
        method: String,
        key: String,
        payload: ByteArray,
        query: String = ""
    ): HttpRequest {
        val now = ZonedDateTime.now(ZoneOffset.UTC)
        val date = now.format(DATE_FMT)
        val datetime = now.format(DATETIME_FMT)
        val credScope = "$date/$region/s3/aws4_request"
        val canonUri = canonicalUri(key)
        val payloadHash = sha256Hex(payload)

        val canonHeaders = "host:$host\nx-amz-content-sha256:$payloadHash\nx-amz-date:$datetime\n"
        val signedHeaders = "host;x-amz-content-sha256;x-amz-date"
        val canonRequest = listOf(method, canonUri, query, canonHeaders, signedHeaders, payloadHash).joinToString("\n")
        val signature = sign(date, credScope, datetime, canonRequest)
        val authorization = "AWS4-HMAC-SHA256 Credential=${cfg.accessKeyId}/$credScope, " +
            "SignedHeaders=$signedHeaders, Signature=$signature"

        val uri = URI("$protocol://$host$canonUri${if (query.isBlank()) "" else "?$query"}")
        val body = if (method == "GET" || method == "DELETE") {
            HttpRequest.BodyPublishers.noBody()
        } else {
            HttpRequest.BodyPublishers.ofByteArray(payload)
        }
        return HttpRequest.newBuilder(uri)
            .method(method, body)
            .header("x-amz-date", datetime)
            .header("x-amz-content-sha256", payloadHash)
            .header("Authorization", authorization)
            .build()
    }

    private fun sign(date: String, credScope: String, datetime: String, canonRequest: String): String {
        val stringToSign = listOf(
            "AWS4-HMAC-SHA256",
            datetime,
            credScope,
            sha256Hex(canonRequest.toByteArray())
        ).joinToString("\n")
        val kDate = hmac("AWS4${cfg.secretAccessKey}".toByteArray(), date)
        val kRegion = hmac(kDate, region)
        val kService = hmac(kRegion, "s3")
        val kSigning = hmac(kService, "aws4_request")
        return hmac(kSigning, stringToSign).toHex()
    }

    // ── OSS (Alibaba, signature V1) ──────────────────────────────────────────

    private fun ossPublicUrl(key: String): String =
        "$protocol://${cfg.bucket}.${cfg.endpoint.replace(Regex("^https?://"), "")}/$key"

    private fun signedOssRequest(
        method: String,
        key: String,
        payload: ByteArray,
        query: String = ""
    ): HttpRequest {
        val ossHost = "${cfg.bucket}.${cfg.endpoint.replace(Regex("^https?://"), "")}"
        val date = ZonedDateTime.now(ZoneOffset.UTC).format(RFC1123_FMT)
        val contentType = if (method == "PUT") "application/octet-stream" else ""
        val resource = "/${cfg.bucket}/$key"
        val stringToSign = listOf(method, "", contentType, date, resource).joinToString("\n")
        val signature = hmacSha1Base64(cfg.secretAccessKey, stringToSign)

        val uri = URI("$protocol://$ossHost/$key${if (query.isBlank()) "" else "?$query"}")
        val body = if (method == "GET" || method == "DELETE") {
            HttpRequest.BodyPublishers.noBody()
        } else {
            HttpRequest.BodyPublishers.ofByteArray(payload)
        }
        val builder = HttpRequest.newBuilder(uri)
            .method(method, body)
            .header("Date", date)
            .header("Authorization", "OSS ${cfg.accessKeyId}:$signature")
        if (contentType.isNotBlank()) builder.header("Content-Type", contentType)
        return builder.build()
    }

    companion object {
        private const val TTL_SECONDS = 3600L * 6
        private val DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd", Locale.US)
        private val DATETIME_FMT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'", Locale.US)
        private val RFC1123_FMT = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US)
        private val KEY_REGEX = Regex("<Key>(.*?)</Key>")
    }
}
