package app.aislespy.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.aislespy.ui.theme.AisleSpyShapes
import app.aislespy.ui.theme.DashedDividerColor

/** Disclaimer outline: rgba(80,60,30,.3). */
private val DisclaimerBorder = Color(0x4D503C1E)

/**
 * 1px dashed horizontal section separator (handoff: rgba(80,60,30,.25)).
 */
@Composable
fun DashedDivider(
    modifier: Modifier = Modifier,
    color: Color = DashedDividerColor,
    thickness: Dp = 1.dp,
    dashLength: Dp = 6.dp,
    gapLength: Dp = 4.dp,
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(thickness),
    ) {
        val dashPx = dashLength.toPx()
        val gapPx = gapLength.toPx()
        drawLine(
            color = color,
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width, size.height / 2f),
            strokeWidth = size.height,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashPx, gapPx), 0f),
        )
    }
}

/**
 * Content wrapper with a dashed border — used for the mandatory disclaimer card.
 */
@Composable
fun DashedBorderCard(
    modifier: Modifier = Modifier,
    shape: Shape = AisleSpyShapes.disclaimer,
    borderColor: Color = DisclaimerBorder,
    borderWidth: Dp = 1.dp,
    contentPadding: Dp = 12.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .border(width = borderWidth, color = borderColor, shape = shape)
            .padding(contentPadding),
        content = content,
    )
}
