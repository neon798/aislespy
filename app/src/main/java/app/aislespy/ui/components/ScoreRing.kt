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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.aislespy.domain.model.ScoreBand
import app.aislespy.ui.theme.scoreBandColors
import app.aislespy.ui.util.rememberReducedMotion

/**
 * Hero 1–100 score ring (docs/COMPONENTS.md).
 *
 * TalkBack: “Score {value} out of 100, {label}” and optional confidence.
 * Respects system reduce-motion (ANIMATOR_DURATION_SCALE == 0 → jump to final).
 */
@Composable
fun ScoreRing(
    value: Int,
    band: ScoreBand,
    label: String,
    modifier: Modifier = Modifier,
    animated: Boolean = true,
    confidenceLabel: String? = null,
    size: Dp = 160.dp,
    strokeWidth: Dp = 14.dp,
) {
    val clamped = value.coerceIn(1, 100)
    val colors = scoreBandColors(band)
    val bandColor = colors.accent
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val reducedMotion = rememberReducedMotion()
    val shouldAnimate = animated && !reducedMotion

    // Grow ring slightly with large system font so center numerals stay readable.
    val fontScale = LocalDensity.current.fontScale
    val ringSize = (size.value * fontScale.coerceIn(1f, 1.45f)).dp
    val ringStroke = (strokeWidth.value * fontScale.coerceIn(1f, 1.2f)).dp

    val progress = remember { Animatable(0f) }
    LaunchedEffect(clamped, shouldAnimate) {
        if (shouldAnimate) {
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = clamped / 100f,
                animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
            )
        } else {
            progress.snapTo(clamped / 100f)
        }
    }

    val a11y = buildString {
        append("Score $clamped out of 100, $label")
        if (!confidenceLabel.isNullOrBlank()) {
            append(", $confidenceLabel")
        }
    }

    // Center score uses sp so it tracks fontScale; clamp style size so it fits the ring.
    val scoreSp = (36f * fontScale.coerceIn(1f, 1.35f)).sp

    Column(
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = a11y },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(ringSize),
        ) {
            Canvas(modifier = Modifier.size(ringSize)) {
                val stroke = ringStroke.toPx()
                val diameter = this.size.minDimension - stroke
                val topLeft = Offset(stroke / 2f, stroke / 2f)
                val arcSize = Size(diameter, diameter)
                drawArc(
                    color = trackColor,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
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
                    fontSize = scoreSp,
                    fontWeight = FontWeight.Bold,
                    color = bandColor,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
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
            textAlign = TextAlign.Center,
        )
    }
}

/** Theme-aware accent for a [ScoreBand] (arcs, labels). */
@Composable
fun bandColor(band: ScoreBand): Color = scoreBandColors(band).accent
