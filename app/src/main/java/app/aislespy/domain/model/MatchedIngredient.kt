package app.aislespy.domain.model

/**
 * Result of knowledge-pack matching before scoring.
 * See DOMAIN_MODELS.md.
 *
 * [categories] are carried from the knowledge-pack entry so engines can apply
 * fragrance / allergen / restricted rules without re-loading the pack.
 */
data class MatchedIngredient(
    val entryId: String,
    val displayName: String,
    val severity: Int,
    val why: String,
    val sources: List<String>,
    val matchedOn: String,
    val listIndex: Int?,
    val categories: List<String> = emptyList(),
)
