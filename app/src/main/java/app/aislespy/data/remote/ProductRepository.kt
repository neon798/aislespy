package app.aislespy.data.remote

import app.aislespy.domain.model.LookupOutcome
import app.aislespy.domain.scoring.CategoryResolver
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Seam for product lookup (production dual-DB repo or test fakes).
 */
fun interface ProductLookup {
    suspend fun lookup(barcode: String): LookupOutcome
}

/**
 * Dual-database product lookup (Open Food Facts + Open Beauty Facts).
 *
 * Implements the parallel algorithm from API_CONTRACTS.md exactly.
 * Mapping: Found DTOs → domain [app.aislespy.domain.model.Product] via [ProductMapper].
 */
class ProductRepository(
    private val offApi: OffApi,
    private val obfApi: ObfApi,
    private val categoryResolver: CategoryResolver = CategoryResolver,
) : ProductLookup {

    /**
     * Look up [barcode] against OFF and OBF in parallel.
     *
     * // TODO(T-500): Check Room product_cache first (TTL ~7 days). On fresh hit,
     * // return Found(cached) or NeedsCategoryChoice if the cached row stored a pair.
     * // Only hit the network when cache is cold or expired.
     */
    override suspend fun lookup(barcode: String): LookupOutcome = coroutineScope {
        val offDeferred = async {
            safeProductApiCall { offApi.getProduct(barcode) }
        }
        val obfDeferred = async {
            safeProductApiCall { obfApi.getProduct(barcode) }
        }

        val offResult = offDeferred.await()
        val obfResult = obfDeferred.await()

        combineResults(barcode, offResult, obfResult)
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
}
