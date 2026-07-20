package app.aislespy.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import app.aislespy.domain.model.ScoreBand

/**
 * Theme-aware score band colors for arcs, labels, and filled chips.
 * Chip container/content pairs are chosen for ~WCAG AA contrast.
 *
 * Band hex values are locked by the product brief / design handoff — do not restyle.
 */
@Immutable
data class ScoreBandColors(
    /** Arc / unfilled label color for the current theme. */
    val accent: Color,
    /** Filled chip/badge background. */
    val chipContainer: Color,
    /** Text on [chipContainer]. */
    val chipContent: Color,
)

@Composable
fun scoreBandColors(band: ScoreBand): ScoreBandColors {
    val dark = isSystemInDarkTheme()
    val accent = when (band) {
        ScoreBand.Excellent -> if (dark) scoreExcellentDark else scoreExcellentLight
        ScoreBand.Ok -> if (dark) scoreOkDark else scoreOkLight
        ScoreBand.Poor -> if (dark) scorePoorDark else scorePoorLight
        ScoreBand.Bad -> if (dark) scoreBadDark else scoreBadLight
    }
    val chipContainer = when (band) {
        ScoreBand.Excellent -> scoreChipExcellent
        ScoreBand.Ok -> scoreChipOk
        ScoreBand.Poor -> scoreChipPoor
        ScoreBand.Bad -> scoreChipBad
    }
    return ScoreBandColors(
        accent = accent,
        chipContainer = chipContainer,
        chipContent = scoreChipOn,
    )
}

/**
 * Severity 1–5 colors (design handoff).
 * Always pair with a numeric "Severity n/5" label — never color-only.
 */
@Immutable
data class SeverityColors(
    val accent: Color,
    val container: Color,
)

fun severityAccent(severity: Int): Color = when (severity.coerceIn(1, 5)) {
    1, 2 -> SeverityLow
    3 -> SeverityMedium
    4 -> SeverityHigh
    else -> SeverityCritical
}

@Composable
fun severityColors(severity: Int): SeverityColors {
    val accent = severityAccent(severity)
    val dark = isSystemInDarkTheme()
    return SeverityColors(
        accent = accent,
        container = accent.copy(alpha = if (dark) 0.28f else 0.14f),
    )
}
