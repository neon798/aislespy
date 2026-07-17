# Design brief — AisleSpy

**Audience:** Professional UI/UX designer joining the project  
**Status:** Phases 1–5 implemented (functional beta on Android emulator); Phase 6 (ship) not started  
**Application ID:** `app.aislespy` · **License:** Apache-2.0  
**Repo:** `/home/zen/Projects/aislespy` (or clone root)

This document is a **self-contained handoff**. You should not need other docs to start; deeper contracts are linked where engineers will implement your work.

---

## 1. What AisleSpy is

**AisleSpy** is a privacy-first Android app that scans food and beauty product barcodes, looks them up in open databases (Open Food Facts and Open Beauty Facts), and shows a clear **1–100 score** plus plain-language explanations of problem ingredients. Target users are **label-conscious shoppers**—people who want a fast, readable judgment in the aisle without accounts, trackers, or Google Play Services. The score is informational (not medical advice): **100 = better** (healthier / fewer concerns), **1 = worse**, with color bands and ranked “suspect” ingredients so users understand *why* in under a few seconds.

**Tagline:** *What’s really in the aisle.*

Brand tone: mischievous but helpful—spy on labels, not users. Light spy flavor is welcome in titles and loading lines; score numbers, severity, and “why” text stay clear and serious.

---

## 2. Current state

| Item | Reality today |
|------|----------------|
| Platform | Android (minSdk 26, targetSdk 35), phone portrait primary |
| UI stack | Jetpack Compose + Material 3 |
| Features | Scan, manual barcode entry, dual API lookup, food + beauty scoring, history (on-device only), settings/trust screens, first-launch onboarding |
| Quality bar | Unit-tested, CI green (`assembleDebug` / `assembleRelease` / tests) |
| Device QA | **Engineer-designed UI**; advisor emulator testing has noted polish gaps (see §5). Full on-device verification still pending. |
| Screenshots | Can be produced on request from an **Android 35 emulator** |
| Distribution | Not shipped yet; F-Droid + GitHub Releases planned |

Code under `app/src/main/java/app/aislespy/ui/` is the living UI. Spec docs (`docs/UI_UX.md`, `docs/COMPONENTS.md`) describe intended contracts; where they diverge, **this brief and the code** reflect what ships today.

---

## 3. Screen inventory

Navigation: bottom bar on **Scan / History / Settings**; other screens push on the stack. Bottom bar uses letter glyphs today (see §5).

| Screen | Route | Purpose | What exists today | Gaps / placeholders |
|--------|-------|---------|-------------------|---------------------|
| **Onboarding** | `onboarding` | First-launch privacy welcome before scan | Single full-screen scroll: spy-flavored title (“Mission brief: AisleSpy”), product pitch, privacy bullets, medical disclaimer, primary CTA “Start scanning” | Text-heavy; no pager, illustration, or multi-step progressive disclosure |
| **Scan** | `scan` (start after onboarding) | Capture barcode; show recents | Permission rationale + denied copy; CameraX full-bleed preview + amber reticle; torch; “Enter barcode”; horizontal recent history strip with score badges; top bar “AisleSpy” + settings | Bottom-nav icons are glyphs; empty/permission states are plain text + buttons (no illustration) |
| **Manual entry** | `manual` | Type EAN/UPC when camera fails | Top bar, digit field, helper + validation, full-width “Look up” | **Look up button shifts when keyboard opens**—layout stability wanted |
| **Result** (hero) | `result/{barcode}?source=…` | One-glance score + explain concerns | Category/source chips; product image (full-width ~200dp crop); name/brand/barcode; **ScoreRing** + summary + optional driver line + confidence chip; horizontally scrollable badges; score breakdown with weight captions (“N% of score”) + omitted-component notes; “Suspect ingredients” concern cards; ingredients text block; disclaimer + “How we score”; loading / not-found / network-error states | Image can dominate hierarchy; badges scroll with **no visual affordance**; loading/error are spinner/text only; no dedicated empty illustrations |
| **Category chooser** | `choose/{barcode}` | User picks Food vs Beauty when both DBs hit | Title “Two dossiers found” / “Which kind of product?”; two product cards (thumb, name, brand) | Functional cards; not highly designed |
| **Ingredient detail** | `ingredient/{concernId}` | Full explanation of one concern | Name, severity chip + “Severity N of 5”, position hint, full why, sources list | No disclaimer footer on this screen (result still has it); visual polish light |
| **History** | `history` | Local-only past scans | Lazy list: thumb, name, score badge, relative time, category; clear-all confirm; per-row delete; empty copy: “No missions yet—scan something in the aisle.” | Empty state is text only (no illustration); no swipe-to-delete polish specs beyond functional delete |
| **Settings** | `settings` | Trust hub | Links: How scoring works, Privacy, Licenses; read-only knowledge pack / methodology / app versions; privacy summary + OFF/OBF attribution | List + body text; no custom illustration or brand moment |
| **Methodology** | `settings/methodology` | Plain-language scoring summary | Bands, food/beauty weights, confidence, disclaimer | Long-form text layout only |
| **Privacy** | `settings/privacy` | In-app privacy summary | Structured sections matching PRIVACY.md short version | Text layout only |
| **Licenses** | `settings/licenses` | OSS attribution | App license, data license, key libraries list | Text layout only |

