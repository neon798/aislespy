package app.aislespy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.aislespy.ui.theme.severityColors

/**
 * Severity indicator 1–5 (docs/COMPONENTS.md).
 * TalkBack: “Severity N of 5”.
 */
@Composable
fun SeverityChip(
    severity: Int,
    modifier: Modifier = Modifier,
) {
    val level = severity.coerceIn(1, 5)
    val colors = severityColors(level)
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 36.dp, minHeight = 36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(colors.container)
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .semantics {
                contentDescription = "Severity $level of 5"
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = level.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = colors.accent,
        )
    }
}
