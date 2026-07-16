package app.aislespy.domain.scoring

import app.aislespy.domain.ScoringConfig
import app.aislespy.domain.model.Confidence
import app.aislespy.domain.model.MatchedIngredient
import app.aislespy.domain.model.Product
import app.aislespy.domain.model.ProductCategory
import app.aislespy.domain.model.ScoreBand
import app.aislespy.domain.model.SourceDb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Golden tests for [BeautyScoreEngine] per docs/VERIFICATION.md § Beauty scoring
 * and docs/SCORING.md v1.0.0.
 *
 * Expected totals are hand-computed in comments; assert within ±1 of those values.
 */
class BeautyScoreEngineTest {

    private val engine = BeautyScoreEngine()

    // -------------------------------------------------------------------------
    // Hazard severity 4 at index 0 of 10 vs last index — position weight matters
    // -------------------------------------------------------------------------
    //
    // n=10, baseDeduction(4)=12
    // index 0: pw = 1.0 - 0.6*(0/9) = 1.0 → deduction 12.0 → hazards = 88
    // index 9: pw = 1.0 - 0.6*(9/9) = 0.4 → deduction 4.8 → round → hazards = 95
    // (round 100-4.8 = 95.2 → floor(95.2+0.5)? we use floor(sum+0.5) on sum of deductions)
    //
    // Allergens=100, Regulatory=100 (no restricted categories on this synthetic match)
    // weights 0.70 / 0.15 / 0.15 (sum 1.0)
    // total@0 = round(88*0.7 + 100*0.15 + 100*0.15) = round(61.6+15+15) = round(91.6) = 92
    // total@9 = round(95*0.7 + 15+15) = round(66.5+30) = round(96.5) = 97
    @Test
    fun hazardSeverity4_index0_vs_last_positionWeightChangesTotal() {
        val ingredients = (0 until 10).joinToString(", ") { "Ing$it" }
        val product = baseProduct(ingredientsText = ingredients)

        val atFirst = engine.score(
            product,
            listOf(
                match(
                    id = "sev4",
                    name = "Hazard",
                    severity = 4,
                    listIndex = 0,
                    categories = listOf("preservative"),
                ),
            ),
        )
        val atLast = engine.score(
            product,
            listOf(
                match(
                    id = "sev4",
                    name = "Hazard",
                    severity = 4,
                    listIndex = 9,
                    categories = listOf("preservative"),
                ),
            ),
        )

        val hFirst = atFirst.components.first { it.id == BeautyScoreEngine.ID_HAZARDS }.score
        val hLast = atLast.components.first { it.id == BeautyScoreEngine.ID_HAZARDS }.score
        assertEquals(88, hFirst)
        assertEquals(95, hLast)
        assertNotEquals(atFirst.total, atLast.total)
        assertEquals(92, atFirst.total)
        assertEquals(97, atLast.total)
        assertTrue(atFirst.total < atLast.total)
        assertEquals(Confidence.High, atFirst.confidence)
    }

    // -------------------------------------------------------------------------
    // Fragrance penalty applied once (−25 on allergens_fragrance)
    // -------------------------------------------------------------------------
    //
    // fragrance match only, order known n=3, listIndex 2
    // hazards: base 7 * pw(2,3)=1.0-0.6*(2/2)=0.4 → 2.8 → round 3 → hazards 97
    // allergens: 100-25 = 75
    // regulatory: 100
    // total = round(97*0.7 + 75*0.15 + 100*0.15)
    //       = round(67.9 + 11.25 + 15) = round(94.15) = 94
    @Test
    fun fragrancePenalty_appliedOnce() {
        val product = baseProduct(
            ingredientsText = "Aqua, Glycerin, Parfum",
        )
        val matches = listOf(
            match(
                id = "fragrance",
                name = "parfum",
                severity = 3,
                listIndex = 2,
                categories = listOf("fragrance", "allergen-risk"),
            ),
        )
        val result = engine.score(product, matches)
        val allergens = result.components.first {
            it.id == BeautyScoreEngine.ID_ALLERGENS_FRAGRANCE
        }
        assertEquals(75, allergens.score)
        assertEquals(94, result.total)
    }

