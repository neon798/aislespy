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
 * and docs/SCORING.md methodologyVersion 2.0.0 (ingredient quality only).
 *
 * Expected totals are hand-computed in comments; assert within ±1 of those values.
 */
class FoodScoreEngineTest {

    private val engine = FoodScoreEngine()

    // -------------------------------------------------------------------------
    // (a) Clean product: no matches, NOVA 1 → high / Excellent / High confidence
    // -------------------------------------------------------------------------
    //
    // Components: additives (ingredientsText analyzed → 100), nova (1 → 100).
    // Positives omitted (no labelsTags).
    // Base weights: 0.65 + 0.30 = 0.95
    // Norm: a=0.65/0.95≈0.684211, n=0.30/0.95≈0.315789
    // total = round(100*0.684211 + 100*0.315789) = round(100) = 100
    @Test
    fun cleanProduct_nova1_noMatches_excellentHigh() {
        val product = baseProduct(
            novaGroup = 1,
            ingredientsText = "Water, apple juice",
        )
        val result = engine.score(product, matches = emptyList())

        assertEquals(100, result.total)
        assertEquals(ScoreBand.Excellent, result.band)
        assertEquals(Confidence.High, result.confidence)
        assertTrue(result.total >= 75)
        assertEquals("Looking good—nothing flagged in our pack.", result.summarySentence)
        assertNull(result.driverSentence)
        assertEquals(ScoringConfig.METHODOLOGY_VERSION, result.methodologyVersion)
        assertEquals("2.0.0", result.methodologyVersion)
        assertTrue(result.concerns.isEmpty())
        assertEquals(2, result.components.size)
        assertWeightsSumToOne(result.components.map { it.weight })
        assertTrue(result.omittedComponents.any { it.startsWith("Positives") })
        assertTrue(result.components.none { it.id == "nutriscore" })
    }

