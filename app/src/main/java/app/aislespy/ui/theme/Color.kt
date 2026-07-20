package app.aislespy.ui.theme

import androidx.compose.ui.graphics.Color

// ---------------------------------------------------------------------------
// Warm & natural light palette (design handoff / ADR-020)
// ---------------------------------------------------------------------------

/** Cream surface — every light screen (`surface`, `background`). */
val CreamSurface = Color(0xFFFAF6EE)

/** Ink — primary text (`onSurface`). */
val Ink = Color(0xFF33301F)

/** Olive — brand primary (buttons, links, active nav). */
val Olive = Color(0xFF5D6633)

/** Olive dark — primary pressed / hover. */
val OliveDark = Color(0xFF4C5429)

/**
 * Olive container — badge chips, Nutri-Score tile bg.
 * Olive @ ~11% alpha: rgba(93, 102, 51, 0.11).
 */
val OliveContainer = Color(0x1C5D6633)

/** Text / icons on olive container. */
val OliveOnContainer = Color(0xFF4C5429)

/** Pale lime — accents on dark scan screen (viewfinder, chips, scan line). */
val PaleLime = Color(0xFFCDD6A3)

/** Scan screen background only ("dark-adjacent", not the app dark theme). */
val ScanBackground = Color(0xFF23211A)

/**
 * Fixed cream used on the scan screen (text, shutter) — intentional constant in both themes.
 * Same hex as [CreamSurface]; named separately so scan UI never picks up theme-aware cream.
 */
val ScanCream = Color(0xFFFAF6EE)

/**
 * Fixed ink on scan product-card overlays — intentional constant in both themes.
 * Same hex as [Ink].
 */
val ScanInk = Color(0xFF33301F)

/** Card fill. */
val CardWhite = Color(0xFFFFFFFF)

/** 1px card outline: rgba(80, 60, 30, 0.12). */
val CardBorder = Color(0x1F503C1E)

/** Stronger card outline variants (~.14–.15). */
val CardBorderStrong = Color(0x26503C1E)

/** 1px dashed section separators: rgba(80, 60, 30, 0.25). */
val DashedDividerColor = Color(0x40503C1E)

/** Disclaimer dashed border: rgba(80, 60, 30, 0.30). */
val DisclaimerBorder = Color(0x4D503C1E)

/** Outline chip border: rgba(80, 60, 30, 0.30). */
val OutlineChipBorder = Color(0x4D503C1E)

/** Muted text ladder — ink @ alpha (labels → body-muted). */
val MutedText45 = Color(0x7333301F) // .45
val MutedText55 = Color(0x8C33301F) // .55
val MutedText60 = Color(0x9933301F) // .60
val MutedText70 = Color(0xB233301F) // .70

/** Error / destructive (same hex as Bad band — intentional). */
val ErrorRed = Color(0xFFC62828)

// ---------------------------------------------------------------------------
// Warm dark palette (ADR-021) — brown-charcoal, not cool gray
// ---------------------------------------------------------------------------

/** Dark background / surface — warm near-black. */
val DarkSurface = Color(0xFF16140E)

/** Alias for dark screen background. */
val DarkBackground = DarkSurface

/** Dark card / surfaceContainer. */
val DarkCard = Color(0xFF221F17)

/** Dark elevated card variant. */
val DarkCardElevated = Color(0xFF2A2619)

/** Dark onSurface / ink — warm off-white. */
val DarkOnSurface = Color(0xFFECE7DB)

/** Alias matching light naming. */
val DarkInk = DarkOnSurface

/** Dark card border — warm off-white @ ~12%. */
val DarkCardBorder = Color(0x1FECE7DB)

/** Stronger dark card border (~.15). */
val DarkCardBorderStrong = Color(0x26ECE7DB)

/** Dark dashed divider — warm off-white @ ~22%. */
val DarkDashedDivider = Color(0x38ECE7DB)

/** Dark disclaimer / outline-chip border — warm off-white @ ~30%. */
val DarkDisclaimerBorder = Color(0x4DECE7DB)

/** Dark muted ladder — #ECE7DB @ .45/.55/.6/.7 */
val DarkMuted45 = Color(0x73ECE7DB)
val DarkMuted55 = Color(0x8CECE7DB)
val DarkMuted60 = Color(0x99ECE7DB)
val DarkMuted70 = Color(0xB2ECE7DB)

/** Dark primary — olive lifted for legibility on dark surfaces. */
val DarkOlivePrimary = Color(0xFFC2CD84)

/** Dark primary pressed — slightly deeper lifted olive. */
val DarkOliveDark = Color(0xFFA8B36A)

