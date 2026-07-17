package app.aislespy.domain

import app.aislespy.domain.model.Product
import app.aislespy.domain.model.ProductCategory
import app.aislespy.domain.model.SourceDb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure JVM tests for [ValuesBadgesResolver] certification label mapping (ADR-017).
 */
class ValuesBadgesTest {

    // --- fair-trade ---

    @Test
    fun fairTrade_exactTags() {
        for (tag in listOf(
            "en:fair-trade",
            "en:fairtrade-international",
            "en:max-havelaar",
            "en:fairtrade",
        )) {
            val badges = badges(listOf(tag))
            assertEquals("tag $tag", 1, badges.size)
            assertEquals(ValuesBadgesResolver.ID_FAIR_TRADE, badges.single().id)
            assertEquals("Fair-trade", badges.single().label)
        }
    }

    // --- organic-certified ---

    @Test
    fun organic_exactTags() {
        for (tag in listOf(
            "en:organic",
            "en:eu-organic",
            "en:usda-organic",
            "en:ab-agriculture-biologique",
        )) {
            val badges = badges(listOf(tag))
            assertEquals("tag $tag", 1, badges.size)
            assertEquals(ValuesBadgesResolver.ID_ORGANIC, badges.single().id)
            assertEquals("Certified organic", badges.single().label)
        }
    }

    @Test
    fun organic_prefix_enOrganicDash() {
        val badges = badges(listOf("en:organic-farming", "en:organic-shop-brand"))
        assertEquals(1, badges.size)
        assertEquals(ValuesBadgesResolver.ID_ORGANIC, badges.single().id)
    }

    @Test
    fun organic_noFalsePositive_inorganic() {
        val badges = badges(listOf("en:inorganic", "en:not-organic"))
        assertTrue(badges.none { it.id == ValuesBadgesResolver.ID_ORGANIC })
    }

    // --- cruelty-free ---

    @Test
    fun crueltyFree_exactTags() {
        for (tag in listOf(
            "en:cruelty-free",
            "en:leaping-bunny",
            "en:not-tested-on-animals",
            "en:cruelty-free-international",
        )) {
            val badges = badges(listOf(tag))
            assertEquals("tag $tag", 1, badges.size)
            assertEquals(ValuesBadgesResolver.ID_CRUELTY_FREE, badges.single().id)
            assertEquals("Cruelty-free", badges.single().label)
        }
    }

    @Test
    fun crueltyFree_noFalsePositive_partialNotTested() {
        // Must not match loose "not-tested" or unrelated tags
        val badges = badges(listOf("en:not-tested", "en:tested-on-animals"))
        assertTrue(badges.none { it.id == ValuesBadgesResolver.ID_CRUELTY_FREE })
    }

    // --- rainforest-alliance ---

    @Test
    fun rainforestAlliance_exactTag() {
        val badges = badges(listOf("en:rainforest-alliance"))
        assertEquals(1, badges.size)
        assertEquals(ValuesBadgesResolver.ID_RAINFOREST, badges.single().id)
        assertEquals("Rainforest Alliance", badges.single().label)
    }

    // --- utz ---

    @Test
    fun utz_exactTag() {
        val badges = badges(listOf("en:utz-certified"))
        assertEquals(1, badges.size)
        assertEquals(ValuesBadgesResolver.ID_UTZ, badges.single().id)
        assertEquals("UTZ certified", badges.single().label)
    }

    // --- b-corp ---

    @Test
    fun bCorp_exactTags() {
        for (tag in listOf("en:b-corp", "en:certified-b-corporation")) {
            val badges = badges(listOf(tag))
            assertEquals("tag $tag", 1, badges.size)
            assertEquals(ValuesBadgesResolver.ID_B_CORP, badges.single().id)
            assertEquals("B Corp", badges.single().label)
        }
    }

    // --- order, uniqueness, case ---

    @Test
    fun stableOrder_asDocumented() {
        val badges = badges(
            listOf(
                "en:b-corp",
                "en:utz-certified",
                "en:rainforest-alliance",
                "en:cruelty-free",
                "en:organic",
                "en:fair-trade",
            ),
        )
        assertEquals(
            listOf(
                ValuesBadgesResolver.ID_FAIR_TRADE,
                ValuesBadgesResolver.ID_ORGANIC,
                ValuesBadgesResolver.ID_CRUELTY_FREE,
                ValuesBadgesResolver.ID_RAINFOREST,
                ValuesBadgesResolver.ID_UTZ,
                ValuesBadgesResolver.ID_B_CORP,
            ),
            badges.map { it.id },
        )
    }

    @Test
    fun oneBadgePerId_duplicateTagsCollapsed() {
        val badges = badges(
            listOf("en:organic", "en:eu-organic", "en:usda-organic", "en:organic-farming"),
        )
        assertEquals(1, badges.size)
        assertEquals(ValuesBadgesResolver.ID_ORGANIC, badges.single().id)
    }

    @Test
    fun caseInsensitive_labelsTags() {
        val badges = badges(listOf("EN:FAIR-TRADE", "En:Organic"))
        assertEquals(
            listOf(
                ValuesBadgesResolver.ID_FAIR_TRADE,
                ValuesBadgesResolver.ID_ORGANIC,
            ),
            badges.map { it.id },
        )
    }

    @Test
    fun unrelatedTags_yieldEmpty() {
        val badges = badges(listOf("en:gluten-free", "en:palm-oil-free", "en:kosher"))
        assertTrue(badges.isEmpty())
    }

    @Test
    fun emptyLabels_yieldEmpty() {
        assertTrue(badges(emptyList()).isEmpty())
    }

    // --- helpers ---

    private fun badges(labels: List<String>): List<ValuesBadge> =
        ValuesBadgesResolver.from(
            Product(
                barcode = "0",
                name = "Test",
                brands = null,
                imageUrl = null,
                category = ProductCategory.Food,
                sourceDb = SourceDb.OpenFoodFacts,
                ingredientsText = null,
                ingredientsTags = emptyList(),
                additivesTags = emptyList(),
                allergensTags = emptyList(),
                labelsTags = labels,
                categoriesTags = emptyList(),
                ingredientsAnalysisTags = emptyList(),
                nutriscoreGrade = null,
                nutriscoreScore = null,
                novaGroup = null,
                nutriments = null,
            ),
        )
}
