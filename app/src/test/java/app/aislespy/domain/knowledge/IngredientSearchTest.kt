package app.aislespy.domain.knowledge

import app.aislespy.data.knowledge.KnowledgePack
import app.aislespy.data.knowledge.KnowledgePackEntry
import app.aislespy.data.knowledge.KnowledgePackLoader
import app.aislespy.data.knowledge.KnowledgePackTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test

/**
 * Pure JVM tests for cross-pack ingredient lookup (ADR-023).
 * Uses real shipped packs plus small in-memory fixtures for ranking/dedupe/cap.
 */
class IngredientSearchTest {

    companion object {
        private lateinit var food: KnowledgePack
        private lateinit var beauty: KnowledgePack

        @JvmStatic
        @BeforeClass
        fun loadRealPacks() {
            food = KnowledgePackLoader.parse(KnowledgePackTest.readFoodPackJson())
            beauty = KnowledgePackLoader.parse(KnowledgePackTest.readBeautyPackJson())
        }
    }

    @Test
    fun blankQuery_returnsEmpty() {
        assertTrue(IngredientSearch.search("", food, beauty).isEmpty())
        assertTrue(IngredientSearch.search("   ", food, beauty).isEmpty())
        assertTrue(IngredientSearch.search("\t\n", food, beauty).isEmpty())
    }

    @Test
    fun nameHit_aspartame() {
        val hits = IngredientSearch.search("aspartame", food, beauty)
        assertTrue("expected aspartame / e951", hits.any { it.entry.id == "e951" })
        val hit = hits.first { it.entry.id == "e951" }
        assertEquals("food", hit.domain)
    }

    @Test
    fun aliasEnPrefix_parfum_hitsBeautyFragrance() {
        val hits = IngredientSearch.search("parfum", food, beauty)
        assertTrue(
            "expected fragrance via en:parfum alias, got ${hits.map { it.entry.id }}",
            hits.any { it.entry.id == "fragrance" && it.domain == "beauty" },
        )
    }

    @Test
    fun eNumber_e250_and_digitsOnly_250() {
        val withE = IngredientSearch.search("e250", food, beauty)
        val digits = IngredientSearch.search("250", food, beauty)
        assertTrue(withE.any { it.entry.id == "e250" && it.domain == "food" })
        assertTrue(digits.any { it.entry.id == "e250" && it.domain == "food" })
        // Case / punctuation normalization
        val upper = IngredientSearch.search("E250", food, beauty)
        assertTrue(upper.any { it.entry.id == "e250" })
    }

    @Test
    fun crossPack_foodAndBeautyTerms() {
        val nitrite = IngredientSearch.search("sodium nitrite", food, beauty)
        assertTrue(nitrite.any { it.domain == "food" && it.entry.id == "e250" })

        val parfum = IngredientSearch.search("parfum", food, beauty)
        assertTrue(parfum.any { it.domain == "beauty" && it.entry.id == "fragrance" })

        // Single query that can hit food (contains) without excluding beauty-only terms
        val wide = IngredientSearch.search("acid", food, beauty)
        val domains = wide.map { it.domain }.toSet()
        assertTrue(
            "expected at least one domain in acid search, got $domains size ${wide.size}",
            wide.isNotEmpty(),
        )
    }

    @Test
    fun dedupe_byDomainAndId() {
        val miniFood = pack(
            "food",
            entry("dup", "food", title = "Dup", names = listOf("dup"), severity = 3),
        )
        val miniBeauty = pack(
            "beauty",
            entry("dup", "beauty", title = "Dup", names = listOf("dup"), severity = 2),
        )
        val hits = IngredientSearch.search("dup", miniFood, miniBeauty)
        assertEquals(2, hits.size)
        assertEquals(setOf("food", "beauty"), hits.map { it.domain }.toSet())
        assertEquals(2, hits.map { "${it.domain}:${it.entry.id}" }.toSet().size)
    }

    @Test
    fun ranking_exactBeforeContains_severityTiebreak() {
        val miniFood = pack(
            "food",
            // Substring-only hit (title contains "nitro")
            entry(
                id = "contains-nitro",
                domain = "food",
                title = "Something nitro else",
                names = listOf("something nitro else"),
                severity = 5,
            ),
            // Exact title match, lower severity than the contains hit
            entry(
                id = "exact-nitro",
                domain = "food",
                title = "nitro",
                names = listOf("nitro"),
                severity = 2,
            ),
            // Exact name match, higher severity — should rank above other exact by severity
            entry(
                id = "exact-nitro-high",
                domain = "food",
                title = "Nitro High",
                names = listOf("nitro"),
                severity = 4,
            ),
        )
        val emptyBeauty = pack("beauty")
        val hits = IngredientSearch.search("nitro", miniFood, emptyBeauty)
        assertTrue(hits.size >= 3)
        // Exact tier first (both exact-name entries), then contains
        val exactIds = hits.takeWhile {
            it.entry.id == "exact-nitro" || it.entry.id == "exact-nitro-high"
        }.map { it.entry.id }
        assertEquals(
            listOf("exact-nitro-high", "exact-nitro"),
            exactIds,
        )
        assertEquals("contains-nitro", hits[2].entry.id)
    }

    @Test
    fun oneCharNoise_noFalsePositiveSubstring() {
        // "e" would otherwise prefix/contain every E-number; must not flood results
        val hits = IngredientSearch.search("e", food, beauty)
        assertTrue(
            "1-char noise should not return broad E-number list, got ${hits.size}",
            hits.isEmpty() || hits.all { fieldExactOrId(it, "e") },
        )
        // Definitely no mass E-number dump
        assertTrue(hits.size < 5)
    }

    @Test
    fun cap_respected() {
        // Build a pack with more than MAX_RESULTS entries that all contain "zz"
        val entries = (1..(IngredientSearch.MAX_RESULTS + 20)).map { i ->
            entry(
                id = "zz-item-$i",
                domain = "food",
                title = "ZZ Item $i",
                names = listOf("zz item $i"),
                severity = (i % 5) + 1,
            )
        }
        val bigFood = pack("food", *entries.toTypedArray())
        val hits = IngredientSearch.search("zz", bigFood, pack("beauty"))
        assertEquals(IngredientSearch.MAX_RESULTS, hits.size)
    }

    // --- helpers ---

    private fun fieldExactOrId(hit: IngredientHit, q: String): Boolean {
        val fields = IngredientSearch.searchableFields(hit.entry)
        return fields.any { it == q }
    }

    private fun pack(domain: String, vararg entries: KnowledgePackEntry): KnowledgePack =
        KnowledgePack(version = "test", domain = domain, entries = entries.toList())

    private fun entry(
        id: String,
        domain: String,
        title: String,
        names: List<String>,
        severity: Int,
        aliases: List<String> = emptyList(),
        categories: List<String> = emptyList(),
    ): KnowledgePackEntry = KnowledgePackEntry(
        id = id,
        names = names,
        aliases = aliases,
        domain = domain,
        severity = severity,
        categories = categories,
        title = title,
        why = "Test why text long enough for schema-ish checks in fixtures.",
        sources = listOf("test"),
    )
}
