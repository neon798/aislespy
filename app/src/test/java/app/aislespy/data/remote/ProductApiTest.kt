package app.aislespy.data.remote

import app.aislespy.domain.model.ProductCategory
import app.aislespy.domain.model.SourceDb
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * MockWebServer coverage for OFF API client + SafeApiCall + ProductMapper (T-210).
 */
class ProductApiTest {

    private lateinit var server: MockWebServer
    private lateinit var api: OffApi
    private lateinit var client: OkHttpClient

    private val testVersion = "0.1.0"
    private val expectedUserAgent = ApiConfig.userAgent(testVersion)

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()

        client = OkHttpClient.Builder()
            .connectTimeout(ApiConfig.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(ApiConfig.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", expectedUserAgent)
                    .build()
                chain.proceed(request)
            }
            .build()

        val contentType = "application/json".toMediaType()
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(OffApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun foundResponse_mapsAllFieldsCorrectly() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(loadResource("nutella_product.json"))
                .addHeader("Content-Type", "application/json"),
        )

        val result = safeProductApiCall { api.getProduct("3017624010701") }

        assertTrue(result is ProductApiResult.Found)
        val dto = (result as ProductApiResult.Found).dto
        val product = ProductMapper.toFoodProduct(dto)

        assertEquals("3017624010701", product.barcode)
        assertEquals("Nutella", product.name)
        assertEquals("Ferrero", product.brands)
        assertEquals(
            "https://images.openfoodfacts.org/images/products/301/762/401/0701/front_en.54.400.jpg",
            product.imageUrl,
        )
        assertEquals(ProductCategory.Food, product.category)
        assertEquals(SourceDb.OpenFoodFacts, product.sourceDb)
        assertTrue(product.ingredientsText!!.contains("hazelnuts"))
        assertTrue(product.ingredientsTags.contains("en:sugar"))
        assertTrue(product.additivesTags.contains("en:e322"))
        assertTrue(product.allergensTags.contains("en:milk"))
        assertTrue(product.categoriesTags.contains("en:chocolate-spreads"))
        assertEquals('e', product.nutriscoreGrade)
        assertEquals(26, product.nutriscoreScore)
        assertEquals(4, product.novaGroup)

        val n = product.nutriments
        assertNotNull(n)
        assertEquals(539.0, n!!.energyKcal100g!!, 0.001)
        assertEquals(56.3, n.sugars100g!!, 0.001)
        assertEquals(0.107, n.salt100g!!, 0.001)
        assertEquals(10.6, n.saturatedFat100g!!, 0.001)
        assertEquals(0.0, n.fiber100g!!, 0.001)
        assertEquals(6.3, n.proteins100g!!, 0.001)

        val recorded = server.takeRequest()
        assertEquals("GET", recorded.method)
        assertTrue(recorded.path!!.startsWith("/api/v2/product/3017624010701"))
        assertEquals(ApiConfig.FIELDS, recorded.requestUrl!!.queryParameter("fields"))
        assertEquals(expectedUserAgent, recorded.getHeader("User-Agent"))
    }

    @Test
    fun statusZero_returnsNotFound() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"status":0,"code":"0000000000000","status_verbose":"product not found"}""")
                .addHeader("Content-Type", "application/json"),
        )

        val result = safeProductApiCall { api.getProduct("0000000000000") }
        assertEquals(ProductApiResult.NotFound, result)
    }

    @Test
    fun http404_returnsNotFound() = runTest {
        server.enqueue(MockResponse().setResponseCode(404).setBody("Not Found"))

        val result = safeProductApiCall { api.getProduct("999") }
        assertEquals(ProductApiResult.NotFound, result)
    }

    @Test
    fun http429_returnsErrorWithExactMessage() = runTest {
        server.enqueue(MockResponse().setResponseCode(429).setBody("rate limited"))

        val result = safeProductApiCall { api.getProduct("3017624010701") }
        assertTrue(result is ProductApiResult.Error)
        assertEquals(TOO_MANY_REQUESTS_MESSAGE, (result as ProductApiResult.Error).message)
    }

    @Test
    fun http500_returnsError() = runTest {
        server.enqueue(MockResponse().setResponseCode(500).setBody("internal error"))

        val result = safeProductApiCall { api.getProduct("3017624010701") }
        assertTrue(result is ProductApiResult.Error)
        assertTrue((result as ProductApiResult.Error).message.isNotBlank())
    }

    @Test
    fun malformedJson_returnsError() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("{not-valid-json")
                .addHeader("Content-Type", "application/json"),
        )

        val result = safeProductApiCall { api.getProduct("3017624010701") }
        assertTrue(result is ProductApiResult.Error)
    }

    @Test
    fun userAgentHeader_matchesTemplate() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"status":0,"code":"1"}""")
                .addHeader("Content-Type", "application/json"),
        )

        safeProductApiCall { api.getProduct("1") }

        val recorded = server.takeRequest()
        assertEquals(
            "AisleSpy/0.1.0 (Android; https://github.com/neon798/aislespy)",
            recorded.getHeader("User-Agent"),
        )
    }

    @Test
    fun fieldsQueryParam_isSent() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"status":0,"code":"1"}""")
                .addHeader("Content-Type", "application/json"),
        )

        safeProductApiCall { api.getProduct("1") }

        val recorded = server.takeRequest()
        assertEquals(ApiConfig.FIELDS, recorded.requestUrl!!.queryParameter("fields"))
        assertTrue(
            recorded.requestUrl!!.queryParameter("fields")!!
                .contains("nutriscore_grade") &&
                recorded.requestUrl!!.queryParameter("fields")!!
                    .contains("nutriments"),
        )
    }

    @Test
    fun emptyProductName_mapsToUnknownProduct() {
        val dto = json.decodeFromString(
            app.aislespy.data.remote.dto.ProductResponseDto.serializer(),
            """
            {
              "status": 1,
              "code": "123",
              "product": {
                "code": "123",
                "product_name": "   ",
                "brands": null
              }
            }
            """.trimIndent(),
        )
        val product = ProductMapper.toFoodProduct(dto)
        assertEquals("Unknown product", product.name)
    }

    @Test
    fun beautyMapper_setsSourceAndCategory() {
        val dto = json.decodeFromString(
            app.aislespy.data.remote.dto.ProductResponseDto.serializer(),
            """
            {
              "status": 1,
              "code": "3600523193905",
              "product": {
                "code": "3600523193905",
                "product_name": "Sample Cream",
                "brands": "TestBrand",
                "ingredients_text": "Aqua, Parfum"
              }
            }
            """.trimIndent(),
        )
        val product = ProductMapper.toBeautyProduct(dto)
        assertEquals(ProductCategory.Beauty, product.category)
        assertEquals(SourceDb.OpenBeautyFacts, product.sourceDb)
        assertEquals("Sample Cream", product.name)
    }

    private fun loadResource(name: String): String {
        val stream = requireNotNull(javaClass.classLoader!!.getResourceAsStream(name)) {
            "Missing test resource: $name"
        }
        return stream.bufferedReader().use { it.readText() }
    }
}
