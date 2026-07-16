package app.aislespy.ui.util

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * True when the system animator duration scale is 0 (reduce motion / disable animations).
 * Callers should skip entrance animations and jump to the final visual state.
 */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        try {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) == 0f
        } catch (_: Settings.SettingNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }
}
