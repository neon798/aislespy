package app.aislespy.domain.scoring

import app.aislespy.domain.ScoringConfig
import app.aislespy.domain.model.Confidence
import app.aislespy.domain.model.MatchedIngredient
import app.aislespy.domain.model.Nutriments
import app.aislespy.domain.model.Product
import app.aislespy.domain.model.ProductCategory
import app.aislespy.domain.model.ScoreBand
import app.aislespy.domain.model.SourceDb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Golden tests for [FoodScoreEngine] per docs/VERIFICATION.md § Food scoring
 * and docs/SCORING.md v1.0.0.
 *
 * Expected totals are hand-computed in comments; assert within ±1 of those values.
 */
class FoodScoreEngineTest {

    private val engine = FoodScoreEngine()

    // -------------------------------------------------------------------------
    // (a) Nutri A + NOVA 1 + no matches → high score, Excellent, High confidence
    // -------------------------------------------------------------------------
    //
    // Components present: nutriscore, nova, additives (ingredientsText analyzed).
    // Positives omitted (no labelsTags / nutriments).
    // Base weights: 0.45 + 0.25 + 0.25 = 0.95
    // Normalized: n=0.45/0.95≈0.473684, v=0.25/0.95≈0.263158, a=0.263158
    // Subscores: Nutri A=95, NOVA1=100, additives (no flags)=100
    // total = round(95*0.473684 + 100*0.263158 + 100*0.263158)
    //       = round(45.0 + 26.3158 + 26.3158) = round(97.6316) = 98
    @Test
    fun highQuality_nutriA_nova1_noMatches_excellentHigh() {
        val product = baseProduct(
            nutriscoreGrade = 'a',
            novaGroup = 1,
            ingredientsText = "Water, apple juice",
        )
        val result = engine.score(product, matches = emptyList())

        assertEquals(98, result.total)
        assertEquals(ScoreBand.Excellent, result.band)
        assertEquals(Confidence.High, result.confidence)
        assertTrue(result.total >= 75)
        assertEquals("Looking good—nothing flagged in our pack.", result.summarySentence)
        assertNull(result.driverSentence)
        assertEquals(ScoringConfig.METHODOLOGY_VERSION, result.methodologyVersion)
        assertTrue(result.concerns.isEmpty())
        assertEquals(3, result.components.size)
        assertWeightsSumToOne(result.components.map { it.weight })
        assertTrue(result.omittedComponents.any { it.startsWith("Positives") })
    }

    // -------------------------------------------------------------------------
    // (b) Nutri E + NOVA 4 + several severity 3–4 additives → low score, sorted
    // -------------------------------------------------------------------------
    //
    // Matches: sev4 (−12), sev4 (−12), sev3 (−7) → additives sub = 100−31 = 69
    // Weights same as (a): 0.45/0.95, 0.25/0.95, 0.25/0.95
    // Subscores: Nutri E=20, NOVA4=20, additives=69
    // total = round(20*0.473684 + 20*0.263158 + 69*0.263158)
    //       = round(9.4737 + 5.2632 + 18.1579) = round(32.8947) = 33
    @Test
    fun lowQuality_nutriE_nova4_flaggedAdditives_sortedConcerns() {
        val product = baseProduct(
            nutriscoreGrade = 'e',
            novaGroup = 4,
            ingredientsText = "Sugar, palm oil, E150d, E250, E621",
            additivesTags = listOf("en:e150d", "en:e250", "en:e621"),
        )
        val matches = listOf(
            match("e621", "Monosodium glutamate", severity = 3),
            match("e250", "Sodium nitrite", severity = 4),
            match("e150d", "Caramel colour", severity = 4),
        )
        val result = engine.score(product, matches)

        assertEquals(33, result.total)
        assertTrue(result.total <= 35)
        assertEquals(ScoreBand.Poor, result.band)
        assertEquals(Confidence.High, result.confidence)
        assertEquals("Several concerns—read carefully.", result.summarySentence)
        // Nutri E + NOVA 4 are both major weighted drags
        val driver = requireNotNull(result.driverSentence)
        assertTrue(driver.startsWith("Main drags:"))
        assertTrue(driver.contains("nutrition (Nutri-Score E)"))
        assertTrue(driver.contains("ultra-processing (NOVA 4)"))

        // Severity desc, then name asc
        assertEquals(3, result.concerns.size)
        assertEquals(4, result.concerns[0].severity)
        assertEquals(4, result.concerns[1].severity)
        assertEquals(3, result.concerns[2].severity)
        // Two severity-4: Caramel colour before Sodium nitrite alphabetically
        assertEquals("Caramel colour", result.concerns[0].displayName)
        assertEquals("Sodium nitrite", result.concerns[1].displayName)
        assertEquals("Monosodium glutamate", result.concerns[2].displayName)

        val additives = result.components.first { it.id == FoodScoreEngine.ID_ADDITIVES }
        assertEquals(69, additives.score)
    }

