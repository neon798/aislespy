# Product — AisleSpy

## Vision

Help people make quicker, clearer decisions about **food and beauty** products in the store—without surrendering their privacy or depending on Google Play.

AisleSpy should feel as instantly readable as Yuka (big score, obvious concerns) while staying as open and ethical as the Open Food Facts ecosystem.

## Tagline

**What’s really in the aisle.**

## Target users

1. **Privacy-conscious shoppers** who refuse or avoid Play Services / heavy trackers.
2. **Label-overwhelmed shoppers** who want a simple 1–100 signal plus plain explanations.
3. **FOSS / F-Droid users** who want a modern UI without proprietary SDKs.

## Core value proposition

| Promise | How |
|---------|-----|
| Fast judgment | 1–100 score + color band |
| Trustworthy detail | Ranked problem ingredients with short “why” |
| Open data | OFF + OBF product databases |
| Private by design | Local history; no accounts; no analytics SDK |
| Works offline from Google | No Play Services; F-Droid + direct APK |

## MVP scope (must ship)

- [x] Spec’d — not yet implemented
- Barcode scan + manual entry
- Parallel lookup: Open Food Facts + Open Beauty Facts
- Category resolution (food / beauty / chooser / not found)
- On-device composite score 1–100
- Concern list with severity + plain-language why
- Local scan history
- Material 3 modern UI
- Privacy onboarding blurb
- FOSS stack suitable for F-Droid

## Explicit non-goals (MVP)

| Non-goal | Why deferred |
|----------|----------------|
| Full offline product database | Huge downloads; complexity |
| User accounts / cloud sync | Privacy surface |
| In-app photo contribution to OFF/OBF | Deep link only at first |
| “Healthier alternative” recommendations | Needs ranking/search product work |
| Medical / allergy guarantees | Legal & data limits |
| Play Store as primary distribution | Conflicts with de-Google story (optional later) |
| Multi-language UI | English first; product data may still be multi-language from OFF |
| Live scraping of EWG or closed DBs | License / ethics |

## Success metrics (qualitative for v1)

- User understands good vs bad **in under 3 seconds** on result screen.
- Network traffic audit shows only OFF/OBF (+ image CDN).
- Installable and usable on a device **without Google Play Services**.
- Scoring methodology is readable by a non-developer in SCORING.md.

## Competitive positioning

```
                 Clear score UI
                      ▲
                      │
           AisleSpy   │   Yuka
                      │
   Open data ─────────┼───────── Closed / proprietary
                      │
        OFF app       │
                      │
                      ▼
              Dense / raw data UI
```

## Brand voice

- **Helpful spy:** curious, lightly playful (“Running recon…”).
- **Not a doctor:** no diagnosis language.
- **Not a scold:** severity without moralizing the user.
- **Honest about limits:** partial data → confidence badge, not fake precision.

## Platform

- Android phone (primary)
- minSdk 26, targetSdk 35
- Application ID: `app.aislespy`

## Monetization

None in MVP. No ads. Optional future: donations link only (must remain F-Droid clean).

## Related docs

- [UI_UX.md](UI_UX.md) — screens and copy
- [SCORING.md](SCORING.md) — score design
- [ROADMAP.md](ROADMAP.md) — delivery plan
- [PRIVACY.md](../PRIVACY.md) — privacy promises
