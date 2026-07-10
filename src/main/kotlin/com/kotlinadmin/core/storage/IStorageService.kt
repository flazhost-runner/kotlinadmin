package com.kotlinadmin.core.storage

import java.io.File

/**
 * Abstraksi penyimpanan object — mirror desain FileService NodeAdmin.
 *
 * DB menyimpan **key** object (mis. `media/logo.webp`), URL render dibangun saat
 * request via [url]. Berpindah `STORAGE_DRIVER` (local ↔ oss/s3) cukup lewat
 * `.env` — tanpa ubah kode/view. Kontrak:
 *  - **local**: file di bawah `STORAGE_BASE_PATH`, disajikan pada prefix URL stabil
 *    `/storage/<key>` (route static didaftarkan hanya saat driver=local). Prefix
 *    URL dipisah dari path filesystem agar base path absolut (mis. `/data/uploads`
 *    di Docker) tetap menghasilkan URL valid.
 *  - **oss/s3**: URL absolut — S3 presigned (TTL) / OSS virtual-hosted; tanpa
 *    penyajian lokal.
 */
interface IStorageService {
    /** Simpan [bytes] pada [key]; buat direktori induk bila perlu (local). */
    suspend fun put(key: String, bytes: ByteArray)

    /** URL render untuk [key] sesuai driver aktif (local → `/storage/<key>`, oss/s3 → absolut). */
    fun url(key: String): String

    /** Daftar object di bawah [prefix]. */
    suspend fun list(prefix: String): List<StorageObject>

    /** Hapus object [key]. */
    suspend fun delete(key: String)

    /**
     * Pasangan (prefix URL, direktori) untuk `staticFiles` saat driver=local;
     * `null` untuk object storage (tidak ada penyajian lokal).
     */
    fun localMount(): Pair<String, File>?
}

/** Metadata object storage: [key] relatif + [size] byte (0 bila tak diketahui). */
data class StorageObject(val key: String, val size: Long = 0L)