    // -------------------------------------------------------------------------
    // (c) Missing Nutri-Score → reweight; hand-computed total within ±1
    // -------------------------------------------------------------------------
    //
    // Present: nova (3→50), additives (no matches→100), positives (organic→70)
    // Base: 0.25 + 0.25 + 0.05 = 0.55
    // Norm: n=0.25/0.55≈0.454545, a=0.454545, p=0.05/0.55≈0.090909
    // total = round(50*0.454545 + 100*0.454545 + 70*0.090909)
    //       = round(22.727 + 45.455 + 6.364) = round(74.545) = 75
    @Test
    fun missingNutriScore_reweightsRemainingComponents() {
        val product = baseProduct(
            nutriscoreGrade = null,
            nutriscoreScore = null,
            novaGroup = 3,
            ingredientsText = "Oats, water",
            labelsTags = listOf("en:organic"),
        )
        val result = engine.score(product, matches = emptyList())

        assertTrue(result.components.none { it.id == FoodScoreEngine.ID_NUTRISCORE })
        assertWeightsSumToOne(result.components.map { it.weight })

        val expected = 75
        assertTrue(
            "total=${result.total} expected ~$expected ±1",
            abs(result.total - expected) <= 1,
        )
        assertEquals(Confidence.Medium, result.confidence) // only NOVA of the core pair

        val weights = result.components.associate { it.id to it.weight }
        assertNear(0.25f / 0.55f, weights.getValue(FoodScoreEngine.ID_NOVA))
        assertNear(0.25f / 0.55f, weights.getValue(FoodScoreEngine.ID_ADDITIVES))
        assertNear(0.05f / 0.55f, weights.getValue(FoodScoreEngine.ID_POSITIVES))
    }

    // -------------------------------------------------------------------------
    // (d) No data at all → Low confidence, total clamped ≥ 1
    // -------------------------------------------------------------------------
    @Test
    fun noData_lowConfidence_clampedAtLeastOne() {
        val product = baseProduct()
        val result = engine.score(product, matches = emptyList())

        assertEquals(Confidence.Low, result.confidence)
        assertTrue(result.total >= 1)
        assertEquals(ScoringConfig.SCORE_MIN, result.total)
        assertTrue(result.components.isEmpty())
        assertEquals(ScoreBand.Bad, result.band)
        // 0 concerns → do not imply flags exist
        assertEquals("Very low score—nutrition and processing look rough.", result.summarySentence)
        assertTrue(result.omittedComponents.contains("Nutri-Score (no data)"))
        assertTrue(result.omittedComponents.contains("NOVA (no data)"))
        assertTrue(result.omittedComponents.contains("Additives (no data)"))
        assertTrue(result.omittedComponents.contains("Positives (no data)"))
    }

    // -------------------------------------------------------------------------
    // (e) Additives soft floor: many mild flags cannot push subscore below 5
    // -------------------------------------------------------------------------
    //
    // 50 × severity 1 → raw deduction 50*2 = 100 → would be 0, soft floor → 5
    // Nutri C=60, NOVA 2=80, additives=5
    // Weights 0.95; total = round(60*0.473684 + 80*0.263158 + 5*0.263158)
    //                     = round(28.421 + 21.053 + 1.316) = round(50.789) = 51
    @Test
    fun additivesSoftFloor_manyMildFlagsStayAtFive() {
        val mild = (1..50).map { i ->
            match("mild-$i", "Mild flag $i", severity = 1)
        }
        val product = baseProduct(
            nutriscoreGrade = 'c',
            novaGroup = 2,
            ingredientsText = "Many additives",
            additivesTags = mild.map { "en:${it.entryId}" },
        )
        val result = engine.score(product, mild)

        val additives = result.components.first { it.id == FoodScoreEngine.ID_ADDITIVES }
        assertEquals(ScoringConfig.ADDITIVES_SOFT_FLOOR, additives.score)
        assertEquals(50, result.concerns.size)
        assertEquals(51, result.total)
    }

