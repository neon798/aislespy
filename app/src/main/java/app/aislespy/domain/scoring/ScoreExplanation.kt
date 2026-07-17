package app.aislespy.domain.scoring

import app.aislespy.domain.model.ScoreComponent

/**
 * Shared explanation helpers for [FoodScoreEngine] / [BeautyScoreEngine].
 * Does not affect totals or component subscores (ADR-015).
 */
internal object ScoreExplanation {

    /** Weighted loss above this (points of total) is listed in [driverSentence]. */
    const val DRIVER_LOSS_THRESHOLD: Double = 5.0

    /**
     * `(100 - subscore) * normalizedWeight` per component; list contributors
     * with loss > [DRIVER_LOSS_THRESHOLD], highest first.
     * Returns null when nothing qualifies.
     */
    fun driverSentence(
        components: List<ScoreComponent>,
        labelFor: (ScoreComponent) -> String,
    ): String? {
        val drags = components
            .map { c -> c to (100 - c.score).toDouble() * c.weight.toDouble() }
            .filter { it.second > DRIVER_LOSS_THRESHOLD }
            .sortedByDescending { it.second }
        if (drags.isEmpty()) return null
        return "Main drags: " + drags.joinToString(", ") { labelFor(it.first) } + "."
    }

    fun foodDriverLabel(component: ScoreComponent): String = when (component.id) {
        FoodScoreEngine.ID_NUTRISCORE ->
            "nutrition (${component.detail ?: "Nutri-Score"})"
        FoodScoreEngine.ID_NOVA ->
            "ultra-processing (${component.detail ?: "NOVA"})"
        FoodScoreEngine.ID_ADDITIVES ->
            "additives" + (component.detail?.let { " ($it)" } ?: "")
        FoodScoreEngine.ID_POSITIVES ->
            "positives" + (component.detail?.let { " ($it)" } ?: "")
        else -> component.label.lowercase()
    }

    fun beautyDriverLabel(component: ScoreComponent): String = when (component.id) {
        BeautyScoreEngine.ID_HAZARDS -> "flagged ingredients (hazards)"
        BeautyScoreEngine.ID_ALLERGENS_FRAGRANCE -> "allergens / fragrance"
        BeautyScoreEngine.ID_REGULATORY -> "regulatory"
        else -> component.label.lowercase()
    }
}
