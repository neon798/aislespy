package app.aislespy.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import app.aislespy.data.local.entity.HistoryEntryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Local scan history CRUD. Newest first for list / recent strip.
 */
@Dao
interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: HistoryEntryEntity)

    @Query("SELECT * FROM history ORDER BY scannedAtEpochMs DESC")
    fun observeLatestFirst(): Flow<List<HistoryEntryEntity>>

    @Query("SELECT * FROM history ORDER BY scannedAtEpochMs DESC LIMIT :limit")
    fun observeLatestFirst(limit: Int): Flow<List<HistoryEntryEntity>>

    @Query("DELETE FROM history WHERE barcode = :barcode")
    suspend fun deleteByBarcode(barcode: String)

    @Query("DELETE FROM history")
    suspend fun clearAll()
}