    // -------------------------------------------------------------------------
    // (f) Positives capped: junk + organic cannot reach Excellent
    // -------------------------------------------------------------------------
    //
    // Nutri E=20, NOVA4=20, additives clean=100, organic positives=70
    // Full weights: 0.45+0.25+0.25+0.05 = 1.0
    // total = round(20*0.45 + 20*0.25 + 100*0.25 + 70*0.05)
    //       = round(9 + 5 + 25 + 3.5) = round(42.5) = 43
    @Test
    fun positivesCapped_junkWithOrganic_notExcellent() {
        val product = baseProduct(
            nutriscoreGrade = 'e',
            novaGroup = 4,
            ingredientsText = "Sugar, palm oil",
            labelsTags = listOf("en:organic"),
        )
        val result = engine.score(product, matches = emptyList())

        assertEquals(43, result.total)
        assertTrue(result.total < 75)
        assertTrue(result.band != ScoreBand.Excellent)
        assertEquals(ScoreBand.Poor, result.band)

        val positives = result.components.first { it.id == FoodScoreEngine.ID_POSITIVES }
        assertEquals(70, positives.score) // 50 + 20 organic
    }

    // -------------------------------------------------------------------------
    // Extra: numeric Nutri-Score fallback + position hints
    // -------------------------------------------------------------------------

    @Test
    fun nutriscoreNumericFallback_whenGradeMissing() {
        // score=0 → clamp(100 - (0+15)*3, 1, 100) = 100-45 = 55
        val product = baseProduct(
            nutriscoreGrade = null,
            nutriscoreScore = 0,
            novaGroup = 1,
            ingredientsText = "Water",
        )
        val result = engine.score(product, emptyList())
        val nutri = result.components.first { it.id == FoodScoreEngine.ID_NUTRISCORE }
        assertEquals(55, nutri.score)
        assertTrue(nutri.detail!!.contains("numeric"))
    }

    @Test
    fun positionHint_firstThirdNearTop() {
        assertEquals(
            "Near top of ingredient list",
            engine.positionHint(listIndex = 0, listSize = 9),
        )
        assertEquals(
            "Middle of ingredient list",
            engine.positionHint(listIndex = 4, listSize = 9),
        )
        assertEquals(
            "Near end of ingredient list",
            engine.positionHint(listIndex = 8, listSize = 9),
        )
        assertEquals(null, engine.positionHint(null, 5))
    }

    @Test
    fun concernsIncludePositionHintFromListIndex() {
        // 3 ingredients → thirds of size 1; index 0 = top
        val product = baseProduct(
            nutriscoreGrade = 'c',
            novaGroup = 3,
            ingredientsText = "Sugar, Salt, Water",
        )
        val matches = listOf(
            match("sugar", "Sugar", severity = 2, listIndex = 0),
        )
        val result = engine.score(product, matches)
        assertEquals("Near top of ingredient list", result.concerns.single().positionHint)
    }

    // -------------------------------------------------------------------------
    // Dietary flags must never change ScoreResult (ADR-014 / methodology 1.0.1)
    // -------------------------------------------------------------------------

    @Test
    fun analysisTags_doNotChangeScoreResult() {
        // Only ingredientsAnalysisTags differ — dietary flags must not affect scoring.
        val without = baseProduct(
            nutriscoreGrade = 'e',
            novaGroup = 4,
            ingredientsText = "Sugar, palm oil, milk",
            additivesTags = listOf("en:e322"),
            ingredientsAnalysisTags = emptyList(),
        )
        val with = without.copy(
            ingredientsAnalysisTags = listOf("en:non-vegan", "en:vegetarian", "en:maybe-vegan"),
        )
        val matches = listOf(match("e322", "Lecithins", severity = 2))
        val a = engine.score(without, matches)
        val b = engine.score(with, matches)

        assertEquals(a.total, b.total)
        assertEquals(a.band, b.band)
        assertEquals(a.confidence, b.confidence)
        assertEquals(a.summarySentence, b.summarySentence)
        assertEquals(a.driverSentence, b.driverSentence)
        assertEquals(a.omittedComponents, b.omittedComponents)
        assertEquals(a.methodologyVersion, b.methodologyVersion)
        assertEquals(a.components, b.components)
        assertEquals(a.concerns, b.concerns)
        assertEquals(ScoringConfig.METHODOLOGY_VERSION, a.methodologyVersion)
    }

