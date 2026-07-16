package app.aislespy.ui.util

/**
 * Simple relative time labels for history / recent strip (no external deps).
 * Examples: "just now", "5m ago", "2h ago", "3d ago".
 */
fun formatRelativeTime(scannedAtEpochMs: Long, nowEpochMs: Long): String {
    val deltaMs = (nowEpochMs - scannedAtEpochMs).coerceAtLeast(0L)
    val minutes = deltaMs / 60_000L
    return when {
        minutes < 1L -> "just now"
        minutes < 60L -> "${minutes}m ago"
        minutes < 60L * 24L -> "${minutes / 60L}h ago"
        minutes < 60L * 24L * 14L -> "${minutes / (60L * 24L)}d ago"
        else -> {
            val days = minutes / (60L * 24L)
            if (days < 60L) "${days}d ago" else "${days / 30L}mo ago"
        }
    }
}
