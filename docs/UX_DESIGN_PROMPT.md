# UX design prompt — AisleSpy

**Audience:** Professional UI/UX designer  
**Purpose:** Full creative freedom on visual identity; this document is the functional and product brief only  
**Application ID:** `app.aislespy` · **License:** Apache-2.0  
**Status:** Phases 1–5 implemented (functionally complete, unit-tested); Phase 6 (ship) not started

This prompt is **deliberately the inverse of a visual style guide**. You get complete product context—mission, features, flows, scoring semantics, accessibility, and hard non-visual constraints—so you can invent the look from scratch. **You do not need any other project file to begin.**

---

## Creative freedom (read first)

**The only visual / color specification in this entire document is the rating score-band colors in §2.** Those four bands must keep traffic-light semantics and readable contrast. Everything else is yours:

- Brand palette and surface colors (including whether the app feels warm, cool, minimal, bold, editorial, etc.)
- Typography and type scale
- Iconography and illustration style
- Component shapes, elevation, density, and spacing
- Layout structure and visual hierarchy (beyond the functional content that must appear)
- Dark theme treatment
- Motion language (with a reduce-motion alternative required)
- Empty, loading, and error treatments
- App icon / launcher identity and in-app wordmark treatment

**Do not treat the current app UI as a design system.** It is an engineer-built functional shell. You are not expected to preserve its appearance, component library, or brand treatment. Engineers will implement your work in **Jetpack Compose + Material 3**—that is an *implementation constraint* (see §6), not a request to look like default Material.

---

## 1. Mission

### What AisleSpy is

**AisleSpy** is a **privacy-first Android app** that helps people understand what is actually in food and beauty products while shopping. Users scan a product barcode (or type it in). The app looks the product up in open community databases—**Open Food Facts** (food) and **Open Beauty Facts** (beauty)—then shows:

1. A clear **1–100 score for ingredient quality** (higher = better / fewer concerns).
2. Plain-language explanations of **problem ingredients** and what drives the score.
3. Separate, non-scored context such as nutrition (food), dietary and values badges, and brand ownership.

**Tagline:** *What’s really in the aisle.*

### Why it exists

Shoppers face dense labels, marketing claims, and proprietary “health score” apps that often track users or depend on Google Play Services. AisleSpy aims to feel as instantly readable as the best consumer scanners while staying open, private, and installable without proprietary stacks (including a path to F-Droid and direct APK).

### Target users

- **Label-conscious shoppers** who want a fast at-a-glance verdict *and* the reasoning behind it.
- **Privacy-conscious users** who refuse accounts, cloud sync, and analytics SDKs.
- **FOSS / F-Droid users** who want a modern product experience without Play Services.

### Privacy posture (product promise)

- **No user accounts.**
- **No telemetry / analytics by default.**
- **On-device history only** (local database).
- Network lookups send **only the barcode** (plus normal HTTP headers) to Open Food Facts / Open Beauty Facts and their image CDNs—not to a first-party AisleSpy backend.
- Scoring runs **on the device** from product data + a shipped knowledge pack.

### Brand voice

- **Helpful spy:** lightly playful recon flavor is welcome in titles, section names, and loading lines (e.g. “Running recon…”, “Suspect ingredients”, “Mission complete”).
- **Not a doctor:** score, severity, and health-adjacent copy stay **clear and serious**. No diagnosis language.
- **Not a scold:** do not moralize the user.
- **No fear-mongering absolutes:** avoid “toxic / safe” as categorical claims. Use concerns, flags, severity, and confidence.
- **Honest about limits:** incomplete open data → show confidence and partial states; never invent false precision.

Scores are **informational only**—not medical advice, not an allergen guarantee, not a safety certification. That disclaimer must remain visible on results.

---

## 2. The rating system + its colors (the ONLY color spec)

### What the score means

