package app.aislespy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.aislespy.ui.theme.PublicSans
import app.aislespy.ui.theme.severityColors

/**
 * Compact severity indicator 1–5.
 * Prefer [SeverityBar] on result / detail screens (bar + "Severity n/5").
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
            .defaultMinSize(minWidth = 36.dp, minHeight = 28.dp)
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
            fontFamily = PublicSans,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = colors.accent,
        )
    }
}
