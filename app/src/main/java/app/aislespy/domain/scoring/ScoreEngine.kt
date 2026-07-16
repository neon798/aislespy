package app.aislespy.domain.scoring

import app.aislespy.domain.model.MatchedIngredient
import app.aislespy.domain.model.Product
import app.aislespy.domain.model.ScoreResult

/**
 * Pure scoring engine: product + knowledge matches → [ScoreResult].
 * Implementations must be side-effect free and free of Android imports.
 */
fun interface ScoreEngine {
    fun score(product: Product, matches: List<MatchedIngredient>): ScoreResult
}
