package com.kotlinadmin.modules.home.services

import com.kotlinadmin.core.errors.NotFoundError
import com.kotlinadmin.core.errors.ValidationError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.Instant

/**
 * Implementasi katalog frontend template (paritas GoAdmin fetemplate.Service):
 * memo in-memory (TTL 6 jam) → GitHub tree API → cache disk `_catalog.json` →
 * kurasi 15 item. HTML per-slug di-cache di [cacheDir]; unduhan wajib lolos
 * [isValidFeSlug] (anti-SSRF) dan mengandung `</html>`.
 */
class FeCatalogService(
    private val remote: Boolean = true,
    private val cacheDir: String = "storage/fe/templates"
) : IFeCatalogService {

    private companion object {
        const val PER_PAGE = 12
        val CATALOG_TTL: Duration = Duration.ofHours(6)
        val TREE_TIMEOUT: Duration = Duration.ofSeconds(20)
        val HTML_TIMEOUT: Duration = Duration.ofSeconds(8)
        const val HTTP_OK = 200
        const val TREE_PREFIX = "landings/"
        const val TREE_SUFFIX = ".html"
    }

    private val http: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(4))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    private val memoLock = Mutex()
    private var memo: List<FeTemplateInfo>? = null
    private var memoAt: Instant = Instant.EPOCH

    private fun catalogFile() = File(cacheDir, "_catalog.json")
    private fun htmlFile(slug: String) = File(cacheDir, "$slug.html")

    override suspend fun categories(): List<String> =
        catalog().map { it.category }.distinct().sorted()

    override suspend fun paginate(
        qName: String?,
        qCategory: String?,
        page: Int,
        activeSlug: String
    ): FeCatalogPage {
        val filtered = catalog().filter { t ->
            (qName.isNullOrBlank() || t.name.contains(qName, true) || t.slug.contains(qName, true)) &&
                (qCategory.isNullOrBlank() || t.category == qCategory)
        }
        // Slug aktif di-pin ke urutan pertama agar selalu tampil di halaman 1.
        val pinned = filtered.filter { it.slug == activeSlug } + filtered.filter { it.slug != activeSlug }
        val lastPage = maxOf(1, (pinned.size + PER_PAGE - 1) / PER_PAGE)
        val cur = page.coerceIn(1, lastPage)
        val items = pinned.drop((cur - 1) * PER_PAGE).take(PER_PAGE)
        return FeCatalogPage(items, pinned.size, cur, lastPage, PER_PAGE)
    }

    override suspend fun previewHtml(slug: String): String {
        if (!isValidFeSlug(slug)) throw ValidationError("Invalid template slug")
        cachedHtml(slug)?.let { return it }
        val html = download(slug) ?: throw NotFoundError("Template $slug is not available")
        writeHtml(slug, html)
        return html
    }

    override suspend fun ensure(slug: String) {
        if (!isValidFeSlug(slug) || slug == DEFAULT_FE_TEMPLATE) return
        if (cachedHtml(slug) != null) return
        download(slug)?.let { writeHtml(slug, it) }
    }

    override suspend fun activeHtml(slug: String): String? = when {
        slug == DEFAULT_FE_TEMPLATE || slug.isBlank() || !isValidFeSlug(slug) -> null
        else -> cachedHtml(slug)
            ?: (download(slug) ?: throw NotFoundError("Template $slug is not available"))
                .also { writeHtml(slug, it) }
    }

    // ── Katalog: memo → live → disk → kurasi ────────────────────────────────

    private suspend fun catalog(): List<FeTemplateInfo> = memoLock.withLock {
        val cached = memo
        if (cached != null && Instant.now().isBefore(memoAt.plus(CATALOG_TTL))) return cached
        val live = if (remote) fetchTree() else null
        val result = live ?: readDiskCatalog() ?: curatedFeTemplates()
        if (live != null) writeDiskCatalog(live)
        memo = result
        memoAt = Instant.now()
        result
    }

    private suspend fun fetchTree(): List<FeTemplateInfo>? = runCatching {
        val body = httpGet(FE_TREE_URL, TREE_TIMEOUT) ?: return null
        Json.parseToJsonElement(body).jsonObject["tree"]?.jsonArray
            ?.mapNotNull { node ->
                val path = node.jsonObject["path"]?.jsonPrimitive?.content ?: return@mapNotNull null
                if (!path.startsWith(TREE_PREFIX) || !path.endsWith(TREE_SUFFIX)) return@mapNotNull null
                feTemplateFromSlug(path.removePrefix(TREE_PREFIX).removeSuffix(TREE_SUFFIX))
            }
            ?.sortedWith(compareBy({ it.category }, { it.name }))
            ?.takeIf { it.isNotEmpty() }
    }.getOrNull()

    private fun readDiskCatalog(): List<FeTemplateInfo>? = runCatching {
        val f = catalogFile()
        if (!f.isFile) return null
        Json.parseToJsonElement(f.readText()).jsonArray
            .mapNotNull { el -> feTemplateFromSlug(el.jsonObject["slug"]?.jsonPrimitive?.content ?: "") }
            .takeIf { it.isNotEmpty() }
    }.getOrNull()

    private fun writeDiskCatalog(list: List<FeTemplateInfo>) {
        runCatching {
            catalogFile().parentFile?.mkdirs()
            val json = list.joinToString(",", "[", "]") { """{"slug":"${it.slug}"}""" }
            catalogFile().writeText(json)
        }
    }

    // ── HTML per-slug: cache disk + unduhan tervalidasi ─────────────────────

    private fun cachedHtml(slug: String): String? =
        htmlFile(slug).takeIf { it.isFile }
            ?.let { f -> runCatching { f.readText() }.getOrNull() }
            ?.takeIf { it.contains("</html>") }

    private fun writeHtml(slug: String, html: String) {
        runCatching {
            htmlFile(slug).parentFile?.mkdirs()
            htmlFile(slug).writeText(html)
        }
    }

    private suspend fun download(slug: String): String? =
        if (!remote) {
            null
        } else {
            httpGet("$FE_RAW_BASE_URL/$slug.html", HTML_TIMEOUT)?.takeIf { it.contains("</html>") }
        }

    private suspend fun httpGet(url: String, timeout: Duration): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(timeout)
                    .header("User-Agent", "KotlinAdmin/0.1")
                    .GET()
                    .build()
                val res = http.send(req, HttpResponse.BodyHandlers.ofString())
                if (res.statusCode() == HTTP_OK) res.body() else null
            }.getOrNull()
        }
}
