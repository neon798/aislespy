# Handoff: AisleSpy — full app visual identity & screen designs

## Overview
Complete visual identity and screen designs for **AisleSpy** (`app.aislespy`), a privacy-first Android barcode scanner for food and beauty products. Covers all core screens and result states defined in the product brief (methodology 2.0.0): onboarding, scan, manual entry, scored/partial/not-found results, ambiguous category chooser, ingredient detail, nutrition, history, settings, and the trust pages (methodology, privacy, licenses).

Design direction: **warm & natural** — cream surfaces, deep olive brand accent, characterful grotesque headings. Score semantics use the four fixed band colors from the brief (§2), untouched.

## About the Design Files
The files in this bundle are **design references created in HTML** — an interactive prototype showing intended look and behavior, **not production code to copy**. The task is to **recreate these designs in Jetpack Compose + Material 3**, following the app's existing architecture (bottom destinations Scan / History / Settings; other screens push on the nav stack). Open `AisleSpy Prototype.dc.html` in a browser to click through every screen and state.

## Fidelity
**High-fidelity.** Colors, typography, spacing, radii, copy, and interactions are final intent. Recreate pixel-close using Compose/M3 primitives. Exception: the phone bezel/status bar in the prototype is scaffolding — ignore it. **Dark theme is not yet designed** (see Gaps at the end).

## Design Tokens

### Colors — theme (light)
| Token | Hex | M3 mapping / use |
|---|---|---|
| Cream surface | `#faf6ee` | `surface`, `background` — every light screen |
| Ink | `#33301f` | `onSurface` — primary text |
| Olive | `#5d6633` | `primary` — buttons, links, active nav, brand card |
| Olive dark | `#4c5429` | primary pressed/hover |
| Olive container | `rgba(93,102,51,.10–.12)` | badge chips, Nutri-Score tile bg |
| Olive on-container | `#4c5429` | text on olive container |
| Pale lime | `#cdd6a3` | accents on dark scan screen (viewfinder corners, chips, scan line) |
| Scan screen bg | `#23211a` | camera screen only ("dark-adjacent", not the dark theme) |
| Card white | `#ffffff` | all cards |
| Card border | `rgba(80,60,30,.12)` | 1px card outline (`.14`–`.15` for stronger variants) |
| Dashed divider | `rgba(80,60,30,.25)` | 1px dashed section separators |
| Muted text | `rgba(51,48,31,.55)` | secondary text (`.45` labels, `.6–.7` body-muted) |
| Error/destructive | `#C62828` | manual-entry validation, delete confirm (same hex as Bad band — intentional) |
| Canvas behind phone | `#ece7db` | prototype only, not in app |

### Colors — score bands (REQUIRED, from product brief §2 — do not restyle semantics)
| Band | Range | Accent (ring/bar/driver text) | Filled chip bg (white label text) |
|---|---|---|---|
| Excellent | 75–100 | `#2E7D32` | `#1B5E20` |
| Ok | 50–74 | `#B36B00` | `#8A5A00` |
| Poor | 25–49 | `#D84315` | `#BF360C` |
| Bad | 1–24 | `#C62828` | `#B71C1C` |

Keep these in a dedicated `ScoreBandColors` object, not the M3 `ColorScheme`. Dark-theme accents from the brief: `#81C784` / `#FFB74D` / `#FF8A65` / `#EF9A9A`. Band color is never the only signal — always paired with the text band label.

### Colors — severity (our design system, 1–5 scale)
| Severity | Color |
|---|---|
| 1–2 | `#8f8a5a` (muted khaki) |
| 3 | `#B36B00` |
| 4 | `#D84315` |
| 5 | `#C62828` |

Always shown with the numeric label ("Severity 3/5") and a proportional bar (fill = sev × 20%). Never color-only.

### Typography
| Role | Font | Notes |
|---|---|---|
| Headings, wordmark, score numerals, Nutri-Score letter | **Bricolage Grotesque** 700–800 | OFL license — F-Droid safe. Letter-spacing −0.01em on headings |
| Body, labels, buttons | **Public Sans** 400–800 | OFL |
| Barcodes, subscores, weight shares, versions | **IBM Plex Mono** 400–500 | OFL |

Scale (px in prototype ≈ sp): screen title 24 · result product name 21 · score numeral 42 · body 13–13.5 · secondary 11.5–12.5 · section labels 11/700/letter-spacing .07em UPPERCASE · chips 10.5–12.

### Spacing / shape
- Screen horizontal padding: 22–24
- Card radius: **16** (list rows 14, small tiles 9–13, brand/privacy card 18, chooser option 18)
- Pill radius (buttons, chips): full (99)
- Card padding: 12–16; gaps between cards: 8–10
- Primary button: full-width pill, olive bg, cream text, 15–16 vertical padding
- Secondary button: 1.5px olive outline pill, olive text

