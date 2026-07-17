package app.aislespy.domain.scoring

import app.aislespy.domain.ScoringConfig
import app.aislespy.domain.model.Concern
import app.aislespy.domain.model.Confidence
import app.aislespy.domain.model.MatchedIngredient
import app.aislespy.domain.model.Product
import app.aislespy.domain.model.ScoreBand
import app.aislespy.domain.model.ScoreComponent
import app.aislespy.domain.model.ScoreResult

/**
 * Food scoring per docs/SCORING.md methodologyVersion [ScoringConfig.METHODOLOGY_VERSION].
 *
 * **Methodology 2.0.0 (ADR-018):** primary score is **ingredient quality only**.
 * Components (base weights): additives 0.65 / nova 0.30 / positives 0.05.
 * Nutri-Score and nutriments are not scored (display-only on the nutrition screen).
 *
 * ## No ingredient-quality data
 * If the product has no ingredients text/tags, no additives tags, and no NOVA,
 * callers should **not** invent a score — use the Partial UI path instead
 * (see [hasIngredientQualityData]), same pattern as [BeautyScoreEngine.hasIngredientData].
 *
 * Pure, side-effect free, JVM-testable. No Android imports.
 */
class FoodScoreEngine : ScoreEngine {

    override fun score(product: Product, matches: List<MatchedIngredient>): ScoreResult {
        val uniqueMatches = dedupeByEntryId(matches)
        val concerns = buildConcerns(uniqueMatches, listSize = ingredientListSize(product))

        val raw = mutableListOf<RawComponent>()

        additivesComponent(product, uniqueMatches)?.let { raw += it }
        novaComponent(product)?.let { raw += it }
        positivesComponent(product)?.let { raw += it }

        val components = reweight(raw)
        // Total from Double base weights (avoids float round-trip error).
        val total = computeTotalFromRaw(raw)
        val band = ScoreBand.fromTotal(total)
        val hasIngredientData = hasAdditiveInputData(product)
        val hasNova = raw.any { it.id == ID_NOVA }
        val confidence = foodConfidence(
            hasIngredientData = hasIngredientData,
            hasNova = hasNova,
        )
        val omitted = omittedFoodComponents(
            hasNova = hasNova,
            hasAdditives = raw.any { it.id == ID_ADDITIVES },
            hasPositives = raw.any { it.id == ID_POSITIVES },
        )

        return ScoreResult(
            total = total,
            band = band,
            confidence = confidence,
            components = components,
            concerns = concerns,
            methodologyVersion = ScoringConfig.METHODOLOGY_VERSION,
            summarySentence = summarySentence(total, concerns.size),
            driverSentence = ScoreExplanation.driverSentence(
                components,
                ScoreExplanation::foodDriverLabel,
            ),
            omittedComponents = omitted,
        )
    }

    // --- components ---

    private fun novaComponent(product: Product): RawComponent? {
        val group = product.novaGroup ?: return null
        if (group !in 1..4) return null
        val sub = when (group) {
            1 -> 100
            2 -> 80
            3 -> 50
            4 -> 20
            else -> return null
        }
        return RawComponent(
            id = ID_NOVA,
            label = "NOVA",
            score = sub,
            baseWeight = ScoringConfig.FoodWeights.NOVA,
            detail = "NOVA $group",
        )
    }

    /**
     * Present when ingredients/additives data was analyzed; omitted when no such data.
     * Subscore starts at 100; severity deductions once per unique entry id; soft floor 5.
     */
    private fun additivesComponent(
        product: Product,
        uniqueMatches: List<MatchedIngredient>,
    ): RawComponent? {
        if (!hasAdditiveInputData(product)) return null

        var sub = 100
        for (m in uniqueMatches) {
            sub -= ScoringConfig.severityDeduction(m.severity)
        }
        sub = maxOf(sub, ScoringConfig.ADDITIVES_SOFT_FLOOR)

        val detail = when {
            uniqueMatches.isEmpty() -> "No flagged additives"
            uniqueMatches.size == 1 -> "1 flagged additive"
            else -> "${uniqueMatches.size} flagged additives"
        }
        return RawComponent(
            id = ID_ADDITIVES,
            label = "Additives",
            score = sub,
            baseWeight = ScoringConfig.FoodWeights.ADDITIVES,
            detail = detail,
        )
    }

    /**
     * Included only when labelsTags are present (else omit at 5% weight).
     * Start 50; organic +20; fair-trade +10; clamp 1..100.
     * Fiber is nutrition-only and does not affect this component (ADR-018).
     */
    private fun positivesComponent(product: Product): RawComponent? {
        if (product.labelsTags.isEmpty()) return null

        var sub = 50
        val notes = mutableListOf<String>()

        if (hasOrganicLabel(product.labelsTags)) {
            sub += 20
            notes += "Organic +20"
        }
        if (hasFairTradeLabel(product.labelsTags)) {
            sub += 10
            notes += "Fair-trade"
        }

        sub = clampScore(sub)
        return RawComponent(
            id = ID_POSITIVES,
            label = "Positives",
            score = sub,
            baseWeight = ScoringConfig.FoodWeights.POSITIVES,
            detail = if (notes.isEmpty()) "Neutral" else notes.joinToString(", "),
        )
    }

    // --- reweight / total ---

    private fun reweight(raw: List<RawComponent>): List<ScoreComponent> {
        if (raw.isEmpty()) return emptyList()
        val sum = raw.sumOf { it.baseWeight }
        if (sum <= 0.0) return emptyList()
        return raw.map { c ->
            ScoreComponent(
                id = c.id,
                label = c.label,
                score = c.score,
                weight = (c.baseWeight / sum).toFloat(),
                detail = c.detail,
            )
        }
    }

