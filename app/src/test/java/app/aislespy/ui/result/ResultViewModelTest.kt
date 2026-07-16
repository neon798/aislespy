package app.aislespy.ui.result

import app.aislespy.data.knowledge.KnowledgePack
import app.aislespy.data.knowledge.KnowledgePackEntry
import app.aislespy.data.local.HistoryWriter
import app.aislespy.data.remote.ApiConfig
import app.aislespy.data.remote.ProductLookup
import app.aislespy.domain.model.Confidence
import app.aislespy.domain.model.HistoryEntry
import app.aislespy.domain.model.LookupOutcome
import app.aislespy.domain.model.Nutriments
import app.aislespy.domain.model.Product
import app.aislespy.domain.model.ProductCategory
import app.aislespy.domain.model.ScoreBand
import app.aislespy.domain.model.ScoreComponent
import app.aislespy.domain.model.ScoreResult
import app.aislespy.domain.model.SourceDb
import app.aislespy.domain.scoring.BeautyScoreEngine
import app.aislespy.domain.scoring.FoodScoreEngine
import app.aislespy.domain.scoring.ScoreEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ResultViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val barcode = "3017624010701"

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun successFood_emitsSuccessWithScoreAndConcerns() = runTest {
        val food = sampleProduct(
            name = "Nutella",
            brands = "Ferrero",
            category = ProductCategory.Food,
            sourceDb = SourceDb.OpenFoodFacts,
            ingredientsText = "Sugar, palm oil, hazelnuts",
            nutriscoreGrade = 'e',
            novaGroup = 4,
            additivesTags = listOf("en:e322"),
        )
        val pack = KnowledgePack(
            version = "1.0.0",
            domain = "food",
            entries = listOf(
                KnowledgePackEntry(
                    id = "e322",
                    names = listOf("Lecithins"),
                    aliases = listOf("en:e322"),
                    domain = "food",
                    severity = 2,
                    categories = listOf("emulsifier"),
                    title = "Lecithins",
                    why = "Common emulsifier; mild note for some.",
                    sources = listOf("https://example.com/e322"),
                ),
            ),
        )
        val store = ConcernDetailStore()
        val vm = ResultViewModel(
            repository = FakeProductLookup(LookupOutcome.Found(food)),
            barcode = barcode,
            foodKnowledgePack = pack,
            foodScoreEngine = FoodScoreEngine(),
            concernStore = store,
            defaultDispatcher = testDispatcher,
        )
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state is ResultUiState.Success)
        val success = state as ResultUiState.Success
        assertEquals("Nutella", success.product.name)
        assertEquals("Ferrero", success.product.brand)
        assertEquals(ProductCategory.Food, success.product.category)
        assertEquals(SourceDb.OpenFoodFacts, success.product.sourceDb)
        assertEquals(barcode, success.product.barcode)
        assertEquals("Sugar, palm oil, hazelnuts", success.ingredientsText)

        assertNotNull(success.score)
        val score = success.score!!
        assertTrue(score.value in 1..100)
        assertEquals(score.band, ScoreBand.fromTotal(score.value))
        assertTrue(success.breakdown.isNotEmpty())
        assertFalse(success.beautyScoringPending)
        assertTrue(success.disclaimerVisible)

        // Knowledge pack should produce a concern for e322
        assertTrue(success.concerns.any { it.id == "e322" })
        val concern = success.concerns.first { it.id == "e322" }
        assertEquals(2, concern.severity)

        // Badges for Nutri-Score + NOVA
        assertTrue(success.badges.any { it.label.contains("Nutri-Score") })
        assertTrue(success.badges.any { it.label.contains("NOVA") })

        // Ingredient detail store receives the concern
        val detail = vm.concernDetail("e322")
        assertNotNull(detail)
        assertEquals("e322", detail!!.id)
        assertTrue(detail.fullWhy.isNotBlank())
        assertEquals(listOf("https://example.com/e322"), detail.sources)
    }

    @Test
    fun nutellaLike_dietaryBadges_notVegan_vegetarian_containsDairy() = runTest {
        val food = sampleProduct(
            name = "Nutella",
            brands = "Ferrero",
            category = ProductCategory.Food,
            sourceDb = SourceDb.OpenFoodFacts,
            ingredientsText = "Sugar, palm oil, hazelnuts, skimmed milk powder",
            nutriscoreGrade = 'e',
            novaGroup = 4,
            allergensTags = listOf("en:milk"),
            ingredientsAnalysisTags = listOf("en:non-vegan", "en:vegetarian"),
        )
        val vm = ResultViewModel(
            repository = FakeProductLookup(LookupOutcome.Found(food)),
            barcode = barcode,
            foodScoreEngine = FoodScoreEngine(),
            concernStore = ConcernDetailStore(),
            defaultDispatcher = testDispatcher,
        )
        advanceUntilIdle()

        val success = vm.uiState.value as ResultUiState.Success
        val labels = success.badges.map { it.label }
        assertTrue(labels.contains("Not vegan"))
        assertTrue(labels.contains("Vegetarian"))
        assertTrue(labels.contains("Contains dairy"))
        assertFalse(labels.contains("Vegan"))
        assertFalse(labels.contains("Dairy-free"))
        // Negative dietary badges use warn style, not red-alarm
        val notVegan = success.badges.first { it.label == "Not vegan" }
        assertEquals("warn", notVegan.style)
        val containsDairy = success.badges.first { it.label == "Contains dairy" }
        assertEquals("warn", containsDairy.style)
        val vegetarian = success.badges.first { it.label == "Vegetarian" }
        assertEquals("positive", vegetarian.style)
        // Score still present and independent of dietary badges
        assertNotNull(success.score)
    }

    @Test
    fun beautyProduct_skipsDietaryBadges() = runTest {
        val beauty = sampleProduct(
            name = "Shampoo",
            category = ProductCategory.Beauty,
            sourceDb = SourceDb.OpenBeautyFacts,
            ingredientsText = "Aqua, Glycerin",
            ingredientsAnalysisTags = listOf("en:vegan"),
            labelsTags = listOf("en:vegan", "en:vegetarian"),
        )
        val vm = ResultViewModel(
            repository = FakeProductLookup(LookupOutcome.Found(beauty)),
            barcode = barcode,
            beautyScoreEngine = BeautyScoreEngine(),
            concernStore = ConcernDetailStore(),
            defaultDispatcher = testDispatcher,
        )
        advanceUntilIdle()

        val success = vm.uiState.value as ResultUiState.Success
        val labels = success.badges.map { it.label }
        assertFalse(labels.contains("Vegan"))
        assertFalse(labels.contains("Vegetarian"))
        assertFalse(labels.contains("Dairy-free"))
    }

    @Test
    fun successBeauty_withIngredients_carriesScoreUi() = runTest {
        val beauty = sampleProduct(
            name = "Shampoo",
            category = ProductCategory.Beauty,
            sourceDb = SourceDb.OpenBeautyFacts,
            ingredientsText = "Aqua, Glycerin, Parfum, Limonene",
        )
        val beautyPack = KnowledgePack(
            version = "1.0.0",
            domain = "beauty",
            entries = listOf(
                KnowledgePackEntry(
                    id = "fragrance",
                    names = listOf("fragrance", "parfum"),
                    aliases = listOf("en:parfum"),
                    domain = "beauty",
                    severity = 3,
                    categories = listOf("fragrance"),
                    title = "Fragrance",
                    why = "Umbrella scent term that may hide allergens for sensitive users.",
                    sources = listOf("EU Cosmetic Regulation fragrance labelling"),
                ),
                KnowledgePackEntry(
                    id = "limonene",
                    names = listOf("limonene"),
                    aliases = listOf("en:limonene"),
                    domain = "beauty",
                    severity = 1,
                    categories = listOf("allergen"),
                    title = "Limonene",
                    why = "EU-listed fragrance allergen with mild contact-allergy relevance.",
                    sources = listOf("EU Cosmetic Regulation Annex III"),
                ),
            ),
        )
        val store = ConcernDetailStore()
        val vm = ResultViewModel(
            repository = FakeProductLookup(LookupOutcome.Found(beauty)),
            barcode = barcode,
            beautyKnowledgePack = beautyPack,
            beautyScoreEngine = BeautyScoreEngine(),
            concernStore = store,
            defaultDispatcher = testDispatcher,
        )
        advanceUntilIdle()

        val state = vm.uiState.value as ResultUiState.Success
        assertFalse(state.beautyScoringPending)
        assertNull(state.partialMessage)
        assertNotNull(state.score)
        val score = state.score!!
        assertTrue(score.value in 1..100)
        assertEquals(score.band, ScoreBand.fromTotal(score.value))
        assertTrue(state.breakdown.isNotEmpty())
        assertTrue(state.breakdown.any { it.id == BeautyScoreEngine.ID_HAZARDS })
        assertTrue(state.concerns.any { it.id == "fragrance" || it.id == "limonene" })
        assertNotNull(vm.concernDetail(state.concerns.first().id))
    }

    @Test
    fun successBeauty_noIngredients_producesPartialState() = runTest {
        val beauty = sampleProduct(
            name = "Mystery cream",
            category = ProductCategory.Beauty,
            sourceDb = SourceDb.OpenBeautyFacts,
            ingredientsText = null,
        )
        val vm = ResultViewModel(
            repository = FakeProductLookup(LookupOutcome.Found(beauty)),
            barcode = barcode,
            beautyKnowledgePack = KnowledgePack(
                version = "1.0.0",
                domain = "beauty",
                entries = emptyList(),
            ),
            beautyScoreEngine = BeautyScoreEngine(),
            concernStore = ConcernDetailStore(),
            defaultDispatcher = testDispatcher,
        )
        advanceUntilIdle()

        val state = vm.uiState.value as ResultUiState.Success
        assertNull(state.score)
        assertEquals(ResultViewModel.PARTIAL_NO_INGREDIENTS, state.partialMessage)
        assertFalse(state.beautyScoringPending)
        assertTrue(state.concerns.isEmpty())
        assertTrue(state.breakdown.isEmpty())
    }

    @Test
    fun foodSuccess_withFakeEngine_mapsScoreAndIngredientDetail() = runTest {
        val food = sampleProduct(
            name = "Clean oats",
            category = ProductCategory.Food,
            sourceDb = SourceDb.OpenFoodFacts,
            ingredientsText = "Oats",
            nutriscoreGrade = 'a',
            novaGroup = 1,
        )
        val fakeEngine = ScoreEngine { _, _ ->
            ScoreResult(
                total = 90,
                band = ScoreBand.Excellent,
                confidence = Confidence.High,
                components = listOf(
                    ScoreComponent("nutriscore", "Nutri-Score", 95, 0.5f, "Nutri-Score A"),
                    ScoreComponent("nova", "NOVA", 100, 0.5f, "NOVA 1"),
                ),
                concerns = listOf(
                    app.aislespy.domain.model.Concern(
                        id = "palm",
                        displayName = "Palm oil",
                        severity = 3,
                        shortWhy = "Often linked to processing.",
                        sources = listOf("https://example.com/palm"),
                        positionHint = "Near top of ingredient list",
                        matchedOn = "name:palm oil",
                    ),
                ),
                methodologyVersion = "1.0.0",
                summarySentence = "Looking good—few red flags.",
            )
        }
        val store = ConcernDetailStore()
        val vm = ResultViewModel(
            repository = FakeProductLookup(LookupOutcome.Found(food)),
            barcode = barcode,
            foodKnowledgePack = null,
            foodScoreEngine = fakeEngine,
            concernStore = store,
            defaultDispatcher = testDispatcher,
        )
        advanceUntilIdle()

        val success = vm.uiState.value as ResultUiState.Success
        assertEquals(90, success.score!!.value)
        assertEquals(ScoreBand.Excellent, success.score!!.band)
        assertEquals("Looking good—few red flags.", success.score!!.summarySentence)
        assertEquals("High confidence", success.score!!.confidenceLabel)
        assertEquals(1, success.concerns.size)
        assertEquals("palm", success.concerns[0].id)

        val detail = vm.concernDetail("palm")
        assertNotNull(detail)
        assertEquals("Palm oil", detail!!.name)
        assertEquals(3, detail.severity)
        assertEquals("Often linked to processing.", detail.fullWhy)
        assertEquals("Near top of ingredient list", detail.positionHint)
        assertEquals(listOf("https://example.com/palm"), detail.sources)
    }

    @Test
    fun notFound_emitsContributeUrls() = runTest {
        val vm = ResultViewModel(
            repository = FakeProductLookup(LookupOutcome.NotFound(barcode)),
            barcode = barcode,
            defaultDispatcher = testDispatcher,
        )
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state is ResultUiState.NotFound)
        val notFound = state as ResultUiState.NotFound
        assertEquals(barcode, notFound.barcode)
        assertEquals(ApiConfig.contributeFoodUrl(barcode), notFound.contributeFoodUrl)
        assertEquals(ApiConfig.contributeBeautyUrl(barcode), notFound.contributeBeautyUrl)
        assertTrue(notFound.contributeFoodUrl.contains("openfoodfacts.org"))
        assertTrue(notFound.contributeBeautyUrl.contains("openbeautyfacts.org"))
        assertTrue(notFound.contributeFoodUrl.contains(barcode))
        assertTrue(notFound.contributeBeautyUrl.contains(barcode))
    }

    @Test
    fun networkError_emitsMessage() = runTest {
        val vm = ResultViewModel(
            repository = FakeProductLookup(
                LookupOutcome.NetworkError(message = "timeout", barcode = barcode),
            ),
            barcode = barcode,
            defaultDispatcher = testDispatcher,
        )
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state is ResultUiState.NetworkError)
        val err = state as ResultUiState.NetworkError
        assertEquals(barcode, err.barcode)
        assertEquals("timeout", err.message)
    }

    @Test
    fun sourceFood_withStoredPair_usesStoredProductWithoutLookup() = runTest {
        val food = sampleProduct(
            name = "Stored Food",
            category = ProductCategory.Food,
            sourceDb = SourceDb.OpenFoodFacts,
            ingredientsText = "Wheat flour",
            nutriscoreGrade = 'b',
            novaGroup = 2,
        )
        val beauty = sampleProduct(
            name = "Stored Beauty",
            category = ProductCategory.Beauty,
            sourceDb = SourceDb.OpenBeautyFacts,
            ingredientsText = "Aqua",
        )
        val store = ChoicePairStore()
        store.put(barcode, food, beauty)

        var lookupCount = 0
        val repository = ProductLookup {
            lookupCount++
            error("lookup must not be called when pair is stored")
        }

        val vm = ResultViewModel(
            repository = repository,
            barcode = barcode,
            source = ResultViewModel.SOURCE_FOOD,
            foodScoreEngine = FoodScoreEngine(),
            concernStore = ConcernDetailStore(),
            choicePairStore = store,
            defaultDispatcher = testDispatcher,
        )
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state is ResultUiState.Success)
        val success = state as ResultUiState.Success
        assertEquals("Stored Food", success.product.name)
        assertEquals(ProductCategory.Food, success.product.category)
        assertEquals("Wheat flour", success.ingredientsText)
        assertNotNull(success.score)
        assertEquals(0, lookupCount)
    }

    @Test
    fun sourceBeauty_storeEmpty_refetchesAndResolvesBeauty() = runTest {
        val food = sampleProduct(
            name = "Ambiguous Food",
            category = ProductCategory.Food,
            sourceDb = SourceDb.OpenFoodFacts,
            ingredientsText = "Wheat flour",
        )
        val beauty = sampleProduct(
            name = "Ambiguous Beauty",
            category = ProductCategory.Beauty,
            sourceDb = SourceDb.OpenBeautyFacts,
            ingredientsText = "Aqua, Glycerin",
        )
        var lookupCount = 0
        val repository = ProductLookup {
            lookupCount++
            LookupOutcome.NeedsCategoryChoice(food = food, beauty = beauty)
        }
        val emptyStore = ChoicePairStore()
        assertNull(emptyStore.get(barcode))

        val vm = ResultViewModel(
            repository = repository,
            barcode = barcode,
            source = ResultViewModel.SOURCE_BEAUTY,
            beautyScoreEngine = BeautyScoreEngine(),
            concernStore = ConcernDetailStore(),
            choicePairStore = emptyStore,
            defaultDispatcher = testDispatcher,
        )
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state is ResultUiState.Success)
        val success = state as ResultUiState.Success
        assertEquals("Ambiguous Beauty", success.product.name)
        assertEquals(ProductCategory.Beauty, success.product.category)
        assertEquals(1, lookupCount)
    }

    @Test
    fun foodSuccess_recordsHistoryEntry() = runTest {
        val food = sampleProduct(
            name = "Nutella",
            brands = "Ferrero",
            category = ProductCategory.Food,
            sourceDb = SourceDb.OpenFoodFacts,
            ingredientsText = "Sugar, palm oil",
            nutriscoreGrade = 'e',
            novaGroup = 4,
        )
        val recorded = mutableListOf<HistoryEntry>()
        val writer = HistoryWriter { entry -> recorded += entry }
        val fixedNow = 1_700_000_111_000L

        val vm = ResultViewModel(
            repository = FakeProductLookup(LookupOutcome.Found(food)),
            barcode = barcode,
            foodScoreEngine = FoodScoreEngine(),
            concernStore = ConcernDetailStore(),
            historyWriter = writer,
            clock = { fixedNow },
            defaultDispatcher = testDispatcher,
            ioDispatcher = testDispatcher,
        )
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state is ResultUiState.Success)
        val success = state as ResultUiState.Success
        assertNotNull(success.score)

        assertEquals(1, recorded.size)
        val entry = recorded.single()
        assertEquals(barcode, entry.barcode)
        assertEquals("Nutella", entry.name)
        assertEquals(success.score!!.value, entry.score)
        assertEquals(ProductCategory.Food, entry.category)
        assertEquals(fixedNow, entry.scannedAtEpochMs)
    }

    @Test
    fun beautyPartial_doesNotRecordHistory() = runTest {
        val beauty = sampleProduct(
            name = "Mystery cream",
            category = ProductCategory.Beauty,
            sourceDb = SourceDb.OpenBeautyFacts,
            ingredientsText = null,
        )
        val recorded = mutableListOf<HistoryEntry>()
        val writer = HistoryWriter { entry -> recorded += entry }

        val vm = ResultViewModel(
            repository = FakeProductLookup(LookupOutcome.Found(beauty)),
            barcode = barcode,
            beautyKnowledgePack = KnowledgePack(
                version = "1.0.0",
                domain = "beauty",
                entries = emptyList(),
            ),
            beautyScoreEngine = BeautyScoreEngine(),
            concernStore = ConcernDetailStore(),
            historyWriter = writer,
            defaultDispatcher = testDispatcher,
            ioDispatcher = testDispatcher,
        )
        advanceUntilIdle()

        val state = vm.uiState.value as ResultUiState.Success
        assertNull(state.score)
        assertTrue(recorded.isEmpty())
    }

    @Test
    fun autoAmbiguous_emitsNavigateToChooserExactlyOnce() = runTest {
        val food = sampleProduct(
            name = "Ambiguous Food",
            category = ProductCategory.Food,
            sourceDb = SourceDb.OpenFoodFacts,
            ingredientsText = "Wheat flour",
        )
        val beauty = sampleProduct(
            name = "Ambiguous Beauty",
            category = ProductCategory.Beauty,
            sourceDb = SourceDb.OpenBeautyFacts,
            ingredientsText = "Aqua",
        )
        var lookupCount = 0
        val repository = ProductLookup {
            lookupCount++
            LookupOutcome.NeedsCategoryChoice(food = food, beauty = beauty)
        }
        val store = ChoicePairStore()
        val navigateEmissions = mutableListOf<ResultUiState.NavigateToCategoryChooser>()

        val vm = ResultViewModel(
            repository = repository,
            barcode = barcode,
            source = ResultViewModel.SOURCE_AUTO,
            concernStore = ConcernDetailStore(),
            choicePairStore = store,
            defaultDispatcher = testDispatcher,
        )
        // Collect from creation so we count each Navigate emission (exactly one load).
        val collectJob = launch {
            vm.uiState.collect { state ->
                if (state is ResultUiState.NavigateToCategoryChooser) {
                    navigateEmissions += state
                }
            }
        }
        advanceUntilIdle()
        collectJob.cancel()

        assertEquals(1, lookupCount)
        assertEquals(
            "navigate-to-chooser must be emitted exactly once per ambiguous auto load",
            1,
            navigateEmissions.size,
        )
        assertEquals(ResultUiState.NavigateToCategoryChooser(barcode), navigateEmissions.single())
        assertEquals(ResultUiState.NavigateToCategoryChooser(barcode), vm.uiState.value)

        // retry must not re-stack navigations without a new emission cycle; still one stable final state
        // and store holds the pair for the chooser / source=food|beauty screens
        val stored = store.get(barcode)
        assertNotNull(stored)
        assertEquals("Ambiguous Food", stored!!.food.name)
        assertEquals("Ambiguous Beauty", stored.beauty.name)

        // A second load (retry) publishes navigate again as a new cycle — count still
        // one emission per load when observed over a single load only (asserted above).
        vm.retry()
        advanceUntilIdle()
        assertEquals(2, lookupCount)
        assertEquals(ResultUiState.NavigateToCategoryChooser(barcode), vm.uiState.value)
    }

    private class FakeProductLookup(
        private val outcome: LookupOutcome,
    ) : ProductLookup {
        override suspend fun lookup(barcode: String): LookupOutcome = outcome
    }

    private fun sampleProduct(
        name: String,
        brands: String? = "Brand",
        category: ProductCategory,
        sourceDb: SourceDb,
        ingredientsText: String? = null,
        code: String = barcode,
        nutriscoreGrade: Char? = null,
        novaGroup: Int? = null,
        additivesTags: List<String> = emptyList(),
        allergensTags: List<String> = emptyList(),
        labelsTags: List<String> = emptyList(),
        ingredientsAnalysisTags: List<String> = emptyList(),
    ): Product = Product(
        barcode = code,
        name = name,
        brands = brands,
        imageUrl = null,
        category = category,
        sourceDb = sourceDb,
        ingredientsText = ingredientsText,
        ingredientsTags = emptyList(),
        additivesTags = additivesTags,
        allergensTags = allergensTags,
        labelsTags = labelsTags,
        categoriesTags = emptyList(),
        ingredientsAnalysisTags = ingredientsAnalysisTags,
        nutriscoreGrade = nutriscoreGrade,
        nutriscoreScore = null,
        novaGroup = novaGroup,
        nutriments = null as Nutriments?,
    )
}