    // -------------------------------------------------------------------------
    // 3+ EU allergens: each −5, cap total allergen deduction at −40
    // -------------------------------------------------------------------------
    //
    // 8 allergen matches, no fragrance: allergenDeduction = min(40, 40) = 40
    // allergens sub = 60
    // hazards: 8 * severity1 * pw — use order unknown path for simpler hand math:
    // free-text single blob → order unknown, pw=0.7 each
    // each sev1 base 2 * 0.7 = 1.4; 8*1.4 = 11.2 → round 11 → hazards 89
    // regulatory 100
    // total = round(89*0.7 + 60*0.15 + 100*0.15)
    //       = round(62.3 + 9 + 15) = round(86.3) = 86
    //
    // Cap check: 3 allergens → deduction 15, not capped yet (allergens=85)
    @Test
    fun euAllergens_cappedAt40() {
        val productUnknown = baseProduct(
            ingredientsText = "complex free text blend without commas",
        )
        val eight = (1..8).map { i ->
            match(
                id = "allergen$i",
                name = "Allergen $i",
                severity = 1,
                listIndex = null,
                categories = listOf("allergen"),
            )
        }
        val capped = engine.score(productUnknown, eight)
        val allergensCapped = capped.components.first {
            it.id == BeautyScoreEngine.ID_ALLERGENS_FRAGRANCE
        }
        assertEquals(60, allergensCapped.score)

        val three = eight.take(3)
        val notCapped = engine.score(productUnknown, three)
        val allergensThree = notCapped.components.first {
            it.id == BeautyScoreEngine.ID_ALLERGENS_FRAGRANCE
        }
        assertEquals(85, allergensThree.score)
    }

    // -------------------------------------------------------------------------
    // Restricted severity 5 → regulatory −20
    // -------------------------------------------------------------------------
    //
    // MIT-like: sev 5, restricted, index 0 of 4
    // hazards: 18 * 1.0 = 18 → 82
    // allergens: not allergen category alone if we only use restricted+preservative
    //            (no fragrance, no allergen cat) → 100
    // regulatory: −20 → 80
    // total = round(82*0.7 + 100*0.15 + 80*0.15)
    //       = round(57.4 + 15 + 12) = round(84.4) = 84
    @Test
    fun restrictedSeverity5_regulatoryMinus20() {
        val product = baseProduct(
            ingredientsText = "Aqua, Glycerin, MIT, Salt",
        )
        val result = engine.score(
            product,
            listOf(
                match(
                    id = "methylisothiazolinone",
                    name = "methylisothiazolinone",
                    severity = 5,
                    listIndex = 2,
                    categories = listOf("preservative", "restricted"),
                ),
            ),
        )
        val reg = result.components.first { it.id == BeautyScoreEngine.ID_REGULATORY }
        assertEquals(80, reg.score)

        // n=4, i=2: pw = 1.0 - 0.6*(2/3) = 1.0 - 0.4 = 0.6
        // hazards: 18*0.6 = 10.8 → 11 → 89
        val hazards = result.components.first { it.id == BeautyScoreEngine.ID_HAZARDS }
        assertEquals(89, hazards.score)
        // total = round(89*0.7 + 100*0.15 + 80*0.15) = round(62.3+15+12)=round(89.3)=89
        assertEquals(89, result.total)
    }

    // -------------------------------------------------------------------------
    // No ingredients → hasIngredientData false (partial/no-score path for ViewModel)
    // Engine still callable but product has no data — confidence Low
    // -------------------------------------------------------------------------
    @Test
    fun noIngredientData_hasIngredientDataFalse() {
        val product = baseProduct(ingredientsText = null)
        assertTrue(!BeautyScoreEngine.hasIngredientData(product))
        val result = engine.score(product, emptyList())
        assertEquals(100, result.total)
        assertEquals(Confidence.Low, result.confidence)
    }

