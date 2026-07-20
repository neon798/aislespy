package app.aislespy.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import app.aislespy.ui.theme.AisleSpyShapes
import app.aislespy.ui.theme.AisleSpyTextStyles
import app.aislespy.ui.theme.CreamSurface
import app.aislespy.ui.theme.Olive

/**
 * Full-width primary CTA — olive pill, cream text, 15–16 vertical padding.
 */
@Composable
fun AislePrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp),
        shape = AisleSpyShapes.pill,
        colors = ButtonDefaults.buttonColors(
            containerColor = Olive,
            contentColor = CreamSurface,
            disabledContainerColor = Olive.copy(alpha = 0.4f),
            disabledContentColor = CreamSurface.copy(alpha = 0.7f),
        ),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 15.dp),
    ) {
        Text(text = text, style = AisleSpyTextStyles.button)
    }
}

/**
 * Full-width secondary CTA — 1.5px olive outline pill, olive text.
 */
@Composable
fun AisleSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp),
        shape = AisleSpyShapes.pill,
        border = BorderStroke(1.5.dp, if (enabled) Olive else Olive.copy(alpha = 0.4f)),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = Olive,
            disabledContentColor = Olive.copy(alpha = 0.4f),
        ),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 13.dp),
    ) {
        Text(text = text, style = AisleSpyTextStyles.button)
    }
}

/** Compact olive filled pill (e.g. "Saved ✓") — not full-width. */
@Composable
fun AislePrimaryPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = AisleSpyShapes.pill,
        colors = ButtonDefaults.buttonColors(
            containerColor = Olive,
            contentColor = CreamSurface,
            disabledContainerColor = Olive.copy(alpha = 0.4f),
            disabledContentColor = CreamSurface.copy(alpha = 0.7f),
        ),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Text(
            text = text,
            style = AisleSpyTextStyles.badgeChip.copy(fontWeight = FontWeight.Bold),
        )
    }
}

/** Compact outline olive pill (e.g. Save, confidence). */
@Composable
fun AisleOutlinePill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = AisleSpyShapes.pill,
        border = BorderStroke(1.dp, Olive.copy(alpha = 0.4f)),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = Olive,
            containerColor = Color.Transparent,
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 5.dp),
    ) {
        Text(text = text, style = AisleSpyTextStyles.badgeChip)
    }
}
