# AGENTS.md — AisleSpy

**STATUS:** `public beta — app fully implemented (v0.1.0-beta.x via GitHub Releases); F-Droid submission planned; phases 1–5 done, phase 6 in progress`

This file is the primary entry point for any human or coding agent working on AisleSpy. You do **not** need prior chat context if you follow the read order and non-negotiables below.

---

## What this project is

**AisleSpy** is a privacy-first Android app that scans product barcodes (food and beauty), looks them up in **Open Food Facts** and **Open Beauty Facts**, and shows a clear **1–100 score** plus plain-language explanations of problem ingredients.

- Tagline: *What’s really in the aisle.*
- Brand tone: mischievous but helpful—spy on labels, not users.
- Application ID: `app.aislespy`
- License: Apache-2.0
- Repo path: `/home/zen/Projects/aislespy` (or clone root)

Full product vision: [docs/PRODUCT.md](docs/PRODUCT.md)

---

## Current phase

| Phase | Name | Status |
|-------|------|--------|
| 0 | Documentation & agent handoff | **Done** |
| 1 | Android project bootstrap | **Done** |
| 2 | Scan + dual API lookup | **Done** |
| 3 | Food scoring + concerns | **Done** |
| 4 | Beauty scoring + concerns | **Done** |
| 5 | History, polish, trust | **Done** |
| 6 | Ship (GitHub Releases + F-Droid) | **In progress** |

Phases 1–5 are shipped under `app/` (Kotlin / Compose), covered by JVM unit tests and green CI. The app is in **public beta** as signed APKs on GitHub Releases (`v0.1.0-beta.x`). Phase 6 continues with F-Droid metadata verification and inclusion request (GitHub Releases already in use).

**Do not invent product decisions.** If something is unspecified, check `docs/DECISIONS.md` and open a decision entry rather than guessing.

---

## Read order before coding

1. [docs/PRODUCT.md](docs/PRODUCT.md) — goals, non-goals, MVP
2. [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) — layers, packages, data flow
3. [docs/DOMAIN_MODELS.md](docs/DOMAIN_MODELS.md) — entities and UI models
4. [docs/SCORING.md](docs/SCORING.md) — 1–100 formulas
5. [docs/UI_UX.md](docs/UI_UX.md) — screens, routes, states, copy
6. [docs/API_CONTRACTS.md](docs/API_CONTRACTS.md) — OFF/OBF usage
7. [docs/ROADMAP.md](docs/ROADMAP.md) — tasks with acceptance criteria
8. [docs/VERIFICATION.md](docs/VERIFICATION.md) — how to prove it works

Also read when relevant:

- [docs/COMPONENTS.md](docs/COMPONENTS.md) — shared Compose widgets
- [docs/KNOWLEDGE_PACK.md](docs/KNOWLEDGE_PACK.md) — ingredient risk JSON
- [docs/DATA_SOURCES.md](docs/DATA_SOURCES.md) — licensing & attribution
- [docs/FDROID.md](docs/FDROID.md) — distribution constraints
- [PRIVACY.md](PRIVACY.md) — user-facing privacy promises

---

## Non-negotiables

1. **No Google Play Services**, Firebase, Crashlytics, proprietary analytics, or Google ML Kit barcode APIs.
2. **No user accounts**, no cloud sync, no telemetry by default in MVP.
3. **No scraping EWG** or other non-open / TOS-restricted sources. Use open regulatory lists and OFF/OBF only for product data.
4. Scores are **informational, not medical advice**. Keep disclaimers in UI and store text.
5. Network destinations limited to:
   - `world.openfoodfacts.org` (and documented OFF CDN image hosts)
   - `world.openbeautyfacts.org` (and documented OBF CDN image hosts)
   - Document any new host in PRIVACY.md + DATA_SOURCES.md before shipping.
6. **F-Droid-ready**: all dependencies FOSS; disclose Network anti-feature.
7. Prefer **transparency** over clever black-box scores—methodology lives in SCORING.md.

---

## Stack (locked)

| Area | Choice |
|------|--------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Barcode | CameraX + **zxing-cpp** (not ML Kit) |
| HTTP | Retrofit + OkHttp (ADR-013; not Ktor) |
| JSON | Kotlinx Serialization |
| Local DB | Room (history + product cache) |
| Prefs | DataStore |
| Images | Coil |
| Async | Coroutines + Flow |
| minSdk | 26 |
| targetSdk | 35 |
| Package | `app.aislespy` |

DI: **manual composition root** preferred over Hilt for simpler F-Droid review unless complexity forces otherwise.

---

## How to implement a feature

1. Pick a task ID from [docs/ROADMAP.md](docs/ROADMAP.md).
2. Match **screen IDs** and view states in [docs/UI_UX.md](docs/UI_UX.md).
3. Use domain types from [docs/DOMAIN_MODELS.md](docs/DOMAIN_MODELS.md).
4. Implement in the package layout from [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).
5. Add/adjust tests per [docs/VERIFICATION.md](docs/VERIFICATION.md).
6. Do not expand scope beyond the task’s acceptance criteria without updating ROADMAP + DECISIONS.

---

## How to change scoring

1. Update formulas in [docs/SCORING.md](docs/SCORING.md).
2. Bump `methodologyVersion`.
3. Update knowledge packs under `knowledge/` (and future `app/src/main/assets/knowledge/`).
4. Add an entry in [docs/DECISIONS.md](docs/DECISIONS.md).
5. Update golden tests / fixtures.

Never hard-code mystery multipliers only in code.

---

## Brand & copy

- Name: **AisleSpy**
- Light spy microcopy is OK (“Running recon…”, “Suspect ingredients”, “Mission complete”).
- Score number, severity, and “why” text must stay **clear and serious**.
- Never sound like a doctor or guarantee safety.

---

## Definition of done (any feature)

- [ ] Matches ROADMAP acceptance criteria
- [ ] UI states cover loading / success / empty / error as specified
- [ ] No proprietary dependencies introduced
- [ ] Tests or manual checks from VERIFICATION.md pass
- [ ] Privacy surface unchanged (or docs updated)

## MVP definition of done

- Scan food or beauty barcode → 1–100 score → problem ingredients with plain-language why → history on device only → works without Play Services → installable via GitHub/F-Droid path.

---

## Current state

Application code lives under `app/` (Kotlin, Jetpack Compose). The app is in public beta (`v0.1.0-beta.x`) via GitHub Releases; F-Droid submission is planned. The Gradle wrapper is committed; build with `./gradlew assembleDebug` / `test`. Docs under `docs/` remain the product and architecture source of truth. End-user / product-facing entry is [README.md](README.md); this file is the technical and agent handoff. Scoring or knowledge-pack changes still require updates to [docs/SCORING.md](docs/SCORING.md) and [docs/DECISIONS.md](docs/DECISIONS.md) (and golden tests / `knowledge/` as appropriate)—never mystery multipliers only in code.