### Result screen — success layout (order today)

1. Category + source chips  
2. Product image (full-bleed crop, rounded)  
3. Name, brand, barcode  
4. Score ring (animated arc, band color) + summary sentence + driver line + confidence chip  
5. Badge row (Nutri-Score, NOVA, organic, etc.—when present)  
6. Score breakdown (component label, detail, weight %, numeric subscore)  
7. Suspect ingredients cards (severity chip, name, short why, position hint, chevron)  
8. Ingredients free text  
9. Disclaimer + “How we score” → methodology  

**States on result:** Loading (“Running recon…” + spinner), Success, Partial (no score / weak data message), Not found (contribute links to OFF/OBF + scan another), Network error (retry + scan another), handoff to category chooser.

---

## 4. Design language today

Engineer baseline—not a finished brand system. **Source of truth for colors:**

| File | Role |
|------|------|
| `app/src/main/java/app/aislespy/ui/theme/Color.kt` | Brand greens, surfaces, score band light/dark, chip fills, teal/amber accents |
| `app/src/main/java/app/aislespy/ui/theme/ScoreColors.kt` | Theme-aware score band + severity chip mapping |
| `app/src/main/java/app/aislespy/ui/theme/Theme.kt` | Material 3 light/dark schemes; **dynamic color intentionally off** for consistent brand greens |
| `app/src/main/java/app/aislespy/ui/theme/Type.kt` | Typography (mostly Material defaults + a few overrides) |

### Brand palette (current)

- Primary family: deep greens (`#1B5E20` light primary, softer greens on dark).  
- Surfaces: cool green-tinted light (`#F7FBF7`) / near-black dark (`#101410`).  
- Accents from older teal/amber language still in tokens: `brandTeal` `#0F6B6B`, `brandAmber` `#F5A524` (scan reticle uses amber).  
- Dynamic Material You color: **disabled**.

### Score bands (must stay exact)

| Score | Band label | Color role |
|------:|------------|------------|
| 75–100 | Excellent | Green |
| 50–74 | Ok | Yellow / amber |
| 25–49 | Poor | Orange |
| 1–24 | Bad | Red |

Chip fills use darker containers with white on-chip text for contrast (see `scoreChip*` in `Color.kt`). Severity chips **1–5**: 1–2 amber-ish, 3 orange, 4–5 red.

### Components already in code (`ui/components/`)

| Component | Role |
|-----------|------|
| `ScoreRing` | Hero circular score; ~700ms sweep; TalkBack “Score N out of 100, {label}”; respects reduce-motion |
| `ScoreBadge` | Compact score for history/recents |
| `SeverityChip` | 1–5 severity |
| `InfoChip` | Generic chips (confidence, badges, category) |
| `LoadingRecon` | Spinner + “Running recon…” |
| `ProductImagePlaceholder` | Missing/error image |
| `SectionHeader` | Section titles |

