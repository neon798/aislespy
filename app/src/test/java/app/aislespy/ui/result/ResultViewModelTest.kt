package app.aislespy.ui.result

import app.aislespy.data.knowledge.KnowledgePack
import app.aislespy.data.knowledge.KnowledgePackEntry
import app.aislespy.data.remote.ApiConfig
import app.aislespy.data.remote.ProductLookup
import app.aislespy.domain.model.Confidence
import app.aislespy.domain.model.LookupOutcome
import app.aislespy.domain.model.Nutriments
import app.aislespy.domain.model.Product
import app.aislespy.domain.model.ProductCategory
import app.aislespy.domain.model.ScoreBand
import app.aislespy.domain.model.ScoreComponent
import app.aislespy.domain.model.ScoreResult
import app.aislespy.domain.model.SourceDb
import app.aislespy.domain.scoring.FoodScoreEngine
import app.aislespy.domain.scoring.ScoreEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
            knowledgePack = pack,
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
    fun successBeauty_showsComingSoonWithoutScore() = runTest {
        val beauty = sampleProduct(
            name = "Shampoo",
            category = ProductCategory.Beauty,
            sourceDb = SourceDb.OpenBeautyFacts,
            ingredientsText = "Aqua, Sodium Laureth Sulfate",
        )
        val vm = ResultViewModel(
            repository = FakeProductLookup(LookupOutcome.Found(beauty)),
            barcode = barcode,
            knowledgePack = null,
            foodScoreEngine = FoodScoreEngine(),
            concernStore = ConcernDetailStore(),
            defaultDispatcher = testDispatcher,
        )
        advanceUntilIdle()

        val state = vm.uiState.value as ResultUiState.Success
        assertTrue(state.beautyScoringPending)
        assertNull(state.score)
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
            knowledgePack = null,
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
    fun needsCategoryChoice_thenChooseFood_emitsSuccessWithoutRelookup() = runTest {
        val food = sampleProduct(
            name = "Ambiguous Food",
            category = ProductCategory.Food,
            sourceDb = SourceDb.OpenFoodFacts,
            ingredientsText = "Wheat flour",
            nutriscoreGrade = 'b',
            novaGroup = 2,
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

        val vm = ResultViewModel(
            repository = repository,
            barcode = barcode,
            foodScoreEngine = FoodScoreEngine(),
            concernStore = ConcernDetailStore(),
            defaultDispatcher = testDispatcher,
        )
        advanceUntilIdle()

        val choiceState = vm.uiState.value
        assertTrue(choiceState is ResultUiState.NeedsCategoryChoice)
        val choice = choiceState as ResultUiState.NeedsCategoryChoice
        assertEquals("Ambiguous Food", choice.foodName)
        assertEquals("Ambiguous Beauty", choice.beautyName)
        assertEquals(1, lookupCount)

        vm.choose(ProductCategory.Food)
        advanceUntilIdle()
        val successState = vm.uiState.value
        assertTrue(successState is ResultUiState.Success)
        val success = successState as ResultUiState.Success
        assertEquals("Ambiguous Food", success.product.name)
        assertEquals(ProductCategory.Food, success.product.category)
        assertEquals("Wheat flour", success.ingredientsText)
        assertNotNull(success.score)
        assertEquals(1, lookupCount)
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
        allergensTags = emptyList(),
        labelsTags = emptyList(),
        categoriesTags = emptyList(),
        nutriscoreGrade = nutriscoreGrade,
        nutriscoreScore = null,
        novaGroup = novaGroup,
        nutriments = null as Nutriments?,
    )
}
