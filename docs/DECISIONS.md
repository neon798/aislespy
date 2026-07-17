# Architecture / product decisions (ADR log)

Append-only. Newest at bottom. When changing a decision, add a new entry that supersedes the old id.

---

## ADR-001 — Project name: AisleSpy

- **Date:** 2026-07-16
- **Status:** Accepted
- **Context:** Need a memorable, fun name for a privacy-first food+beauty scanner.
- **Decision:** Name the app **AisleSpy**; tagline “What’s really in the aisle.”
- **Consequences:** Application ID `app.aislespy`; brand microcopy may use light spy metaphors.

---

## ADR-002 — Stack: Kotlin + Jetpack Compose

- **Date:** 2026-07-16
- **Status:** Accepted
- **Context:** Native Android, modern UI, F-Droid friendliness.
- **Decision:** Kotlin, Jetpack Compose, Material 3.
- **Consequences:** No Flutter/RN; agents implement Compose screens per UI_UX.md.

---

## ADR-003 — MVP includes food and beauty together

- **Date:** 2026-07-16
- **Status:** Accepted
- **Context:** User wants both categories like Yuka.
- **Decision:** Ship dual OFF+OBF support in MVP, not food-only first.
- **Consequences:** Parallel API lookup + category chooser required early.

---

## ADR-004 — Online product lookup only (v1)

- **Date:** 2026-07-16
- **Status:** Accepted
- **Context:** Full offline DB is large and complex.
- **Decision:** v1 queries OFF/OBF online; local cache + history only; knowledge packs offline.
- **Consequences:** Network anti-feature; airplane mode limited for new scans.

---

## ADR-005 — No first-party backend

- **Date:** 2026-07-16
- **Status:** Accepted
- **Context:** Free hosting + privacy.
- **Decision:** Client talks directly to OFF/OBF; scoring on-device.
- **Consequences:** No user accounts; knowledge pack updates via app release (or later static URL).

---

## ADR-006 — Barcode: CameraX + zxing-cpp (not ML Kit)

- **Date:** 2026-07-16
- **Status:** Accepted
- **Context:** F-Droid forbids proprietary Google barcode stacks.
- **Decision:** FOSS zxing-cpp Android wrapper + CameraX analysis.
- **Consequences:** Slightly more integration work; works without Play Services.

---

## ADR-007 — License Apache-2.0

- **Date:** 2026-07-16
- **Status:** Accepted
- **Context:** F-Droid-friendly, simple reuse.
- **Decision:** Apache License 2.0 for app code and knowledge packs authored here.
- **Consequences:** OFF/OBF data remains ODbL—attribution required.

---

## ADR-008 — Score scale 1–100 (higher is better)

- **Date:** 2026-07-16
- **Status:** Accepted
- **Context:** Users want a single glanceable number.
- **Decision:** Composite score 1–100; methodology in SCORING.md v1.0.0.
- **Consequences:** Must show confidence when data partial; disclaimer required.

---

## ADR-009 — No analytics / no Play Services

- **Date:** 2026-07-16
- **Status:** Accepted
- **Context:** Differentiate from Yuka; F-Droid.
- **Decision:** Zero analytics SDKs by default; no GMS.
- **Consequences:** Crash reports only if later optional privacy-preserving path is designed (not planned).

---

## ADR-010 — Manual DI preferred

- **Date:** 2026-07-16
- **Status:** Accepted
- **Context:** Simpler review and fewer plugins.
- **Decision:** `AppContainer` composition root; revisit Hilt only if pain is high.
- **Consequences:** Slightly more boilerplate wiring.

---

## ADR-011 — Documentation-first Phase 0

- **Date:** 2026-07-16
- **Status:** Accepted
- **Context:** Multiple agents/humans need shared truth before code.
- **Decision:** Complete docs/schemas before Gradle project.
- **Consequences:** Repo may look “empty” of code initially; AGENTS.md status must stay accurate.

---

## ADR-012 — Do not use EWG as a data source

