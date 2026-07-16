package app.aislespy.ui.history

import app.aislespy.domain.model.HistoryEntry
import app.aislespy.domain.model.ProductCategory
import app.aislespy.domain.model.ScoreBand
import app.aislespy.ui.util.formatRelativeTime

/**
 * History list row (DOMAIN_MODELS.md [HistoryItemUi]).
 */
data class HistoryItemUi(
    val barcode: String,
    val name: String,
    val score: Int,
    val band: ScoreBand,
    val category: ProductCategory,
    val scannedAtLabel: String,
    val thumbnailUrl: String?,
)

/**
 * History screen state (DOMAIN_MODELS.md [HistoryUiState]).
 */
data class HistoryUiState(
    val items: List<HistoryItemUi> = emptyList(),
    val empty: Boolean = true,
    /** When true, show confirm dialog before wipe. */
    val showClearConfirm: Boolean = false,
)

fun HistoryEntry.toHistoryItemUi(nowEpochMs: Long): HistoryItemUi = HistoryItemUi(
    barcode = barcode,
    name = name,
    score = score,
    band = ScoreBand.fromTotal(score),
    category = category,
    scannedAtLabel = formatRelativeTime(scannedAtEpochMs, nowEpochMs),
    thumbnailUrl = thumbnailUrl,
)
