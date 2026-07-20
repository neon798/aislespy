package app.aislespy.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Light M3 scheme — warm & natural handoff tokens (cream / olive / ink).
 */
private val LightColorScheme = lightColorScheme(
    primary = Olive,
    onPrimary = CreamSurface,
    primaryContainer = OliveContainer,
    onPrimaryContainer = OliveOnContainer,
    secondary = OliveDark,
    onSecondary = CreamSurface,
    secondaryContainer = OliveContainer,
    onSecondaryContainer = OliveOnContainer,
    tertiary = PaleLime,
    onTertiary = Ink,
    tertiaryContainer = PaleLime.copy(alpha = 0.35f),
    onTertiaryContainer = OliveDark,
    background = CreamSurface,
    onBackground = Ink,
    surface = CreamSurface,
    onSurface = Ink,
    surfaceVariant = CardWhite,
    onSurfaceVariant = MutedText55,
    surfaceContainerLowest = CardWhite,
    surfaceContainerLow = CreamSurface,
    surfaceContainer = CreamSurface,
    surfaceContainerHigh = CardWhite,
    surfaceContainerHighest = CardWhite,
    outline = CardBorder,
    outlineVariant = DashedDividerColor,
    error = ErrorRed,
    onError = Color.White,
    errorContainer = ErrorRed.copy(alpha = 0.12f),
    onErrorContainer = ErrorRed,
    inverseSurface = ScanBackground,
    inverseOnSurface = CreamSurface,
    inversePrimary = PaleLime,
    scrim = Color.Black.copy(alpha = 0.4f),
)

/**
 * Dark M3 scheme — warm brown-charcoal palette (ADR-021).
 */
private val DarkColorScheme = darkColorScheme(
    primary = DarkOlivePrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkOliveContainer,
    onPrimaryContainer = DarkOliveOnContainer,
    secondary = PaleLime,
    onSecondary = DarkOnPrimary,
    secondaryContainer = DarkOliveContainer,
    onSecondaryContainer = DarkOliveOnContainer,
    tertiary = PaleLime,
    onTertiary = DarkOnPrimary,
    tertiaryContainer = PaleLime.copy(alpha = 0.22f),
    onTertiaryContainer = DarkOliveOnContainer,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkCard,
    onSurfaceVariant = DarkMuted55,
    surfaceContainerLowest = DarkCard,
    surfaceContainerLow = DarkCard,
    surfaceContainer = DarkCard,
    surfaceContainerHigh = DarkCardElevated,
    surfaceContainerHighest = DarkCardElevated,
    outline = DarkCardBorder,
    outlineVariant = DarkDashedDivider,
    error = DarkError,
    onError = DarkOnPrimary,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkError,
    inverseSurface = ScanBackground,
    inverseOnSurface = ScanCream,
    inversePrimary = PaleLime,
    scrim = Color.Black.copy(alpha = 0.5f),
)

@Composable
fun AisleSpyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color OFF — consistent cream/olive brand across devices (parameter kept for call-site stability)
    @Suppress("UNUSED_PARAMETER") dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val aisleColors = if (darkTheme) DarkAisleColors else LightAisleColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val insetsController = WindowCompat.getInsetsController(window, view)
            // Light icons on dark theme; dark icons on light theme.
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalAisleColors provides aisleColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content,
        )
    }
}
