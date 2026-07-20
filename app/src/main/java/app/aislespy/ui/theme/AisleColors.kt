package app.aislespy.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Theme-aware semantic color tokens used by screens and components.
 *
 * Raw light/dark vals live in [Color.kt]; UI code should read
 * [AisleColors.current] so surfaces and text follow system light/dark.
 *
 * Scan-screen fixed colors ([ScanBackground], [PaleLime], [ScanCream], [ScanInk])
 * stay outside this layer — intentional dark-adjacent camera chrome in both themes.
 */
@Immutable
data class AisleColors(
    val surface: Color,
    val background: Color,
    val card: Color,
    val cardElevated: Color,
    val cardBorder: Color,
    val cardBorderStrong: Color,
    val dashedDivider: Color,
    val disclaimerBorder: Color,
    val outlineChipBorder: Color,
    val ink: Color,
    val muted45: Color,
    val muted55: Color,
    val muted60: Color,
    val muted70: Color,
    /** Brand primary (olive in light, lifted olive in dark). */
    val olive: Color,
    val primary: Color,
    val oliveDark: Color,
    val oliveContainer: Color,
    val oliveOnContainer: Color,
    val paleLime: Color,
    /** Fixed scan camera surface — same in light and dark. */
    val scanSurface: Color,
    val error: Color,
    val errorContainer: Color,
    /** Text/icons on primary-filled controls (cream light / near-black dark). */
    val onPrimary: Color,
    val onOlive: Color,
) {
    companion object {
        val current: AisleColors
            @Composable
            @ReadOnlyComposable
            get() = LocalAisleColors.current
    }
}

val LocalAisleColors = staticCompositionLocalOf { LightAisleColors }

val LightAisleColors = AisleColors(
    surface = CreamSurface,
    background = CreamSurface,
    card = CardWhite,
    cardElevated = CardWhite,
    cardBorder = CardBorder,
    cardBorderStrong = CardBorderStrong,
    dashedDivider = DashedDividerColor,
    disclaimerBorder = DisclaimerBorder,
    outlineChipBorder = OutlineChipBorder,
    ink = Ink,
    muted45 = MutedText45,
    muted55 = MutedText55,
    muted60 = MutedText60,
    muted70 = MutedText70,
    olive = Olive,
    primary = Olive,
    oliveDark = OliveDark,
    oliveContainer = OliveContainer,
    oliveOnContainer = OliveOnContainer,
    paleLime = PaleLime,
    scanSurface = ScanBackground,
    error = ErrorRed,
    errorContainer = ErrorRed.copy(alpha = 0.12f),
    onPrimary = CreamSurface,
    onOlive = CreamSurface,
)

val DarkAisleColors = AisleColors(
    surface = DarkSurface,
    background = DarkBackground,
    card = DarkCard,
    cardElevated = DarkCardElevated,
    cardBorder = DarkCardBorder,
    cardBorderStrong = DarkCardBorderStrong,
    dashedDivider = DarkDashedDivider,
    disclaimerBorder = DarkDisclaimerBorder,
    outlineChipBorder = DarkDisclaimerBorder,
    ink = DarkInk,
    muted45 = DarkMuted45,
    muted55 = DarkMuted55,
    muted60 = DarkMuted60,
    muted70 = DarkMuted70,
    olive = DarkOlivePrimary,
    primary = DarkOlivePrimary,
    oliveDark = DarkOliveDark,
    oliveContainer = DarkOliveContainer,
    oliveOnContainer = DarkOliveOnContainer,
    paleLime = PaleLime,
    scanSurface = ScanBackground,
    error = DarkError,
    errorContainer = DarkErrorContainer,
    onPrimary = DarkOnPrimary,
    onOlive = DarkOnPrimary,
)