| Property | Rule |
|----------|------|
| Scale | **1–100** integer |
| Direction | **100 = best** (better ingredient quality / fewer concerns); **1 = worst** |
| Scope (food, methodology **2.0.0**) | **Ingredient quality only** — flagged ingredients, ultra-processing (NOVA), small label positives. **Nutrition is not part of this number.** |
| Scope (beauty) | Formula / hazard / fragrance / regulatory signals from the beauty knowledge pack |
| Partial data | If there is **no ingredient-quality data at all**, show no invented number (placeholder like “—”) and a clear message instead of a fake mid-score |

### Four score bands (required rating colors)

These band boundaries and **traffic-light semantics** are fixed. Hex values below are the **required rating UI colors** from the current theme implementation. You may refine shade slightly for your system, but you must:

1. Keep the **four-band** structure and boundaries.
2. Preserve **green → amber/yellow → orange → red** meaning (excellent → ok → poor → bad).
3. Maintain **readable contrast** for filled chips and labels (see accessibility below).

| Band | Score range | Semantic | Light theme accent | Dark theme accent | Filled chip background (light-tuned) |
|------|-------------|----------|--------------------|-------------------|--------------------------------------|
| **Excellent** | 75–100 | Green | `#2E7D32` | `#81C784` | `#1B5E20` |
| **Ok** | 50–74 | Amber / yellow | `#B36B00` | `#FFB74D` | `#8A5A00` |
| **Poor** | 25–49 | Orange | `#D84315` | `#FF8A65` | `#BF360C` |
| **Bad** | 1–24 | Red | `#C62828` | `#EF9A9A` | `#B71C1C` |

Filled score chips/badges use near-white label text (`#FFFFFF`) on the chip backgrounds above so contrast remains workable.

**Accessibility (hard requirement for rating UI):**

- Band color must **never be the only signal**. Always pair color with a **text band label** (Excellent / Ok / Poor / Bad).
- TalkBack (and similar) should announce something like: **“Score N out of 100, \<band\>”** (plus confidence when shown).
- Filled chips and score accents must remain legible in light and dark themes.

### Confidence (functional, not colored by us)

Confidence is **High / Medium / Low**, derived from how complete the ingredient-quality inputs are. It is a transparency control—not a second score. How you visualize it is yours; the three levels and their meaning must stay clear.

### Ingredient severity (functional only — colors are yours)

Each flagged ingredient has a **severity 1–5**:

| Severity | Meaning (functional) |
|----------|----------------------|
| 1 | Mild note / sensitive individuals |
| 2 | Minor concern / limited-evidence debate |
| 3 | Moderate concern / notable caveats |
| 4 | Strong concern / restricted in some regions |
| 5 | Highest concern in our pack (ban/restrict / strong evidence flags) |

**Do not assign severity colors from this document.** Severity color, shape, and hierarchy are entirely your design system. Severity must still be readable as a **numeric or labeled 1–5 scale** (not color alone).

---

## 3. All features (functional inventory)

Describe *what* the app does. Nothing here dictates look-and-feel.

### 3.1 Capture

- **Barcode scanning:** live camera; FOSS decoder (no Google ML Kit / Play Services); continuous decode with **debounce** so the same code does not fire repeatedly.
- **Manual barcode entry:** type EAN/UPC when the camera fails or is denied; validate digits; look up.
- **Camera permission:** rationale when needed; usable denied state with path to manual entry / system settings.

### 3.2 Lookup & category

- **Dual lookup:** query **Open Food Facts** and **Open Beauty Facts** in parallel for the barcode.
- **Category resolution:** product may resolve as food, beauty, not found, network error, or **ambiguous** (both databases return a plausible hit).
- **Category chooser:** when ambiguous, user picks which dossier (food vs beauty product card) to use for scoring.
- **Cache:** successful product payloads cached on device with a **7-day TTL** for faster repeat lookups and limited offline reuse of recent data.
- **History offline:** past scans remain available from local history even when the network is down; brand-new barcodes generally need network.

### 3.3 Primary score — ingredient quality (methodology 2.0.0)

Food components and base weights:

