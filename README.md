# AisleSpy

**What’s really in the aisle.**

Privacy-first Android app to scan food and beauty barcodes, score products **1–100**, and explain problem ingredients in plain language—using open data, not user surveillance.

[![Status](https://img.shields.io/badge/status-docs%20only-blue)](#project-status)
[![License](https://img.shields.io/badge/license-Apache%202.0-green)](LICENSE)

---

## Why AisleSpy?

| Alternative | Problem | AisleSpy |
|-------------|---------|----------|
| **Yuka** | Heavy data collection; Google Play dependency | No accounts, no analytics by default; F-Droid-ready; no Play Services |
| **Open Food Facts app** | Powerful but hard to tell good vs bad at a glance | Big 1–100 score, ranked concerns, short “why it matters” copy |

Data comes from **[Open Food Facts](https://world.openfoodfacts.org/)** and **[Open Beauty Facts](https://world.openbeautyfacts.org/)**. Scoring and ingredient explanations run **on your device**.

---

## Project status

```
STATUS: documentation complete; implementation not started
```

Phase 0 is done: product specs, architecture, UI/view contracts, scoring methodology, API contracts, and agent handoff docs. **No Android application source yet.**

| Phase | Description | Status |
|-------|-------------|--------|
| 0 | Docs & agent handoff | Done |
| 1 | Android bootstrap (Compose) | Pending |
| 2 | Barcode scan + OFF/OBF lookup | Pending |
| 3 | Food scoring | Pending |
| 4 | Beauty scoring | Pending |
| 5 | History & polish | Pending |
| 6 | GitHub Releases + F-Droid | Pending |

See [docs/ROADMAP.md](docs/ROADMAP.md).

---

## For coding agents

Start at **[AGENTS.md](AGENTS.md)**. Read order and non-negotiables are defined there.

---

## For humans

| Doc | Contents |
|-----|----------|
| [docs/PRODUCT.md](docs/PRODUCT.md) | Vision, users, MVP, non-goals |
| [docs/UI_UX.md](docs/UI_UX.md) | Screens, navigation, copy |
| [docs/SCORING.md](docs/SCORING.md) | How the 1–100 score works |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Technical design |
| [PRIVACY.md](PRIVACY.md) | What we do and don’t collect |
| [docs/FDROID.md](docs/FDROID.md) | Distribution plan |

---

## Planned features (MVP)

- Barcode scan (CameraX + FOSS decoder) and manual entry
- Food (Open Food Facts) + beauty (Open Beauty Facts)
- Composite **1–100** score (higher = better)
- Problem ingredients with severity and plain-language explanations
- Local scan history only (Room)
- Modern Material 3 UI
- Works without Google Play Services
- Free distribution (GitHub Releases + F-Droid)

**Not medical advice.** Scores are informational tools for reading labels.

---

## Stack (planned)

Kotlin · Jetpack Compose · Material 3 · CameraX · zxing-cpp · Room · OkHttp/Ktor · Coil

Application ID: `app.aislespy`

---

## License

Apache License 2.0 — see [LICENSE](LICENSE).

Product data is © Open Food Facts / Open Beauty Facts contributors, available under the [Open Database License](https://opendatacommons.org/licenses/odbl/). See [docs/DATA_SOURCES.md](docs/DATA_SOURCES.md).

---

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md). Implementation has not started; documentation PRs and scoring methodology feedback are welcome once the repo is public.
