package app.aislespy.domain

import app.aislespy.data.knowledge.KnowledgePackLoader
import app.aislespy.domain.model.Product
import app.aislespy.domain.model.ProductCategory
import app.aislespy.domain.model.SourceDb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.File

/**
 * Pure JVM tests for brand ownership resolver + pack integrity (ADR-019).
 */
class BrandOwnershipTest {

    companion object {
        private lateinit var shippedPack: BrandOwnershipPack

        @JvmStatic
        @BeforeClass
        fun loadShippedPack() {
            shippedPack = KnowledgePackLoader.parseBrandOwnership(readBrandOwnershipJson())
        }

        fun readBrandOwnershipJson(): String {
            val candidates = listOf(
                File("src/main/assets/knowledge/brand_ownership_v1.json"),
                File("app/src/main/assets/knowledge/brand_ownership_v1.json"),
            )
            val file = candidates.firstOrNull { it.isFile }
                ?: error(
                    "brand_ownership_v1.json not found. Tried: " +
                        candidates.joinToString { it.absolutePath },
                )
            return file.readText()
        }

        private fun miniPack(vararg entries: BrandOwnershipEntry): BrandOwnershipPack =
            BrandOwnershipPack(
                version = "1.0.0",
                domain = "brand_ownership",
                entries = entries.toList(),
            )
    }

    // --- resolver behaviour ---

    @Test
    fun conglomerate_alpro_ownedByDanone() {
        val pack = miniPack(
            BrandOwnershipEntry(
                id = "alpro",
                ownership = BrandOwnershipEntry.OWNERSHIP_CONGLOMERATE,
                brandAliases = listOf("en:alpro", "alpro"),
                parent = "danone",
                parentDisplay = "Danone",
                sources = listOf("https://en.wikipedia.org/wiki/Alpro"),
            ),
        )
        val result = BrandOwnershipResolver.resolve(
            product(brandsTags = listOf("en:alpro")),
            pack,
        )
        assertTrue(result is BrandOwnership.Corporate)
        val corporate = result as BrandOwnership.Corporate
        assertEquals("Danone", corporate.parentDisplay)
        assertEquals(listOf("https://en.wikipedia.org/wiki/Alpro"), corporate.sources)
    }

    @Test
    fun independent_goldStarStyleData() {
        val pack = miniPack(
            BrandOwnershipEntry(
                id = "dr-bronners",
                ownership = BrandOwnershipEntry.OWNERSHIP_INDEPENDENT,
                brandAliases = listOf("en:dr-bronner-s", "dr-bronners"),
                display = "Dr. Bronner's",
                note = "Family-owned",
                sources = listOf("https://www.drbronner.com/pages/about-us"),
            ),
        )
        val result = BrandOwnershipResolver.resolve(
            product(brandsTags = listOf("en:dr-bronner-s")),
            pack,
        )
        assertTrue(result is BrandOwnership.Independent)
        val independent = result as BrandOwnership.Independent
        assertEquals("Dr. Bronner's", independent.display)
        assertEquals("Family-owned", independent.note)
    }

    @Test
    fun unmatchedBrand_yieldsNull_noBadge() {
        val pack = miniPack(
            BrandOwnershipEntry(
                id = "alpro",
                ownership = BrandOwnershipEntry.OWNERSHIP_CONGLOMERATE,
                brandAliases = listOf("en:alpro"),
                parent = "danone",
                parentDisplay = "Danone",
                sources = listOf("https://example.com"),
            ),
        )
        assertNull(
            BrandOwnershipResolver.resolve(
                product(brandsTags = listOf("en:some-local-bakery")),
                pack,
            ),
        )
        assertNull(
            BrandOwnershipResolver.resolve(
                product(brandsTags = emptyList()),
                pack,
            ),
        )
    }

    @Test
    fun normalization_caseAndWhitespace() {
        val pack = miniPack(
            BrandOwnershipEntry(
                id = "alpro",
                ownership = BrandOwnershipEntry.OWNERSHIP_CONGLOMERATE,
                brandAliases = listOf("en:alpro"),
                parent = "danone",
                parentDisplay = "Danone",
                sources = listOf("https://example.com"),
            ),
        )
        val result = BrandOwnershipResolver.resolve(
            product(brandsTags = listOf("  EN:ALPRO  ")),
            pack,
        )
        assertTrue(result is BrandOwnership.Corporate)
        assertEquals("Danone", (result as BrandOwnership.Corporate).parentDisplay)
    }

    @Test
    fun noFalsePositive_unrelatedBrandTag() {
        val pack = miniPack(
            BrandOwnershipEntry(
                id = "dove",
                ownership = BrandOwnershipEntry.OWNERSHIP_CONGLOMERATE,
                brandAliases = listOf("en:dove", "dove"),
                parent = "unilever",
                parentDisplay = "Unilever",
                sources = listOf("https://example.com"),
            ),
        )
        // Must not match substring / partial tokens
        assertNull(
            BrandOwnershipResolver.resolve(
                product(brandsTags = listOf("en:dove-cottage", "en:undove")),
                pack,
            ),
        )
        assertNull(
            BrandOwnershipResolver.resolve(
                product(brandsTags = listOf("en:not-dove")),
                pack,
            ),
        )
    }

