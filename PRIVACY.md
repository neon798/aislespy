# Privacy Policy — AisleSpy

**Last updated:** 2026-07-16  
**Status:** Draft for pre-release documentation. Update version date when the first public APK ships.

AisleSpy is designed so that **your scans stay on your device** and **we do not run a user-tracking backend**.

---

## Short version

- No accounts.
- No analytics or advertising SDKs in the default build.
- Scan history is stored only on your phone.
- Looking up a product sends the **barcode** (and a normal HTTP request) to Open Food Facts and/or Open Beauty Facts so the app can fetch public product data.
- We do not sell your data. We do not have a cloud profile of you.

---

## Data the app processes

### On your device only

| Data | Purpose | Leaves device? |
|------|---------|----------------|
| Scan history (barcode, name, score, time, optional thumbnail URL) | Show recent scans | No |
| Cached product JSON (optional TTL cache) | Faster repeat lookups, less API load | No |
| Settings / preferences | App behavior | No |

You can clear history from the app (once implemented). Uninstalling the app removes local storage.

### Network requests (when you look up a product)

When you scan or enter a barcode, AisleSpy may request:

| Destination | Data sent | Purpose |
|-------------|-----------|---------|
| `https://world.openfoodfacts.org` | Barcode, User-Agent, standard HTTP metadata (IP visible to server) | Fetch food product data |
| `https://world.openbeautyfacts.org` | Same | Fetch beauty/cosmetics product data |
| Image CDNs used by OFF/OBF | Image URL fetch | Show product photos |

**User-Agent** format (planned):  
`AisleSpy/<version> (Android; https://github.com/<owner>/aislespy)`

We do **not** attach your name, email, advertising ID, or scan history to these requests beyond what is required for a normal HTTPS product lookup (the barcode you asked about).

Open Food Facts / Open Beauty Facts are third-party non-profit projects. Their own privacy practices apply to traffic they receive. See their websites for details.

### Permissions (planned)

| Permission | Why |
|------------|-----|
| Camera | Scan barcodes |
| Internet | Fetch product data and images |

No location, contacts, microphone, or SMS permissions for MVP.

---

## What we do not do (MVP)

- No user accounts or login
- No cloud sync of history
- No Google Play Services / Firebase Analytics / Crashlytics
- No ads or ad identifiers
- No selling or brokering personal data
- No bulk uploading of your scan history

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

Project contact will be listed on the public GitHub repository once published. For pre-release local development, use the repository maintainer’s preferred channel.

---

## Open source

AisleSpy source will be open. You can audit what the app sends by reading the code and by using a local network inspector (e.g. PCAPdroid) as described in [docs/VERIFICATION.md](docs/VERIFICATION.md).