- **Date:** 2026-07-16
- **Status:** Accepted
- **Context:** Proprietary TOS / scraping risk.
- **Decision:** Knowledge packs from open regulatory & scientific sources only.
- **Consequences:** Pack building is slower but legally/ethically cleaner.

---

## ADR-013 — HTTP stack: Retrofit + OkHttp + kotlinx-serialization

- **Date:** 2026-07-16
- **Status:** Accepted
- **Context:** ARCHITECTURE.md / API docs allowed either Ktor (OkHttp engine) or Retrofit + OkHttp for OFF/OBF clients. Project advisor chose for library maturity, a small dependency surface on Android, and canonical MockWebServer testing patterns used across the Android ecosystem.
- **Decision:** Use **Retrofit 2.11.0** + **OkHttp 4.12.0** + **kotlinx-serialization** (`converter-kotlinx-serialization` 2.11.0, JSON 1.7.3) for product API clients. Mandatory User-Agent interceptor; connect 10s / read 20s timeouts.
- **Consequences:** No Ktor in the app module. Repository and unit tests use Retrofit interfaces + MockWebServer. DTOs stay in `data/remote/dto` and map to domain models before leaving the data layer.

---

## ADR-014 — Dietary flags: vegan / vegetarian / dairy-free (informational only)

- **Date:** 2026-07-16
- **Status:** Accepted
- **Context:** Users want glanceable dietary signals (vegan, vegetarian, dairy-free) from Open Food Facts tags. Scoring must stay nutrition/processing/hazard-focused; dietary lifestyle flags must not shift the 1–100 total.
- **Decision:** Resolve vegan, vegetarian, and dairy-free as a **tri-state** (`Yes` / `No` / `Unknown`) from OFF `ingredients_analysis_tags`, `labels_tags`, and `allergens_tags` (see DOMAIN_MODELS.md). Display only as result-screen badges for **food** products. **Never** feed flags into `FoodScoreEngine` / `BeautyScoreEngine` or any `ScoreResult` component. Conservative display: **Unknown is hidden**; definitive negatives are shown with clear copy (“Not vegan”, “Not vegetarian”, “Contains dairy”); positives use “Vegan”, “Vegetarian”, “Dairy-free”. Beauty products skip dietary badges in MVP.
- **Consequences:** `ingredients_analysis_tags` added to the API fields filter; domain `Product.ingredientsAnalysisTags` + pure `DietaryFlags` resolver; methodologyVersion bumped to `1.0.1` (copy/version only, no formula change). Badge styles for negatives stay neutral/warn—not red-alarm.

---

## ADR-015 — Score explanation: concern-aware copy, drivers, omitted components

- **Date:** 2026-07-16
- **Status:** Accepted
- **Context:** User testing showed two explanation failures: (a) summary sentences said “red flags” / “notes below” even when the concerns list was empty; (b) a ~50 score on a 100% organic product did not explain that nutrition/processing drove the total, that organic is only a 5% positives bonus, or that missing components were silently dropped and reweighted.
- **Decision:** Explanation-only improvements (no weight or subscore formula changes). Bump methodologyVersion to **1.0.2** (copy only). (1) **Summary sentences** are conditioned on score band × concern count (see SCORING.md matrix): with zero concerns, copy must not imply flagged ingredients exist. Beauty keeps its tone under the same principle. (2) **`ScoreResult.driverSentence`**: optional one-liner naming the biggest weighted drags, computed as `(100 - subscore) * normalizedWeight` per component; list contributors whose weighted loss exceeds 5 points (e.g. “Main drags: nutrition (Nutri-Score D), ultra-processing (NOVA 4).”). Null when nothing exceeds the threshold. (3) **`ScoreResult.omittedComponents`**: human labels for components dropped for missing data (e.g. “NOVA (no data)”), shown in the breakdown as muted “no data (score reweighted)” rows. (4) Breakdown UI shows each component’s normalized weight as “N% of score” so the organic +20 at 5% weight is visibly small; positives detail appends “Organic +20” when organic is detected.
- **Consequences:** `ScoreResult` / `ScoreUi` / `ScoreComponentUi` gain explanation fields; `FoodScoreEngine` and `BeautyScoreEngine` populate them; Result screen renders driver line, weight captions, and omitted rows. Golden tests updated only where they assert sentences or version; numeric totals and component scores remain identical.