    private fun computeTotalFromRaw(raw: List<RawComponent>): Int {
        if (raw.isEmpty()) return ScoringConfig.SCORE_MIN
        val sum = raw.sumOf { it.baseWeight }
        if (sum <= 0.0) return ScoringConfig.SCORE_MIN
        val weighted = raw.sumOf { it.score.toDouble() * (it.baseWeight / sum) }
        // Half-up rounding to match hand-computed SCORING.md examples.
        return clampScore(kotlin.math.floor(weighted + 0.5).toInt())
    }

    // --- confidence / copy ---

    /**
     * High: ingredient data analyzed AND nova present.
     * Medium: ingredient data OR nova (exactly one).
     * Low: sparse.
     */
    private fun foodConfidence(
        hasIngredientData: Boolean,
        hasNova: Boolean,
    ): Confidence = when {
        hasIngredientData && hasNova -> Confidence.High
        hasIngredientData || hasNova -> Confidence.Medium
        else -> Confidence.Low
    }

    /**
     * Band × concern-count matrix (docs/SCORING.md, ADR-015 / 2.0.0 copy).
     * Zero concerns must not imply flagged ingredients exist.
     */
    private fun summarySentence(total: Int, concernCount: Int): String {
        val hasConcerns = concernCount > 0
        return when {
            total >= 75 && !hasConcerns ->
                "Looking good—nothing flagged in our pack."
            total >= 75 ->
                "Looking good—only minor flags below."
            total >= 50 && !hasConcerns ->
                "Middling score—mostly processing signals, not flagged ingredients."
            total >= 50 ->
                "Mixed bag—check the notes below."
            total >= 25 && !hasConcerns ->
                "Low score—driven by heavy processing; see the breakdown."
            total >= 25 ->
                "Several concerns—read carefully."
            !hasConcerns ->
                "Very low score—heavily processed formulation."
            else ->
                "Lots of flags—you may want to skip."
        }
    }

    private fun omittedFoodComponents(
        hasNova: Boolean,
        hasAdditives: Boolean,
        hasPositives: Boolean,
    ): List<String> {
        val out = mutableListOf<String>()
        if (!hasNova) out += "NOVA (no data)"
        if (!hasAdditives) out += "Additives (no data)"
        if (!hasPositives) out += "Positives (no data)"
        return out
    }

    // --- concerns ---

    private fun buildConcerns(
        uniqueMatches: List<MatchedIngredient>,
        listSize: Int?,
    ): List<Concern> {
        return uniqueMatches
            .filter { it.severity >= 1 }
            .map { m ->
                Concern(
                    id = m.entryId,
                    displayName = m.displayName,
                    severity = m.severity,
                    shortWhy = m.why,
                    sources = m.sources,
                    positionHint = positionHint(m.listIndex, listSize),
                    matchedOn = m.matchedOn,
                )
            }
            .sortedWith(
                compareByDescending<Concern> { it.severity }
                    .thenBy { it.displayName.lowercase() },
            )
    }

    /**
     * First / middle / last third of the ingredient list when [listIndex] is known.
     */
    internal fun positionHint(listIndex: Int?, listSize: Int?): String? {
        if (listIndex == null || listSize == null || listSize <= 0 || listIndex < 0) return null
        if (listSize == 1) return "Near top of ingredient list"
        val third = listSize / 3.0
        return when {
            listIndex < third -> "Near top of ingredient list"
            listIndex < 2 * third -> "Middle of ingredient list"
            else -> "Near end of ingredient list"
        }
    }

    // --- helpers ---

    private fun dedupeByEntryId(matches: List<MatchedIngredient>): List<MatchedIngredient> {
        val seen = LinkedHashSet<String>()
        val out = ArrayList<MatchedIngredient>(matches.size)
        for (m in matches) {
            if (seen.add(m.entryId)) out += m
        }
        return out
    }

    private fun hasAdditiveInputData(product: Product): Boolean =
        !product.ingredientsText.isNullOrBlank() ||
            product.additivesTags.isNotEmpty() ||
            product.ingredientsTags.isNotEmpty()

    private fun hasOrganicLabel(labels: List<String>): Boolean =
        labels.any { tag ->
            val t = tag.lowercase()
            t == "en:organic" || t.startsWith("en:organic-") || t.contains("organic")
        }

    private fun hasFairTradeLabel(labels: List<String>): Boolean =
        labels.any { tag ->
            val t = tag.lowercase()
            t.contains("fair-trade") || t.contains("fairtrade") || t == "en:fair-trade"
        }

    private fun ingredientListSize(product: Product): Int? {
        val text = product.ingredientsText?.takeIf { it.isNotBlank() } ?: return null
        val parts = text.split(Regex("[,;]"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        return parts.size.takeIf { it > 0 }
    }

    private fun clampScore(value: Int): Int =
        value.coerceIn(ScoringConfig.SCORE_MIN, ScoringConfig.SCORE_MAX)

    private data class RawComponent(
        val id: String,
        val label: String,
        val score: Int,
        val baseWeight: Double,
        val detail: String?,
    )

    companion object {
        const val ID_NOVA = "nova"
        const val ID_ADDITIVES = "additives"
        const val ID_POSITIVES = "positives"

        /**
         * True when the product has any ingredient-quality signal worth scoring
         * (ingredients text/tags, additives tags, or NOVA group).
         * When false, ViewModel should use the Partial “not enough ingredient data” path.
         */
        fun hasIngredientQualityData(product: Product): Boolean =
            !product.ingredientsText.isNullOrBlank() ||
                product.ingredientsTags.isNotEmpty() ||
                product.additivesTags.isNotEmpty() ||
                (product.novaGroup != null && product.novaGroup in 1..4)
    }
}
