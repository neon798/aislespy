package app.aislespy.data.local

import app.aislespy.domain.model.Product
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Serializable body stored in [app.aislespy.data.local.entity.ProductCacheEntity.payloadJson].
 * Mirrors cache-hit outcomes: single Found product or ambiguous food/beauty pair.
 */
@Serializable
sealed class CachedLookupPayload {
    @Serializable
    @SerialName("single")
    data class Single(val product: Product) : CachedLookupPayload()

    @Serializable
    @SerialName("pair")
    data class Pair(
        val food: Product,
        val beauty: Product,
    ) : CachedLookupPayload()
}
