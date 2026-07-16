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

    /** Beauty component base weights (docs/SCORING.md). */
    object BeautyWeights {
        const val HAZARDS: Double = 0.70
        const val ALLERGENS_FRAGRANCE: Double = 0.15
        const val REGULATORY: Double = 0.15
    }

    /** Fragrance umbrella deduction on the allergens/fragrance subscore. */
    const val BEAUTY_FRAGRANCE_DEDUCTION: Int = 25

    /** Per EU-listed allergen match; total allergen-only deductions capped. */
    const val BEAUTY_ALLERGEN_DEDUCTION: Int = 5
    const val BEAUTY_ALLERGEN_DEDUCTION_CAP: Int = 40

    /** Regulatory category deductions. */
    const val BEAUTY_REGULATORY_SEV5: Int = 20
    const val BEAUTY_REGULATORY_SEV4_OR_LOWER: Int = 12

    /**
     * Position weight when ordered list is unknown (free text / tags only).
     * docs/SCORING.md: use 0.7 for all matches and cap confidence ≤ Medium.
     */
    const val BEAUTY_UNKNOWN_POSITION_WEIGHT: Double = 0.7

    /** Severity → base deduction (food additives & beauty hazards). */
    fun severityDeduction(severity: Int): Int = when (severity) {
        1 -> 2
        2 -> 4
        3 -> 7
        4 -> 12
        5 -> 18
        else -> if (severity > 5) 18 else 0
    }
}
