package app.aislespy.domain.scoring

import app.aislespy.domain.ScoringConfig
import app.aislespy.domain.model.Concern
import app.aislespy.domain.model.Confidence
import app.aislespy.domain.model.MatchedIngredient
import app.aislespy.domain.model.Product
import app.aislespy.domain.model.ScoreBand
import app.aislespy.domain.model.ScoreComponent
import app.aislespy.domain.model.ScoreResult
import kotlin.math.max

/**
 * Beauty scoring per docs/SCORING.md methodologyVersion [ScoringConfig.METHODOLOGY_VERSION].
 *
 * Components (base weights): hazards 0.70 / allergens_fragrance 0.15 / regulatory 0.15.
 *
 * ## Missing-component handling
 * When ingredient data exists and scoring runs, all three components are always included:
 * - **hazards** — 100 if no pack matches (unknown INCI names do not penalise).
 * - **allergens_fragrance** — always computed from matches (and fragrance name signals);
 *   starts at 100 even when [Product.allergensTags] is empty.
 * - **regulatory** — 100 when no restricted/banned pack matches; otherwise severity-based
 *   deductions. Kept simple: never omitted once we score (avoids inflating hazards weight
 *   when the pack simply has no restricted hits).
 *
 * If the product has **no ingredient data at all**, callers should **not** invoke this
 * engine and should show the Partial UI path instead (see [hasIngredientData]).
 *
 * Pure, side-effect free, JVM-testable. No Android imports.
 */
class BeautyScoreEngine : ScoreEngine {