    // -------------------------------------------------------------------------
    // ADR-015 — summary sentence matrix (all 8 band × concern cells)
    // -------------------------------------------------------------------------

    @Test
    fun summaryMatrix_excellent_zeroConcerns() {
        val result = engine.score(
            baseProduct(nutriscoreGrade = 'a', novaGroup = 1, ingredientsText = "Water"),
            emptyList(),
        )
        assertTrue(result.total >= 75)
        assertTrue(result.concerns.isEmpty())
        assertEquals("Looking good—nothing flagged in our pack.", result.summarySentence)
    }

    @Test
    fun summaryMatrix_excellent_withConcerns() {
        // Mild flag keeps total ≥ 75; copy acknowledges minor flags.
        val result = engine.score(
            baseProduct(
                nutriscoreGrade = 'a',
                novaGroup = 1,
                ingredientsText = "Water, E322",
                additivesTags = listOf("en:e322"),
            ),
            listOf(match("e322", "Lecithins", severity = 1)),
        )
        assertTrue(result.total >= 75)
        assertTrue(result.concerns.isNotEmpty())
        assertEquals("Looking good—only minor flags below.", result.summarySentence)
    }

    @Test
    fun summaryMatrix_ok_zeroConcerns() {
        // Nutri C + NOVA 3 + clean additives ≈ 68
        val result = engine.score(
            baseProduct(nutriscoreGrade = 'c', novaGroup = 3, ingredientsText = "Oats, water"),
            emptyList(),
        )
        assertTrue(result.total in 50..74)
        assertTrue(result.concerns.isEmpty())
        assertEquals(
            "Middling score—mostly nutrition and processing, not flagged ingredients.",
            result.summarySentence,
        )
    }

    @Test
    fun summaryMatrix_ok_withConcerns() {
        val result = engine.score(
            baseProduct(
                nutriscoreGrade = 'c',
                novaGroup = 3,
                ingredientsText = "Oats, E621",
                additivesTags = listOf("en:e621"),
            ),
            listOf(match("e621", "MSG", severity = 3)),
        )
        assertTrue(result.total in 50..74)
        assertTrue(result.concerns.isNotEmpty())
        assertEquals("Mixed bag—check the notes below.", result.summarySentence)
    }

    @Test
    fun summaryMatrix_poor_zeroConcerns() {
        // Nutri E + NOVA 4 + clean additives ≈ 41
        val result = engine.score(
            baseProduct(
                nutriscoreGrade = 'e',
                novaGroup = 4,
                ingredientsText = "Sugar, palm oil",
            ),
            emptyList(),
        )
        assertTrue(result.total in 25..49)
        assertTrue(result.concerns.isEmpty())
        assertEquals(
            "Low score—driven by nutrition or processing; see the breakdown.",
            result.summarySentence,
        )
    }

    @Test
    fun summaryMatrix_poor_withConcerns() {
        val result = engine.score(
            baseProduct(
                nutriscoreGrade = 'e',
                novaGroup = 4,
                ingredientsText = "Sugar, E250",
                additivesTags = listOf("en:e250"),
            ),
            listOf(match("e250", "Sodium nitrite", severity = 4)),
        )
        assertTrue(result.total in 25..49)
        assertTrue(result.concerns.isNotEmpty())
        assertEquals("Several concerns—read carefully.", result.summarySentence)
    }

    @Test
    fun summaryMatrix_bad_zeroConcerns() {
        // Nutri E + NOVA 4 only (no additive input) → reweight ≈ 20
        val result = engine.score(
            baseProduct(nutriscoreGrade = 'e', novaGroup = 4),
            emptyList(),
        )
        assertTrue(result.total <= 24)
        assertTrue(result.concerns.isEmpty())
        assertEquals("Very low score—nutrition and processing look rough.", result.summarySentence)
    }

    @Test
    fun summaryMatrix_bad_withConcerns() {
        val matches = (1..5).map { i ->
            match("sev5-$i", "Bad additive $i", severity = 5)
        }
        val result = engine.score(
            baseProduct(
                nutriscoreGrade = 'e',
                novaGroup = 4,
                ingredientsText = "Many bad additives",
                additivesTags = matches.map { "en:${it.entryId}" },
            ),
            matches,
        )
        assertTrue(result.total <= 24)
        assertTrue(result.concerns.isNotEmpty())
        assertEquals("Lots of flags—you may want to skip.", result.summarySentence)
    }