## Screens / Views

All light screens: cream bg, content scrolls, bottom nav fixed (except onboarding).

### 1. Onboarding (3 steps)
- Header: wordmark "AisleSpy" (Bricolage 800, 19) left, "Skip" right (muted).
- Centered: 150px white circle with simple geometric art per step (barcode bars / score ring 68 / padlock), step title (27/700), body (14, 1.6 lh, muted), step dots (8px, olive = active), full-width olive pill CTA ("Continue" → "Start the recon").
- Step 3 includes fine print: "Scores are informational — not medical advice."
- Copy per step: "Your eyes in the aisle" / "Every score shows its work" / "Recon, not surveillance" (bodies in prototype).

### 2. Scan
- Bg `#23211a`. Header: wordmark (cream) + outline chip "only the barcode is sent" (`#cdd6a3`).
- Viewfinder: rounded-20 area (striped placeholder = camera feed), 4 corner brackets in `#cdd6a3` (4px stroke, 34px arms, 8px radius), centered product barcode card.
- While decoding: horizontal scan line (`#cdd6a3`, glow) sweeping top↔bottom, 1.3s ease-in-out loop. **Reduce-motion: no sweep, static state.**
- Status line: "Line up a barcode inside the frame" → "Running recon…". Shutter: 74px circle, cream fill (lime while decoding), 5px translucent ring, viewfinder glyph. Debounce: one decode at a time.
- Underlink: "Type the barcode instead" → Manual entry.

### 3. Manual entry
- Back link, title "Type the barcode", helper text, mono input (16, letter-spacing .1em, 1.5px border → olive on focus, radius 14), digits only, 8–13 length; error text in `#C62828` ("Barcodes are 8–13 digits — keep typing."). Olive pill "Look it up". Sample-code rows (mono code + muted hint) that fill the field on tap.

