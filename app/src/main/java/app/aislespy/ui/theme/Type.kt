@file:OptIn(ExperimentalTextApi::class)

package app.aislespy.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.aislespy.R

/**
 * Bricolage Grotesque — headings, wordmark, score numerals, Nutri-Score letter.
 * Variable font; weights 700–800 with letter-spacing −0.01em on headings.
 */
val BricolageGrotesque = FontFamily(
    Font(
        resId = R.font.bricolage_grotesque_variable,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700)),
    ),
    Font(
        resId = R.font.bricolage_grotesque_variable,
        weight = FontWeight.ExtraBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(800)),
    ),
)

/**
 * Public Sans — body, labels, buttons (400–800).
 */
val PublicSans = FontFamily(
    Font(
        resId = R.font.public_sans_variable,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400)),
    ),
    Font(
        resId = R.font.public_sans_variable,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(500)),
    ),
    Font(
        resId = R.font.public_sans_variable,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600)),
    ),
    Font(
        resId = R.font.public_sans_variable,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700)),
    ),
    Font(
        resId = R.font.public_sans_variable,
        weight = FontWeight.ExtraBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(800)),
    ),
)

/**
 * IBM Plex Mono — barcodes, subscores, weight shares, versions (400–500).
 */
val IbmPlexMono = FontFamily(
    Font(resId = R.font.ibm_plex_mono_regular, weight = FontWeight.Normal),
    Font(resId = R.font.ibm_plex_mono_medium, weight = FontWeight.Medium),
)

/** Extra text styles beyond the M3 [Typography] roles. */
object AisleSpyTextStyles {
    /** Score hero numeral — Bricolage 42/700. */
    val scoreNumeral = TextStyle(
        fontFamily = BricolageGrotesque,
        fontWeight = FontWeight.Bold,
        fontSize = 42.sp,
        lineHeight = 42.sp,
        letterSpacing = 0.sp,
    )

    /** "out of 100" under score numeral. */
    val scoreCaption = TextStyle(
        fontFamily = PublicSans,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        lineHeight = 12.sp,
    )

    /** Section labels: 11/700 UPPERCASE, letter-spacing .07em (apply uppercase at call site). */
    val sectionLabel = TextStyle(
        fontFamily = PublicSans,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.07.em,
    )

    /** Filled band chip label: 12/800 UPPERCASE. */
    val bandChip = TextStyle(
        fontFamily = PublicSans,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 12.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.05.em,
    )

    /** Badge / olive-container chips: 11/600. */
    val badgeChip = TextStyle(
        fontFamily = PublicSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
    )

    /** Bottom nav labels: 10.5/700. */
    val navLabel = TextStyle(
        fontFamily = PublicSans,
        fontWeight = FontWeight.Bold,
        fontSize = 10.5.sp,
        lineHeight = 13.sp,
    )

    /** Primary button label: 15/700. */
    val button = TextStyle(
        fontFamily = PublicSans,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
    )

    /** Mono default for barcodes / versions / shares. */
    val mono = TextStyle(
        fontFamily = IbmPlexMono,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    )

    val monoMedium = TextStyle(
        fontFamily = IbmPlexMono,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    )

    /** Wordmark on light screens. */
    val wordmark = TextStyle(
        fontFamily = BricolageGrotesque,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 19.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.01).em,
    )

    /** Product name on result: 21/700. */
    val productName = TextStyle(
        fontFamily = BricolageGrotesque,
        fontWeight = FontWeight.Bold,
        fontSize = 21.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.01).em,
    )
}

/**
 * Material 3 Typography wired to the handoff type scale.
 *
 * Scale: screen title 24 · product name 21 · score numeral 42 · body 13–13.5 ·
 * secondary 11.5–12.5 · section labels 11/700 · chips 10.5–12.
 */
val Typography = Typography(
    displayLarge = AisleSpyTextStyles.scoreNumeral,
    displayMedium = TextStyle(
        fontFamily = BricolageGrotesque,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.01).em,
    ),
    displaySmall = TextStyle(
        fontFamily = BricolageGrotesque,
        fontWeight = FontWeight.Bold,
        fontSize = 27.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.01).em,
    ),
    headlineLarge = TextStyle(
        fontFamily = BricolageGrotesque,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.01).em,
    ),
    /** Screen title 24/700. */
    headlineMedium = TextStyle(
        fontFamily = BricolageGrotesque,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.01).em,
    ),
    /** Product name 21/700. */
    headlineSmall = AisleSpyTextStyles.productName,
    titleLarge = TextStyle(
        fontFamily = BricolageGrotesque,
        fontWeight = FontWeight.Bold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.01).em,
    ),
    titleMedium = TextStyle(
        fontFamily = PublicSans,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = PublicSans,
        fontWeight = FontWeight.Bold,
        fontSize = 12.5.sp,
        lineHeight = 16.sp,
    ),
    /** Body ~13.5, line-height ~1.55. */
    bodyLarge = TextStyle(
        fontFamily = PublicSans,
        fontWeight = FontWeight.Normal,
        fontSize = 13.5.sp,
        lineHeight = 21.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = PublicSans,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 20.sp,
    ),
    /** Secondary 11.5–12.5. */
    bodySmall = TextStyle(
        fontFamily = PublicSans,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = PublicSans,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    /** Section labels (call sites should uppercase). */
    labelMedium = AisleSpyTextStyles.sectionLabel,
    labelSmall = TextStyle(
        fontFamily = PublicSans,
        fontWeight = FontWeight.Bold,
        fontSize = 10.5.sp,
        lineHeight = 14.sp,
    ),
)
