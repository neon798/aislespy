package app.aislespy.domain.model

/**
 * One weighted subscore contribution inside a [ScoreResult].
 * See DOMAIN_MODELS.md.
 */
data class ScoreComponent(
    val id: String,
    val label: String,
    val score: Int,
    val weight: Float,
    val detail: String?,
)