Typography: system / Material 3 defaults—no custom brand typeface yet. Dark theme is fully wired via `AisleSpyTheme`.

### Microcopy bank (flavor only—never for score data)

| Context | Copy |
|---------|------|
| Loading | “Running recon…” |
| Concerns section | “Suspect ingredients” |
| No concerns | “Clean dossier—nothing flagged in our pack.” |
| Empty history | “No missions yet—scan something in the aisle.” |
| Network error | “Lost contact—check your connection.” (pattern) |
| Not found | “This barcode isn’t in the open databases yet.” |
| Category chooser | “Two dossiers found” |

---

## 5. Known rough edges

From advisor emulator testing—**include all of these in design priorities:**

1. **Bottom navigation icons** — Uses letter glyphs **“S”** / **“H”** and a settings gear character because no icon set is in the dependency catalog. Needs a proper **FOSS icon direction** (Material Symbols are fine if licensing is clean).  
2. **Badges row overflow** — Result badges scroll horizontally but with **no scroll affordance** (e.g. edge fade, peek of next chip). Dairy-free and other badges can sit off-screen unnoticed.  
3. **Result product image hierarchy** — Full-width top crop (~200dp) can dominate; hierarchy between **image / name / score** needs intentional design (score is the product promise).  
4. **Manual entry keyboard** — “Look up” button **shifts when the keyboard opens**; want layout stability (sticky CTA / IME-aware layout).  
5. **Loading / empty / error states** — Functional but plain (spinner + text). Opportunity for calm, on-brand illustrations and clearer structure.  
6. **Launcher icon** — Placeholder white magnifier vector on green adaptive background (`res/drawable/ic_launcher_foreground.xml`).  
7. **Onboarding** — Single text-heavy screen; would benefit from visual storytelling and shorter steps.  
8. **Motion** — Essentially only the **score ring sweep** is designed; must continue to **respect reduce-motion** (system animator scale 0 → jump to final value). No broader motion language yet.

---

## 6. Non-negotiable constraints

These are product and legal/trust requirements—not optional polish.

| Constraint | Detail |
|------------|--------|
| **Implementable stack** | Designs must map cleanly to **Material 3 + Jetpack Compose** (components, color roles, navigation patterns). |
| **FOSS-only assets** | No proprietary fonts/icons without OFL / Apache-style (or similarly permissive) licenses. App is **F-Droid-friendly**. |
| **No Play Services UI** | No Google-only UI dependencies, Firebase, or ML Kit branding. |
| **Accessibility (hard)** | TalkBack semantics e.g. **“Score N out of 100”**; **≥4.5:1** contrast for body text; resilient large font scales; **reduce-motion** honored for animations. |
| **Privacy shapes copy** | No accounts, no tracking/analytics in default build, history on-device only. Lookups send **barcode only** to OFF/OBF. Don’t imply cloud profiles or “we keep your scans.” |
| **Score language stays serious** | Score number, severity, and “why” text: **unambiguous, no fear-mongering**, no absolute **“toxic” / “safe”** claims. Spy microcopy **only** in titles/flavor lines—never in score data or medical-adjacent copy. |
| **Disclaimer always visible on results** | Informational / not medical advice; always read the physical label. “How we score” must remain reachable. |
| **Network / ethics** | Product data from open sources only; no EWG scraping or closed third-party “toxicity” APIs in UI stories. |

**Disclaimer (canonical tone):**  
*AisleSpy scores are informational only. They are not medical advice, an allergen guarantee, or a safety certification. Always read the physical label. Product data comes from community databases and may be incomplete or outdated.*

---

## 7. Deliverables requested

Please prioritize **result** as the hero screen, then scan and history.