    // -------------------------------------------------------------------------
    // Free-text only / order unknown → 0.7 weights + Medium confidence cap
    // -------------------------------------------------------------------------
    //
    // Single free-text blob (no commas) → orderKnown=false, pw=0.7
    // sev 4: 12*0.7=8.4 → 8 → hazards 92
    // allergens 100, reg 100
    // total = round(92*0.7 + 15+15) = round(64.4+30)=round(94.4)=94
    // confidence ≤ Medium
    @Test
    fun freeTextOrderUnknown_usesPointSevenWeights_andMediumConfidence() {
        val product = baseProduct(
            ingredientsText = "various emollients and butylparaben compounds",
        )
        val result = engine.score(
            product,
            listOf(
                match(
                    id = "butylparaben",
                    name = "butylparaben",
                    severity = 4,
                    listIndex = null,
                    categories = listOf("preservative"),
                ),
            ),
        )
        assertEquals(0.7, engine.positionWeight(null, 1, orderKnown = false), 1e-9)
        val hazards = result.components.first { it.id == BeautyScoreEngine.ID_HAZARDS }
        assertEquals(92, hazards.score)
        assertEquals(94, result.total)
        assertEquals(Confidence.Medium, result.confidence)
        assertTrue(result.confidence != Confidence.High)
    }

    // -------------------------------------------------------------------------
    // Unknown ingredients: no matches → hazards 100, total 100
    // -------------------------------------------------------------------------
    @Test
    fun unknownIngredients_noPenalty_scoresOneHundredHazards() {
        // 5 unmatched INCI names — matcher would return empty; engine sees empty matches
        val product = baseProduct(
            ingredientsText = "Aqua, Glycerin, Caprylic Triglyceride, Tocopherol, Xanthan Gum",
        )
        val result = engine.score(product, emptyList())
        val hazards = result.components.first { it.id == BeautyScoreEngine.ID_HAZARDS }
        assertEquals(100, hazards.score)
        assertEquals(100, result.total)
        assertEquals(ScoreBand.Excellent, result.band)
        assertEquals(Confidence.High, result.confidence)
        assertTrue(result.concerns.isEmpty())
        assertEquals(ScoringConfig.METHODOLOGY_VERSION, result.methodologyVersion)
    }

    @Test
    fun positionWeight_endpoints() {
        // n=10: first 1.0, last 0.4
        assertEquals(1.0, engine.positionWeight(0, 10, orderKnown = true), 1e-9)
        assertEquals(0.4, engine.positionWeight(9, 10, orderKnown = true), 1e-9)
        assertEquals(0.7, engine.positionWeight(0, 10, orderKnown = false), 1e-9)
    }

    @Test
    fun restrictedSeverity4_orLower_regulatoryMinus12() {
        val product = baseProduct(ingredientsText = "Aqua, Butylparaben")
        val result = engine.score(
            product,
            listOf(
                match(
                    id = "butylparaben",
                    name = "butylparaben",
                    severity = 4,
                    listIndex = 1,
                    categories = listOf("preservative", "restricted"),
                ),
            ),
        )
        val reg = result.components.first { it.id == BeautyScoreEngine.ID_REGULATORY }
        assertEquals(88, reg.score)
    }

    @Test
    fun weightsSumToOne() {
        val product = baseProduct(ingredientsText = "Aqua, Glycerin, Oil")
        val result = engine.score(product, emptyList())
        val sum = result.components.sumOf { it.weight.toDouble() }
        assertTrue(abs(sum - 1.0) < 0.001)
    }

    // --- fixtures ---

    private fun baseProduct(
        ingredientsText: String? = null,
        ingredientsTags: List<String> = emptyList(),
        allergensTags: List<String> = emptyList(),
    ): Product = Product(
        barcode = "0000000000000",
        name = "Test beauty",
        brands = "Test",
        imageUrl = null,
        category = ProductCategory.Beauty,
        sourceDb = SourceDb.OpenBeautyFacts,
        ingredientsText = ingredientsText,
        ingredientsTags = ingredientsTags,
        additivesTags = emptyList(),
        allergensTags = allergensTags,
        labelsTags = emptyList(),
        categoriesTags = emptyList(),
        nutriscoreGrade = null,
        nutriscoreScore = null,
        novaGroup = null,
        nutriments = null,
    )

    private fun match(
        id: String,
        name: String,
        severity: Int,
        listIndex: Int? = null,
        categories: List<String> = emptyList(),
    ): MatchedIngredient = MatchedIngredient(
        entryId = id,
        displayName = name,
        severity = severity,
        why = "Why $name for testing purposes with enough length.",
        sources = listOf("https://example.com/$id"),
        matchedOn = "name:$name",
        listIndex = listIndex,
        categories = categories,
    )
}
