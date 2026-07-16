package app.aislespy.ui.result

import app.aislespy.data.remote.ApiConfig
import app.aislespy.data.remote.ProductLookup
import app.aislespy.domain.model.LookupOutcome
import app.aislespy.domain.model.Nutriments
import app.aislespy.domain.model.Product
import app.aislespy.domain.model.ProductCategory
import app.aislespy.domain.model.SourceDb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
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
    fun successFood_emitsSuccessWithHeaderAndIngredients() = runTest {
        val food = sampleProduct(
            name = "Nutella",
            brands = "Ferrero",
            category = ProductCategory.Food,
            sourceDb = SourceDb.OpenFoodFacts,
            ingredientsText = "Sugar, palm oil, hazelnuts",
        )
        val vm = ResultViewModel(
            repository = FakeProductLookup(LookupOutcome.Found(food)),
            barcode = barcode,
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
    }

    @Test
    fun notFound_emitsContributeUrls() = runTest {
        val vm = ResultViewModel(
            repository = FakeProductLookup(LookupOutcome.NotFound(barcode)),
            barcode = barcode,
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

        val vm = ResultViewModel(repository = repository, barcode = barcode)
        advanceUntilIdle()

        val choiceState = vm.uiState.value
        assertTrue(choiceState is ResultUiState.NeedsCategoryChoice)
        val choice = choiceState as ResultUiState.NeedsCategoryChoice
        assertEquals("Ambiguous Food", choice.foodName)
        assertEquals("Ambiguous Beauty", choice.beautyName)
        assertEquals(1, lookupCount)

        vm.choose(ProductCategory.Food)
        // choose is synchronous (no coroutine) once pair is cached
        val successState = vm.uiState.value
        assertTrue(successState is ResultUiState.Success)
        val success = successState as ResultUiState.Success
        assertEquals("Ambiguous Food", success.product.name)
        assertEquals(ProductCategory.Food, success.product.category)
        assertEquals("Wheat flour", success.ingredientsText)
        // Must not re-request the network / repository
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
    ): Product = Product(
        barcode = code,
        name = name,
        brands = brands,
        imageUrl = null,
        category = category,
        sourceDb = sourceDb,
        ingredientsText = ingredientsText,
        ingredientsTags = emptyList(),
        additivesTags = emptyList(),
        allergensTags = emptyList(),
        labelsTags = emptyList(),
        categoriesTags = emptyList(),
        nutriscoreGrade = null,
        nutriscoreScore = null,
        novaGroup = null,
        nutriments = null as Nutriments?,
    )
}
