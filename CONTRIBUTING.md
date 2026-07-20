# Contributing to AisleSpy

Thanks for helping build a privacy-respecting product scanner.

AisleSpy is a public beta: scan + dual OFF/OBF lookup, food and beauty scoring, on-device history, and signed APKs on GitHub Releases (`v0.1.0-beta.x`). F-Droid submission is planned. For architecture and agent handoff, start with [AGENTS.md](AGENTS.md) and the [docs/](docs/) tree.

## Principles

1. **Privacy first** — no trackers, no silent data collection.
2. **Transparency** — scoring changes belong in `docs/SCORING.md` + knowledge packs, not only in code.
3. **FOSS only** — dependencies must be acceptable for F-Droid (see [docs/FDROID.md](docs/FDROID.md)).
4. **No medical claims** — plain language, cited concerns, clear disclaimers.
5. **Be kind** — to users reading labels and to people maintaining open databases.

## Build & run

**Requirements**

- JDK **17+**
- Android SDK with **API 35** (compile/target); **minSdk 26**
- Android Studio (recommended) or command line with the Android SDK

**From Android Studio**

1. Open the repository root.
2. Sync Gradle, then run the `app` configuration on a device or emulator (API 26+).

**From the command line**

```bash
./gradlew assembleDebug    # debug APK
./gradlew installDebug     # install on a connected device/emulator
```

Dependency injection is a **manual composition root** (`AppContainer` in `app.aislespy.di`) — there is no Hilt. Prefer keeping it that way for simpler F-Droid review unless complexity forces a change (see [AGENTS.md](AGENTS.md) and [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)).

## Tests

Domain and scoring engines are pure-JVM unit tests (no device required):

```bash
./gradlew test
```

Add or extend tests when you change scoring, knowledge matching, product mapping, or category resolution. See [docs/VERIFICATION.md](docs/VERIFICATION.md) for broader checks (network audit, manual scan flows).

## Project layout

| Path | What lives there |
|------|------------------|
| `app/src/main/java/app/aislespy/ui/` | Jetpack Compose screens, navigation, theme |
| `app/src/main/java/app/aislespy/domain/` | Models, food/beauty score engines, category resolver |
| `app/src/main/java/app/aislespy/data/` | OFF/OBF clients, Room history/cache, knowledge packs, prefs |
| `app/src/main/java/app/aislespy/di/` | Manual composition root (`AppContainer`) |
| `docs/` | Product, architecture, scoring, UI, API contracts, roadmap, verification |
| `knowledge/` | Authored knowledge packs and schemas (bundled under app assets for runtime) |
| `app/src/test/` | JVM unit tests (scoring, mapping, API client, etc.) |

Stack and non-negotiables are locked in [AGENTS.md](AGENTS.md). Do not introduce Google Play Services, Firebase, Crashlytics, proprietary analytics, or Google ML Kit barcode APIs.

## How to contribute

1. **Open or reference** a task from [docs/ROADMAP.md](docs/ROADMAP.md) or a GitHub issue.
2. **Keep PRs focused** — one concern per PR when practical.
3. **Add tests** for scoring and mapping logic when behavior changes.
4. **Update docs** if behavior or the privacy surface changes ([PRIVACY.md](PRIVACY.md), [docs/DATA_SOURCES.md](docs/DATA_SOURCES.md), scoring methodology, etc.).
5. **Do not add** Google Play Services, Firebase, ML Kit, analytics SDKs, or non-FOSS dependencies.

Useful contributions include code fixes and features, scoring methodology clarity, knowledge-pack entries with open citable sources, docs, and accessibility polish.

## Knowledge pack edits

- Prefer open regulatory and scientific summaries (EFSA, CosIng, EU ED lists, SCCS, IARC, IFRA, OFF taxonomies).
- **Do not** scrape or copy proprietary databases (e.g. EWG Skin Deep) into packs.
- Every ingredient entry needs a short `why` and at least one `sources` string.
- Match [knowledge/schema/](knowledge/schema/) and [docs/KNOWLEDGE_PACK.md](docs/KNOWLEDGE_PACK.md).
- Severity is 1–5; be conservative—overstating risk erodes trust.

If scoring formulas change, update [docs/SCORING.md](docs/SCORING.md), bump `methodologyVersion`, record a decision in [docs/DECISIONS.md](docs/DECISIONS.md), and adjust golden tests / fixtures. Never hard-code mystery multipliers only in code.

## Brand & copy

- App name: **AisleSpy**
- Light spy-themed microcopy is welcome; scores and health explanations stay serious.
- Avoid fear-mongering or absolute “toxic / safe” language without nuance.

## License

By contributing, you agree your contributions are licensed under the Apache License 2.0 (see [LICENSE](LICENSE)).