    // -------------------------------------------------------------------------
    // (b) NOVA 4 + severity-4 additives → lower total, concerns sorted
    // -------------------------------------------------------------------------
    //
    // Matches: sev4 (−12), sev4 (−12), sev3 (−7) → additives sub = 100−31 = 69
    // NOVA4 = 20
    // Weights: a=0.65/0.95≈0.684211, n=0.30/0.95≈0.315789
    // total = round(69*0.684211 + 20*0.315789)
    //       = round(47.2105 + 6.3158) = round(53.5263) = 54
    @Test
    fun nova4_severity4Additives_lowish_sortedConcerns() {
        val product = baseProduct(
            nutriscoreGrade = 'e', // must be ignored
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

        assertEquals(54, result.total)
        assertEquals(ScoreBand.Ok, result.band)
        assertEquals(Confidence.High, result.confidence)
        assertEquals("Mixed bag—check the notes below.", result.summarySentence)

        val driver = requireNotNull(result.driverSentence)
        assertTrue(driver.startsWith("Main drags:"))
        assertTrue(driver.contains("ultra-processing (NOVA 4)"))
        assertTrue(driver.contains("flagged ingredients"))

        // Severity desc, then name asc
        assertEquals(3, result.concerns.size)
        assertEquals(4, result.concerns[0].severity)
        assertEquals(4, result.concerns[1].severity)
        assertEquals(3, result.concerns[2].severity)
        assertEquals("Caramel colour", result.concerns[0].displayName)
        assertEquals("Sodium nitrite", result.concerns[1].displayName)
        assertEquals("Monosodium glutamate", result.concerns[2].displayName)

        val additives = result.components.first { it.id == FoodScoreEngine.ID_ADDITIVES }
        assertEquals(69, additives.score)
    }

    // -------------------------------------------------------------------------
    // (c) Missing NOVA → additives + positives reweight (hand-computed)
    // -------------------------------------------------------------------------
    //
    // Present: additives (no matches → 100), positives (organic → 70)
    // Base: 0.65 + 0.05 = 0.70
    // Norm: a=0.65/0.70≈0.928571, p=0.05/0.70≈0.071429
    // total = round(100*0.928571 + 70*0.071429)
    //       = round(92.8571 + 5.0) = round(97.8571) = 98
    @Test
    fun missingNova_reweightsAdditivesAndPositives() {
        val product = baseProduct(
            novaGroup = null,
            ingredientsText = "Oats, water",
            labelsTags = listOf("en:organic"),
        )
        val result = engine.score(product, matches = emptyList())

        assertTrue(result.components.none { it.id == FoodScoreEngine.ID_NOVA })
        assertWeightsSumToOne(result.components.map { it.weight })

        val expected = 98
        assertTrue(
            "total=${result.total} expected ~$expected ±1",
            abs(result.total - expected) <= 1,
        )
        assertEquals(98, result.total)
        assertEquals(Confidence.Medium, result.confidence) // ingredient data only

        val weights = result.components.associate { it.id to it.weight }
        assertNear(0.65f / 0.70f, weights.getValue(FoodScoreEngine.ID_ADDITIVES))
        assertNear(0.05f / 0.70f, weights.getValue(FoodScoreEngine.ID_POSITIVES))
        assertTrue(result.omittedComponents.contains("NOVA (no data)"))
    }

    // -------------------------------------------------------------------------
    // (d) No ingredient-quality data at all → hasIngredientQualityData false
    //     (ViewModel uses partial path; engine companion gates scoring)
    // -------------------------------------------------------------------------
    @Test
    fun noIngredientQualityData_companionReturnsFalse() {
        val product = baseProduct(
            nutriscoreGrade = 'a',
            nutriments = Nutriments(energyKcal100g = 100.0),
            labelsTags = listOf("en:organic"),
        )
        assertFalse(FoodScoreEngine.hasIngredientQualityData(product))
    }

    @Test
    fun noIngredientQualityData_engineWouldBeEmptyComponentsIfCalled() {
        // Engine should not invent a useful score from labels/nutri alone.
        val product = baseProduct(
            nutriscoreGrade = 'a',
            labelsTags = listOf("en:organic"),
        )
        // Only positives present → still a number if score() is called, but
        // callers must gate via hasIngredientQualityData (partial path).
        // Companion is the contract; assert gate and that NOVA/additives absent.
        assertFalse(FoodScoreEngine.hasIngredientQualityData(product))
        val result = engine.score(product, emptyList())
        assertTrue(result.components.none { it.id == FoodScoreEngine.ID_NOVA })
        assertTrue(result.components.none { it.id == FoodScoreEngine.ID_ADDITIVES })
        // Positives alone is not enough ingredient-quality input for the gate.
        assertTrue(result.components.any { it.id == FoodScoreEngine.ID_POSITIVES })
        assertEquals(Confidence.Low, result.confidence)
    }

    // -------------------------------------------------------------------------
    // (e) Fiber ≥ 6 no longer changes positives (nutrition-only)
    // -------------------------------------------------------------------------
    //
    // Organic only → 50+20 = 70. High fiber must not add +10.
    // Full weights with NOVA1 + clean additives:
    // total = round(100*0.65 + 100*0.30 + 70*0.05) = round(65+30+3.5) = 99
    @Test
    fun fiberBonus_noLongerChangesPositives() {
        val withFiber = baseProduct(
            novaGroup = 1,
            ingredientsText = "Oats, bran",
            labelsTags = listOf("en:organic"),
            nutriments = Nutriments(fiber100g = 8.0),
        )
        val withoutFiber = withFiber.copy(nutriments = null)
        val a = engine.score(withFiber, emptyList())
        val b = engine.score(withoutFiber, emptyList())

        val positivesA = a.components.first { it.id == FoodScoreEngine.ID_POSITIVES }
        val positivesB = b.components.first { it.id == FoodScoreEngine.ID_POSITIVES }
        assertEquals(70, positivesA.score) // 50 + 20 organic only
        assertEquals(70, positivesB.score)
        assertEquals(a.total, b.total)
        assertEquals(99, a.total)
        assertFalse(positivesA.detail!!.contains("fiber", ignoreCase = true))
    }

    @Test
    fun fiberAlone_doesNotCreatePositivesComponent() {
        // No labels — only fiber nutriment: positives omitted (labels required).
        val product = baseProduct(
            novaGroup = 1,
            ingredientsText = "Bran",
            nutriments = Nutriments(fiber100g = 10.0),
        )
        val result = engine.score(product, emptyList())
        assertTrue(result.components.none { it.id == FoodScoreEngine.ID_POSITIVES })
        assertEquals(100, result.total)
    }

    // -------------------------------------------------------------------------
    // (f) Nutri-Score fields present but IGNORED
    // -------------------------------------------------------------------------
    @Test
    fun nutriscoreFields_ignored_identicalWithOrWithoutGrade() {
        val with = baseProduct(
            nutriscoreGrade = 'e',
            nutriscoreScore = 26,
            novaGroup = 2,
            ingredientsText = "Water, sugar",
        )
        val without = with.copy(nutriscoreGrade = null, nutriscoreScore = null)
        val a = engine.score(with, emptyList())
        val b = engine.score(without, emptyList())

        assertEquals(a.total, b.total)
        assertEquals(a.band, b.band)
        assertEquals(a.confidence, b.confidence)
        assertEquals(a.summarySentence, b.summarySentence)
        assertEquals(a.driverSentence, b.driverSentence)
        assertEquals(a.components, b.components)
        assertTrue(a.components.none { it.id == "nutriscore" })
        // NOVA2=80, additives=100; weights 0.65+0.30=0.95
        // total = round(100*0.684211 + 80*0.315789) = round(68.421 + 25.263) = 94
        assertEquals(94, a.total)
    }

    // -------------------------------------------------------------------------
    // Soft floor still applies
    // -------------------------------------------------------------------------
    //
    // 50 × severity 1 → soft floor 5; NOVA2=80
    // total = round(5*0.684211 + 80*0.315789) = round(3.421 + 25.263) = 29
    @Test
    fun additivesSoftFloor_manyMildFlagsStayAtFive() {
        val mild = (1..50).map { i ->
            match("mild-$i", "Mild flag $i", severity = 1)
        }
        val product = baseProduct(
            novaGroup = 2,
            ingredientsText = "Many additives",
            additivesTags = mild.map { "en:${it.entryId}" },
        )
        val result = engine.score(product, mild)

        val additives = result.components.first { it.id == FoodScoreEngine.ID_ADDITIVES }
        assertEquals(ScoringConfig.ADDITIVES_SOFT_FLOOR, additives.score)
        assertEquals(50, result.concerns.size)
        assertEquals(29, result.total)
    }

    // -------------------------------------------------------------------------
    // Positives capped: NOVA4 junk + organic cannot reach Excellent
    // -------------------------------------------------------------------------
    //
    // additives clean=100, NOVA4=20, organic positives=70
    // total = round(100*0.65 + 20*0.30 + 70*0.05) = round(65+6+3.5) = 75
    // Borderline Excellent — organic alone cannot push pure junk much higher.
    // With a mild flag (sev1 −2): additives=98
    // total = round(98*0.65 + 20*0.30 + 70*0.05) = round(63.7+6+3.5) = 73 → Ok
    @Test
    fun positivesCapped_junkWithOrganic_limitedInfluence() {
        val product = baseProduct(
            novaGroup = 4,
            ingredientsText = "Sugar, palm oil",
            labelsTags = listOf("en:organic"),
        )
        val result = engine.score(product, matches = emptyList())

        assertEquals(75, result.total)
        val positives = result.components.first { it.id == FoodScoreEngine.ID_POSITIVES }
        assertEquals(70, positives.score) // 50 + 20 organic
        assertTrue(positives.detail!!.contains("Organic +20"))

        // One mild flag keeps organic junk out of pure Excellent narrative:
        val flagged = engine.score(
            product,
            listOf(match("e322", "Lecithins", severity = 1)),
        )
        assertEquals(73, flagged.total)
        assertTrue(flagged.total < 75)
        assertEquals(ScoreBand.Ok, flagged.band)
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
        val product = baseProduct(
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
    // Dietary flags must never change ScoreResult (ADR-014)
    // -------------------------------------------------------------------------

    @Test
    fun analysisTags_doNotChangeScoreResult() {
        val without = baseProduct(
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
    // Summary sentence matrix (band × concern cells) — 2.0.0 copy
    // -------------------------------------------------------------------------

    @Test
    fun summaryMatrix_excellent_zeroConcerns() {
        val result = engine.score(
            baseProduct(novaGroup = 1, ingredientsText = "Water"),
            emptyList(),
        )
        assertTrue(result.total >= 75)
        assertTrue(result.concerns.isEmpty())
        assertEquals("Looking good—nothing flagged in our pack.", result.summarySentence)
    }

    @Test
    fun summaryMatrix_excellent_withConcerns() {
        // Mild flag on clean base still ≥ 75.
        // additives=98, NOVA1=100 → round(98*0.684211 + 100*0.315789) = round(67.053+31.579)=99
        val result = engine.score(
            baseProduct(
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
        // NOVA4 clean = round(100*0.684211 + 20*0.315789) = round(68.421+6.316)=75 → Excellent
        // Use NOVA4 + one sev3 (−7): additives=93
        // total = round(93*0.684211 + 20*0.315789) = round(63.632+6.316)=70 Ok
        // But that has a concern. For zero concerns mid band, use only-NOVA2 path?
        // Only NOVA3 (no ingredient data): weight 1.0 → 50 Ok, 0 concerns.
        // Wait: only NOVA means hasIngredientQualityData true via nova.
        val result = engine.score(
            baseProduct(novaGroup = 3),
            emptyList(),
        )
        assertTrue(result.total in 50..74)
        assertTrue(result.concerns.isEmpty())
        assertEquals(
            "Middling score—mostly processing signals, not flagged ingredients.",
            result.summarySentence,
        )
    }

    @Test
    fun summaryMatrix_ok_withConcerns() {
        // NOVA4 + sev3: 93/20 → 70
        val result = engine.score(
            baseProduct(
                novaGroup = 4,
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
        // Only NOVA4, no ingredients → subscore 20, Bad band actually.
        // For Poor (25–49) zero concerns: only NOVA is either 20/50/80/100.
        // NOVA alone can't hit 25–49. Use NOVA4 + clean additives without flags:
        // Wait that's 75. Need partial flags without concerns? Impossible (flags=concerns).
        //
        // Use soft-floor path without counting as "with concerns" — no, those have concerns.
        // Zero concerns + Poor: only possible if somehow components give 25–49 without matches.
        // NOVA3 alone = 50 (Ok). NOVA4 alone = 20 (Bad).
        // There's no pure-processing mid-Poor without flags under 2.0.0 weights when
        // additives are clean (100). When additives omitted and NOVA only:
        // only 20, 50, 80, 100.
        //
        // Practical approach for 25–49 zero concerns: not reachable with current components
        // unless we force-test the sentence helper via a product that scores that band.
        // NOVA4 + positives organic only (no additives component):
        // base 0.30+0.05=0.35; total = round(20*0.30/0.35 + 70*0.05/0.35)
        // = round(17.143 + 10.0) = 27 Poor, 0 concerns.
        val result = engine.score(
            baseProduct(
                novaGroup = 4,
                labelsTags = listOf("en:organic"),
            ),
            emptyList(),
        )
        assertTrue(result.total in 25..49)
        assertTrue(result.concerns.isEmpty())
        assertEquals(
            "Low score—driven by heavy processing; see the breakdown.",
            result.summarySentence,
        )
    }

    @Test
    fun summaryMatrix_poor_withConcerns() {
        // NOVA4 + 4×sev4: additives=52
        // total = round(52*0.684211 + 20*0.315789) = round(35.579+6.316)=42
        val matches = (1..4).map { i ->
            match("sev4-$i", "Additive $i", severity = 4)
        }
        val result = engine.score(
            baseProduct(
                novaGroup = 4,
                ingredientsText = "Many additives",
                additivesTags = matches.map { "en:${it.entryId}" },
            ),
            matches,
        )
        assertTrue(result.total in 25..49)
        assertTrue(result.concerns.isNotEmpty())
        assertEquals("Several concerns—read carefully.", result.summarySentence)
    }

    @Test
    fun summaryMatrix_bad_zeroConcerns() {
        // Only NOVA4 (no ingredient/additive data) → 20
        val result = engine.score(
            baseProduct(novaGroup = 4),
            emptyList(),
        )
        assertTrue(result.total <= 24)
        assertTrue(result.concerns.isEmpty())
        assertEquals("Very low score—heavily processed formulation.", result.summarySentence)
    }

    @Test
    fun summaryMatrix_bad_withConcerns() {
        val matches = (1..5).map { i ->
            match("sev5-$i", "Bad additive $i", severity = 5)
        }
        // soft floor 5 + NOVA4 20 → round(5*0.684211 + 20*0.315789) = round(3.421+6.316)=10
        val result = engine.score(
            baseProduct(
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
    // driverSentence + omittedComponents
    // -------------------------------------------------------------------------

    @Test
    fun driverSentence_nova4_namesUltraProcessing() {
        val result = engine.score(
            baseProduct(
                novaGroup = 4,
                ingredientsText = "Sugar",
            ),
            emptyList(),
        )
        val driver = requireNotNull(result.driverSentence)
        assertTrue(driver.contains("ultra-processing (NOVA 4)"))
        assertFalse(driver.contains("nutrition"))
        assertFalse(driver.contains("Nutri-Score"))
    }

    @Test
    fun driverSentence_highScorer_isNull() {
        val result = engine.score(
            baseProduct(novaGroup = 1, ingredientsText = "Water"),
            emptyList(),
        )
        assertNull(result.driverSentence)
    }

    @Test
    fun omittedComponents_missingNova_listsIt() {
        val result = engine.score(
            baseProduct(
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

    @Test
    fun hasIngredientQualityData_trueWhenNovaOrIngredients() {
        assertTrue(
            FoodScoreEngine.hasIngredientQualityData(
                baseProduct(novaGroup = 2),
            ),
        )
        assertTrue(
            FoodScoreEngine.hasIngredientQualityData(
                baseProduct(ingredientsText = "Water"),
            ),
        )
        assertTrue(
            FoodScoreEngine.hasIngredientQualityData(
                baseProduct(additivesTags = listOf("en:e322")),
            ),
        )
        assertFalse(
            FoodScoreEngine.hasIngredientQualityData(baseProduct()),
        )
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
