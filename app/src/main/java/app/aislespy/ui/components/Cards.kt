package app.aislespy.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.aislespy.ui.theme.AisleColors
import app.aislespy.ui.theme.AisleSpyShapes
import app.aislespy.ui.theme.BricolageGrotesque
import app.aislespy.ui.theme.PublicSans

/**
 * Standard content card — 1px card-border outline, radius 16.
 */
@Composable
fun AisleCard(
    modifier: Modifier = Modifier,
    shape: Shape = AisleSpyShapes.card,
    onClick: (() -> Unit)? = null,
    contentPadding: Dp = 0.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val aisle = AisleColors.current
    val colors = CardDefaults.cardColors(containerColor = aisle.card)
    val border = BorderStroke(1.dp, aisle.cardBorder)
    val elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    val pad = if (contentPadding > 0.dp) Modifier.padding(contentPadding) else Modifier

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            colors = colors,
            border = border,
            elevation = elevation,
        ) {
            Column(modifier = pad, content = content)
        }
    } else {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            colors = colors,
            border = border,
            elevation = elevation,
        ) {
            Column(modifier = pad, content = content)
        }
    }
}

/** List-row card — radius 14. */
@Composable
fun AisleListRowCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: Dp = 12.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    AisleCard(
        modifier = modifier,
        shape = AisleSpyShapes.listRow,
        onClick = onClick,
        contentPadding = contentPadding,
        content = content,
    )
}

/** Brand / privacy hero card — olive fill, on-primary text, radius 18. */
@Composable
fun AisleBrandCard(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    val aisle = AisleColors.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = AisleSpyShapes.brandCard,
        color = aisle.olive,
        contentColor = aisle.onOlive,
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            Text(
                text = title,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = aisle.onOlive,
            )
            Text(
                text = body,
                fontFamily = PublicSans,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                lineHeight = 18.6.sp,
                color = aisle.onOlive.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 5.dp),
            )
        }
    }
}