| Component | Weight | Role |
|-----------|--------|------|
| Flagged ingredients (knowledge pack / additives) | 65% | Matches curated concern entries; drives most of the score |
| Ultra-processing (NOVA group) | 30% | NOVA 1–4 maps to a processing subscore |
| Positives (labels) | 5% | Small bonus path (e.g. organic, fair-trade); cannot dominate |

- **Missing-data reweight:** if a component’s inputs are missing, drop its weight and renormalize the rest to 100%.
- **No inventing a number:** if there is no ingredient text/tags, no additives tags, and no NOVA, show **partial / unscored** messaging instead of a fake score.
- **Confidence:** High when ingredient data and NOVA are both present; Medium when exactly one; Low when sparse.
- Beauty scoring is parallel but different formula: hazards (INCI, **position-weighted**), fragrance/allergens, regulatory flags; partial when no ingredients.

### 3.4 Score explanation

On a successful scored result, users get:

- **Band + summary sentence** conditioned on band × whether any ingredients were flagged (zero concerns must not imply flags exist).
- Optional **“main drags” driver line** naming the largest score reducers (e.g. ultra-processing, flagged ingredients).
- **Per-component breakdown:** each component’s subscore, human detail, and **weight share** (“N% of score”).
- **Omitted components** for missing data as reweight notes (e.g. “NOVA — no data (score reweighted)”).
- Path to full **methodology** copy (“How we score”).

### 3.5 Suspect ingredients (concerns)

- Ranked list of matched concerns: **severity 1–5**, short plain-language **why**, optional **position hint** (e.g. near top of list), sources available on detail.
- Tap through to **ingredient detail**: full explanation, severity, position, sources.
- Sorted by severity (then name). Empty list is a valid “nothing flagged in our pack” outcome—copy must not invent red flags.

### 3.6 Nutrition (separate, not scored)

- Food products only: a dedicated **nutrition** surface.
- Shows **Nutri-Score grade** (when present) and **per-100 g** nutriments (energy, sugars, salt, saturated fat, fiber, protein) when available.
- Must be **clearly labeled as not affecting the primary AisleSpy score**.
- Nutri-Score is **not** a primary result badge in methodology 2.0.0.

### 3.7 Beauty path

- Same result shell pattern: score (or partial), breakdown, concerns, badges.
- Hazards use ingredient list **order as a concentration proxy** (earlier = more weight).
- Fragrance / parfum and listed allergens contribute to a dedicated component.
- Regulatory-restricted / banned pack entries contribute to another.
- Prefer partial “found but no ingredients to score” when OBF product exists without usable list.
- Beauty catalogue coverage in open data is sparse; not-found states are common—contribute links and honest copy matter.

### 3.8 Informational badges & flags (shown, never scored)

These appear on results for context. **None** enter the 1–100 total.

| Family | Applies to | Behavior |
|--------|------------|----------|
| **Dietary** | Food | Vegan / vegetarian / dairy-free as **tri-state** Yes / No / Unknown. Unknown → **no badge**. Show definitive yes and no with clear copy (“Vegan”, “Not vegan”, “Contains dairy”, etc.). |
| **Values / certification** | Food + beauty | Fair-trade, certified organic, cruelty-free, Rainforest Alliance, UTZ, B Corp—when tags support them. |
| **Brand ownership** | Food + beauty | **“Owned by \<corporate parent\>”** only on curated conglomerate match; **“Independent”** only on verified allowlist. If neither (or conflict): **no badge**—never guess. |
| **NOVA** | Food when present | Processing group shown as informational context on the result (primary food score still uses NOVA as a weighted component). |

Organic can both (a) slightly affect the tiny **positives** score component and (b) appear as a values badge; the badge itself adds no extra score.

### 3.9 History

- On-device only; **newest first**.
- Open a past scan to re-load result (from cache/network as available).
- Delete individual entries; clear all with confirmation.
- Empty state when no missions yet.

### 3.10 Trust, settings, onboarding

