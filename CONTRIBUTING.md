# Contributing to AisleSpy

Thanks for helping build a privacy-respecting product scanner.

## Current stage

**Documentation only.** Application code is not started yet. Useful contributions right now:

- Clarify product docs
- Improve scoring methodology wording and sources
- Expand knowledge-pack **schemas/examples** with well-cited open sources
- Fix typos / structure

When coding starts, follow [AGENTS.md](AGENTS.md) and [docs/ROADMAP.md](docs/ROADMAP.md).

## Principles

1. **Privacy first** — no trackers, no silent data collection.
2. **Transparency** — scoring changes belong in `docs/SCORING.md` + knowledge packs, not only in code.
3. **FOSS only** — dependencies must be acceptable for F-Droid (see [docs/FDROID.md](docs/FDROID.md)).
4. **No medical claims** — plain language, cited concerns, clear disclaimers.
5. **Be kind** — to users reading labels and to people maintaining open databases.

## Knowledge pack edits

- Prefer open regulatory and scientific summaries (EFSA, CosIng, EU ED lists, OFF taxonomies).
- **Do not** scrape or copy proprietary databases (e.g. EWG Skin Deep) into packs.
- Every ingredient entry needs a short `why` and at least one `sources` string.
- Match [knowledge/schema/](knowledge/schema/) and [docs/KNOWLEDGE_PACK.md](docs/KNOWLEDGE_PACK.md).
- Severity is 1–5; be conservative—overstating risk erodes trust.

## Code contributions (when Phase 1+ exists)

1. Open or reference a ROADMAP task (`T-xxx`).
2. Keep PRs focused.
3. Add tests for scoring and mapping logic.
4. Do not add Google Play Services, Firebase, ML Kit, or analytics SDKs.
5. Update docs if behavior or privacy surface changes.

## Brand & copy

- App name: **AisleSpy**
- Light spy-themed microcopy is welcome; scores and health explanations stay serious.
- Avoid fear-mongering or absolute “toxic / safe” language without nuance.

## License

By contributing, you agree your contributions are licensed under the Apache License 2.0 (see [LICENSE](LICENSE)).