### 4. Result — scored (HERO screEN)
Top → bottom:
1. Back link left; **Save** pill right (outline olive → filled olive "Saved ✓" when saved).
2. Identity: source label (10.5/700 olive, "FOOD · OPEN FOOD FACTS"), product name (21/700), brand + mono barcode (muted).
3. **Score hero**: 124px ring — white disc, 7px band-accent arc (sweep = score%, rounded cap, starts 12 o'clock), 42 numeral + "out of 100". TalkBack: **"Score N out of 100, ‹band›, confidence ‹level›"**.
4. Chip row: filled band chip (chip bg, white 12/800 UPPERCASE label) + outline confidence chip ("Confidence: High").
5. Summary sentence (13.5, 1.55 lh). Optional driver line in band accent (12/600, "Main drags: …").
6. Badge row (wrap, centered): olive-container chips — dietary tri-state (Yes/No only, Unknown = omitted), values, NOVA, ownership ("Owned by ‹parent›" / "Independent" only when verified).
7. Dashed divider.
8. **"WHAT'S BEHIND THE NUMBER"** + "How we score" link. White card, one row per component: name + mono weight share ("65% of score"), 5px progress bar (band accent fill) + subscore, detail line. Omitted components render share "omitted", bar empty grey, detail "No NOVA data — score reweighted."
9. **"SUSPECT INGREDIENTS"** (spy voice; "FLAGGED INGREDIENTS" serious variant): ranked by severity desc. Card: name + mono tag + chevron; severity bar (52px track, sev×20% fill, sev color) + "Severity n/5" + position hint ("· near end of list"); short plain-language why. Tap → Ingredient detail. Zero-concern state: single card "Nothing in this product matched our concern pack. That doesn't guarantee perfection — just no known flags."
10. Nutrition entry row (food only): Nutri-Score letter tile (olive container) + "Nutrition / Nutri-Score + per-100 g figures · not part of the score" + chevron.
11. Ingredients text (10.5, muted).
12. **Mandatory disclaimer** — dashed-border card, 11px: "Scores are informational only — not medical advice or an allergen guarantee. Open data can be incomplete; the physical label is authoritative." + "How we score" link. Present on every result state except not-found.

### 5. Result — partial / unscored
Identity block; 110px circle with "—" + "unscored" (no invented number, no band chip); outline chip "Confidence: Low"; message "Found in Open Beauty Facts, but there's no usable ingredient list — so we won't invent a score."; secondary CTA "Add the ingredient list"; disclaimer.

### 6. Result — not found
110px dashed circle "?", title "Cold case" (spy) / "Not found", mono barcode, copy, primary "Scan another" + secondary "Contribute this product".

### 7. Category chooser (ambiguous barcode)
Title "One barcode, two matches", explainer, two option cards (source label olive 10.5 UPPERCASE, product name 17/700, meta muted; border → olive on hover/press). Tap → scores that dossier.

### 8. Ingredient detail
Mono breadcrumb (product · tag), concern name (26/700), 80px severity bar + "Severity n of 5" (sev color, 800), severity meaning line (fixed strings for 1–5, in prototype logic), position card ("Position on label: … — earlier usually means more of it."), full explanation (13.5, 1.65 lh), SOURCES card (olive rows), footer "Informational only — not medical advice."

### 9. Nutrition (food only, never scored)
Olive-container banner: "Shown for context only — nutrition does not affect the AisleSpy score." Nutri-Score card (52px letter tile + caption). "PER 100 G" card: label/value mono rows — Energy, Sugars, Salt, Saturated fat, Fibre, Protein.

### 10. History ("Mission log")
Title + "Clear all" (red, two-tap confirm → "Really clear all?"). Rows: 44px filled band-chip score tile (white numeral; grey "—" tile + "Unscored" for partials), name, "‹Band› · ‹time›", per-row "×" (two-tap: × → "Delete?" red). Newest first. Tap opens result. Empty state: "No missions yet. Scan something in the aisle to start the log."

### 11. Settings (trust hub)
Olive brand card "Recon, not surveillance" + privacy posture copy. TRUST card: How we score / Privacy / Licenses & attribution rows (chevrons). VERSIONS card: App 0.9.0 · Methodology 2.0.0 · Knowledge pack 2026.07.12 (mono values).

### 12–14. Methodology / Privacy / Licenses
Methodology: intro (ingredient quality only), four-band table (filled chips + mono ranges), food weights table (65/30/5), reweight + beauty formula + confidence paragraph, disclaimer. Privacy: five head+body cards (no accounts / no telemetry / on-device history / barcode-only lookups / on-device scoring). Licenses: Apache-2.0, ODbL attribution to OFF/OBF, FOSS decoder note, knowledge-pack sources.

### Bottom nav
3 destinations: Scan (viewfinder icon) / History (clock) / Settings (sliders). 22px stroke icons, 10.5/700 labels. Active = olive; inactive = `rgba(51,48,31,.45)`. Active follows the tab's stack root (a result pushed from History keeps History active).

## Interactions & Behavior
- **Nav model**: bottom tabs reset their stack; Manual, Result, Chooser, Detail, Nutrition, Methodology, Privacy, Licenses push; Back pops.
- **Scan**: debounced decode → 1.6s lookup → outcome (scored / partial / chooser / not-found). Successful scans prepend to History ("Just now"), deduped by barcode.
- **Result entrance**: content fades up 8px, 350ms ease. Ring sweep may animate on entry (animate `sweepAngle` 0→score%). **All motion gated on reduce-motion.**
- **Destructive confirmations**: two-tap inline pattern (Clear all, per-row delete) — no dialogs in prototype; a `ConfirmationDialog` is an acceptable M3 substitute for Clear all.
- **Manual validation**: digits only, max 13; error under field; unknown code → not-found result.
- **States covered**: loading (scanning), success, partial/unscored, not-found, ambiguous chooser, empty history. Network-error state not designed yet — reuse not-found layout with retry copy.

## State Management
Nav stack (root tab + pushed screens) · scanning flag · scan history list (newest first, on-device) · saved/bookmarked map · manual-entry code + validation error · two-tap confirm state. Data per product: identity, kind (food/beauty), score (nullable), band (derived), confidence, summary, driver, badges, components (subscore + weight share + detail + omitted flag), concerns (severity 1–5, tag, position hint, short/long copy, sources), ingredients text, Nutri-Score + nutriments.

## Accessibility (from brief — hard requirements)
- Score TalkBack: "Score N out of 100, ‹band›" (+ confidence).
- Band and severity never color-only (band label chip; "Severity n/5" text).
- Layouts must survive large system fonts; reduce-motion alternatives for scan line, fade-up, ring sweep.
- WCAG AA text contrast floor (muted text ≥ `rgba(51,48,31,.45)` on cream is the minimum used; verify at implementation).

## Assets
No image assets. All iconography is simple geometric strokes (viewfinder, clock, sliders, corner brackets) — redraw as vector or use any OFL/Apache icon set. Fonts: Bricolage Grotesque, Public Sans, IBM Plex Mono — all OFL, bundle as font resources (F-Droid compatible). Camera preview in prototype is a striped placeholder.

## Files
- `AisleSpy Prototype.dc.html` — interactive prototype, all screens/states (open in browser; needs `support.js` + `android-frame.jsx` alongside)
- `android-frame.jsx` — phone-bezel scaffolding (ignore for implementation)
- `support.js` — prototype runtime (ignore)
- `Style Explorations.dc.html` — earlier visual-direction exploration (context only)

Scan queue in the prototype cycles through: Ok result → Bad result → ambiguous chooser → Excellent (zero concerns) → partial → not found.

## Gaps / not yet designed
- **Dark theme** (required by brief) — light only so far; dark band accents listed above.
- Network-error result state; camera-permission rationale/denied states.
- App icon / launcher identity.
