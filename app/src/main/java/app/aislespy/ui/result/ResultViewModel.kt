package app.aislespy.ui.result

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.aislespy.AisleSpyApp
import app.aislespy.data.knowledge.KnowledgeMatcher
import app.aislespy.data.knowledge.KnowledgePack
import app.aislespy.data.local.HistoryWriter
import app.aislespy.data.remote.ApiConfig
import app.aislespy.data.remote.ProductLookup
import app.aislespy.domain.BrandOwnership
import app.aislespy.domain.BrandOwnershipPack
import app.aislespy.domain.BrandOwnershipResolver
import app.aislespy.domain.DietaryFlagsResolver
import app.aislespy.domain.DietaryStatus
import app.aislespy.domain.ValuesBadgesResolver
import app.aislespy.domain.model.Concern
import app.aislespy.domain.model.Confidence
import app.aislespy.domain.model.HistoryEntry
import app.aislespy.domain.model.LookupOutcome
import app.aislespy.domain.model.Product
import app.aislespy.domain.model.ProductCategory
import app.aislespy.domain.model.ScoreComponent
import app.aislespy.domain.model.ScoreResult
import app.aislespy.domain.scoring.BeautyScoreEngine
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
 * Loads product data, runs knowledge matcher + score engine by category, maps to UI state.
 *
 * Food → [foodKnowledgePack] + [FoodScoreEngine]; products with no ingredient-quality data
 * take the Partial path (score hidden, message shown). Nutri-Score is display-only via
 * [nutritionStore] (ADR-018).
 * Beauty → [beautyKnowledgePack] + [BeautyScoreEngine]; products with no ingredient data
 * take the Partial path (score hidden, message shown).
 *
 * Category choice (T-420): on auto + ambiguous dual hit, publishes the pair to
 * [ChoicePairStore] and emits [ResultUiState.NavigateToCategoryChooser]. When
 * re-opened with source=food|beauty, uses the stored product (no refetch) when
 * present; otherwise refetches and resolves by requested source.
 *
 * ## History recording (T-500)
 *
 * After a [ResultUiState.Success] emission that includes a **numeric** score, upserts a
 * [HistoryEntry] via [historyWriter] on [ioDispatcher] (fire-and-forget).
 *
 * **MVP policy:** partial results (`score == null`) are **not** history-worthy.
 * Only Success states with a non-null [ScoreUi] are recorded.
 * Do not invent score 0 for partials.
 */
