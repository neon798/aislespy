package app.aislespy.ui.result

import app.aislespy.domain.model.Confidence
import app.aislespy.domain.model.ProductCategory
import app.aislespy.domain.model.ScoreBand
import app.aislespy.domain.model.SourceDb

/**
 * Product header for the result screen (DOMAIN_MODELS.md [ProductHeaderUi]).
 */
data class ProductHeaderUi(
    val name: String,
    val brand: String?,
    val imageUrl: String?,
    val category: ProductCategory,
    val barcode: String,
    val sourceDb: SourceDb,
)

/** DOMAIN_MODELS.md [ScoreUi]. */
data class ScoreUi(
    val value: Int,
    val band: ScoreBand,
    val label: String,
    val confidence: Confidence,
    val confidenceLabel: String,
    val summarySentence: String,
)

/** DOMAIN_MODELS.md [ScoreComponentUi]. */
data class ScoreComponentUi(
    val id: String,
    val label: String,
    val score: Int,
    val detail: String?,
)

/** DOMAIN_MODELS.md [ConcernUi]. */
data class ConcernUi(
    val id: String,
    val name: String,
    val severity: Int,
    val shortWhy: String,
    val positionHint: String?,
)

/** DOMAIN_MODELS.md [BadgeUi]. */
data class BadgeUi(
    val id: String,
    val label: String,
    val style: String,
)

/**
 * Ingredient detail payload (DOMAIN_MODELS.md [IngredientDetailUiState]).
 * Held in [ConcernDetailStore] after scoring for the ingredient route.
 */
data class IngredientDetailUi(
    val id: String,
    val name: String,
    val severity: Int,
    val fullWhy: String,
    val sources: List<String>,
    val positionHint: String?,
)

/**
 * Result screen UI state (DOMAIN_MODELS.md [ResultUiState]).
 */
sealed class ResultUiState {
    data class Loading(val barcode: String) : ResultUiState()

    data class Success(
        val product: ProductHeaderUi,
        val score: ScoreUi?,
        val breakdown: List<ScoreComponentUi>,
        val concerns: List<ConcernUi>,
        val badges: List<BadgeUi>,
        val disclaimerVisible: Boolean = true,
        /** Free-text ingredients for display / transparency. */
        val ingredientsText: String?,
        /**
         * Beauty products keep raw display until T-410; when true, show
         * "Beauty scoring coming soon" instead of [score].
         */
        val beautyScoringPending: Boolean = false,
    ) : ResultUiState()

    data class NotFound(
        val barcode: String,
        val contributeFoodUrl: String,
        val contributeBeautyUrl: String,
    ) : ResultUiState()

    data class NetworkError(
        val barcode: String,
        val message: String,
    ) : ResultUiState()

    /**
     * Both OFF and OBF returned data; user must pick a category.
     */
    data class NeedsCategoryChoice(
        val barcode: String,
        val foodName: String,
        val beautyName: String,
    ) : ResultUiState()
}
