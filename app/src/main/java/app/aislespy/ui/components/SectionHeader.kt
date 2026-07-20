package app.aislespy.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import app.aislespy.ui.theme.AisleColors
import app.aislespy.ui.theme.AisleSpyTextStyles

/**
 * Section label — 11/700 UPPERCASE, letter-spacing .07em, muted.
 * Used on result, settings, nutrition, and similar screens.
 */
@Composable
fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text.uppercase(),
        style = AisleSpyTextStyles.sectionLabel,
        color = AisleColors.current.muted45,
        modifier = modifier.semantics { heading() },
    )
}

/** Alias matching the design-handoff name. */
@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    SectionHeader(text = text, modifier = modifier)
}
