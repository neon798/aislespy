package app.aislespy.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import app.aislespy.data.local.entity.ProductCacheEntity

/**
 * Product lookup cache. TTL freshness is evaluated by the repository (not in SQL alone).
 */
@Dao
interface ProductCacheDao {
    @Query("SELECT * FROM product_cache WHERE barcode = :barcode LIMIT 1")
    suspend fun get(barcode: String): ProductCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ProductCacheEntity)

    /** Remove rows with [ProductCacheEntity.fetchedAtEpochMs] strictly before [beforeEpochMs]. */
    @Query("DELETE FROM product_cache WHERE fetchedAtEpochMs < :beforeEpochMs")
    suspend fun purgeExpired(beforeEpochMs: Long)
}
