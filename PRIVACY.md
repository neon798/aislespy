# Privacy Policy — AisleSpy

**Last updated:** 2026-07-20

AisleSpy is designed so that **your scans stay on your device** and **we do not run a user-tracking backend**.

The app is currently distributed as a signed **beta** APK via [GitHub Releases](https://github.com/neon798/aislespy/releases) (`v0.1.0-beta.x`). F-Droid submission is planned; AisleSpy is not on F-Droid yet.

---

## Short version

- No accounts.
- No analytics or advertising SDKs in the default build.
- Scan history and product cache are stored only on your phone.
- Looking up a product sends the **barcode** (and a normal HTTPS request) to Open Food Facts and/or Open Beauty Facts so the app can fetch public product data.
- We do not sell your data. We do not have a cloud profile of you.
- There is no first-party AisleSpy backend.

---

## Data the app processes

### On your device only

| Data | Purpose | Leaves device? |
|------|---------|----------------|
| Scan history (barcode, name, score, time, optional thumbnail URL) | Show recent scans | No |
| Cached product JSON (short-lived cache) | Faster repeat lookups, less API load | No |
| Settings / preferences | App behavior (e.g. onboarding) | No |

You can clear history from the app. Uninstalling the app removes local storage.

### Network requests (when you look up a product)

When you scan or enter a barcode, AisleSpy may request:

| Destination | Data sent | Purpose |
|-------------|-----------|---------|
| `https://world.openfoodfacts.org` | Barcode, User-Agent, standard HTTP metadata (IP visible to server) | Fetch food product data |
| `https://world.openbeautyfacts.org` | Same | Fetch beauty/cosmetics product data |
| Image CDNs used by OFF/OBF (e.g. `images.openfoodfacts.org` and documented OBF image hosts) | Image URL fetch | Show product photos |

**User-Agent** format:

```
AisleSpy/<version> (Android; https://github.com/neon798/aislespy)
```

We do **not** attach your name, email, advertising ID, or scan history to these requests beyond what is required for a normal HTTPS product lookup (the barcode you asked about). Requests use HTTPS.

Open Food Facts / Open Beauty Facts are third-party non-profit projects. Their own privacy practices apply to traffic they receive. See their websites for details.

### Permissions

| Permission | Why |
|------------|-----|
| Camera | Scan barcodes only |
| Internet | Fetch product data and images |

No location, contacts, microphone, or SMS permissions. The camera is not used for anything other than barcode scanning (no photo upload, no cloud vision API).

---

## What we do not do

- No user accounts or login
- No cloud sync of history
- No Google Play Services / Firebase Analytics / Crashlytics in the default build
- No ads or advertising identifiers
- No selling or brokering personal data
- No bulk uploading of your scan history
- No first-party product or analytics backend

---

## Children

AisleSpy is a general-purpose label helper. It is not directed at children under 13. Do not submit personal information about children through the app (the app does not provide a channel for that).

---

## Medical / safety disclaimer

AisleSpy scores and ingredient notes are **informational only**. They are **not** medical advice, allergen guarantees, or safety certifications. Always read the physical label and consult professionals for health decisions.

---

## Changes

If network destinations, analytics, or data practices change, we will update this file and the in-app privacy summary before shipping that change. Material changes should also appear in the release changelog.

---

## Contact

Questions or privacy concerns: open an issue on the public repository at [https://github.com/neon798/aislespy](https://github.com/neon798/aislespy).

---

## Open source

AisleSpy source is open under the Apache License 2.0. You can audit what the app sends by reading the code and by using a local network inspector (e.g. PCAPdroid) as described in [docs/VERIFICATION.md](docs/VERIFICATION.md).
