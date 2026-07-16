package app.aislespy.data.local

import app.aislespy.data.local.entity.toDomain
import app.aislespy.data.local.entity.toEntity
import app.aislespy.domain.model.HistoryEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Domain-facing history store over [HistoryDao].
 * UI and ViewModels depend on this rather than Room entities.
 */
class HistoryRepository(
    private val historyDao: HistoryDao,
) {
    fun observeLatestFirst(): Flow<List<HistoryEntry>> =
        historyDao.observeLatestFirst().map { rows -> rows.map { it.toDomain() } }

    fun observeRecent(limit: Int = RECENT_LIMIT): Flow<List<HistoryEntry>> =
        historyDao.observeLatestFirst(limit).map { rows -> rows.map { it.toDomain() } }

    suspend fun upsert(entry: HistoryEntry) {
        historyDao.upsert(entry.toEntity())
    }

    suspend fun deleteByBarcode(barcode: String) {
        historyDao.deleteByBarcode(barcode)
    }

    suspend fun clearAll() {
        historyDao.clearAll()
    }

    companion object {
        const val RECENT_LIMIT = 10
    }
}

/**
 * Seam for history writes from result scoring (testable without Room).
 */
fun interface HistoryWriter {
    suspend fun upsert(entry: HistoryEntry)
}
