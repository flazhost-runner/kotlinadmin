package com.kotlinadmin.modules.home.services

// Konstanta + parsing frontend template (paritas NodeAdmin src/config/feTemplates.ts /
// GoAdmin internal/modules/home/fetemplate). Katalog live (~640 landing opentailwind)
// di-fetch FeCatalogService; file ini = fallback kurasi + validasi slug (anti-SSRF)
// + slug default yang di-bundle.

/** Slug template yang di-bundle & dirender view FreeMarker native (home/index.ftl). */
const val DEFAULT_FE_TEMPLATE = "agency-consulting-002-creative-agency"

/** Sumber HTML mentah tiap template (raw GitHub, branch master, subpath landings). */
const val FE_RAW_BASE_URL =
    "https://raw.githubusercontent.com/lindoai/opentailwind/master/landings"

/** GitHub tree API untuk enumerasi katalog penuh. */
const val FE_TREE_URL =
    "https://api.github.com/repos/lindoai/opentailwind/git/trees/master?recursive=1"

/** Pola slug ketat — satu-satunya gerbang sebelum fetch apa pun (anti-SSRF). */
val FE_SLUG_REGEX = Regex("^([a-z]+(?:-[a-z]+)*)-([0-9]{3})-([a-z0-9-]+)$")

/** Metadata satu template katalog, diturunkan dari slug-nya. */
data class FeTemplateInfo(
    val slug: String,
    val name: String,
    val category: String,
    val number: String
) {
    fun toMap(): Map<String, String> = mapOf(
        "slug" to slug,
        "name" to name,
        "category" to category,
        "number" to number
    )
}

fun isValidFeSlug(slug: String): Boolean = FE_SLUG_REGEX.matches(slug)

/** Derivasi metadata dari slug (`agency-consulting-002-creative-agency` → dst). */
fun feTemplateFromSlug(slug: String): FeTemplateInfo? {
    val m = FE_SLUG_REGEX.matchEntire(slug) ?: return null
    val (category, number, name) = m.destructured
    return FeTemplateInfo(slug, titleize(name), titleize(category), number)
}

private fun titleize(kebab: String): String =
    kebab.split('-').filter { it.isNotBlank() }
        .joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } }

/**
 * Katalog kurasi fallback — 15 slug, IDENTIK dengan FE_TEMPLATES NodeAdmin /
 * `curated` GoAdmin (default di urutan pertama). Dipakai saat fetch live gagal/offline.
 */
fun curatedFeTemplates(): List<FeTemplateInfo> = listOf(
    DEFAULT_FE_TEMPLATE,
    "agency-consulting-001-digital-marketing-agency",
    "technology-saas-001-hero-focused-conversion-page",
    "technology-saas-002-feature-rich-multi-section",
    "ecommerce-retail-001-fashion-boutique",
    "ecommerce-retail-002-luxury-fashion-brand",
    "portfolio-creative-001-creative-portfolio",
    "portfolio-creative-002-minimal-portfolio",
    "professional-services-001-law-firm",
    "real-estate-property-001-real-estate-agency",
    "food-hospitality-001-fine-dining-restaurant",
    "healthcare-wellness-001-family-doctor-clinic",
    "education-training-001-private-school",
    "fitness-sports-001-fitness-center",
    "travel-tourism-001-travel-agency"
).mapNotNull { feTemplateFromSlug(it) }
