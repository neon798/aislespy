package app.aislespy.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room row for OFF/OBF product lookup cache (TTL enforced in [app.aislespy.data.remote.ProductRepository]).
 *
 * [payloadJson] is kotlinx-serialization JSON for [app.aislespy.data.local.CachedLookupPayload]
 * (single product or food+beauty pair).
 *
 * [sourceCategory] is one of [SOURCE_FOOD], [SOURCE_BEAUTY], [SOURCE_PAIR].
 */
@Entity(tableName = "product_cache")
data class ProductCacheEntity(
    @PrimaryKey val barcode: String,
    val payloadJson: String,
    val sourceCategory: String,
    val fetchedAtEpochMs: Long,
) {
    companion object {
        const val SOURCE_FOOD = "food"
        const val SOURCE_BEAUTY = "beauty"
        const val SOURCE_PAIR = "pair"
    }
}