---

## ADR-016 — Beauty knowledge pack v1.1.0 (85+ entries) and not-found coverage copy

- **Date:** 2026-07-16
- **Status:** Accepted
- **Context:** User testing showed that most cosmetics are not in Open Beauty Facts at all (~67k products vs Open Food Facts ~4.6M). Lookup misses dominate the beauty path. Expanding the INCI knowledge pack improves scoring quality only for products that *are* found; it cannot fix sparse OBF catalogue coverage.
- **Decision:** (1) Expand `beauty_ingredients_v1.json` from 35 to **85+** entries and bump pack version to **1.1.0**, keeping all existing entries. Add remaining EU-labelled fragrance allergens, EU-banned substances (e.g. Lilial, Lyral/HICC, zinc pyrithione, hydroquinone, lead acetate, mercury compounds, DBP), restricted/scrutinized families (isothiazolinones, formaldehyde releasers, hair-dye intermediates, selected UV filters, cyclic siloxane D6), and lower-severity nuance ingredients with conservative severity and open citable sources only (SCCS, CosIng, EU 1223/2009, CMR/IARC/IFRA as applicable—no EWG). (2) On the Result **NotFound** state, under the contribute buttons, add one muted caption: “Beauty products are still sparse in open databases—adding one takes a minute and helps everyone.” Mitigations for lookup coverage remain OBF contribute links + this messaging, not offline databases or scraping.
- **Consequences:** Beauty scoring can flag more ingredients when OBF returns a product with ingredient text/tags. Pack loaders/tests assert ≥85 beauty entries and version 1.1.0. Scoring engines, matcher logic, food pack, and methodology version are unchanged. Users still hit NotFound often for beauty barcodes until OBF coverage grows.

---

## ADR-017 — Values badges: certification labels (informational only)

- **Date:** 2026-07-16
- **Status:** Accepted
- **Context:** Users want glanceable certification signals (fair-trade, certified organic, cruelty-free, etc.) from OFF/OBF `labels_tags`. These are lifestyle/values labels, not hazard or nutrition drivers. A brand political-affiliation indicator was also requested and is **deferred** (not rejected) pending an acceptable open data source and methodology; revisiting that requires its own ADR amending AGENTS.md non-negotiable #3 implications. EWG/Skin Deep integration remains excluded per ADR-012 (proprietary/TOS-restricted) and may be revisited **only** via an explicit commercial license from EWG, which would need its own ADR.
- **Decision:** Derive **values badges** solely from certification label tags already present in OFF/OBF `labels_tags` (conservative exact tags and documented prefixes only—see DOMAIN_MODELS.md). Display them on the result screen for **food and beauty** with a distinct **values** style (gold/amber outline + leading star). **Never** factor values badges into `ScoreResult` or any score engine. Organic continues to contribute **solely** through the existing food **positives** component (5% weight) per SCORING.md; the “Certified organic” badge itself adds nothing. Replace the plain T-330 “Organic” badge with the values-style “Certified organic” badge to avoid duplication. Nutri-Score and NOVA badges remain unchanged.
- **Consequences:** Domain `ValuesBadge` / `ValuesBadgesResolver`; ResultViewModel assembles `style = "values"` badges; UI chip renders gold-star treatment with meaningful TalkBack. No scoring, knowledge-pack, dietary-flags, or methodologyVersion change.

---

## Template for new entries

```markdown
## ADR-0xx — Title
- **Date:** YYYY-MM-DD
- **Status:** Proposed | Accepted | Superseded by ADR-0yy
- **Context:** …
- **Decision:** …
- **Consequences:** …
```
