package app.aislespy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.aislespy.ui.theme.AisleColors
import app.aislespy.ui.theme.AisleSpyTextStyles
import app.aislespy.ui.theme.PublicSans
import app.aislespy.ui.theme.severityAccent

/**
 * Severity bar + "Severity n/5" label (never color-only).
 *
 * Track ~52px wide; fill = severity × 20%; colors from design handoff.
 *
 * @param ofFiveLabel when true, uses "Severity n of 5" (ingredient detail);
 *   otherwise "Severity n/5" (concern list).
 */
@Composable
fun SeverityBar(
    severity: Int,
    modifier: Modifier = Modifier,
    trackWidth: Dp = 52.dp,
    trackHeight: Dp = 5.dp,
    ofFiveLabel: Boolean = false,
    showLabel: Boolean = true,
) {
    val level = severity.coerceIn(1, 5)
    val accent = severityAccent(level)
    val fillFraction = level / 5f
    val talkBack = if (ofFiveLabel) {
        "Severity $level of 5"
    } else {
        "Severity $level of 5"
    }
    val labelText = if (ofFiveLabel) {
        "Severity $level of 5"
    } else {
        "Severity $level/5"
    }
    val trackColor = AisleColors.current.cardBorder.copy(alpha = 0.4f)

    Row(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = talkBack
        },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .width(trackWidth)
                .height(trackHeight)
                .clip(RoundedCornerShape(3.dp))
                .background(trackColor),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fillFraction)
                    .clip(RoundedCornerShape(3.dp))
                    .background(accent),
            )
        }
        if (showLabel) {
            Text(
                text = labelText,
                style = AisleSpyTextStyles.badgeChip.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = if (ofFiveLabel) 12.sp else 10.5.sp,
                    fontFamily = PublicSans,
                ),
                color = accent,
            )
        }
    }
}