| Deliverable | Notes |
|-------------|--------|
| **Visual identity refresh** | Palette refinement **within the green brand**; clearer secondary/accent roles; light + dark cohesion |
| **Launcher icon** | Adaptive icon (foreground + background), FOSS/original; readable at small sizes |
| **Iconography direction** | Bottom nav + key actions; Material Symbols or equivalent FOSS set is acceptable |
| **High-fidelity mocks** | **Scan**, **Result** (hero—all major states), **History** (list + empty); bonus: onboarding, manual entry, settings |
| **Component specs** | Score ring (sizes, stroke, labels, a11y); badge/chip system (score, confidence, severity, product badges); concern cards |
| **Motion spec** | Entrance/score/list motion with **reduce-motion** alternatives (static final state) |
| **Dark theme variants** | Same components, both schemes |
| **Empty / error illustrations** | Loading, not found, network error, empty history—**original or FOSS-licensed** only |

Preferred handoff: Figma (or similar) with tokens named so engineers can map to Compose (`Color.kt` / `ScoreColors.kt`). Export notes for adaptive icon layers.

---

## 8. Working with the team

| Topic | Practice |
|-------|----------|
| **Docs-driven repo** | `docs/UI_UX.md` is the **UI contract** (routes, states, copy). Flow/copy changes → update via PR. |
| **Scoring visuals** | Color bands and “100 = best” semantics must match `docs/SCORING.md` **exactly**. Do not invent new band thresholds. |
| **Polish baseline** | ROADMAP **T-520** shipped a baseline visual + a11y pass (dark theme, reduce motion, TalkBack score, sparingly applied microcopy). Your work is the next design layer on top. |
| **Score presentation semantics** | Any change to what the score *means* visually (bands, labels, confidence meaning, severity scale) needs a **`docs/DECISIONS.md` ADR**—not a silent mock change. |
| **Components doc** | `docs/COMPONENTS.md` lists intended shared widgets; code under `ui/components/` is current implementation. |
| **Engineering path** | Design → PR updating UI_UX (and COMPONENTS if needed) → Compose implementation → unit tests / emulator check. |
| **Screenshots** | Ask maintainers for Android 35 emulator captures of current UI before finalizing if parity matters. |

### Related references (optional deeper reads)

- Product vision: `docs/PRODUCT.md`  
- UI contract: `docs/UI_UX.md`  
- Shared widgets: `docs/COMPONENTS.md`  
- Score formulas & bands: `docs/SCORING.md`  
- Privacy promises: `PRIVACY.md`  
- Agent/dev non-negotiables: `AGENTS.md`

---

## 9. Quick glossary

| Term | Meaning |
|------|---------|
| **Nutri-Score** | EU-style nutrition grade A–E on many packaged foods; major food-score input when present. |
| **NOVA** | Ultra-processing classification (groups 1–4); higher group = more processed. |
| **OFF** | [Open Food Facts](https://world.openfoodfacts.org) — open food product database. |
| **OBF** | [Open Beauty Facts](https://world.openbeautyfacts.org) — open cosmetics database. |
| **Knowledge pack** | Curated on-device ingredient/additive metadata (severity, why, sources) used to flag concerns and feed scoring. |
| **Concern** | A flagged ingredient/additive shown to the user with severity + plain-language why. |
| **Severity 1–5** | Concern intensity in our pack (1 mild note → 5 highest concern in pack). Not a legal ban label by itself. |
| **Confidence** | How complete the data was for scoring: High / Medium (partial) / Low—shown so we don’t fake precision. |
| **Band** | Score color group: Excellent / Ok / Poor / Bad at 75 / 50 / 25 boundaries. |

---

## Welcome

Thank you for joining. The product is functionally complete for its MVP feature set; the opportunity is to make **trust, clarity, and delight** match the privacy and scoring rigor already in the code. The result screen should feel instantly readable in a bright grocery aisle—big score, honest confidence, calm concerns—with just enough spy personality to stay memorable without scaring anyone.

When in doubt: **clarity over cleverness**, **FOSS over proprietary**, **serious scores over scary copy**.

— AisleSpy team
