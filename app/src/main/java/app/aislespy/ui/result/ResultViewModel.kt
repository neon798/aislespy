package app.aislespy.ui.result

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.aislespy.AisleSpyApp
import app.aislespy.data.remote.ApiConfig
import app.aislespy.data.remote.ProductLookup
import app.aislespy.domain.model.LookupOutcome
import app.aislespy.domain.model.Product
import app.aislespy.domain.model.ProductCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Loads raw product data for the result screen (no scoring — T-330).
 *
 * When both DBs hit ambiguously, keeps the food/beauty pair in memory so
 * [choose] can resolve without a second network request.
 */
class ResultViewModel(
    private val repository: ProductLookup,
    private val barcode: String,
    private val source: String = SOURCE_AUTO,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ResultUiState>(ResultUiState.Loading(barcode))
    val uiState: StateFlow<ResultUiState> = _uiState.asStateFlow()

    /** Cached pair when [LookupOutcome.NeedsCategoryChoice] — used by [choose]. */
    private var pendingFood: Product? = null
    private var pendingBeauty: Product? = null

    init {
        load()
    }

    fun retry() {
        load()
    }

    /**
     * User picks Food or Beauty after [ResultUiState.NeedsCategoryChoice].
     * Resolves from the already-fetched pair; does not re-request.
     */
    fun choose(category: ProductCategory) {
        val product = when (category) {
            ProductCategory.Food -> pendingFood
            ProductCategory.Beauty -> pendingBeauty
        }
        if (product == null) return
        pendingFood = null
        pendingBeauty = null
        _uiState.value = product.toSuccessState()
    }

    private fun load() {
        pendingFood = null
        pendingBeauty = null
        _uiState.value = ResultUiState.Loading(barcode)
        viewModelScope.launch {
            _uiState.value = mapOutcome(repository.lookup(barcode))
        }
    }

    private fun mapOutcome(outcome: LookupOutcome): ResultUiState = when (outcome) {
        is LookupOutcome.Found -> outcome.product.toSuccessState()

        is LookupOutcome.NotFound -> ResultUiState.NotFound(
            barcode = outcome.barcode,
            contributeFoodUrl = ApiConfig.contributeFoodUrl(outcome.barcode),
            contributeBeautyUrl = ApiConfig.contributeBeautyUrl(outcome.barcode),
        )

        is LookupOutcome.NetworkError -> ResultUiState.NetworkError(
            barcode = outcome.barcode,
            message = outcome.message.ifBlank { DEFAULT_NETWORK_MESSAGE },
        )

        is LookupOutcome.NeedsCategoryChoice -> {
            pendingFood = outcome.food
            pendingBeauty = outcome.beauty
            when (source.lowercase()) {
                SOURCE_FOOD -> outcome.food.toSuccessState()
                SOURCE_BEAUTY -> outcome.beauty.toSuccessState()
                else -> ResultUiState.NeedsCategoryChoice(
                    barcode = barcode,
                    foodName = outcome.food.name.ifBlank { "Food product" },
                    beautyName = outcome.beauty.name.ifBlank { "Beauty product" },
                )
            }
        }
    }

    private fun Product.toSuccessState(): ResultUiState.Success =
        ResultUiState.Success(
            product = ProductHeaderUi(
                name = name.ifBlank { "Unknown product" },
                brand = brands,
                imageUrl = imageUrl,
                category = category,
                barcode = barcode,
                sourceDb = sourceDb,
            ),
            ingredientsText = ingredientsText?.takeIf { it.isNotBlank() },
        )

    /**
     * Manual DI factory: reads [app.aislespy.di.AppContainer] from [AisleSpyApp].
     * Tests construct [ResultViewModel] directly with a fake [ProductLookup].
     */
    class Factory(
        private val application: Application,
        private val barcode: String,
        private val source: String = SOURCE_AUTO,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ResultViewModel::class.java)) {
                "Unknown ViewModel class: ${modelClass.name}"
            }
            val repository = (application as AisleSpyApp).container.repository
            return ResultViewModel(repository, barcode, source) as T
        }
    }

    companion object {
        const val SOURCE_AUTO = "auto"
        const val SOURCE_FOOD = "food"
        const val SOURCE_BEAUTY = "beauty"

        private const val DEFAULT_NETWORK_MESSAGE = "Lost contact—check your connection."
    }
}
