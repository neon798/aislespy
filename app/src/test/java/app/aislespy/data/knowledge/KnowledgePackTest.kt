package app.aislespy.data.knowledge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.BeforeClass
import org.junit.Test
import java.io.File

/**
 * T-300 / T-310: real food pack shape + KnowledgeMatcher behaviour.
 */
class KnowledgePackTest {

    companion object {
        private lateinit var pack: KnowledgePack

        @JvmStatic
        @BeforeClass
        fun loadRealPack() {
            pack = KnowledgePackLoader.parse(readFoodPackJson())
        }

        /** Read the shipped asset from disk (JVM unit test, no Android Context). */
        fun readFoodPackJson(): String {
            val candidates = listOf(
                File("src/main/assets/knowledge/food_additives_v1.json"),
                File("app/src/main/assets/knowledge/food_additives_v1.json"),
            )
            val file = candidates.firstOrNull { it.isFile }
                ?: error(
                    "food_additives_v1.json not found. Tried: " +
                        candidates.joinToString { it.absolutePath },
                )
            return file.readText()
        }
    }

    // --- pack load / schema-shape ---

    @Test
    fun pack_parsesFromRealAssetFile() {
        assertEquals("1.0.0", pack.version)
        assertEquals("food", pack.domain)
        assertTrue("expected >= 50 entries, got ${pack.entries.size}", pack.entries.size >= 50)
    }

    @Test
    fun pack_everyEntry_hasWhyAndSources() {
        for (entry in pack.entries) {
            assertTrue("empty why for ${entry.id}", entry.why.isNotBlank())
            assertTrue("why too short for ${entry.id}", entry.why.length >= 20)
            assertTrue("empty sources for ${entry.id}", entry.sources.isNotEmpty())
            assertTrue(
                "blank source for ${entry.id}",
                entry.sources.all { it.isNotBlank() },
            )
        }
    }

    @Test
    fun pack_schemaShape_severityDomainIds() {
        val ids = pack.entries.map { it.id }
        assertEquals("ids must be unique", ids.size, ids.toSet().size)
        for (entry in pack.entries) {
            assertEquals("domain food for ${entry.id}", "food", entry.domain)
            assertTrue(
                "severity 1..5 for ${entry.id}, was ${entry.severity}",
                entry.severity in 1..5,
            )
            assertTrue("non-empty names for ${entry.id}", entry.names.isNotEmpty())
            assertTrue("non-empty title for ${entry.id}", entry.title.isNotBlank())
            assertTrue(
                "id slug pattern for ${entry.id}",
                entry.id.matches(Regex("^[a-z0-9][a-z0-9_-]*$")),
            )
        }
    }

    @Test
    fun parse_invalidJson_failsFastWithClearMessage() {
        try {
            KnowledgePackLoader.parse("{ not json")
            fail("expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(
                "message should mention invalid knowledge pack JSON, was: ${e.message}",
                e.message?.contains("Invalid knowledge pack JSON", ignoreCase = true) == true,
            )
        }
    }

    // --- matcher ---

    @Test
    fun matcher_aliasTagHit_enE211() {
        val matches = KnowledgeMatcher.match(
            pack = pack,
            additivesTags = listOf("en:e211"),
        )
        val hit = matches.find { it.entryId == "e211" }
        assertNotNull("expected e211 alias match", hit)
        assertTrue(
            "matchedOn should record alias, was ${hit!!.matchedOn}",
            hit.matchedOn.startsWith("alias:"),
        )
        assertEquals(3, hit.severity)
        assertTrue(hit.why.isNotBlank())
        assertTrue(hit.sources.isNotEmpty())
    }

    @Test
    fun matcher_nameHit_fromIngredientsText_sodiumNitrite() {
        val matches = KnowledgeMatcher.match(
            pack = pack,
            ingredientsText = "Pork, water, salt, sodium nitrite, spices",
        )
        val hit = matches.find { it.entryId == "e250" }
        assertNotNull("expected sodium nitrite / e250 match", hit)
        assertTrue(
            "display should mention nitrite, was ${hit!!.displayName}",
            hit.displayName.contains("nitrite", ignoreCase = true) ||
                hit.displayName.contains("e250", ignoreCase = true),
        )
    }

    @Test
    fun matcher_noFalsePositive_shortTokenMsg_inMessage() {
        val matches = KnowledgeMatcher.match(
            pack = pack,
            ingredientsText = "This product carries a clear message for consumers",
        )
        assertFalse(
            "msg must not match inside 'message'",
            matches.any { it.entryId == "e621" },
        )
    }

    @Test
    fun matcher_longerNamePreference_onOverlap() {
        // Synthetic mini-pack: short "nitrite" vs longer "sodium nitrite" share list index 0
        val mini = KnowledgePack(
            version = "test",
            domain = "food",
            entries = listOf(
                KnowledgePackEntry(
                    id = "short-nitrite",
                    names = listOf("nitrite"),
                    domain = "food",
                    severity = 2,
                    title = "Nitrite",
                    why = "Short name used only to test longer-name preference on overlap.",
                    sources = listOf("test"),
                ),
                KnowledgePackEntry(
                    id = "long-sodium-nitrite",
                    names = listOf("sodium nitrite"),
                    domain = "food",
                    severity = 4,
                    title = "Sodium nitrite",
                    why = "Longer name used only to test longer-name preference on overlap.",
                    sources = listOf("test"),
                ),
            ),
        )
        val matches = KnowledgeMatcher.match(
            pack = mini,
            ingredientsText = "sodium nitrite",
        )
        assertEquals(1, matches.size)
        assertEquals("long-sodium-nitrite", matches.single().entryId)
    }

    @Test
    fun matcher_oneMatchPerEntry() {
        val matches = KnowledgeMatcher.match(
            pack = pack,
            additivesTags = listOf("en:e250", "en:sodium-nitrite"),
            ingredientsText = "Pork, sodium nitrite, E250, water",
        )
        val e250 = matches.filter { it.entryId == "e250" }
        assertEquals("entry id must match at most once", 1, e250.size)
    }

    @Test
    fun matcher_listIndex_recordedFromIngredientsText() {
        val matches = KnowledgeMatcher.match(
            pack = pack,
            ingredientsText = "Water, sugar, sodium benzoate, salt",
        )
        val hit = matches.find { it.entryId == "e211" }
        assertNotNull(hit)
        assertEquals(
            "sodium benzoate should be index 2 (0-based)",
            2,
            hit!!.listIndex,
        )
    }

    @Test
    fun matcher_normalize_stripsPunctuationKeepsHyphens() {
        assertEquals("sodium-benzoate", KnowledgeMatcher.normalize("  Sodium-Benzoate!  "))
        assertEquals("fdc yellow 5", KnowledgeMatcher.normalize("FD&C Yellow 5"))
        assertEquals("e-211", KnowledgeMatcher.normalize("E-211."))
    }

    @Test
    fun matcher_parseIngredientList_splitsCommasAndParentheses() {
        val parts = KnowledgeMatcher.parseIngredientList(
            "Water, sugar (sucrose), sodium nitrite",
        )
        assertEquals(listOf("Water", "sugar", "sucrose", "sodium nitrite"), parts)
    }
}
