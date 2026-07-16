package app.aislespy.data.remote

import app.aislespy.domain.model.LookupOutcome
import app.aislespy.domain.model.ProductCategory
import app.aislespy.domain.model.SourceDb
import app.aislespy.domain.scoring.CategoryResolver
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Resolution-matrix coverage for [ProductRepository] (T-220 / API_CONTRACTS parallel algorithm).
 * Two MockWebServers stand in for OFF and OBF base URLs.
 */
class ProductRepositoryTest {

    private lateinit var offServer: MockWebServer
    private lateinit var obfServer: MockWebServer
    private lateinit var repository: ProductRepository

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val barcode = "3017624010701"

    @Before
    fun setUp() {
        offServer = MockWebServer().also { it.start() }
        obfServer = MockWebServer().also { it.start() }

        val client = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()

        val contentType = "application/json".toMediaType()
        val offApi = Retrofit.Builder()
            .baseUrl(offServer.url("/"))
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(OffApi::class.java)

        val obfApi = Retrofit.Builder()
            .baseUrl(obfServer.url("/"))
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(ObfApi::class.java)

        repository = ProductRepository(
            offApi = offApi,
            obfApi = obfApi,
            categoryResolver = CategoryResolver,
        )
    }

    @After
    fun tearDown() {
        offServer.shutdown()
        obfServer.shutdown()
    }

    @Test
    fun foodOnlyFound_returnsFoundFood() = runTest {
        offServer.enqueue(jsonFound(foundFoodBody(barcode, "Nutella")))
        obfServer.enqueue(notFoundBody(barcode))

        val outcome = repository.lookup(barcode)

        assertTrue(outcome is LookupOutcome.Found)
        val product = (outcome as LookupOutcome.Found).product
        assertEquals("Nutella", product.name)
        assertEquals(ProductCategory.Food, product.category)
        assertEquals(SourceDb.OpenFoodFacts, product.sourceDb)
    }

    @Test
    fun beautyOnlyFound_returnsFoundBeauty() = runTest {
        offServer.enqueue(notFoundBody(barcode))
        obfServer.enqueue(jsonFound(foundBeautyBody(barcode, "Sample Cream")))

        val outcome = repository.lookup(barcode)

        assertTrue(outcome is LookupOutcome.Found)
        val product = (outcome as LookupOutcome.Found).product
        assertEquals("Sample Cream", product.name)
        assertEquals(ProductCategory.Beauty, product.category)
        assertEquals(SourceDb.OpenBeautyFacts, product.sourceDb)
    }

    @Test
    fun bothFoundAmbiguous_returnsNeedsCategoryChoice() = runTest {
        // Sparse food + sparse beauty (no clear signals) → Ambiguous
        offServer.enqueue(
            jsonFound(
                """
                {
                  "status": 1,
                  "code": "$barcode",
                  "product": {
                    "code": "$barcode",
                    "product_name": "Ambiguous Food Name",
                    "brands": "BrandA"
                  }
                }
                """.trimIndent(),
            ),
        )
        obfServer.enqueue(
            jsonFound(
                """
                {
                  "status": 1,
                  "code": "$barcode",
                  "product": {
                    "code": "$barcode",
                    "product_name": "Ambiguous Beauty Name",
                    "brands": "BrandB"
                  }
                }
                """.trimIndent(),
            ),
        )

        val outcome = repository.lookup(barcode)

        assertTrue(outcome is LookupOutcome.NeedsCategoryChoice)
        val choice = outcome as LookupOutcome.NeedsCategoryChoice
        assertEquals("Ambiguous Food Name", choice.food.name)
        assertEquals(ProductCategory.Food, choice.food.category)
        assertEquals("Ambiguous Beauty Name", choice.beauty.name)
        assertEquals(ProductCategory.Beauty, choice.beauty.category)
    }