    @Test
    fun oneResultPerProduct() {
        val pack = miniPack(
            BrandOwnershipEntry(
                id = "nestle",
                ownership = BrandOwnershipEntry.OWNERSHIP_CONGLOMERATE,
                brandAliases = listOf("en:nestle"),
                parent = "nestle",
                parentDisplay = "Nestlé",
                sources = listOf("https://example.com/n"),
            ),
            BrandOwnershipEntry(
                id = "kitkat",
                ownership = BrandOwnershipEntry.OWNERSHIP_CONGLOMERATE,
                brandAliases = listOf("en:kitkat"),
                parent = "nestle",
                parentDisplay = "Nestlé",
                sources = listOf("https://example.com/k"),
            ),
        )
        // Multiple matching conglomerate tags still yield a single Corporate result
        val result = BrandOwnershipResolver.resolve(
            product(brandsTags = listOf("en:nestle", "en:kitkat")),
            pack,
        )
        assertTrue(result is BrandOwnership.Corporate)
        assertEquals("Nestlé", (result as BrandOwnership.Corporate).parentDisplay)
    }

    @Test
    fun dataConflict_bothLists_failSafeNull() {
        val pack = miniPack(
            BrandOwnershipEntry(
                id = "conflict-corp",
                ownership = BrandOwnershipEntry.OWNERSHIP_CONGLOMERATE,
                brandAliases = listOf("en:mystery-brand"),
                parent = "bigco",
                parentDisplay = "BigCo",
                sources = listOf("https://example.com/corp"),
            ),
            BrandOwnershipEntry(
                id = "conflict-indie",
                ownership = BrandOwnershipEntry.OWNERSHIP_INDEPENDENT,
                brandAliases = listOf("en:mystery-brand"),
                display = "Mystery Brand",
                note = "Should not win",
                sources = listOf("https://example.com/indie"),
            ),
        )
        assertNull(
            BrandOwnershipResolver.resolve(
                product(brandsTags = listOf("en:mystery-brand")),
                pack,
            ),
        )
    }

    @Test
    fun mostSpecific_longestAliasWins() {
        val pack = miniPack(
            BrandOwnershipEntry(
                id = "short",
                ownership = BrandOwnershipEntry.OWNERSHIP_CONGLOMERATE,
                brandAliases = listOf("en:mac"),
                parent = "wrong",
                parentDisplay = "Wrong Parent",
                sources = listOf("https://example.com/s"),
            ),
            BrandOwnershipEntry(
                id = "long",
                ownership = BrandOwnershipEntry.OWNERSHIP_CONGLOMERATE,
                brandAliases = listOf("en:mac-cosmetics"),
                parent = "estee-lauder",
                parentDisplay = "Estée Lauder",
                sources = listOf("https://example.com/l"),
            ),
        )
        val result = BrandOwnershipResolver.resolve(
            product(brandsTags = listOf("en:mac", "en:mac-cosmetics")),
            pack,
        )
        assertTrue(result is BrandOwnership.Corporate)
        assertEquals("Estée Lauder", (result as BrandOwnership.Corporate).parentDisplay)
    }

    // --- shipped pack integrity ---

    @Test
    fun pack_parses_idsUnique_sourcesPresent_ownershipValid() {
        assertEquals("1.0.0", shippedPack.version)
        assertEquals("brand_ownership", shippedPack.domain)
        assertTrue(
            "expected 40–100 entries, got ${shippedPack.entries.size}",
            shippedPack.entries.size in 40..100,
        )
        val ids = shippedPack.entries.map { it.id }
        assertEquals("ids must be unique", ids.size, ids.toSet().size)

        var conglomerate = 0
        var independent = 0
        for (entry in shippedPack.entries) {
            assertTrue(
                "ownership field invalid for ${entry.id}: ${entry.ownership}",
                entry.ownership == BrandOwnershipEntry.OWNERSHIP_CONGLOMERATE ||
                    entry.ownership == BrandOwnershipEntry.OWNERSHIP_INDEPENDENT,
            )
            assertTrue("empty sources for ${entry.id}", entry.sources.isNotEmpty())
            assertTrue("empty brandAliases for ${entry.id}", entry.brandAliases.isNotEmpty())
            when (entry.ownership) {
                BrandOwnershipEntry.OWNERSHIP_CONGLOMERATE -> {
                    conglomerate++
                    assertNotNull(entry.parent)
                    assertTrue(entry.parentDisplay.orEmpty().isNotBlank())
                }
                BrandOwnershipEntry.OWNERSHIP_INDEPENDENT -> {
                    independent++
                    assertTrue(entry.display.orEmpty().isNotBlank())
                }
            }
        }
        assertTrue("expected conglomerate entries", conglomerate >= 30)
        assertTrue("expected independent entries (10–20 range)", independent in 8..25)
    }

    @Test
    fun shippedPack_alproMapsToDanone() {
        val result = BrandOwnershipResolver.resolve(
            product(brandsTags = listOf("en:alpro")),
            shippedPack,
        )
        assertTrue(result is BrandOwnership.Corporate)
        assertEquals("Danone", (result as BrandOwnership.Corporate).parentDisplay)
    }

    @Test
    fun shippedPack_drBronnersIndependent() {
        val result = BrandOwnershipResolver.resolve(
            product(brandsTags = listOf("en:dr-bronner-s")),
            shippedPack,
        )
        assertTrue(result is BrandOwnership.Independent)
        assertEquals("Dr. Bronner's", (result as BrandOwnership.Independent).display)
    }

    // --- helpers ---

    private fun product(
        brandsTags: List<String>,
        category: ProductCategory = ProductCategory.Food,
    ): Product = Product(
        barcode = "0",
        name = "Test",
        brands = null,
        brandsTags = brandsTags,
        imageUrl = null,
        category = category,
        sourceDb = SourceDb.OpenFoodFacts,
        ingredientsText = null,
        ingredientsTags = emptyList(),
        additivesTags = emptyList(),
        allergensTags = emptyList(),
        labelsTags = emptyList(),
        categoriesTags = emptyList(),
        ingredientsAnalysisTags = emptyList(),
        nutriscoreGrade = null,
        nutriscoreScore = null,
        novaGroup = null,
        nutriments = null,
    )
}
