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

## Template for new entries

```markdown
## ADR-0xx — Title
- **Date:** YYYY-MM-DD
- **Status:** Proposed | Accepted | Superseded by ADR-0yy
- **Context:** …
- **Decision:** …
- **Consequences:** …
```