    override fun score(product: Product, matches: List<MatchedIngredient>): ScoreResult {
        val uniqueMatches = dedupeByEntryId(matches)
        val ordered = orderedIngredientList(product)
        val orderKnown = ordered != null && ordered.size >= 2
        val listSize = ordered?.size
        val n = max(listSize ?: 1, 1)

        val concerns = buildConcerns(uniqueMatches, listSize, orderKnown)

        val hazards = hazardsComponent(uniqueMatches, n, orderKnown)
        val allergens = allergensFragranceComponent(uniqueMatches)
        val regulatory = regulatoryComponent(uniqueMatches)

        val raw = listOf(hazards, allergens, regulatory)
        val components = reweight(raw)
        val total = computeTotalFromRaw(raw)
        val band = ScoreBand.fromTotal(total)
        val confidence = beautyConfidence(product, ordered, orderKnown)

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
                ScoreExplanation::beautyDriverLabel,
            ),
            // All three beauty components are always included once we score (see class KDoc).
            omittedComponents = emptyList(),
        )
    }

    // --- components ---

    /**
     * Start 100; each match: deduction = baseDeduction(severity) * positionWeight;
     * clamp 1..100. Unknown ingredients (no match) add no penalty.
     */
    private fun hazardsComponent(
        uniqueMatches: List<MatchedIngredient>,
        n: Int,
        orderKnown: Boolean,
    ): RawComponent {
        var sum = 0.0
        for (m in uniqueMatches) {
            if (m.severity < 1) continue
            val base = ScoringConfig.severityDeduction(m.severity).toDouble()
            val pw = positionWeight(m.listIndex, n, orderKnown)
            sum += base * pw
        }
        val sub = clampScore(100 - kotlin.math.floor(sum + 0.5).toInt())
        val detail = when {
            uniqueMatches.isEmpty() -> "No flagged ingredients"
            uniqueMatches.size == 1 -> "1 flagged ingredient"
            else -> "${uniqueMatches.size} flagged ingredients"
        }
        return RawComponent(
            id = ID_HAZARDS,
            label = "Hazards",
            score = sub,
            baseWeight = ScoringConfig.BeautyWeights.HAZARDS,
            detail = detail,
        )
    }

    /**
     * Start 100; fragrance/parfum/aroma −25 once; each EU allergen match −5 (cap −40).
     */
    private fun allergensFragranceComponent(
        uniqueMatches: List<MatchedIngredient>,
    ): RawComponent {
        var sub = 100
        val notes = mutableListOf<String>()

        val hasFragrance = uniqueMatches.any { isFragranceMatch(it) }
        if (hasFragrance) {
            sub -= ScoringConfig.BEAUTY_FRAGRANCE_DEDUCTION
            notes += "Fragrance"
        }

        val allergenMatches = uniqueMatches.filter { isEuAllergenMatch(it) }
        val allergenDeduction = minOf(
            allergenMatches.size * ScoringConfig.BEAUTY_ALLERGEN_DEDUCTION,
            ScoringConfig.BEAUTY_ALLERGEN_DEDUCTION_CAP,
        )
        if (allergenDeduction > 0) {
            sub -= allergenDeduction
            notes += when (allergenMatches.size) {
                1 -> "1 fragrance allergen"
                else -> "${allergenMatches.size} fragrance allergens"
            }
        }

        sub = clampScore(sub)
        return RawComponent(
            id = ID_ALLERGENS_FRAGRANCE,
            label = "Allergens / fragrance",
            score = sub,
            baseWeight = ScoringConfig.BeautyWeights.ALLERGENS_FRAGRANCE,
            detail = if (notes.isEmpty()) "No fragrance flags" else notes.joinToString(", "),
        )
    }

    /**
     * Start 100; each restricted/banned entry: −20 (sev 5) or −12 (sev ≤ 4).
     */
    private fun regulatoryComponent(
        uniqueMatches: List<MatchedIngredient>,
    ): RawComponent {
        var sub = 100
        var restrictedCount = 0
        for (m in uniqueMatches) {
            if (!isRestrictedOrBanned(m)) continue
            restrictedCount++
            sub -= if (m.severity >= 5) {
                ScoringConfig.BEAUTY_REGULATORY_SEV5
            } else {
                ScoringConfig.BEAUTY_REGULATORY_SEV4_OR_LOWER
            }
        }
        sub = clampScore(sub)
        val detail = when (restrictedCount) {
            0 -> "No restricted ingredients"
            1 -> "1 restricted ingredient"
            else -> "$restrictedCount restricted ingredients"
        }
        return RawComponent(
            id = ID_REGULATORY,
            label = "Regulatory",
            score = sub,
            baseWeight = ScoringConfig.BeautyWeights.REGULATORY,
            detail = detail,
        )
    }

    // --- position weight ---

    /**
     * `positionWeight = 1.0 - 0.6 * (i / max(n - 1, 1))` when order is known.
     * First ≈ 1.0, last ≈ 0.4. If order unknown: [ScoringConfig.BEAUTY_UNKNOWN_POSITION_WEIGHT].
     */
    internal fun positionWeight(listIndex: Int?, n: Int, orderKnown: Boolean): Double {
        if (!orderKnown) return ScoringConfig.BEAUTY_UNKNOWN_POSITION_WEIGHT
        val i = listIndex ?: return ScoringConfig.BEAUTY_UNKNOWN_POSITION_WEIGHT
        if (i < 0) return ScoringConfig.BEAUTY_UNKNOWN_POSITION_WEIGHT
        val denom = max(n - 1, 1).toDouble()
        return 1.0 - 0.6 * (i.coerceAtMost(n - 1).coerceAtLeast(0) / denom)
    }

    /**
     * First / middle / last third of the ingredient list when [listIndex] is known
     * and order is considered known.
     */
    internal fun positionHint(listIndex: Int?, listSize: Int?, orderKnown: Boolean): String? {
        if (!orderKnown) return null
        if (listIndex == null || listSize == null || listSize <= 0 || listIndex < 0) return null
        if (listSize == 1) return "Near top of ingredient list"
        val third = listSize / 3.0
        return when {
            listIndex < third -> "Near top of ingredient list"
            listIndex < 2 * third -> "Middle of ingredient list"
            else -> "Near end of ingredient list"
        }
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
        return clampScore(kotlin.math.floor(weighted + 0.5).toInt())
    }

    // --- confidence / copy ---

    /**
     * High: structured ordered list with ≥ 3 items.
     * Medium: free-text only, short list, or order unknown.
     * Low: almost no ingredient data.
     * Order-unknown always caps at Medium.
     */
    private fun beautyConfidence(
        product: Product,
        ordered: List<String>?,
        orderKnown: Boolean,
    ): Confidence {
        val structuredCount = ordered?.size ?: 0
        val hasText = !product.ingredientsText.isNullOrBlank()
        val tagCount = product.ingredientsTags.size + product.allergensTags.size

        val base = when {
            structuredCount >= 3 && orderKnown -> Confidence.High
            hasText || tagCount > 0 || structuredCount > 0 -> Confidence.Medium
            else -> Confidence.Low
        }
        return if (!orderKnown && base == Confidence.High) Confidence.Medium else base
    }

    /**
     * Band × concern-count matrix (docs/SCORING.md, ADR-015).
     * Beauty tone retained; zero concerns must not imply flagged ingredients exist.
     */
    private fun summarySentence(total: Int, concernCount: Int): String {
        val hasConcerns = concernCount > 0
        return when {
            total >= 75 && !hasConcerns ->
                "Formula looks gentle—nothing flagged in our pack."
            total >= 75 ->
                "Formula looks gentle—only minor flags below."
            total >= 50 && !hasConcerns ->
                "Middling score—mostly formula signals, not flagged ingredients."
            total >= 50 ->
                "Mixed bag—check the notes below."
            total >= 25 && !hasConcerns ->
                "Low score—driven by hazards or other formula signals; see the breakdown."
            total >= 25 ->
                "Several suspect ingredients—read carefully."
            !hasConcerns ->
                "Very low score—formula signals look rough."
            else ->
                "Lots of flags—you may want to skip."
        }
    }

    // --- concerns ---

    private fun buildConcerns(
        uniqueMatches: List<MatchedIngredient>,
        listSize: Int?,
        orderKnown: Boolean,
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
                    positionHint = positionHint(m.listIndex, listSize, orderKnown),
                    matchedOn = m.matchedOn,
                )
            }
            .sortedWith(
                compareByDescending<Concern> { it.severity }
                    .thenBy { it.displayName.lowercase() },
            )
    }

    // --- match classifiers ---

    private fun isFragranceMatch(m: MatchedIngredient): Boolean {
        val cats = m.categories.map { it.lowercase() }
        if (cats.any { it == "fragrance" || it == "parfum" }) return true
        val id = m.entryId.lowercase()
        if (id == "fragrance" || id == "parfum" || id == "aroma") return true
        val name = m.displayName.lowercase()
        return name == "fragrance" || name == "parfum" || name == "aroma" || name == "perfume"
    }

    /**
     * EU-listed fragrance allergens from the pack (category `allergen`).
     * Fragrance umbrella is handled separately (−25), so it is excluded here.
     */
    private fun isEuAllergenMatch(m: MatchedIngredient): Boolean {
        if (isFragranceMatch(m)) return false
        return m.categories.any { it.equals("allergen", ignoreCase = true) }
    }

    private fun isRestrictedOrBanned(m: MatchedIngredient): Boolean =
        m.categories.any {
            val c = it.lowercase()
            c == "restricted" || c == "banned"
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

    /**
     * Ordered list from [Product.ingredientsText] via top-level comma split.
     * Returns null when text is blank.
     */
    private fun orderedIngredientList(product: Product): List<String>? {
        val text = product.ingredientsText?.takeIf { it.isNotBlank() } ?: return null
        val parts = parseOrderedIngredientList(text)
        return parts.takeIf { it.isNotEmpty() }
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
        const val ID_HAZARDS = "hazards"
        const val ID_ALLERGENS_FRAGRANCE = "allergens_fragrance"
        const val ID_REGULATORY = "regulatory"

        /**
         * True when the product has any ingredient/allergen signal worth scoring.
         * When false, ViewModel should use the Partial “no ingredients to score” path.
         */
        fun hasIngredientData(product: Product): Boolean =
            !product.ingredientsText.isNullOrBlank() ||
                product.ingredientsTags.isNotEmpty() ||
                product.allergensTags.isNotEmpty() ||
                product.additivesTags.isNotEmpty()

        /**
         * Pure ordered-list parse (top-level commas, strip parentheticals).
         * Mirrors [app.aislespy.data.knowledge.KnowledgeMatcher.parseIngredientList]
         * so domain scoring does not depend on the data layer.
         */
        fun parseOrderedIngredientList(text: String): List<String> {
            if (text.isBlank()) return emptyList()
            val parts = ArrayList<String>()
            val current = StringBuilder()
            var depth = 0
            for (ch in text) {
                when {
                    ch == '(' || ch == '[' -> {
                        depth++
                        current.append(ch)
                    }
                    ch == ')' || ch == ']' -> {
                        if (depth > 0) depth--
                        current.append(ch)
                    }
                    ch == ',' && depth == 0 -> {
                        val fragment = stripParentheticals(current.toString())
                        if (fragment.isNotEmpty()) parts += fragment
                        current.clear()
                    }
                    else -> current.append(ch)
                }
            }
            val last = stripParentheticals(current.toString())
            if (last.isNotEmpty()) parts += last
            return parts
        }

        private fun stripParentheticals(raw: String): String {
            val without = raw
                .replace(Regex("\\([^)]*\\)"), " ")
                .replace(Regex("\\[[^]]*\\]"), " ")
            return without.replace(Regex("\\s+"), " ").trim()
        }
    }
}