    // -------------------------------------------------------------------------
    // ADR-015 — driverSentence + omittedComponents
    // -------------------------------------------------------------------------

    @Test
    fun driverSentence_nutriE_nova4_namesBoth() {
        val result = engine.score(
            baseProduct(
                nutriscoreGrade = 'e',
                novaGroup = 4,
                ingredientsText = "Sugar",
            ),
            emptyList(),
        )
        val driver = requireNotNull(result.driverSentence)
        assertTrue(driver.contains("nutrition (Nutri-Score E)"))
        assertTrue(driver.contains("ultra-processing (NOVA 4)"))
        // Nutrition loss is larger than NOVA at base weights → nutrition first
        assertTrue(
            driver.indexOf("nutrition") < driver.indexOf("ultra-processing"),
        )
    }

    @Test
    fun driverSentence_highScorer_isNull() {
        val result = engine.score(
            baseProduct(nutriscoreGrade = 'a', novaGroup = 1, ingredientsText = "Water"),
            emptyList(),
        )
        assertNull(result.driverSentence)
    }

    @Test
    fun omittedComponents_missingNova_listsIt() {
        val result = engine.score(
            baseProduct(
                nutriscoreGrade = 'a',
                novaGroup = null,
                ingredientsText = "Water",
            ),
            emptyList(),
        )
        assertTrue(result.components.none { it.id == FoodScoreEngine.ID_NOVA })
        assertTrue(result.omittedComponents.contains("NOVA (no data)"))
        assertFalse(result.omittedComponents.any { it.startsWith("Nutri-Score") })
    }

    @Test
    fun positivesDetail_organicAppendsBonusExplicitly() {
        val result = engine.score(
            baseProduct(
                nutriscoreGrade = 'c',
                novaGroup = 2,
                ingredientsText = "Oats",
                labelsTags = listOf("en:organic"),
            ),
            emptyList(),
        )
        val positives = result.components.first { it.id == FoodScoreEngine.ID_POSITIVES }
        assertEquals(70, positives.score)
        assertTrue(positives.detail!!.contains("Organic +20"))
    }

    // --- fixtures ---

    private fun baseProduct(
        nutriscoreGrade: Char? = null,
        nutriscoreScore: Int? = null,
        novaGroup: Int? = null,
        ingredientsText: String? = null,
        additivesTags: List<String> = emptyList(),
        ingredientsTags: List<String> = emptyList(),
        labelsTags: List<String> = emptyList(),
        ingredientsAnalysisTags: List<String> = emptyList(),
        allergensTags: List<String> = emptyList(),
        nutriments: Nutriments? = null,
    ): Product = Product(
        barcode = "0000000000000",
        name = "Test product",
        brands = "Test",
        imageUrl = null,
        category = ProductCategory.Food,
        sourceDb = SourceDb.OpenFoodFacts,
        ingredientsText = ingredientsText,
        ingredientsTags = ingredientsTags,
        additivesTags = additivesTags,
        allergensTags = allergensTags,
        labelsTags = labelsTags,
        categoriesTags = emptyList(),
        ingredientsAnalysisTags = ingredientsAnalysisTags,
        nutriscoreGrade = nutriscoreGrade,
        nutriscoreScore = nutriscoreScore,
        novaGroup = novaGroup,
        nutriments = nutriments,
    )

    private fun match(
        id: String,
        name: String,
        severity: Int,
        listIndex: Int? = null,
    ): MatchedIngredient = MatchedIngredient(
        entryId = id,
        displayName = name,
        severity = severity,
        why = "Why $name",
        sources = listOf("https://example.com/$id"),
        matchedOn = "alias:en:$id",
        listIndex = listIndex,
    )

    private fun assertWeightsSumToOne(weights: List<Float>) {
        val sum = weights.sum()
        assertTrue("weights sum=$sum", abs(sum - 1.0f) < 0.001f)
    }

    private fun assertNear(expected: Float, actual: Float, eps: Float = 0.001f) {
        assertTrue(
            "expected $expected got $actual",
            abs(expected - actual) < eps,
        )
    }
}
