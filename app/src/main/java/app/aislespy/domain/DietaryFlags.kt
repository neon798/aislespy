package app.aislespy.domain

import app.aislespy.domain.model.Product

/**
 * Tri-state dietary status from OFF tags.
 * Informational only — never factored into scores (ADR-014, SCORING.md).
 */
enum class DietaryStatus {
    Yes,
    No,
    Unknown,
}

/**
 * Resolved vegan / vegetarian / dairy-free flags for a product.
 */
data class DietaryFlags(
    val vegan: DietaryStatus,
    val vegetarian: DietaryStatus,
    val dairyFree: DietaryStatus,
)

/**
 * Pure resolver: OFF `ingredients_analysis_tags`, `labels_tags`, `allergens_tags` → [DietaryFlags].
 * Rules match docs/DOMAIN_MODELS.md Dietary flags section.
 */
object DietaryFlagsResolver {

    fun from(product: Product): DietaryFlags {
        val analysis = product.ingredientsAnalysisTags.map { it.lowercase() }
        val labels = product.labelsTags.map { it.lowercase() }
        val allergens = product.allergensTags.map { it.lowercase() }
        val analysisOrLabels = analysis + labels

        val vegan = resolveVegan(analysisOrLabels, analysis)
        val vegetarian = resolveVegetarian(analysisOrLabels, analysis, vegan)
        val dairyFree = resolveDairyFree(allergens, labels, vegan)

        return DietaryFlags(
            vegan = vegan,
            vegetarian = vegetarian,
            dairyFree = dairyFree,
        )
    }

    private fun resolveVegan(analysisOrLabels: List<String>, analysis: List<String>): DietaryStatus {
        if (analysisOrLabels.contains(TAG_VEGAN)) return DietaryStatus.Yes
        if (analysis.contains(TAG_NON_VEGAN) || analysisOrLabels.contains(TAG_NON_VEGAN)) {
            return DietaryStatus.No
        }
        // en:maybe-vegan or absent → Unknown
        return DietaryStatus.Unknown
    }

    private fun resolveVegetarian(
        analysisOrLabels: List<String>,
        analysis: List<String>,
        vegan: DietaryStatus,
    ): DietaryStatus {
        val raw = when {
            analysisOrLabels.contains(TAG_VEGETARIAN) -> DietaryStatus.Yes
            analysis.contains(TAG_NON_VEGETARIAN) ||
                analysisOrLabels.contains(TAG_NON_VEGETARIAN) -> DietaryStatus.No
            else -> DietaryStatus.Unknown
        }
        // Yes vegan implies Yes vegetarian when vegetarian would be Unknown
        if (raw == DietaryStatus.Unknown && vegan == DietaryStatus.Yes) {
            return DietaryStatus.Yes
        }
        return raw
    }

    private fun resolveDairyFree(
        allergens: List<String>,
        labels: List<String>,
        vegan: DietaryStatus,
    ): DietaryStatus {
        // Allergen milk is definitive No
        if (allergens.contains(TAG_MILK)) return DietaryStatus.No
        if (labels.any { it in DAIRY_FREE_LABELS }) return DietaryStatus.Yes
        if (vegan == DietaryStatus.Yes) return DietaryStatus.Yes
        return DietaryStatus.Unknown
    }

    private const val TAG_VEGAN = "en:vegan"
    private const val TAG_NON_VEGAN = "en:non-vegan"
    private const val TAG_VEGETARIAN = "en:vegetarian"
    private const val TAG_NON_VEGETARIAN = "en:non-vegetarian"
    private const val TAG_MILK = "en:milk"

    private val DAIRY_FREE_LABELS = setOf(
        "en:no-lactose",
        "en:lactose-free",
        "en:dairy-free",
        "en:milk-free",
    )
}
