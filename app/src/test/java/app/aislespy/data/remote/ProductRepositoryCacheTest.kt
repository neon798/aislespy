package app.aislespy.data.remote

import app.aislespy.data.local.CachedLookupPayload
import app.aislespy.data.local.ProductCacheDao
import app.aislespy.data.local.entity.ProductCacheEntity
import app.aislespy.domain.model.LookupOutcome
import app.aislespy.domain.model.Product
import app.aislespy.domain.model.ProductCategory
import app.aislespy.domain.model.SourceDb
import app.aislespy.domain.scoring.CategoryResolver
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/**
 * Cache TTL behaviour for [ProductRepository] (T-500 / API_CONTRACTS).
 * Uses a fake [ProductCacheDao] and injectable clock — pure JVM, no Robolectric.
 */
class ProductRepositoryCacheTest {

    private lateinit var offServer: MockWebServer
    private lateinit var obfServer: MockWebServer
    private lateinit var fakeCache: FakeProductCacheDao
    private val clockMs = AtomicLong(1_700_000_000_000L)
    private val ttlMs = ApiConfig.PRODUCT_CACHE_TTL_MS
    private val json = ProductRepository.defaultCacheJson
    private val barcode = "3017624010701"

    private val wireJson = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Before
    fun setUp() {
        offServer = MockWebServer().also { it.start() }
        obfServer = MockWebServer().also { it.start() }
        fakeCache = FakeProductCacheDao()
        clockMs.set(1_700_000_000_000L)
    }

    @After
    fun tearDown() {
        offServer.shutdown()
        obfServer.shutdown()
    }

    @Test
    fun freshCacheHit_returnsCachedFound_withoutNetwork() = runTest {
        val product = sampleFood("Cached Nutella")
        seedCache(
            barcode = barcode,
            payload = CachedLookupPayload.Single(product),
            source = ProductCacheEntity.SOURCE_FOOD,
            fetchedAt = clockMs.get() - 1_000L, // 1s ago — fresh
        )

        val repo = buildRepo()
        val outcome = repo.lookup(barcode)

        assertTrue(outcome is LookupOutcome.Found)
        assertEquals("Cached Nutella", (outcome as LookupOutcome.Found).product.name)
        assertEquals(0, offServer.requestCount)
        assertEquals(0, obfServer.requestCount)
    }

    @Test
    fun freshCacheHit_pair_returnsNeedsCategoryChoice_withoutNetwork() = runTest {
        val food = sampleFood("Amb Food")
        val beauty = sampleBeauty("Amb Beauty")
        seedCache(
            barcode = barcode,
            payload = CachedLookupPayload.Pair(food = food, beauty = beauty),
            source = ProductCacheEntity.SOURCE_PAIR,
            fetchedAt = clockMs.get(),
        )

        val repo = buildRepo()
        val outcome = repo.lookup(barcode)

        assertTrue(outcome is LookupOutcome.NeedsCategoryChoice)
        val choice = outcome as LookupOutcome.NeedsCategoryChoice
        assertEquals("Amb Food", choice.food.name)
        assertEquals("Amb Beauty", choice.beauty.name)
        assertEquals(0, offServer.requestCount)
        assertEquals(0, obfServer.requestCount)
    }

    @Test
    fun expiredCache_triggersNetwork() = runTest {
        val product = sampleFood("Stale")
        seedCache(
            barcode = barcode,
            payload = CachedLookupPayload.Single(product),
            source = ProductCacheEntity.SOURCE_FOOD,
            fetchedAt = clockMs.get() - ttlMs - 1L,
        )
        offServer.enqueue(jsonFound(foundFoodBody(barcode, "Fresh Nutella")))
        obfServer.enqueue(notFoundBody(barcode))

        val repo = buildRepo()
        val outcome = repo.lookup(barcode)

        assertTrue(outcome is LookupOutcome.Found)
        assertEquals("Fresh Nutella", (outcome as LookupOutcome.Found).product.name)
        assertEquals(1, offServer.requestCount)
        assertEquals(1, obfServer.requestCount)
    }

    @Test
    fun networkSuccess_storesProductInCache() = runTest {
        offServer.enqueue(jsonFound(foundFoodBody(barcode, "Network Nutella")))
        obfServer.enqueue(notFoundBody(barcode))

        val repo = buildRepo()
        val outcome = repo.lookup(barcode)

        assertTrue(outcome is LookupOutcome.Found)
        val row = fakeCache.get(barcode)
        assertTrue(row != null)
        assertEquals(ProductCacheEntity.SOURCE_FOOD, row!!.sourceCategory)
        assertEquals(clockMs.get(), row.fetchedAtEpochMs)
        val payload = json.decodeFromString<CachedLookupPayload>(row.payloadJson)
        assertTrue(payload is CachedLookupPayload.Single)
        assertEquals("Network Nutella", (payload as CachedLookupPayload.Single).product.name)
    }

