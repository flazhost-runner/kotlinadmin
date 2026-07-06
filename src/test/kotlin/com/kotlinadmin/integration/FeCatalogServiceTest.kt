package com.kotlinadmin.integration

import com.kotlinadmin.core.errors.ValidationError
import com.kotlinadmin.modules.home.services.DEFAULT_FE_TEMPLATE
import com.kotlinadmin.modules.home.services.FeCatalogService
import com.kotlinadmin.modules.home.services.curatedFeTemplates
import com.kotlinadmin.modules.home.services.feTemplateFromSlug
import com.kotlinadmin.modules.home.services.isValidFeSlug
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import java.nio.file.Files

// Offline (remote=false): katalog jatuh ke kurasi 15 item — test deterministik tanpa jaringan.
class FeCatalogServiceTest : DescribeSpec({

    val cacheDir = Files.createTempDirectory("fe-cache-test").toString()
    fun service() = FeCatalogService(remote = false, cacheDir = cacheDir)

    describe("FeTemplates constants") {

        it("slug validation follows the canonical pattern (anti-SSRF)") {
            isValidFeSlug(DEFAULT_FE_TEMPLATE) shouldBe true
            isValidFeSlug("../etc/passwd") shouldBe false
            isValidFeSlug("no-number-here") shouldBe false
            isValidFeSlug("Agency-002-name") shouldBe false
        }

        it("metadata derives from the slug") {
            val t = feTemplateFromSlug(DEFAULT_FE_TEMPLATE)!!
            t.category shouldBe "Agency Consulting"
            t.number shouldBe "002"
            t.name shouldBe "Creative Agency"
        }

        it("curated fallback has the canonical 15 slugs, default first") {
            val c = curatedFeTemplates()
            c.size shouldBe 15
            c.first().slug shouldBe DEFAULT_FE_TEMPLATE
        }
    }

    describe("FeCatalogService (offline → curated fallback)") {

        it("paginate pins the active slug to page 1 and pages by 12") {
            val page = service().paginate(null, null, 1, "travel-tourism-001-travel-agency")
            page.total shouldBe 15
            page.lastPage shouldBe 2
            page.items.first().slug shouldBe "travel-tourism-001-travel-agency"
            page.items.size shouldBe 12
        }

        it("paginate filters by name and category") {
            val byName = service().paginate("law", null, 1, DEFAULT_FE_TEMPLATE)
            byName.items.map { it.slug } shouldContain "professional-services-001-law-firm"
            byName.total shouldBe 1

            val byCategory = service().paginate(null, "Travel Tourism", 1, DEFAULT_FE_TEMPLATE)
            byCategory.total shouldBe 1
        }

        it("categories are distinct and sorted") {
            val cats = service().categories()
            cats shouldBe cats.distinct().sorted()
            cats shouldContain "Agency Consulting"
        }

        it("previewHtml rejects an invalid slug (anti-SSRF)") {
            shouldThrow<ValidationError> { service().previewHtml("../../evil") }
        }

        it("activeHtml returns null for the bundled default slug") {
            service().activeHtml(DEFAULT_FE_TEMPLATE) shouldBe null
            service().activeHtml("") shouldBe null
        }
    }
})