    @Test
    fun bothFoundClearlyFood_returnsFoundFood() = runTest {
        // OFF: nutriscore + nova + food categories; OBF: empty categories → clearly food
        offServer.enqueue(
            jsonFound(
                """
                {
                  "status": 1,
                  "code": "$barcode",
                  "product": {
                    "code": "$barcode",
                    "product_name": "Clear Food",
                    "nutriscore_grade": "e",
                    "nova_group": 4,
                    "categories_tags": ["en:snacks", "en:chocolate-spreads"],
                    "additives_tags": ["en:e322"]
                  }
                }
                """.trimIndent(),
            ),
        )
        obfServer.enqueue(
            jsonFound(
                """
                {
                  "status": 1,
                  "code": "$barcode",
                  "product": {
                    "code": "$barcode",
                    "product_name": "Empty Beauty Mirror",
                    "categories_tags": []
                  }
                }
                """.trimIndent(),
            ),
        )

        val outcome = repository.lookup(barcode)

        assertTrue(outcome is LookupOutcome.Found)
        val product = (outcome as LookupOutcome.Found).product
        assertEquals("Clear Food", product.name)
        assertEquals(ProductCategory.Food, product.category)
        assertEquals(SourceDb.OpenFoodFacts, product.sourceDb)
    }

    @Test
    fun neitherFound_returnsNotFoundWithBarcode() = runTest {
        offServer.enqueue(notFoundBody(barcode))
        obfServer.enqueue(notFoundBody(barcode))

        val outcome = repository.lookup(barcode)

        assertTrue(outcome is LookupOutcome.NotFound)
        assertEquals(barcode, (outcome as LookupOutcome.NotFound).barcode)
    }

    @Test
    fun offError_obfNotFound_returnsNetworkError() = runTest {
        offServer.enqueue(MockResponse().setResponseCode(500).setBody("internal error"))
        obfServer.enqueue(notFoundBody(barcode))

        val outcome = repository.lookup(barcode)

        assertTrue(outcome is LookupOutcome.NetworkError)
        val err = outcome as LookupOutcome.NetworkError
        assertEquals(barcode, err.barcode)
        assertTrue(err.message.isNotBlank())
    }

    @Test
    fun bothError_returnsNetworkError() = runTest {
        offServer.enqueue(MockResponse().setResponseCode(500).setBody("off down"))
        obfServer.enqueue(MockResponse().setResponseCode(503).setBody("obf down"))

        val outcome = repository.lookup(barcode)

        assertTrue(outcome is LookupOutcome.NetworkError)
        val err = outcome as LookupOutcome.NetworkError
        assertEquals(barcode, err.barcode)
        assertTrue(err.message.isNotBlank())
    }

    @Test
    fun offNotFound_obfError_returnsNetworkError() = runTest {
        // prefer error over false NotFound when one fails
        offServer.enqueue(notFoundBody(barcode))
        obfServer.enqueue(
            MockResponse()
                .setResponseCode(429)
                .setBody("rate limited"),
        )

        val outcome = repository.lookup(barcode)

        assertTrue(outcome is LookupOutcome.NetworkError)
        val err = outcome as LookupOutcome.NetworkError
        assertEquals(barcode, err.barcode)
        assertEquals(TOO_MANY_REQUESTS_MESSAGE, err.message)
    }

    private fun jsonFound(body: String): MockResponse =
        MockResponse()
            .setResponseCode(200)
            .setBody(body)
            .addHeader("Content-Type", "application/json")

    private fun notFoundBody(code: String): MockResponse =
        MockResponse()
            .setResponseCode(200)
            .setBody("""{"status":0,"code":"$code","status_verbose":"product not found"}""")
            .addHeader("Content-Type", "application/json")

    private fun foundFoodBody(code: String, name: String): String =
        """
        {
          "status": 1,
          "code": "$code",
          "product": {
            "code": "$code",
            "product_name": "$name",
            "brands": "Ferrero",
            "nutriscore_grade": "e",
            "nova_group": 4,
            "categories_tags": ["en:chocolate-spreads"],
            "additives_tags": ["en:e322"]
          }
        }
        """.trimIndent()

    private fun foundBeautyBody(code: String, name: String): String =
        """
        {
          "status": 1,
          "code": "$code",
          "product": {
            "code": "$code",
            "product_name": "$name",
            "brands": "TestBrand",
            "categories_tags": ["en:skin-care"],
            "ingredients_text": "Aqua, Parfum"
          }
        }
        """.trimIndent()
}
