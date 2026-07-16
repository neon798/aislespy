package app.aislespy.domain.model

/**
 * User-facing problem ingredient after the scoring pipeline.
 * See DOMAIN_MODELS.md.
 */
data class Concern(
    val id: String,
    val displayName: String,
    val severity: Int,
    val shortWhy: String,
    val sources: List<String>,
    val positionHint: String?,
    val matchedOn: String,
)
