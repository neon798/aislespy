package app.aislespy.domain.model

/**
 * Color band for a total score (docs/SCORING.md, DOMAIN_MODELS.md).
 * Boundaries: Excellent ≥75, Ok 50–74, Poor 25–49, Bad ≤24.
 */
enum class ScoreBand {
    Excellent,
    Ok,
    Poor,
    Bad,
    ;

    val label: String
        get() = when (this) {
            Excellent -> "Excellent"
            Ok -> "Ok"
            Poor -> "Poor"
            Bad -> "Bad"
        }

    companion object {
        fun fromTotal(total: Int): ScoreBand = when {
            total >= 75 -> Excellent
            total >= 50 -> Ok
            total >= 25 -> Poor
            else -> Bad
        }
    }
}
