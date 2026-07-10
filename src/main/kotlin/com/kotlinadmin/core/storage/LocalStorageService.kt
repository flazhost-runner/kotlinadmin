package com.kotlinadmin.core.storage

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Driver filesystem lokal.
 *
 * File disimpan di bawah [basePath]; URL render memakai [urlPrefix] (default
 * `/storage`) yang **dipisah** dari path filesystem. Dengan begitu `basePath`
 * absolut (mis. `/data/uploads`) tetap menghasilkan URL `/storage/<key>` yang
 * valid — bukan `//data/uploads/<key>`. [localMount] memberi tahu Application
 * agar memasang `staticFiles(urlPrefix, baseDir)`.
 */
class LocalStorageService(
    basePath: String,
    private val urlPrefix: String = "/storage"
) : IStorageService {

    private val baseDir: File = File(basePath).let { dir ->
        if (dir.isAbsolute) dir else File(System.getProperty("user.dir"), basePath)
    }

    init {
        baseDir.mkdirs()
    }

    override suspend fun put(key: String, bytes: ByteArray) = withContext(Dispatchers.IO) {
        val dest = resolve(key)
        dest.parentFile?.mkdirs()
        dest.writeBytes(bytes)
    }

    override fun url(key: String): String =
        "$urlPrefix/${key.trimStart('/')}".replace(Regex("(?<!:)//+"), "/")

    override suspend fun list(prefix: String): List<StorageObject> = withContext(Dispatchers.IO) {
        val dir = resolve(prefix)
        if (!dir.exists() || !dir.isDirectory) return@withContext emptyList()
        dir.listFiles()
            ?.filter { it.isFile }
            ?.map { StorageObject(key = "${prefix.trimEnd('/')}/${it.name}", size = it.length()) }
            ?: emptyList()
    }

    override suspend fun delete(key: String) = withContext(Dispatchers.IO) {
        val dest = resolve(key)
        // Cegah path traversal: pastikan target masih di bawah baseDir.
        if (dest.canonicalPath.startsWith(baseDir.canonicalPath) && dest.exists()) {
            dest.delete()
        }
        Unit
    }

    override fun localMount(): Pair<String, File> = urlPrefix to baseDir

    private fun resolve(key: String): File = File(baseDir, key.trimStart('/'))
}
