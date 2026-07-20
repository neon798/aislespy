package app.aislespy.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Corner radii from the design handoff (spacing / shape tokens).
 */
object AisleSpyShapes {
    /** Default content cards — radius 16. */
    val card = RoundedCornerShape(16.dp)

    /** History / list rows — radius 14. */
    val listRow = RoundedCornerShape(14.dp)

    /** Small tiles (Nutri-Score letter, etc.) — mid of 9–13. */
    val smallTile = RoundedCornerShape(11.dp)

    /** Brand / privacy card, chooser option — radius 18. */
    val brandCard = RoundedCornerShape(18.dp)

    /** Chooser option cards — radius 18. */
    val chooserOption = RoundedCornerShape(18.dp)

    /** Manual-entry field — radius 14. */
    val input = RoundedCornerShape(14.dp)

    /** Scan viewfinder area — radius 20. */
    val viewfinder = RoundedCornerShape(20.dp)

    /** Buttons, chips, pills — full radius. */
    val pill = RoundedCornerShape(99.dp)

    /** Disclaimer dashed-border card — radius 14. */
    val disclaimer = RoundedCornerShape(14.dp)
}
