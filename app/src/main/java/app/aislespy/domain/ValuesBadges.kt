package app.aislespy.domain

import app.aislespy.domain.model.Product

/**
 * Certification / values label badge (informational only).
 * Never factored into scores (ADR-017). Organic still contributes solely via the
 * food positives component in [app.aislespy.domain.scoring.FoodScoreEngine].
 */
data class ValuesBadge(
    val id: String,
    val label: String,
)

/**
 * Pure resolver: OFF/OBF `labels_tags` → ordered [ValuesBadge] list.
 * Conservative exact-tag / documented-prefix matching only (ADR-017).
 */
object ValuesBadgesResolver {

    fun from(product: Product): List<ValuesBadge> {
        val tags = product.labelsTags.map { it.lowercase() }
        val out = mutableListOf<ValuesBadge>()
        // Stable order as documented (DOMAIN_MODELS.md / ADR-017).
        if (matchesAny(tags, FAIR_TRADE_TAGS)) {
            out += ValuesBadge(id = ID_FAIR_TRADE, label = "Fair-trade")
        }
        if (matchesOrganic(tags)) {
            out += ValuesBadge(id = ID_ORGANIC, label = "Certified organic")
        }
        if (matchesAny(tags, CRUELTY_FREE_TAGS)) {
            out += ValuesBadge(id = ID_CRUELTY_FREE, label = "Cruelty-free")
        }
        if (matchesAny(tags, RAINFOREST_TAGS)) {
            out += ValuesBadge(id = ID_RAINFOREST, label = "Rainforest Alliance")
        }
        if (matchesAny(tags, UTZ_TAGS)) {
            out += ValuesBadge(id = ID_UTZ, label = "UTZ certified")
        }
        if (matchesAny(tags, B_CORP_TAGS)) {
            out += ValuesBadge(id = ID_B_CORP, label = "B Corp")
        }
        return out
    }

    private fun matchesAny(tags: List<String>, exact: Set<String>): Boolean =
        tags.any { it in exact }

    /**
     * Organic: exact known tags, or prefix `en:organic-` (e.g. en:organic-farming).
     * Does not loose-substring match (e.g. en:inorganic is excluded).
     */
    private fun matchesOrganic(tags: List<String>): Boolean =
        tags.any { tag ->
            tag in ORGANIC_EXACT_TAGS || tag.startsWith(ORGANIC_PREFIX)
        }

    const val ID_FAIR_TRADE = "fair-trade"
    const val ID_ORGANIC = "organic-certified"
    const val ID_CRUELTY_FREE = "cruelty-free"
    const val ID_RAINFOREST = "rainforest-alliance"
    const val ID_UTZ = "utz"
    const val ID_B_CORP = "b-corp"

    private const val ORGANIC_PREFIX = "en:organic-"

    private val FAIR_TRADE_TAGS = setOf(
        "en:fair-trade",
        "en:fairtrade-international",
        "en:max-havelaar",
        "en:fairtrade",
    )

    private val ORGANIC_EXACT_TAGS = setOf(
        "en:organic",
        "en:eu-organic",
        "en:usda-organic",
        "en:ab-agriculture-biologique",
    )

    private val CRUELTY_FREE_TAGS = setOf(
        "en:cruelty-free",
        "en:leaping-bunny",
        "en:not-tested-on-animals",
        "en:cruelty-free-international",
    )

    private val RAINFOREST_TAGS = setOf(
        "en:rainforest-alliance",
    )

    private val UTZ_TAGS = setOf(
        "en:utz-certified",
    )

    private val B_CORP_TAGS = setOf(
        "en:b-corp",
        "en:certified-b-corporation",
    )
}
