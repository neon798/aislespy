package app.aislespy.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.aislespy.ui.theme.AisleColors
import app.aislespy.ui.theme.PublicSans

/**
 * Handoff-style “← Back” text link used on pushed screens.
 */
@Composable
fun BackLink(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "← Back",
    contentDescription: String = "Navigate back",
) {
    Text(
        text = label,
        fontFamily = PublicSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.5.sp,
        color = AisleColors.current.muted60,
        modifier = modifier
            .clickable(onClick = onClick, role = Role.Button)
            .semantics {
                this.contentDescription = contentDescription
                role = Role.Button
            }
            .padding(horizontal = 8.dp, vertical = 6.dp),
    )
}
