package app.aislespy.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import app.aislespy.data.local.entity.HistoryEntryEntity
import app.aislespy.data.local.entity.ProductCacheEntity

/**
 * App Room database: scan history + product lookup cache.
 *
 * Pre-1.0: [fallbackToDestructiveMigration] is acceptable when building the DB
 * (no production user data to preserve yet). Bump version + add migrations before
 * shipping a schema that must survive upgrades.
 */
@Database(
    entities = [
        HistoryEntryEntity::class,
        ProductCacheEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AisleSpyDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
    abstract fun productCacheDao(): ProductCacheDao
}
