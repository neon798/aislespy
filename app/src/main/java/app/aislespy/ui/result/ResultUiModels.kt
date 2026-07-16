package app.aislespy.ui.result

import app.aislespy.domain.model.ProductCategory
import app.aislespy.domain.model.SourceDb

/**
 * Product header for the result screen (DOMAIN_MODELS.md [ProductHeaderUi]).
 * [sourceDb] is included so the UI can show a source-db badge before scoring lands (T-330).
 */
data class ProductHeaderUi(
    val name: String,
    val brand: String?,
    val imageUrl: String?,
    val category: ProductCategory,
    val barcode: String,
    val sourceDb: SourceDb,
)

/**
 * Result screen UI state without score/breakdown/concerns/badges (T-230).
 * Full Success shape (score, concerns, etc.) arrives with T-330.
 *
 * See DOMAIN_MODELS.md [ResultUiState].
 */
sealed class ResultUiState {
    data class Loading(val barcode: String) : ResultUiState()

    data class Success(
        val product: ProductHeaderUi,
        val ingredientsText: String?,
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
     * Dedicated chooser route is T-420; for T-230 this is rendered inline.
     */
    data class NeedsCategoryChoice(
        val barcode: String,
        val foodName: String,
        val beautyName: String,
    ) : ResultUiState()
}
