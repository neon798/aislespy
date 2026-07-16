package app.aislespy.domain.model

/**
 * One local scan history row (DOMAIN_MODELS.md).
 * Persisted in Room; never synced.
 */
data class HistoryEntry(
    val barcode: String,
    val name: String,
    val score: Int,
    val category: ProductCategory,
    val scannedAtEpochMs: Long,
    val thumbnailUrl: String?,
)