/** On primary in dark (buttons). */
val DarkOnPrimary = Color(0xFF1B1810)

/** Dark olive container — olive #5d6633 @ ~30%. */
val DarkOliveContainer = Color(0x4D5D6633)

/** Text / icons on dark olive container. */
val DarkOliveOnContainer = Color(0xFFDDE4B4)

/** Dark error — matches dark Bad band accent. */
val DarkError = Color(0xFFEF9A9A)

/** Dark error container — error @ ~16%. */
val DarkErrorContainer = Color(0x29EF9A9A)

// ---------------------------------------------------------------------------
// Severity 1–5 (design system; never color-only — always pair with "Severity n/5")
// ---------------------------------------------------------------------------

val SeverityLow = Color(0xFF8F8A5A) // 1–2 muted khaki
val SeverityMedium = Color(0xFFB36B00) // 3
val SeverityHigh = Color(0xFFD84315) // 4
val SeverityCritical = Color(0xFFC62828) // 5

// ---------------------------------------------------------------------------
// Semantic score band colors (docs/COMPONENTS.md / UI_UX.md / design handoff §2)
// LOCKED — do not restyle score semantics. Used by ScoreBandColors / ScoreRing.
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

// ---------------------------------------------------------------------------
// Values / amber badges (ADR-017) — informational, not score signals
// ---------------------------------------------------------------------------

val brandAmber = Color(0xFFF5A524)

/** Darker amber for text on light surfaces. */
val brandAmberOnLight = Color(0xFF9A6700)

/** Lighter amber for accents on dark surfaces. */
val brandAmberOnDark = Color(0xFFFFB74D)

// ---------------------------------------------------------------------------
// Legacy aliases (prefer Olive / Cream / Dark* tokens)
// ---------------------------------------------------------------------------

@Deprecated("Use Olive", ReplaceWith("Olive"))
val GreenPrimary = Olive

@Deprecated("Use OliveDark", ReplaceWith("OliveDark"))
val GreenPrimaryDark = OliveDark

@Deprecated("Use PaleLime", ReplaceWith("PaleLime"))
val GreenPrimaryLight = PaleLime

@Deprecated("Use Olive", ReplaceWith("Olive"))
val GreenSecondary = Olive

@Deprecated("Use PaleLime", ReplaceWith("PaleLime"))
val GreenTertiary = PaleLime

@Deprecated("Use OliveContainer", ReplaceWith("OliveContainer"))
val GreenContainer = OliveContainer

@Deprecated("Use OliveOnContainer", ReplaceWith("OliveOnContainer"))
val GreenOnContainer = OliveOnContainer

@Deprecated("Use DarkOlivePrimary", ReplaceWith("DarkOlivePrimary"))
val DarkPrimary = DarkOlivePrimary

@Deprecated("Use DarkOnPrimary", ReplaceWith("DarkOnPrimary"))
val GreenDarkPrimary = DarkOlivePrimary

@Deprecated("Use DarkOnPrimary", ReplaceWith("DarkOnPrimary"))
val GreenDarkOnPrimary = DarkOnPrimary

@Deprecated("Use DarkOliveContainer", ReplaceWith("DarkOliveContainer"))
val DarkPrimaryContainer = DarkOliveContainer

@Deprecated("Use DarkOliveOnContainer", ReplaceWith("DarkOliveOnContainer"))
val DarkOnPrimaryContainer = DarkOliveOnContainer

@Deprecated("Use DarkOliveContainer", ReplaceWith("DarkOliveContainer"))
val GreenDarkPrimaryContainer = DarkOliveContainer

@Deprecated("Use DarkOliveOnContainer", ReplaceWith("DarkOliveOnContainer"))
val GreenDarkOnPrimaryContainer = DarkOliveOnContainer

@Deprecated("Use DarkOlivePrimary", ReplaceWith("DarkOlivePrimary"))
val GreenDarkSecondary = DarkOlivePrimary

@Deprecated("Use PaleLime", ReplaceWith("PaleLime"))
val GreenDarkTertiary = PaleLime

@Deprecated("Use CreamSurface", ReplaceWith("CreamSurface"))
val LightBackground = CreamSurface

@Deprecated("Use Ink", ReplaceWith("Ink"))
val LightOnBackground = Ink

@Deprecated("Use CreamSurface", ReplaceWith("CreamSurface"))
val LightSurface = CreamSurface

@Deprecated("Use Ink", ReplaceWith("Ink"))
val LightOnSurface = Ink

@Deprecated("Unused brand extra")
val brandTeal = Color(0xFF0F6B6B)
