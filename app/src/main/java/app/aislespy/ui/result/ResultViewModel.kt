package app.aislespy.ui.result

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.aislespy.AisleSpyApp
import app.aislespy.data.knowledge.KnowledgeMatcher
import app.aislespy.data.knowledge.KnowledgePack
import app.aislespy.data.remote.ApiConfig
import app.aislespy.data.remote.ProductLookup
import app.aislespy.domain.model.Concern
import app.aislespy.domain.model.Confidence
import app.aislespy.domain.model.LookupOutcome
import app.aislespy.domain.model.Product
import app.aislespy.domain.model.ProductCategory
import app.aislespy.domain.model.ScoreComponent
import app.aislespy.domain.model.ScoreResult
import app.aislespy.domain.scoring.FoodScoreEngine
import app.aislespy.domain.scoring.ScoreEngine
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Loads product data, runs food matcher + [FoodScoreEngine], and maps to UI state.
 * Beauty products keep raw display with a “coming soon” score placeholder (T-410).
 */
class ResultViewModel(
    private val repository: ProductLookup,
    private val barcode: String,
    private val source: String = SOURCE_AUTO,
    private val knowledgePack: KnowledgePack? = null,
    private val foodScoreEngine: ScoreEngine = FoodScoreEngine(),
    private val concernStore: ConcernDetailStore = ConcernDetailStore(),
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
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
        viewModelScope.launch {
            _uiState.value = product.toSuccessState()
        }
    }

    /** Resolve ingredient detail from the last scored product (for nav tests / screen). */
    fun concernDetail(concernId: String): IngredientDetailUi? = concernStore.get(concernId)

    private fun load() {
        pendingFood = null
        pendingBeauty = null
        concernStore.publishEmpty()
        _uiState.value = ResultUiState.Loading(barcode)
        viewModelScope.launch {
            _uiState.value = mapOutcome(repository.lookup(barcode))
        }
    }

    private suspend fun mapOutcome(outcome: LookupOutcome): ResultUiState = when (outcome) {
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

    private suspend fun Product.toSuccessState(): ResultUiState.Success {
        val header = ProductHeaderUi(
            name = name.ifBlank { "Unknown product" },
            brand = brands,
            imageUrl = imageUrl,
            category = category,
            barcode = barcode,
            sourceDb = sourceDb,
        )
        val ingredients = ingredientsText?.takeIf { it.isNotBlank() }

        if (category == ProductCategory.Beauty) {
            concernStore.publishEmpty()
            return ResultUiState.Success(
                product = header,
                score = null,
                breakdown = emptyList(),
                concerns = emptyList(),
                badges = emptyList(),
                disclaimerVisible = true,
                ingredientsText = ingredients,
                beautyScoringPending = true,
            )
        }

        val scoreResult = withContext(defaultDispatcher) {
            val matches = if (knowledgePack != null) {
                KnowledgeMatcher.match(
                    pack = knowledgePack,
                    additivesTags = additivesTags,
                    ingredientsTags = ingredientsTags,
                    allergensTags = allergensTags,
                    ingredientsText = ingredientsText,
                )
            } else {
                emptyList()
            }
            foodScoreEngine.score(this@toSuccessState, matches)
        }
        concernStore.publish(scoreResult)

        return ResultUiState.Success(
            product = header,
            score = scoreResult.toScoreUi(),
            breakdown = scoreResult.components.map { it.toUi() },
            concerns = scoreResult.concerns.map { it.toUi() },
            badges = buildBadges(this),
            disclaimerVisible = true,
            ingredientsText = ingredients,
            beautyScoringPending = false,
        )
    }

    private fun ScoreResult.toScoreUi(): ScoreUi = ScoreUi(
        value = total,
        band = band,
        label = band.label,
        confidence = confidence,
        confidenceLabel = confidence.toLabel(),
        summarySentence = summarySentence,
    )

    private fun ScoreComponent.toUi(): ScoreComponentUi = ScoreComponentUi(
        id = id,
        label = label,
        score = score,
        detail = detail,
    )

    private fun Concern.toUi(): ConcernUi = ConcernUi(
        id = id,
        name = displayName,
        severity = severity,
        shortWhy = shortWhy,
        positionHint = positionHint,
    )

    private fun buildBadges(product: Product): List<BadgeUi> {
        val badges = mutableListOf<BadgeUi>()
        product.nutriscoreGrade?.lowercaseChar()?.takeIf { it in 'a'..'e' }?.let { g ->
            badges += BadgeUi(
                id = "nutriscore",
                label = "Nutri-Score ${g.uppercaseChar()}",
                style = "nutriscore",
            )
        }
        product.novaGroup?.takeIf { it in 1..4 }?.let { n ->
            badges += BadgeUi(
                id = "nova",
                label = "NOVA $n",
                style = "nova",
            )
        }
        if (product.labelsTags.any { it.lowercase().contains("organic") }) {
            badges += BadgeUi(
                id = "organic",
                label = "Organic",
                style = "organic",
            )
        }
        return badges
    }

    private fun Confidence.toLabel(): String = when (this) {
        Confidence.High -> "High confidence"
        Confidence.Medium -> "Partial data"
        Confidence.Low -> "Low confidence"
    }

    /**
     * Manual DI factory: reads [app.aislespy.di.AppContainer] from [AisleSpyApp].
     * Tests construct [ResultViewModel] directly with fakes.
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
            val container = (application as AisleSpyApp).container
            return ResultViewModel(
                repository = container.repository,
                barcode = barcode,
                source = source,
                knowledgePack = container.knowledgePack,
                foodScoreEngine = container.foodScoreEngine,
                concernStore = container.concernDetailStore,
            ) as T
        }
    }

    companion object {
        const val SOURCE_AUTO = "auto"
        const val SOURCE_FOOD = "food"
        const val SOURCE_BEAUTY = "beauty"

        /** Mandatory disclaimer from docs/SCORING.md (also shown in UI footer). */
        const val DISCLAIMER_TEXT =
            "AisleSpy scores are informational only. They are not medical advice, " +
                "an allergen guarantee, or a safety certification. Always read the physical label. " +
                "Product data comes from community databases and may be incomplete or outdated."

        private const val DEFAULT_NETWORK_MESSAGE = "Lost contact—check your connection."
    }
}
