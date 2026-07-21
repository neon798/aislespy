# F-Droid distribution — AisleSpy

Maintainer notes for IzzyOnDroid and F-Droid.org. App is **not** claimed to be listed on either store yet; this doc tracks readiness and the planned submission path.

**Current distribution:** public GitHub repo; signed APKs on [GitHub Releases](https://github.com/n3wth/aislespy/releases) (`v0.1.0-beta.x`, production signing key).

---

## Inclusion requirements (checklist)

- [x] **Free and open source license** — Apache-2.0
- [x] **All libraries FOSS; no proprietary binaries** — deps: AndroidX / Compose / CameraX / Room / DataStore, zxing-cpp, Retrofit / OkHttp, kotlinx-serialization, Coil (all Apache-2.0)
- [x] **No** Google Play Services, Firebase, Crashlytics, proprietary ads/analytics
- [x] **No** Google ML Kit (barcode: zxing-cpp)
- [x] **Accurate fastlane metadata** in `fastlane/metadata/android/en-US/` — title, short/full description, icon, 6 phoneScreenshots, per-versionCode changelogs
- [x] **Anti-Features declared** — `Network` (OFF/OBF product lookup)
- [ ] **Source built by F-Droid / reproducible builds** — not yet; release APKs ship prebuilt native `.so` files (see [Prebuilt native libraries](#prebuilt-native-libraries)). IzzyOnDroid does not require a source build; F-Droid.org main does and will need a build recipe (zxing-cpp native in particular).

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

| Store path | Implication |
|------------|-------------|
| **IzzyOnDroid** | Scans the published APK; no from-source build. Prebuilt FOSS natives are fine. |
| **F-Droid.org main** | Builds from source. AndroidX natives are handled by F-Droid’s standard tooling. **zxing-cpp** may need to be built from source in the recipe, or covered by a maintainer note—do not assume the GitHub Release APK is accepted as-is. |

---

## Signing

Releases are signed with **AisleSpy’s production key** (RSA-4096).

- **IzzyOnDroid** pulls the APK from GitHub Releases and locks to this signature for updates.
- The throwaway beta key used for `v0.1.0-beta.1`–`beta.7` is **retired**. Current and future releases use the production key (from `v0.1.0-beta.8` / versionCode 8 onward).
- Production key backup and secrecy are the **maintainer’s responsibility**. Do not commit keystores, passwords, or fingerprints to the repo or this doc.

---

## Metadata layout

Fastlane-compatible structure under `fastlane/metadata/android/en-US/` (canonical path auto-detected by fdroidserver / IzzyOnDroid / F-Droid):

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

## Submission path

Two tracks. Prefer **(A)** first for a fast public listing; **(B)** when ready to invest in a source build recipe.

### (A) IzzyOnDroid — first

Fast path: IzzyOnDroid pulls the **signed APK** from GitHub Releases (no F-Droid source build).

**Ready now:**

- Public repo
- GitHub Release with signed APK (production key)
- Fastlane metadata in `fastlane/metadata/android/en-US/`

**Action:** open a [Request-For-Packaging](https://gitlab.com/IzzyOnDroid/repo/-/issues) issue on `gitlab.com/IzzyOnDroid/repo` with repo URL, latest release tag, and Anti-Feature note (`Network`).

### (B) F-Droid.org main — second

Official main repo: F-Droid **builds from source** via a metadata / build recipe MR to [fdroiddata](https://gitlab.com/fdroid/fdroiddata).

**Extra work vs (A):**

- Submit `app.aislespy` metadata (Gradle build, tags, anti-features)
- Address **zxing-cpp** native library (build from source or maintainer guidance)
- Expect review on permissions and Network anti-feature

### Permissions justification (store text)

- **Camera:** Scan product barcodes  
- **Internet:** Fetch product data from Open Food Facts and Open Beauty Facts  

---

## Related

- [PRIVACY.md](../PRIVACY.md)
- [DATA_SOURCES.md](DATA_SOURCES.md)
- Fastlane metadata: `/fastlane/metadata/android/en-US/`
