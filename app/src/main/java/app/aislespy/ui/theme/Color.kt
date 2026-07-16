package app.aislespy.ui.theme

import androidx.compose.ui.graphics.Color

// Brand greens — spy-on-labels palette (not medical/clinical)
val GreenPrimary = Color(0xFF1B5E20)
val GreenPrimaryLight = Color(0xFF4C8C4A)
val GreenPrimaryDark = Color(0xFF003300)
val GreenSecondary = Color(0xFF2E7D32)
val GreenTertiary = Color(0xFF66BB6A)
val GreenContainer = Color(0xFFC8E6C9)
val GreenOnContainer = Color(0xFF0D3311)

val GreenDarkPrimary = Color(0xFF81C784)
val GreenDarkOnPrimary = Color(0xFF003910)
val GreenDarkPrimaryContainer = Color(0xFF1B5E20)
val GreenDarkOnPrimaryContainer = Color(0xFFC8E6C9)
val GreenDarkSecondary = Color(0xFFA5D6A7)
val GreenDarkTertiary = Color(0xFFB2DFDB)

val LightBackground = Color(0xFFF7FBF7)
val LightOnBackground = Color(0xFF1A1C19)
val LightSurface = Color(0xFFF7FBF7)
val LightOnSurface = Color(0xFF1A1C19)

val DarkBackground = Color(0xFF101410)
val DarkOnBackground = Color(0xFFE1E3DD)
val DarkSurface = Color(0xFF101410)
val DarkOnSurface = Color(0xFFE1E3DD)

// ---------------------------------------------------------------------------
// Semantic score band colors (docs/COMPONENTS.md / UI_UX.md)
// Light: dark enough for text on light surfaces (~WCAG AA vs white).
// Dark: light enough for arcs/labels on dark surfaces.
// Chip fills are separate so white (or near-white) label text stays AA.
// ---------------------------------------------------------------------------

/** Light-theme arc / label colors. */
val scoreExcellentLight = Color(0xFF2E7D32)
val scoreOkLight = Color(0xFFB36B00) // darkened amber — better text contrast than pure yellow
val scorePoorLight = Color(0xFFD84315)
val scoreBadLight = Color(0xFFC62828)

/** Dark-theme arc / label colors. */
val scoreExcellentDark = Color(0xFF81C784)
val scoreOkDark = Color(0xFFFFB74D)
val scorePoorDark = Color(0xFFFF8A65)
val scoreBadDark = Color(0xFFEF9A9A)

/**
 * Filled chip / badge container colors.
 * Tuned so [scoreChipOn] (white) meets ~WCAG AA contrast on both themes.
 */
val scoreChipExcellent = Color(0xFF1B5E20)
val scoreChipOk = Color(0xFF8A5A00)
val scoreChipPoor = Color(0xFFBF360C)
val scoreChipBad = Color(0xFFB71C1C)

/** Text/icon color on filled score chips. */
val scoreChipOn = Color(0xFFFFFFFF)

// Backward-compatible aliases (light defaults) used by non-composable call sites.
val scoreExcellent = scoreExcellentLight
val scoreOk = scoreOkLight
val scorePoor = scorePoorLight
val scoreBad = scoreBadLight

// Brand extras (UI_UX visual language)
val brandTeal = Color(0xFF0F6B6B)
val brandAmber = Color(0xFFF5A524)
/** Darker amber for text on light surfaces. */
val brandAmberOnLight = Color(0xFF9A6700)
/** Lighter amber for accents on dark surfaces. */
val brandAmberOnDark = Color(0xFFFFB74D)