class ResultViewModel(
    private val repository: ProductLookup,
    private val barcode: String,
    private val source: String = SOURCE_AUTO,
    private val foodKnowledgePack: KnowledgePack? = null,
    private val beautyKnowledgePack: KnowledgePack? = null,
    private val brandOwnershipPack: BrandOwnershipPack? = null,
    private val foodScoreEngine: ScoreEngine = FoodScoreEngine(),
    private val beautyScoreEngine: ScoreEngine = BeautyScoreEngine(),
    private val concernStore: ConcernDetailStore = ConcernDetailStore(),
    private val nutritionStore: NutritionStore = NutritionStore(),
    private val choicePairStore: ChoicePairStore = ChoicePairStore(),
    private val historyWriter: HistoryWriter? = null,
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ResultUiState>(ResultUiState.Loading(barcode))
    val uiState: StateFlow<ResultUiState> = _uiState.asStateFlow()

    private val sourceNorm: String = source.lowercase()

    init {
        load()
    }

    fun retry() {
        load()
    }

    /** Resolve ingredient detail from the last scored product (for nav tests / screen). */
    fun concernDetail(concernId: String): IngredientDetailUi? = concernStore.get(concernId)

    /** Resolve nutrition display data for the current product (tests / screen). */
    fun nutritionFor(barcode: String): NutritionUi? = nutritionStore.get(barcode)

    private fun load() {
        concernStore.publishEmpty()
        nutritionStore.publishEmpty()
        _uiState.value = ResultUiState.Loading(barcode)
        viewModelScope.launch {
            // Explicit source + pair still in memory → score without network.
            if (sourceNorm == SOURCE_FOOD || sourceNorm == SOURCE_BEAUTY) {
                val stored = choicePairStore.get(barcode)
                if (stored != null) {
                    val product = when (sourceNorm) {
                        SOURCE_FOOD -> stored.food
                        else -> stored.beauty
                    }
                    emitSuccess(product.toSuccessState())
                    return@launch
                }
            }

            when (val mapped = mapOutcome(repository.lookup(barcode))) {
                is ResultUiState.Success -> emitSuccess(mapped)
                else -> _uiState.value = mapped
            }
        }
    }

    /**
     * Emit Success and, when a numeric total exists, record history (async IO).
     * Partials (`score == null`) intentionally skip history — see class KDoc.
     */
    private fun emitSuccess(success: ResultUiState.Success) {
        _uiState.value = success
        val scoreUi = success.score ?: return
        val writer = historyWriter ?: return
        val entry = HistoryEntry(
            barcode = success.product.barcode,
            name = success.product.name,
            score = scoreUi.value,
            category = success.product.category,
            scannedAtEpochMs = clock(),
            thumbnailUrl = success.product.imageUrl,
        )
        viewModelScope.launch(ioDispatcher) {
            writer.upsert(entry)
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
            when (sourceNorm) {
                SOURCE_FOOD -> outcome.food.toSuccessState()
                SOURCE_BEAUTY -> outcome.beauty.toSuccessState()
                else -> {
                    // auto: hand off to dedicated chooser (once per load).
                    choicePairStore.put(barcode, outcome.food, outcome.beauty)
                    ResultUiState.NavigateToCategoryChooser(barcode = barcode)
                }
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

        // Publish nutrition for food products even on partial score paths (display-only).
        if (category == ProductCategory.Food) {
            nutritionStore.publish(this)
        } else {
            nutritionStore.publishEmpty()
        }

        if (category == ProductCategory.Beauty && !BeautyScoreEngine.hasIngredientData(this)) {
            concernStore.publishEmpty()
            return ResultUiState.Success(
                product = header,
                score = null,
                breakdown = emptyList(),
                omittedComponents = emptyList(),
                concerns = emptyList(),
                badges = buildBadges(this),
                disclaimerVisible = true,
                ingredientsText = ingredients,
                beautyScoringPending = false,
                partialMessage = PARTIAL_NO_INGREDIENTS,
            )
        }

        // Food with no ingredient-quality inputs → partial (no invented score). ADR-018.
        if (category == ProductCategory.Food && !FoodScoreEngine.hasIngredientQualityData(this)) {
            concernStore.publishEmpty()
            return ResultUiState.Success(
                product = header,
                score = null,
                breakdown = emptyList(),
                omittedComponents = emptyList(),
                concerns = emptyList(),
                badges = buildBadges(this),
                disclaimerVisible = true,
                ingredientsText = ingredients,
                beautyScoringPending = false,
                partialMessage = PARTIAL_NO_INGREDIENT_DATA,
            )
        }

        val pack = when (category) {
            ProductCategory.Food -> foodKnowledgePack
            ProductCategory.Beauty -> beautyKnowledgePack
        }
        val engine = when (category) {
            ProductCategory.Food -> foodScoreEngine
            ProductCategory.Beauty -> beautyScoreEngine
        }

        val scoreResult = withContext(defaultDispatcher) {
            val matches = if (pack != null) {
                KnowledgeMatcher.match(
                    pack = pack,
                    additivesTags = additivesTags,
                    ingredientsTags = ingredientsTags,
                    allergensTags = allergensTags,
                    ingredientsText = ingredientsText,
                )
            } else {
                emptyList()
            }
            engine.score(this@toSuccessState, matches)
        }
        concernStore.publish(scoreResult)

        return ResultUiState.Success(
            product = header,
            score = scoreResult.toScoreUi(),
            breakdown = scoreResult.components.map { it.toUi() },
            omittedComponents = scoreResult.omittedComponents,
            concerns = scoreResult.concerns.map { it.toUi() },
            badges = buildBadges(this),
            disclaimerVisible = true,
            ingredientsText = ingredients,
            beautyScoringPending = false,
            partialMessage = null,
        )
    }

    private fun ScoreResult.toScoreUi(): ScoreUi = ScoreUi(
        value = total,
        band = band,
        label = band.label,
        confidence = confidence,
        confidenceLabel = confidence.toLabel(),
        summarySentence = summarySentence,
        driverSentence = driverSentence,
    )

    private fun ScoreComponent.toUi(): ScoreComponentUi = ScoreComponentUi(
        id = id,
        label = label,
        score = score,
        detail = detail,
        weight = weight,
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
        // Nutri-Score removed from primary badges row (ADR-018) — lives on nutrition screen.
        product.novaGroup?.takeIf { it in 1..4 }?.let { n ->
            badges += BadgeUi(
                id = "nova",
                label = "NOVA $n",
                style = "nova",
            )
        }
        // Brand ownership (food + beauty): informational only — never scored (ADR-019).
        ownershipBadge(product)?.let { badges += it }
        // Values badges (food + beauty): certification labels only — never scored (ADR-017).
        for (vb in ValuesBadgesResolver.from(product)) {
            badges += BadgeUi(
                id = vb.id,
                label = vb.label,
                style = STYLE_VALUES,
            )
        }
        // Dietary flags: food only, informational badges — never affect ScoreResult (ADR-014).
        if (product.category == ProductCategory.Food) {
            badges += dietaryBadges(product)
        }
        return badges
    }

    /**
     * Resolve ownership chip from pack + brands_tags.
     * Corporate → neutral "Owned by X"; Independent → gold-star "Independent"; no match → null.
     */
    private fun ownershipBadge(product: Product): BadgeUi? {
        val pack = brandOwnershipPack ?: return null
        return when (val ownership = BrandOwnershipResolver.resolve(product, pack)) {
            is BrandOwnership.Corporate -> BadgeUi(
                id = "ownership-corporate",
                label = "Owned by ${ownership.parentDisplay}",
                style = STYLE_OWNERSHIP,
                contentDescription = "Owned by ${ownership.parentDisplay}, ownership information",
            )
            is BrandOwnership.Independent -> BadgeUi(
                id = "ownership-independent",
                label = "Independent",
                style = STYLE_VALUES,
                contentDescription = "Independent brand",
            )
            null -> null
        }
    }

    /**
     * Map [DietaryFlagsResolver] tri-state to badges.
     * Yes → positive; No → neutral/warn (not red-alarm); Unknown → omitted.
     */
    private fun dietaryBadges(product: Product): List<BadgeUi> {
        val flags = DietaryFlagsResolver.from(product)
        val out = mutableListOf<BadgeUi>()
        when (flags.vegan) {
            DietaryStatus.Yes -> out += BadgeUi(id = "vegan", label = "Vegan", style = "positive")
            DietaryStatus.No -> out += BadgeUi(id = "vegan", label = "Not vegan", style = "warn")
            DietaryStatus.Unknown -> Unit
        }
        when (flags.vegetarian) {
            DietaryStatus.Yes ->
                out += BadgeUi(id = "vegetarian", label = "Vegetarian", style = "positive")
            DietaryStatus.No ->
                out += BadgeUi(id = "vegetarian", label = "Not vegetarian", style = "warn")
            DietaryStatus.Unknown -> Unit
        }
        when (flags.dairyFree) {
            DietaryStatus.Yes ->
                out += BadgeUi(id = "dairy_free", label = "Dairy-free", style = "positive")
            DietaryStatus.No ->
                out += BadgeUi(id = "dairy_free", label = "Contains dairy", style = "warn")
            DietaryStatus.Unknown -> Unit
        }
        return out
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
                foodKnowledgePack = container.foodKnowledgePack,
                beautyKnowledgePack = container.beautyKnowledgePack,
                brandOwnershipPack = container.brandOwnershipPack,
                foodScoreEngine = container.foodScoreEngine,
                beautyScoreEngine = container.beautyScoreEngine,
                concernStore = container.concernDetailStore,
                nutritionStore = container.nutritionStore,
                choicePairStore = container.choicePairStore,
                historyWriter = HistoryWriter { entry ->
                    container.historyRepository.upsert(entry)
                },
                clock = container.clock,
            ) as T
        }
    }

    companion object {
        const val SOURCE_AUTO = "auto"
        const val SOURCE_FOOD = "food"
        const val SOURCE_BEAUTY = "beauty"

        /** Partial path copy for beauty without ingredients (docs/SCORING.md). */
        const val PARTIAL_NO_INGREDIENTS =
            "Found product, but no ingredients to score"

        /** Partial path for food without ingredient-quality data (ADR-018). */
        const val PARTIAL_NO_INGREDIENT_DATA =
            "Found product, but not enough ingredient data to score"

        /** Mandatory disclaimer from docs/SCORING.md (also shown in UI footer). */
        const val DISCLAIMER_TEXT =
            "AisleSpy scores are informational only. They are not medical advice, " +
                "an allergen guarantee, or a safety certification. Always read the physical label. " +
                "Product data comes from community databases and may be incomplete or outdated."

        /** BadgeUi.style for certification / values labels (ADR-017). */
        const val STYLE_VALUES = "values"

        /**
         * BadgeUi.style for corporate ownership (ADR-019): neutral InfoChip, not alarm-red.
         * Independent brands reuse [STYLE_VALUES].
         */
        const val STYLE_OWNERSHIP = "ownership"

        private const val DEFAULT_NETWORK_MESSAGE = "Lost contact—check your connection."
    }
}
