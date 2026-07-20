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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.aislespy.domain.model.ScoreBand
import app.aislespy.ui.theme.BricolageGrotesque
import app.aislespy.ui.theme.scoreBandColors

/**
 * Score tile for history / recents — filled band chip bg, white Bricolage numeral.
 * Uses locked [scoreBandColors] (do not restyle band semantics).
 */
@Composable
fun ScoreBadge(
    score: Int,
    band: ScoreBand,
    modifier: Modifier = Modifier,
    contentDescription: String? = "Score $score",
    minSize: Dp = 44.dp,
    compact: Boolean = false,
) {
    val colors = scoreBandColors(band)
    val width = if (compact) 32.dp else minSize
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = width, minHeight = if (compact) 28.dp else 36.dp)
            .clip(RoundedCornerShape(if (compact) 8.dp else 11.dp))
            .background(colors.chipContainer)
            .padding(horizontal = if (compact) 6.dp else 0.dp, vertical = if (compact) 4.dp else 8.dp)
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
            fontFamily = BricolageGrotesque,
            fontWeight = FontWeight.ExtraBold,
            fontSize = if (compact) 12.sp else 15.sp,
            color = colors.chipContent,
        )
    }
}

/**
 * Filled score-band chip — band chip bg + white 12/800 UPPERCASE label.
 * Keeps [scoreBandColors] for locked band semantics.
 */
@Composable
fun ScoreBandChip(
    label: String,
    band: ScoreBand,
    modifier: Modifier = Modifier,
) {
    val colors = scoreBandColors(band)
    Box(
        modifier = modifier
            .clip(app.aislespy.ui.theme.AisleSpyShapes.pill)
            .background(colors.chipContainer)
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label.uppercase(),
            style = app.aislespy.ui.theme.AisleSpyTextStyles.bandChip,
            color = colors.chipContent,
        )
    }
}
