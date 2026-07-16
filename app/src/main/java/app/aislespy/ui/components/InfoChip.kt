package app.aislespy.ui.components

import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

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
