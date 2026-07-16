package app.aislespy.domain.scoring

import app.aislespy.domain.model.Product

/**
 * Heuristics for deciding food vs beauty when both OFF and OBF return a product.
 * Pure Kotlin; see API_CONTRACTS.md § Heuristics.
 */
object CategoryResolver {

    /**
     * Decision when combining optional OFF (food-mapped) and OBF (beauty-mapped) products.
     */
    sealed class Decision {
        data object Food : Decision()
        data object Beauty : Decision()
        data object Ambiguous : Decision()
    }

    /**
     * Resolve which category to use given optional products from each database.
     *
     * - Both present: apply [clearlyFood] / [clearlyBeauty] (API_CONTRACTS both-Found rules).
     * - Only OFF: [Decision.Food]
     * - Only OBF: [Decision.Beauty]
     * - Neither: [Decision.Ambiguous]
     */
    fun resolve(off: Product?, obf: Product?): Decision {
        return when {
            off != null && obf != null -> {
                val foodClear = clearlyFood(off)
                val beautyClear = clearlyBeauty(obf)
                when {
                    foodClear && !beautyClear -> Decision.Food
                    beautyClear && !foodClear -> Decision.Beauty
                    else -> Decision.Ambiguous
                }
            }
            off != null -> Decision.Food
            obf != null -> Decision.Beauty
            else -> Decision.Ambiguous
        }
    }

    /**
     * Food signals (any one is enough to mark clearly food):
     * - Nutri-Score grade present
     * - NOVA group present
     * - food-like [Product.categoriesTags]
     * - non-empty additives with E-number tags
     */
    fun clearlyFood(product: Product): Boolean {
        if (product.nutriscoreGrade != null) return true
        if (product.novaGroup != null) return true
        if (hasFoodLikeCategories(product.categoriesTags)) return true
        if (hasENumberAdditives(product.additivesTags)) return true
        return false
    }

    /**
     * Beauty signals:
     * - cosmetic [Product.categoriesTags]
     * - cosmetic categories with absence of nutriments and nutriscore (strong cosmetic path)
     */
    fun clearlyBeauty(product: Product): Boolean {
        if (!hasCosmeticCategories(product.categoriesTags)) return false
        // Cosmetic tags alone are a beauty signal; lack of food nutrition fields reinforces it.
        return true
    }

    // Conservative food category prefixes / keywords (lowercase, match as substring of tag).
    private val foodCategorySignals: Set<String> = setOf(
        "en:plant-based-foods",
        "en:snacks",
        "en:beverages",
        "en:breakfasts",
        "en:breakfast-cereals",
        "en:chocolates",
        "en:chocolate-spreads",
        "en:spreads",
        "en:dairies",
        "en:meats",
        "en:fruits",
        "en:vegetables",
        "en:cereals",
        "en:confectioneries",
        "en:sweet-snacks",
        "en:salty-snacks",
        "en:frozen-foods",
        "en:canned-foods",
        "en:groceries",
        "en:foods",
        "en:desserts",
        "en:cheeses",
        "en:breads",
        "en:pastas",
        "en:rices",
        "en:seafood",
        "en:fishes",
        "en:soups",
        "en:sauces",
        "en:condiments",
        "en:nuts",
        "en:seeds",
        "en:oils",
        "en:fats",
        "en:sugars",
        "en:sweeteners",
        "en:baby-foods",
        "en:meals",
        "en:prepared-meals",
        "en:pizzas",
        "en:sandwiches",
        "en:yogurts",
        "en:milks",
        "en:waters",
        "en:juices",
        "en:sodas",
        "en:biscuits",
        "en:cookies",
        "en:cakes",
        "en:ice-creams",
    )

    // Cosmetic category prefixes / keywords.
    private val beautyCategorySignals: Set<String> = setOf(
        "en:skin-care",
        "en:skincare",
        "en:hair-care",
        "en:haircare",
        "en:makeup",
        "en:make-up",
        "en:hygiene",
        "en:body-care",
        "en:face-care",
        "en:facial-care",
        "en:cosmetics",
        "en:personal-care",
        "en:shampoos",
        "en:conditioners",
        "en:soaps",
        "en:shower-gels",
        "en:deodorants",
        "en:perfumes",
        "en:fragrances",
        "en:nail-care",
        "en:oral-care",
        "en:toothpastes",
        "en:suncare",
        "en:sun-care",
        "en:lotions",
        "en:creams",
        "en:serums",
        "en:moisturizers",
        "en:cleansers",
        "en:lipsticks",
        "en:foundations",
        "en:mascaras",
        "en:eyeliner",
        "en:beauty",
    )

    private fun hasFoodLikeCategories(tags: List<String>): Boolean =
        tags.any { tag ->
            val lower = tag.lowercase()
            foodCategorySignals.any { signal -> lower == signal || lower.startsWith("$signal-") || lower.startsWith("$signal/") }
        }

    private fun hasCosmeticCategories(tags: List<String>): Boolean =
        tags.any { tag ->
            val lower = tag.lowercase()
            beautyCategorySignals.any { signal -> lower == signal || lower.startsWith("$signal-") || lower.startsWith("$signal/") }
        }

    /**
     * E-number additives appear as tags like `en:e322`, `en:e330i`.
     * Requires at least one such tag (non-empty list alone is not enough).
     */
    private fun hasENumberAdditives(additivesTags: List<String>): Boolean {
        if (additivesTags.isEmpty()) return false
        val eNumber = Regex("""(?:^|:)e\d{3,4}[a-z]?$""", RegexOption.IGNORE_CASE)
        return additivesTags.any { eNumber.containsMatchIn(it) }
    }
}
