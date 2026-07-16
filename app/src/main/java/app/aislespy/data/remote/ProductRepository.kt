package app.aislespy.data.remote

import app.aislespy.data.local.CachedLookupPayload
import app.aislespy.data.local.ProductCacheDao
import app.aislespy.data.local.entity.ProductCacheEntity
import app.aislespy.domain.model.LookupOutcome
import app.aislespy.domain.model.ProductCategory
import app.aislespy.domain.scoring.CategoryResolver
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Seam for product lookup (production dual-DB repo or test fakes).
 */
fun interface ProductLookup {
    suspend fun lookup(barcode: String): LookupOutcome
}

/**
 * Dual-database product lookup (Open Food Facts + Open Beauty Facts).
 *
 * Implements the parallel algorithm from API_CONTRACTS.md exactly, with Room
 * product_cache (TTL [ApiConfig.PRODUCT_CACHE_TTL_MS]):
 * - Fresh cache hit → Found or NeedsCategoryChoice without network
 * - Miss / expired → network; on success store product or pair
 * - Expired + offline → NetworkError (do not serve stale cache)
 */
class ProductRepository(
    private val offApi: OffApi,
    private val obfApi: ObfApi,
    private val categoryResolver: CategoryResolver = CategoryResolver,
    private val productCacheDao: ProductCacheDao? = null,
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val cacheTtlMs: Long = ApiConfig.PRODUCT_CACHE_TTL_MS,
    private val json: Json = defaultCacheJson,
) : ProductLookup {

    override suspend fun lookup(barcode: String): LookupOutcome {
        val cachedOutcome = readFreshCache(barcode)
        if (cachedOutcome != null) return cachedOutcome

        return coroutineScope {
            val offDeferred = async {
                safeProductApiCall { offApi.getProduct(barcode) }
            }
            val obfDeferred = async {
                safeProductApiCall { obfApi.getProduct(barcode) }
            }

            val offResult = offDeferred.await()
            val obfResult = obfDeferred.await()

            val outcome = combineResults(barcode, offResult, obfResult)
            storeSuccessfulLookup(barcode, outcome)
            outcome
        }
    }

    /**
     * @return non-null when a cache row exists and is within TTL.
     */
    private suspend fun readFreshCache(barcode: String): LookupOutcome? {
        val dao = productCacheDao ?: return null
        val row = dao.get(barcode) ?: return null
        val ageMs = clock() - row.fetchedAtEpochMs
        if (ageMs < 0L || ageMs >= cacheTtlMs) {
            // Expired: leave row for opportunistic purge; do not serve stale data.
            return null
        }
        return try {
            when (val payload = json.decodeFromString<CachedLookupPayload>(row.payloadJson)) {
                is CachedLookupPayload.Single -> LookupOutcome.Found(payload.product)
                is CachedLookupPayload.Pair -> LookupOutcome.NeedsCategoryChoice(
                    food = payload.food,
                    beauty = payload.beauty,
                )
            }
        } catch (_: Exception) {
            // Corrupt payload — treat as miss and fall through to network.
            null
        }
    }

    private suspend fun storeSuccessfulLookup(barcode: String, outcome: LookupOutcome) {
        val dao = productCacheDao ?: return
        val payload: CachedLookupPayload
        val sourceCategory: String
        when (outcome) {
            is LookupOutcome.Found -> {
                payload = CachedLookupPayload.Single(outcome.product)
                sourceCategory = when (outcome.product.category) {
                    ProductCategory.Food -> ProductCacheEntity.SOURCE_FOOD
                    ProductCategory.Beauty -> ProductCacheEntity.SOURCE_BEAUTY
                }
            }
            is LookupOutcome.NeedsCategoryChoice -> {
                payload = CachedLookupPayload.Pair(food = outcome.food, beauty = outcome.beauty)
                sourceCategory = ProductCacheEntity.SOURCE_PAIR
            }
            is LookupOutcome.NotFound,
            is LookupOutcome.NetworkError,
            -> return
        }
        val entity = ProductCacheEntity(
            barcode = barcode,
            payloadJson = json.encodeToString(payload),
            sourceCategory = sourceCategory,
            fetchedAtEpochMs = clock(),
        )
        dao.upsert(entity)
        // Opportunistic cleanup of rows older than TTL.
        dao.purgeExpired(clock() - cacheTtlMs)
    }

    private fun combineResults(
        barcode: String,
        offResult: ProductApiResult,
        obfResult: ProductApiResult,
    ): LookupOutcome {
        val offError = offResult as? ProductApiResult.Error
        val obfError = obfResult as? ProductApiResult.Error
        val offFound = offResult as? ProductApiResult.Found
        val obfFound = obfResult as? ProductApiResult.Found

        // both Error → NetworkError
        if (offError != null && obfError != null) {
            return LookupOutcome.NetworkError(
                message = offError.message.ifBlank { obfError.message },
                barcode = barcode,
            )
        }

        // both Found → resolver heuristics
        if (offFound != null && obfFound != null) {
            val food = ProductMapper.toFoodProduct(offFound.dto)
            val beauty = ProductMapper.toBeautyProduct(obfFound.dto)
            return when (categoryResolver.resolve(food, beauty)) {
                CategoryResolver.Decision.Food -> LookupOutcome.Found(food)
                CategoryResolver.Decision.Beauty -> LookupOutcome.Found(beauty)
                CategoryResolver.Decision.Ambiguous ->
                    LookupOutcome.NeedsCategoryChoice(food = food, beauty = beauty)
            }
        }

        // one Found → Found
        if (offFound != null) {
            return LookupOutcome.Found(ProductMapper.toFoodProduct(offFound.dto))
        }
        if (obfFound != null) {
            return LookupOutcome.Found(ProductMapper.toBeautyProduct(obfFound.dto))
        }

        // either Error and neither Found → NetworkError (prefer error over false NotFound)
        val singleError = offError ?: obfError
        if (singleError != null) {
            return LookupOutcome.NetworkError(
                message = singleError.message,
                barcode = barcode,
            )
        }

        return LookupOutcome.NotFound(barcode)
    }

    companion object {
        val defaultCacheJson: Json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            classDiscriminator = "type"
        }
    }
}
