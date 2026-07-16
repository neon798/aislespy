package app.aislespy.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import app.aislespy.domain.model.HistoryEntry
import app.aislespy.domain.model.ProductCategory

/**
 * Room row for local scan history (barcode is PK — re-scan upserts).
 */
@Entity(tableName = "history")
data class HistoryEntryEntity(
    @PrimaryKey val barcode: String,
    val name: String,
    val score: Int,
    /** [ProductCategory.name] e.g. Food / Beauty */
    val category: String,
    val scannedAtEpochMs: Long,
    val thumbnailUrl: String?,
)

fun HistoryEntryEntity.toDomain(): HistoryEntry = HistoryEntry(
    barcode = barcode,
    name = name,
    score = score,
    category = ProductCategory.valueOf(category),
    scannedAtEpochMs = scannedAtEpochMs,
    thumbnailUrl = thumbnailUrl,
)

fun HistoryEntry.toEntity(): HistoryEntryEntity = HistoryEntryEntity(
    barcode = barcode,
    name = name,
    score = score,
    category = category.name,
    scannedAtEpochMs = scannedAtEpochMs,
    thumbnailUrl = thumbnailUrl,
)
