package app.aislespy.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.aislespy.ui.theme.brandAmber
import app.aislespy.ui.theme.brandAmberOnDark
import app.aislespy.ui.theme.brandAmberOnLight

/**
 * Read-only chip for badges, category, confidence, source DB, etc.
 * Unified style across result and related screens.
 */
@Composable
fun InfoChip(
    label: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    SuggestionChip(
        onClick = {},
        label = { Text(label) },
        enabled = false,
        modifier = if (contentDescription != null) {
            modifier.semantics { this.contentDescription = contentDescription }
        } else {
            modifier
        },
    )
}

/**
 * Values / certification badge (ADR-017): gold/amber outline + leading ★.
 * Informational only — not a score signal. Dark-scheme uses lighter amber for contrast.
 */
@Composable
fun ValuesInfoChip(
    label: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val dark = isSystemInDarkTheme()
    val outline = if (dark) brandAmberOnDark else brandAmber
    val content = if (dark) brandAmberOnDark else brandAmberOnLight
    val container = if (dark) {
        brandAmberOnDark.copy(alpha = 0.14f)
    } else {
        brandAmber.copy(alpha = 0.12f)
    }
    val talkBack = contentDescription ?: "$label, values badge"
    Surface(
        modifier = modifier.semantics { this.contentDescription = talkBack },
        shape = RoundedCornerShape(8.dp),
        color = container,
        contentColor = content,
        border = BorderStroke(1.5.dp, outline),
    ) {
        Text(
            text = "★ $label",
            style = MaterialTheme.typography.labelLarge,
            color = content,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

/**
 * Result-screen badge row entry: routes [style] to the right chip treatment.
 *
 * Styles: `values` (gold-star), `ownership` (neutral corporate chip), others → [InfoChip].
 */
@Composable
fun ResultBadgeChip(
    label: String,
    style: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    when (style) {
        "values" -> ValuesInfoChip(
            label = label,
            modifier = modifier,
            contentDescription = contentDescription ?: "$label, values badge",
        )
        "ownership" -> InfoChip(
            label = label,
            modifier = modifier,
            contentDescription = contentDescription ?: "$label, ownership information",
        )
        else -> InfoChip(
            label = label,
            modifier = modifier,
            contentDescription = contentDescription ?: label,
        )
    }
}
