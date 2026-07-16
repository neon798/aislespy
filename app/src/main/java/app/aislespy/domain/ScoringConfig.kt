package app.aislespy.domain

/**
 * Single source of truth for scoring methodology version and shared constants.
 * Keep in sync with docs/SCORING.md (methodologyVersion).
 */
object ScoringConfig {
    const val METHODOLOGY_VERSION: String = "1.0.0"

    const val SCORE_MIN: Int = 1
    const val SCORE_MAX: Int = 100

    /** Soft floor for food additives subscore (SCORING.md). */
    const val ADDITIVES_SOFT_FLOOR: Int = 5

    /** Food component base weights before missing-data reweight. */
    object FoodWeights {
        const val NUTRISCORE: Double = 0.45
        const val NOVA: Double = 0.25
        const val ADDITIVES: Double = 0.25
        const val POSITIVES: Double = 0.05
    }

    /** Severity → additives subscore deduction (applied once per unique entry id). */
    fun severityDeduction(severity: Int): Int = when (severity) {
        1 -> 2
        2 -> 4
        3 -> 7
        4 -> 12
        5 -> 18
        else -> if (severity > 5) 18 else 0
    }
}
