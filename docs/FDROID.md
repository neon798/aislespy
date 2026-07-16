# F-Droid distribution — AisleSpy

Primary store target: **F-Droid**. Secondary: GitHub Releases (direct APK).

---

## Inclusion requirements (checklist)

- [ ] Free and open source license (Apache-2.0 ✓)
- [ ] All libraries FOSS; no proprietary binaries
- [ ] **No** Google Play Services, Firebase, Crashlytics, proprietary ads/analytics
- [ ] **No** Google ML Kit (use zxing-cpp)
- [ ] Source built by F-Droid or reproducible builds
- [ ] Accurate metadata in `metadata/`
- [ ] Anti-Features declared if applicable

### Expected Anti-Features

| Anti-Feature | Reason |
|--------------|--------|
| `Network` | Looks up products on OFF/OBF |

Do **not** trigger `Tracking`, `Ads`, `NonFreeNet` unnecessarily—OFF/OBF are free/open network services; still document Network usage clearly. If F-Droid policy labels any dependency NonFree, replace it.

---

## Metadata layout

Fastlane-compatible structure (already stubbed):

```
metadata/en-US/
  title.txt
  short_description.txt
  full_description.txt
  changelogs/<versionCode>.txt
  images/  (add later: icon, phoneScreenshots)
```

---

## Build notes for maintainers

When Android project exists:

- Flavor: single free flavor only (no “play” vs “fdroid” split needed if always FOSS)
- `minSdk` 26, `targetSdk` current stable
- No secret API keys (OFF/OBF need none)
- User-Agent must not contain private tokens

---

## Submission path

1. Publish source on public Git forge
2. Tag release; attach APK on GitHub Releases for early users
3. Open F-Droid inclusion request (or submit metadata MR per current F-Droid docs)
4. Respond to review on anti-features and permissions (Camera, Internet)

---

## Permissions justification (store text)

- **Camera:** Scan product barcodes  
- **Internet:** Fetch product data from Open Food Facts and Open Beauty Facts  

---

## Related

- [PRIVACY.md](../PRIVACY.md)
- [DATA_SOURCES.md](DATA_SOURCES.md)
- metadata stubs under `/metadata/en-US/`