    @Test
    fun networkSuccess_ambiguous_storesPair() = runTest {
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

        val repo = buildRepo()
        val outcome = repo.lookup(barcode)

        assertTrue(outcome is LookupOutcome.NeedsCategoryChoice)
        val row = fakeCache.get(barcode)
        assertTrue(row != null)
        assertEquals(ProductCacheEntity.SOURCE_PAIR, row!!.sourceCategory)
        val payload = json.decodeFromString<CachedLookupPayload>(row.payloadJson)
        assertTrue(payload is CachedLookupPayload.Pair)
    }

    @Test
    fun networkError_doesNotStoreCache() = runTest {
        offServer.enqueue(MockResponse().setResponseCode(500).setBody("down"))
        obfServer.enqueue(MockResponse().setResponseCode(503).setBody("down"))

        val repo = buildRepo()
        val outcome = repo.lookup(barcode)

        assertTrue(outcome is LookupOutcome.NetworkError)
        assertNull(fakeCache.get(barcode))
    }

    @Test
    fun expiredCache_andNetworkError_returnsNetworkError() = runTest {
        seedCache(
            barcode = barcode,
            payload = CachedLookupPayload.Single(sampleFood("Stale offline")),
            source = ProductCacheEntity.SOURCE_FOOD,
            fetchedAt = clockMs.get() - ttlMs - 5_000L,
        )
        offServer.enqueue(MockResponse().setResponseCode(500).setBody("down"))
        obfServer.enqueue(MockResponse().setResponseCode(500).setBody("down"))

        val repo = buildRepo()
        val outcome = repo.lookup(barcode)

        assertTrue(outcome is LookupOutcome.NetworkError)
        // Stale row may still exist; must not be returned as Found
        assertTrue(outcome !is LookupOutcome.Found)
    }

    private fun buildRepo(): ProductRepository {
        val client = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()
        val contentType = "application/json".toMediaType()
        val offApi = Retrofit.Builder()
            .baseUrl(offServer.url("/"))
            .client(client)
            .addConverterFactory(wireJson.asConverterFactory(contentType))
            .build()
            .create(OffApi::class.java)
        val obfApi = Retrofit.Builder()
            .baseUrl(obfServer.url("/"))
            .client(client)
            .addConverterFactory(wireJson.asConverterFactory(contentType))
            .build()
            .create(ObfApi::class.java)

        return ProductRepository(
            offApi = offApi,
            obfApi = obfApi,
            categoryResolver = CategoryResolver,
            productCacheDao = fakeCache,
            clock = { clockMs.get() },
            cacheTtlMs = ttlMs,
            json = json,
        )
    }

    private fun seedCache(
        barcode: String,
        payload: CachedLookupPayload,
        source: String,
        fetchedAt: Long,
    ) {
        fakeCache.store(
            ProductCacheEntity(
                barcode = barcode,
                payloadJson = json.encodeToString(payload),
                sourceCategory = source,
                fetchedAtEpochMs = fetchedAt,
            ),
        )
    }

    private fun sampleFood(name: String): Product = Product(
        barcode = barcode,
        name = name,
        brands = "Ferrero",
        imageUrl = null,
        category = ProductCategory.Food,
        sourceDb = SourceDb.OpenFoodFacts,
        ingredientsText = "Sugar",
        ingredientsTags = emptyList(),
        additivesTags = emptyList(),
        allergensTags = emptyList(),
        labelsTags = emptyList(),
        categoriesTags = listOf("en:snacks"),
        nutriscoreGrade = 'e',
        nutriscoreScore = null,
        novaGroup = 4,
        nutriments = null,
    )

    private fun sampleBeauty(name: String): Product = Product(
        barcode = barcode,
        name = name,
        brands = "Brand",
        imageUrl = null,
        category = ProductCategory.Beauty,
        sourceDb = SourceDb.OpenBeautyFacts,
        ingredientsText = "Aqua",
        ingredientsTags = emptyList(),
        additivesTags = emptyList(),
        allergensTags = emptyList(),
        labelsTags = emptyList(),
        categoriesTags = listOf("en:skin-care"),
        nutriscoreGrade = null,
        nutriscoreScore = null,
        novaGroup = null,
        nutriments = null,
    )

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

    /** In-memory fake — implements Room DAO contract without Android. */
    class FakeProductCacheDao : ProductCacheDao {
        private val rows = linkedMapOf<String, ProductCacheEntity>()

        fun store(entity: ProductCacheEntity) {
            rows[entity.barcode] = entity
        }

        override suspend fun get(barcode: String): ProductCacheEntity? = rows[barcode]

        override suspend fun upsert(entity: ProductCacheEntity) {
            rows[entity.barcode] = entity
        }

        override suspend fun purgeExpired(beforeEpochMs: Long) {
            rows.entries.removeAll { it.value.fetchedAtEpochMs < beforeEpochMs }
        }
    }
}
