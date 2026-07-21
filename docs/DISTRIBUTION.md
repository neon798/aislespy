# Distribution — AisleSpy

Maintainer notes for how AisleSpy is (and will be) distributed. **Do not claim the app is listed on any app store.**

**Current distribution:** public GitHub repo; production-signed APKs on [GitHub Releases](https://github.com/neon798/aislespy/releases) (`v0.1.0-beta.x`).

---

## Channels

| Channel | Status | Notes |
|---------|--------|-------|
| **(a) Direct GitHub Releases** | **Canonical** | Production-signed APK + `.sha256` checksum each release. Source of truth for binaries. |
| **(b) Obtainium** | **PRIMARY recommended user path** | Users add the repo URL in [Obtainium](https://github.com/ImranR98/Obtainium); it tracks GitHub Releases and auto-updates when a new APK is published. |
| **(c) Self-hosted F-Droid repo** | **PLANNED** | Run `fdroidserver`, host the repo on a static host (e.g. GitHub Pages). Users add the repo URL to their F-Droid client. No third-party gatekeeper. |
| **(d) F-Droid.org main** | **Possible later** | See [F-Droid.org main status](#fdroidorg-main-status). Not a near-term target. |

### Obtainium (user path)

1. Install [Obtainium](https://github.com/ImranR98/Obtainium).
2. **Add App** → source URL: `https://github.com/neon798/aislespy`
3. Obtainium finds the latest release and the `aislespy-<version>.apk` asset.
4. It notifies and updates when a new release is published.

Manual alternative: download the APK from Releases, verify SHA-256, sideload or `adb install`.

### Self-hosted F-Droid repo (planned)

When ready: maintain a private/self-hosted F-Droid repository with `fdroidserver`, publish index + APKs on a static host, document the repo URL for users who prefer the F-Droid client without waiting on F-Droid.org inclusion.

---

## F-Droid.org main status

F-Droid’s official Inclusion Policy currently has **no clause about AI-generated code** (unlike some third-party repos that explicitly reject AI-assisted apps). AisleSpy is **not explicitly barred** on that basis.

Caveats:

- The topic is **unsettled / contentious** in the F-Droid community; there was no official position either way as of 2025 discussions.
- F-Droid **builds from source**; a reviewer sees the full history.
- Prebuilt native libs (especially **zxing-cpp**) need building from source or clear justification — do not assume the GitHub Release APK is accepted as-is.

Treat F-Droid.org main as a **possible-later** track, not a near-term target. Revisit deliberately when willing to invest in a source build recipe and community review.

---

## Inclusion-readiness facts

Useful if/when pursuing self-hosted F-Droid or F-Droid.org main. Not a claim of listing.

- [x] **Free and open source license** — Apache-2.0
- [x] **All libraries FOSS; no proprietary binaries** — deps: AndroidX / Compose / CameraX / Room / DataStore, zxing-cpp, Retrofit / OkHttp, kotlinx-serialization, Coil (all Apache-2.0)
- [x] **No** Google Play Services, Firebase, Crashlytics, proprietary ads/analytics
- [x] **No** Google ML Kit (barcode: zxing-cpp)
- [x] **Accurate fastlane metadata** in `fastlane/metadata/android/en-US/` — title, short/full description, icon, 6 phoneScreenshots, per-versionCode changelogs
- [x] **Anti-Features declared** — `Network` (OFF/OBF product lookup)
- [ ] **Source built by F-Droid / reproducible builds** — not yet; release APKs ship prebuilt native `.so` files (see [Prebuilt native libraries](#prebuilt-native-libraries)). F-Droid.org main would need a build recipe (zxing-cpp native in particular). Self-hosted repos can ship signed APKs under maintainer control.

### Expected Anti-Features

| Anti-Feature | Reason |
|--------------|--------|
| `Network` | Looks up products on OFF/OBF |

Do **not** claim `Tracking` or `Ads`. OFF/OBF are free/open network services; document Network clearly. If F-Droid policy ever labels a dependency NonFree, replace it.

---

## Prebuilt native libraries

The signed release APK bundles prebuilt `.so` files from FOSS libraries, including:

- **zxing-cpp** — `libzxingcpp_android.so`
- **AndroidX** — native pieces from CameraX, DataStore, graphics, etc.

| Path | Implication |
|------|-------------|
| **GitHub Releases / Obtainium / self-hosted F-Droid** | Ship the production-signed APK with prebuilt FOSS natives. |
| **F-Droid.org main** | Builds from source. AndroidX natives are handled by F-Droid’s standard tooling. **zxing-cpp** may need to be built from source in the recipe, or covered by a maintainer note—do not assume the GitHub Release APK is accepted as-is. |

---

## Signing

Releases are signed with **AisleSpy’s production key** (RSA-4096).

- GitHub Releases (and thus Obtainium) use this signature for the APK asset.
- The throwaway beta key used for `v0.1.0-beta.1`–`beta.7` is **retired**. Current and future releases use the production key (from `v0.1.0-beta.8` / versionCode 8 onward).
- Production key backup and secrecy are the **maintainer’s responsibility**. Do not commit keystores, passwords, or fingerprints to the repo or this doc.

---

## Metadata layout

Fastlane-compatible structure under `fastlane/metadata/android/en-US/` (canonical path auto-detected by fdroidserver / F-Droid tooling):

```
fastlane/metadata/android/en-US/
  title.txt
  short_description.txt
  full_description.txt
  changelogs/
    2.txt … 8.txt          # one file per versionCode
  images/
    icon.png
    phoneScreenshots/
      1.png … 6.png
```

Changelogs are named by **versionCode** (not versionName). Do not invent stub codes that never shipped.

---

## Build notes for maintainers

- Single free flavor only (always FOSS; no play vs fdroid split)
- `minSdk` 26, `targetSdk` 35
- No secret API keys (OFF/OBF need none)
- User-Agent must not contain private tokens
- Network allowlist and attribution: [DATA_SOURCES.md](DATA_SOURCES.md), [PRIVACY.md](../PRIVACY.md)

---

## Permissions justification

- **Camera:** Scan product barcodes
- **Internet:** Fetch product data from Open Food Facts and Open Beauty Facts

---

## Related

- [PRIVACY.md](../PRIVACY.md)
- [DATA_SOURCES.md](DATA_SOURCES.md)
- Fastlane metadata: `/fastlane/metadata/android/en-US/`
