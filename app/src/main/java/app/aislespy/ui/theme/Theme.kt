package app.aislespy.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Light scheme — warm & natural handoff tokens (cream / olive / ink).
 * This is the designed, default/showcase theme.
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
 * Dark scheme — functional only. Full dark visual identity is not designed yet
 * (handoff Gaps). Keeps the app usable under system dark mode until then.
 */
private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = PaleLime,
    onSecondary = DarkOnPrimary,
    tertiary = PaleLime,
    onTertiary = DarkOnPrimary,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = Color(0xFF2A2820),
    onSurfaceVariant = DarkOnSurface.copy(alpha = 0.7f),
    outline = Color(0xFF5A5648),
    error = ErrorRed,
    onError = Color.White,
)

@Composable
fun AisleSpyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color OFF — consistent cream/olive brand across devices (parameter kept for call-site stability)
    @Suppress("UNUSED_PARAMETER") dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