- **Onboarding (first launch):** privacy intro—what is scanned, what leaves the device, no accounts/tracking, medical disclaimer; CTA into scan.
- **Settings:** app / methodology / knowledge-pack versions; links to Methodology, Privacy, Licenses.
- **Methodology:** plain-language how scoring works (bands, weights, confidence, disclaimer).
- **Privacy:** short in-app privacy summary consistent with user-facing privacy promises.
- **Licenses / attribution:** app license, open-data license notes (OFF/OBF ODbL attribution), key FOSS libraries.

### 3.11 Mandatory result disclaimer

Every scored (and partial) result must surface that scores are **informational, not medical advice**, data may be incomplete, and the physical label remains authoritative—plus a path into **how we score**.

### 3.12 Explicit non-features (MVP)

Not in scope for design as product features: user accounts, cloud sync, ads, analytics dashboards, full offline product DB download, “healthier alternative” recommender, medical/allergy guarantees, EWG or other proprietary toxicity scrapes, multi-language UI (English first).

---

## 4. Current operation (end-to-end as it ships today)

Use this as the **functional journey** to design for. Appearance is temporary.

### Flow

1. **Launch** → if first run, **Onboarding**; else **Scan**.
2. User **scans** a barcode (debounced) or opens **Manual entry** and submits digits.
3. **Lookup:** check local product cache → else network to OFF + OBF in parallel.
4. Outcomes:
   - **Found food or beauty** → score on device → **Result**.
   - **Both hit ambiguously** → **Category chooser** → user picks → **Result**.
   - **Not found** → Result not-found state with contribute links + scan another.
   - **Network error** → retry / scan another.
5. **Result success:** product identity, **score (or —)**, band, summary, optional driver line, confidence, informational badges, component breakdown (+ reweight notes), suspect ingredients, ingredients text when present, nutrition entry point (food), disclaimer + how we score.
6. User may open **Ingredient detail** for a concern, or **Nutrition** for food.
7. Successful scans are **saved to History** (local); History and Scan share bottom-level navigation with Settings.

### Screens that exist today

| Screen | Role |
|--------|------|
| Onboarding | First-launch privacy / mission brief |
| Scan | Camera + recents + entry to manual / history / settings |
| Manual entry | Typed barcode lookup |
| Result | Hero product verdict + explanation states |
| Category chooser | Food vs beauty when both databases match |
| Ingredient detail | Full concern explanation |
| Nutrition | Nutri-Score + nutriments (not scored) |
| History | Local past scans |
| Settings | Trust hub + versions |
| Methodology | How scoring works |
| Privacy | Privacy summary |
| Licenses | Attribution |

Navigation pattern today: bottom destinations **Scan / History / Settings**; other screens push on the stack.

### States the result path must cover

Loading · Success (scored) · Partial / unscored (not enough ingredient data) · Not found · Network error · Handoff to category chooser.

### Important framing for you

**The current UI is engineer-built and placeholder.** It proves features and accessibility hooks; it is **not** a brand system to preserve. Redesign structure, chrome, hierarchy, and identity freely—as long as the functional content, rating-band semantics, privacy posture, and accessibility requirements remain.

---

## 5. What we’re asking for

### Ownership

A **complete visual identity and design system from scratch**, except for the **rating band semantics and colors** in §2 (which you may refine slightly but not replace with a different meaning system).

You own: palette (outside score bands), type, icons, components, layouts, light + dark, motion, empty/error/loading, app and launcher identity, and how spy microcopy is expressed without harming clarity.

### Hero screen

The **result screen** is the hero. A user should understand good vs bad **in a few seconds**, then be able to drill into *why* (drivers, breakdown, concerns) without confusion about what is scored vs informational.

### Deliverables

1. **High-fidelity mocks** — at minimum: **Result (success)**, **Scan**, **History**. Strongly preferred additions: partial/unscored result, not-found, category chooser, ingredient detail, nutrition, onboarding, settings.
2. **Component / design-system spec** — how score, band label, confidence, badges (dietary / values / ownership), severity, lists, forms, navigation, and states are expressed.
3. **Light and dark** — both required.
4. **Assets** — FOSS-licensable or original only (F-Droid distribution; no proprietary icon packs with incompatible licenses).

