package app.aislespy.domain.model

/**
 * Result of knowledge-pack matching before scoring.
 * See DOMAIN_MODELS.md.
 */
data class MatchedIngredient(
    val entryId: String,
    val displayName: String,
    val severity: Int,
    val why: String,
    val sources: List<String>,
    val matchedOn: String,
    val listIndex: Int?,
)
