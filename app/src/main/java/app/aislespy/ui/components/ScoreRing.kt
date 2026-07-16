package app.aislespy.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.aislespy.domain.model.ScoreBand
import app.aislespy.ui.theme.scoreBad
import app.aislespy.ui.theme.scoreExcellent
import app.aislespy.ui.theme.scoreOk
import app.aislespy.ui.theme.scorePoor

/**
 * Hero 1–100 score ring (docs/COMPONENTS.md).
 *
 * TalkBack: “Score {value} out of 100, {label}”.
 */
@Composable
fun ScoreRing(
    value: Int,
    band: ScoreBand,
    label: String,
    modifier: Modifier = Modifier,
    animated: Boolean = true,
    size: Dp = 160.dp,
    strokeWidth: Dp = 14.dp,
) {
    val clamped = value.coerceIn(1, 100)
    val bandColor = bandColor(band)
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    val progress = remember { Animatable(0f) }
    LaunchedEffect(clamped, animated) {
        if (animated) {
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = clamped / 100f,
                animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
            )
        } else {
            progress.snapTo(clamped / 100f)
        }
    }

    val a11y = "Score $clamped out of 100, $label"

    Column(
        modifier = modifier.semantics { contentDescription = a11y },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(size),
        ) {
            Canvas(modifier = Modifier.size(size)) {
                val stroke = strokeWidth.toPx()
                val diameter = this.size.minDimension - stroke
                val topLeft = Offset(stroke / 2f, stroke / 2f)
                val arcSize = Size(diameter, diameter)
                // Background track
                drawArc(
                    color = trackColor,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
                // Progress arc
                drawArc(
                    color = bandColor,
                    startAngle = -90f,
                    sweepAngle = 360f * progress.value,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = clamped.toString(),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = bandColor,
                )
                Text(
                    text = "/100",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = bandColor,
        )
    }
}

@Composable
fun bandColor(band: ScoreBand): Color = when (band) {
    ScoreBand.Excellent -> scoreExcellent
    ScoreBand.Ok -> scoreOk
    ScoreBand.Poor -> scorePoor
    ScoreBand.Bad -> scoreBad
}
