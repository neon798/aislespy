package app.aislespy.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.aislespy.domain.model.ScoreBand
import app.aislespy.ui.theme.AisleSpyTextStyles
import app.aislespy.ui.theme.CardBorder
import app.aislespy.ui.theme.CardWhite
import app.aislespy.ui.theme.MutedText55
import app.aislespy.ui.theme.scoreBandColors
import app.aislespy.ui.util.rememberReducedMotion

/**
 * Hero 1–100 score ring (design handoff).
 *
 * White disc, 7px band-accent arc (sweep = score%, rounded cap, starts 12 o'clock),
 * Bricolage numeral 42 + "out of 100".
 *
 * TalkBack: “Score N out of 100, ‹band›, confidence ‹level›”.
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
    size: Dp = 124.dp,
    strokeWidth: Dp = 7.dp,
    showBandLabelBelow: Boolean = false,
) {
    val clamped = value.coerceIn(1, 100)
    val colors = scoreBandColors(band)
    val bandColor = colors.accent
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
            // Accept either "High" or "Confidence: High"
            val conf = if (confidenceLabel.startsWith("Confidence", ignoreCase = true)) {
                confidenceLabel
            } else {
                "confidence $confidenceLabel"
            }
            append(", $conf")
        }
    }

    Column(
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = a11y },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(ringSize)
                .clip(CircleShape)
                .background(CardWhite)
                .border(1.dp, CardBorder, CircleShape),
        ) {
            Canvas(modifier = Modifier.size(ringSize)) {
                val stroke = ringStroke.toPx()
                val diameter = this.size.minDimension - stroke
                val topLeft = Offset(stroke / 2f, stroke / 2f)
                val arcSize = Size(diameter, diameter)
                // Band-accent arc only (white disc is the track); starts at 12 o'clock.
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
                    style = AisleSpyTextStyles.scoreNumeral,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
                Text(
                    text = "out of 100",
                    style = AisleSpyTextStyles.scoreCaption,
                    color = MutedText55,
                    textAlign = TextAlign.Center,
                )
            }
        }
        if (showBandLabelBelow) {
            Text(
                text = label,
                style = AisleSpyTextStyles.bandChip,
                color = bandColor,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Theme-aware accent for a [ScoreBand] (arcs, labels). */
@Composable
fun bandColor(band: ScoreBand): Color = scoreBandColors(band).accent
