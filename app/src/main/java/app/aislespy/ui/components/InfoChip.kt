package app.aislespy.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.aislespy.ui.theme.AisleSpyShapes
import app.aislespy.ui.theme.AisleSpyTextStyles
import app.aislespy.ui.theme.CardBorder
import app.aislespy.ui.theme.CardWhite
import app.aislespy.ui.theme.MutedText60
import app.aislespy.ui.theme.Olive
import app.aislespy.ui.theme.OliveContainer
import app.aislespy.ui.theme.OliveOnContainer
import app.aislespy.ui.theme.brandAmber
import app.aislespy.ui.theme.brandAmberOnDark
import app.aislespy.ui.theme.brandAmberOnLight

/** Outline for confidence chips: rgba(80,60,30,.3). */
private val OutlineChipBorder = Color(0x4D503C1E)

/**
 * Olive-container badge chip (dietary, NOVA, source labels, generic badges).
 * Pill shape, olive container bg, on-container text.
 */
@Composable
fun InfoChip(
    label: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val talkBack = contentDescription ?: label
    Surface(
        modifier = modifier.semantics { this.contentDescription = talkBack },
        shape = AisleSpyShapes.pill,
        color = OliveContainer,
        contentColor = OliveOnContainer,
        border = BorderStroke(1.dp, Olive.copy(alpha = 0.25f)),
    ) {
        Text(
            text = label,
            style = AisleSpyTextStyles.badgeChip,
            color = OliveOnContainer,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
        )
    }
}

/**
 * Outline confidence / meta chip ("Confidence: High").
 */
@Composable
fun OutlineInfoChip(
    label: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val talkBack = contentDescription ?: label
    Surface(
        modifier = modifier.semantics { this.contentDescription = talkBack },
        shape = AisleSpyShapes.pill,
        color = Color.Transparent,
        contentColor = MutedText60,
        border = BorderStroke(1.5.dp, OutlineChipBorder),
    ) {
        Text(
            text = label,
            style = AisleSpyTextStyles.badgeChip.copy(fontWeight = FontWeight.Bold),
            color = MutedText60,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

/**
 * Values / certification badge (ADR-017): gold/amber outline + leading ★.
 * Informational only — not a score signal. Pill shape; semantics unchanged.
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
        shape = AisleSpyShapes.pill,
        color = container,
        contentColor = content,
        border = BorderStroke(1.5.dp, outline),
    ) {
        Text(
            text = "★ $label",
            style = AisleSpyTextStyles.badgeChip,
            color = content,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
        )
    }
}

/**
 * Neutral ownership chip (ADR-019 corporate parent) — factual, not alarm-red.
 * Pill shape with muted outline; distinct from values gold-star treatment.
 */
@Composable
fun OwnershipInfoChip(
    label: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val talkBack = contentDescription ?: "$label, ownership information"
    Surface(
        modifier = modifier.semantics { this.contentDescription = talkBack },
        shape = AisleSpyShapes.pill,
        color = CardWhite,
        contentColor = MutedText60,
        border = BorderStroke(1.dp, CardBorder),
    ) {
        Text(
            text = label,
            style = AisleSpyTextStyles.badgeChip,
            color = MutedText60,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
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
        "ownership" -> OwnershipInfoChip(
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
