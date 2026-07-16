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
 * Pure, side-effect free, JVM-testable. No Android imports.
 */
class FoodScoreEngine : ScoreEngine {

    override fun score(product: Product, matches: List<MatchedIngredient>): ScoreResult {
        val uniqueMatches = dedupeByEntryId(matches)
        val concerns = buildConcerns(uniqueMatches, listSize = ingredientListSize(product))

        val raw = mutableListOf<RawComponent>()

        nutriscoreComponent(product)?.let { raw += it }
        novaComponent(product)?.let { raw += it }
        additivesComponent(product, uniqueMatches)?.let { raw += it }
        positivesComponent(product)?.let { raw += it }

        val components = reweight(raw)
        // Total from Double base weights (avoids float round-trip error on 0.45/0.05).
        val total = computeTotalFromRaw(raw)
        val band = ScoreBand.fromTotal(total)
        val confidence = foodConfidence(
            hasNutri = raw.any { it.id == ID_NUTRISCORE },
            hasNova = raw.any { it.id == ID_NOVA },
            strongAdditiveList = hasStrongAdditiveList(product, uniqueMatches),
        )

        return ScoreResult(
            total = total,
            band = band,
            confidence = confidence,
            components = components,
            concerns = concerns,
            methodologyVersion = ScoringConfig.METHODOLOGY_VERSION,
            summarySentence = summarySentence(total),
        )
    }

    // --- components ---

    private fun nutriscoreComponent(product: Product): RawComponent? {
        val grade = product.nutriscoreGrade?.lowercaseChar()
        if (grade != null && grade in 'a'..'e') {
            val sub = when (grade) {
                'a' -> 95
                'b' -> 80
                'c' -> 60
                'd' -> 40
                'e' -> 20
                else -> return null
            }
            return RawComponent(
                id = ID_NUTRISCORE,
                label = "Nutri-Score",
                score = sub,
                baseWeight = ScoringConfig.FoodWeights.NUTRISCORE,
                detail = "Nutri-Score ${grade.uppercaseChar()}",
            )
        }
        // Best-effort fallback when grade missing but OFF numeric score present.
        // Prefer grade when available. Formula: clamp(100 - (score + 15) * 3, 1, 100).
        val numeric = product.nutriscoreScore ?: return null
        val sub = clampScore(100 - (numeric + 15) * 3)
        return RawComponent(
            id = ID_NUTRISCORE,
            label = "Nutri-Score",
            score = sub,
            baseWeight = ScoringConfig.FoodWeights.NUTRISCORE,
            detail = "Nutri-Score numeric $numeric",
        )
    }

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
     * Included only when labelsTags or nutriments are present (else omit at 5% weight).
     * Start 50; organic +20; fair-trade +10; fiber ≥6 g/100g +10; clamp 1..100.
     */
    private fun positivesComponent(product: Product): RawComponent? {
        val hasLabels = product.labelsTags.isNotEmpty()
        val hasNutriments = product.nutriments != null
        if (!hasLabels && !hasNutriments) return null

        var sub = 50
        val notes = mutableListOf<String>()

        if (hasOrganicLabel(product.labelsTags)) {
            sub += 20
            notes += "Organic"
        }
        if (hasFairTradeLabel(product.labelsTags)) {
            sub += 10
            notes += "Fair-trade"
        }
        val fiber = product.nutriments?.fiber100g
        if (fiber != null && fiber >= 6.0) {
            sub += 10
            notes += "High fiber"
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

    private fun foodConfidence(
        hasNutri: Boolean,
        hasNova: Boolean,
        strongAdditiveList: Boolean,
    ): Confidence = when {
        hasNutri && hasNova -> Confidence.High
        hasNutri || hasNova -> Confidence.Medium
        strongAdditiveList -> Confidence.Medium
        else -> Confidence.Low
    }

    private fun summarySentence(total: Int): String = when {
        total >= 75 -> "Looking good—few red flags."
        total >= 50 -> "Mixed bag—check the notes below."
        total >= 25 -> "Several concerns—read carefully."
        else -> "Lots of flags—you may want to skip."
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

    private fun hasStrongAdditiveList(
        product: Product,
        matches: List<MatchedIngredient>,
    ): Boolean =
        product.additivesTags.size >= 3 || matches.size >= 2

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
        const val ID_NUTRISCORE = "nutriscore"
        const val ID_NOVA = "nova"
        const val ID_ADDITIVES = "additives"
        const val ID_POSITIVES = "positives"
    }
}
