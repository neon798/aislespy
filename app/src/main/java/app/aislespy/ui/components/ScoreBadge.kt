package app.aislespy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.aislespy.domain.model.ScoreBand
import app.aislespy.ui.theme.scoreBandColors

/**
 * Circular 1–100 score badge with AA-ish chip colors (history, recent chips).
 */
@Composable
fun ScoreBadge(
    score: Int,
    band: ScoreBand,
    modifier: Modifier = Modifier,
    contentDescription: String? = "Score $score",
    minSize: Dp = 40.dp,
    compact: Boolean = false,
) {
    val colors = scoreBandColors(band)
    val size = if (compact) 28.dp else minSize
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = size, minHeight = size)
            .clip(CircleShape)
            .background(colors.chipContainer)
            .padding(horizontal = if (compact) 4.dp else 6.dp, vertical = if (compact) 2.dp else 4.dp)
            .then(
                if (contentDescription != null) {
                    Modifier.semantics { this.contentDescription = contentDescription }
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = score.toString(),
            style = if (compact) {
                MaterialTheme.typography.labelSmall
            } else {
                MaterialTheme.typography.labelLarge
            },
            color = colors.chipContent,
        )
    }
}