### Accessibility (required)

- TalkBack semantics for the score: e.g. **“Score N out of 100, \<band\>”** (and confidence when relevant).
- Band and severity never color-only.
- Large system font resilience (layouts must not collapse or clip critical content).
- Reduce-motion alternatives for any decorative or score animation.
- Sufficient text contrast for body and interactive labels (WCAG AA as a floor for text).

### Brand within copy

Spy flavor in titles and flavor text is welcome; **score, severity, and health-adjacent sentences stay serious**. Disclaimer always present on results.

---

## 6. Hard constraints (non-visual)

| Constraint | Detail |
|------------|--------|
| **Implementation** | Designs must be **implementable in Jetpack Compose + Material 3**. Use that as an engineering reality check for component complexity—not as a mandate to look like stock Material templates. |
| **Platform** | Android phone primary; minSdk 26, targetSdk 35; portrait-first. |
| **FOSS assets** | Original or FOSS-licensable only; F-Droid-ready. |
| **No proprietary services UI** | No Play Services, Firebase, Crashlytics, or analytics product surfaces. |
| **Privacy-shaped product** | No accounts UI; no tracking/consent-for-ads flows; history is on-device; lookups send only the barcode to OFF/OBF. |
| **Score honesty** | Serious score/health language; **disclaimer always on results**; confidence and partial states when data is weak. |
| **Open-data ethos** | Product data from OFF/OBF; knowledge packs from open regulatory/scientific sources. **No EWG** or other proprietary toxicity scrapes as a data source. |
| **Informational badges ≠ score** | Dietary, values, ownership never alter the 1–100 total. Nutrition never alters it. |
| **Distribution intent** | GitHub Releases + F-Droid path; Network anti-feature will be disclosed for online lookup. |

---

## 7. Glossary

| Term | Meaning |
|------|---------|
| **Nutri-Score** | Front-of-pack nutrition grade (A–E) from Open Food Facts when present. **Display-only** on the nutrition screen; **not** part of the primary AisleSpy score in methodology 2.0.0. |
| **NOVA** | Ultra-processing classification (groups 1–4). Used as a weighted **food score component** and may also appear as informational context. |
| **OFF** | Open Food Facts — open food product database. |
| **OBF** | Open Beauty Facts — open cosmetics/beauty product database. |
| **Knowledge pack** | Curated on-device JSON of ingredients/additives (and a separate brand-ownership pack) with plain-language reasons and sources used for matching and scoring. |
| **Concern** | A user-facing flagged ingredient/additive after matching; includes severity, short why, optional position hint, sources. |
| **Severity** | Integer **1–5** intensity of a concern (functional scale; visual treatment is yours). |
| **Confidence** | High / Medium / Low indicator of how complete the inputs were for the score. |
| **Band** | One of four score ranges: Excellent (75–100), Ok (50–74), Poor (25–49), Bad (1–24). |
| **Values badge** | Informational certification chip (fair-trade, certified organic, cruelty-free, etc.); **not scored**. |
| **Ownership flag** | Informational “Owned by \<parent\>” or verified “Independent”; **not scored**; omitted when unknown. |
| **Methodology version** | Currently **2.0.0** for food ingredient-quality scoring; shown in settings/trust surfaces. |
| **Driver sentence** | Optional “main drags” line naming the largest weighted reducers of the total. |
| **Reweight** | Dropping missing components and renormalizing remaining weights so the total still uses available signals fairly. |

---

## Closing

You have the full product brain and almost no visual leash—by design. Anchor on **ingredient-quality truth at a glance**, **explainable distrust of marketing**, and **privacy as a feature**. Keep the four rating bands legible and honest. Invent everything else.

Welcome aboard. What’s really in the aisle—and what it *looks* like—is yours to define.
