package com.kotlinadmin.modules.home.services

/** Satu halaman katalog untuk switcher di halaman Setting. */
data class FeCatalogPage(
    val items: List<FeTemplateInfo>,
    val total: Int,
    val page: Int,
    val lastPage: Int,
    val perPage: Int
)

/**
 * Katalog + cache frontend template (paritas NodeAdmin FeCatalog/FeTemplateService,
 * GoAdmin fetemplate.Service). Resolusi katalog: memo (TTL 6 jam) → GitHub tree →
 * cache disk `_catalog.json` → kurasi 15 item.
 */
interface IFeCatalogService {
    /** Daftar kategori unik (dropdown filter switcher). */
    suspend fun categories(): List<String>

    /** Halaman katalog: filter nama/kategori, 12/halaman, slug aktif di-pin ke depan. */
    suspend fun paginate(qName: String?, qCategory: String?, page: Int, activeSlug: String): FeCatalogPage

    /** HTML preview sebuah slug: cache disk → unduh (validasi anti-SSRF + `</html>`). */
    suspend fun previewHtml(slug: String): String

    /** Pastikan slug terunduh + ter-cache (dipanggil best-effort saat Save Setting). */
    suspend fun ensure(slug: String)

    /**
     * HTML landing untuk slug aktif: `null` untuk [DEFAULT_FE_TEMPLATE] (dirender view
     * native), selain itu HTML mentah hasil unduhan.
     */
    suspend fun activeHtml(slug: String): String?
}
