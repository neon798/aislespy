# Roadmap — AisleSpy

Tasks use IDs `T-xxx`. Agents should implement one task (or a small dependent set) at a time and meet acceptance criteria.

---

## Phase 0 — Documentation (current)

### T-000 Project documentation scaffold
- **Phase:** 0
- **Depends on:** —
- **Files:** AGENTS.md, README.md, PRIVACY.md, LICENSE, CONTRIBUTING.md, docs/*, metadata/*, knowledge/*
- **Acceptance criteria:**
  - [x] Full docs tree exists
  - [x] AGENTS.md has status + read order + non-negotiables
  - [x] No application Kotlin/Gradle source
- **Verify:** Directory listing matches PRODUCT plan Phase 0 tree

---

## Phase 1 — Android bootstrap

### T-100 Create Gradle Compose project
- **Phase:** 1
- **Depends on:** T-000
- **Files (expected):** `settings.gradle.kts`, `app/build.gradle.kts`, `MainActivity.kt`, theme files, `AndroidManifest.xml`
- **Acceptance criteria:**
  - [ ] Application ID `app.aislespy`
  - [ ] minSdk 26, targetSdk 35
  - [ ] Empty Compose “AisleSpy” screen runs on emulator
  - [ ] No Play Services dependencies
- **Verify:** `./gradlew assembleDebug`

### T-110 Navigation shell
- **Phase:** 1
- **Depends on:** T-100
- **Files:** `ui/navigation/NavGraph.kt`, stub screens for all UI_UX routes
- **Acceptance criteria:**
  - [ ] All routes in UI_UX.md exist as stubs
  - [ ] Bottom/top nav: Scan, History, Settings reachable
- **Verify:** Manual navigation smoke test

### T-120 Manual DI container
- **Phase:** 1
- **Depends on:** T-100
- **Files:** `di/AppContainer.kt`
- **Acceptance criteria:**
  - [ ] Container constructed from Application/Activity
  - [ ] Placeholders for APIs/DB documented
- **Verify:** App launches

### T-130 CI workflow
- **Phase:** 1
- **Depends on:** T-100
- **Files:** `.github/workflows/android.yml`
- **Acceptance criteria:**
  - [ ] On push: `assembleDebug` (+ unit tests when present)
- **Verify:** Workflow file valid YAML

---

## Phase 2 — Scan + dual lookup

### T-200 CameraX + zxing-cpp scanner
- **Phase:** 2
- **Depends on:** T-110
- **Files:** `ui/scan/*`, camera permission flow
- **Acceptance criteria:**
  - [ ] Continuous scan; debounce 2s
  - [ ] Works without Play Services
  - [ ] Manual entry path works
- **Verify:** VERIFICATION § Scanner

### T-210 OFF/OBF API clients
- **Phase:** 2
- **Depends on:** T-120
- **Files:** `data/remote/*`
- **Acceptance criteria:**
  - [ ] Correct User-Agent + fields filter
  - [ ] Maps to domain Product
  - [ ] MockWebServer tests for found/not found
- **Verify:** VERIFICATION § API

### T-220 ProductRepository + CategoryResolver
- **Phase:** 2
- **Depends on:** T-210
- **Files:** `ProductRepository.kt`, `CategoryResolver.kt`
- **Acceptance criteria:**
  - [ ] Parallel lookup per API_CONTRACTS
  - [ ] NeedsCategoryChoice when both hit ambiguously
- **Verify:** Unit tests for resolution matrix

### T-230 Result screen (raw product, no custom score)
- **Phase:** 2
- **Depends on:** T-200, T-220
- **Files:** `ui/result/*`
- **Acceptance criteria:**
  - [ ] Shows name, brand, image, ingredients text
  - [ ] Loading / not found / network error states
- **Verify:** Manual lookup Nutella barcode

---

## Phase 3 — Food scoring

### T-300 Food knowledge pack seed
- **Phase:** 3
- **Depends on:** T-000
- **Files:** `assets/knowledge/food_additives_v1.json` (≥ 50 entries)
- **Acceptance criteria:**
  - [ ] Schema-valid; sources on every entry
- **Verify:** JSON schema validation

### T-310 KnowledgePackLoader + match
- **Phase:** 3
- **Depends on:** T-300, T-100
- **Files:** `data/knowledge/*`
- **Acceptance criteria:**
  - [ ] Matches aliases/tags per KNOWLEDGE_PACK.md
  - [ ] Unit tests with sample product tags
- **Verify:** VERIFICATION § Knowledge

### T-320 FoodScoreEngine
- **Phase:** 3
- **Depends on:** T-310
- **Files:** `domain/scoring/FoodScoreEngine.kt`
- **Acceptance criteria:**
  - [ ] Implements SCORING.md v1.0.0 food section
  - [ ] Golden tests (high / low / partial data)
- **Verify:** VERIFICATION § Food scoring

### T-330 Result UI score + concerns (food)
- **Phase:** 3
- **Depends on:** T-320, T-230
- **Files:** `ScoreRing`, result wiring, ingredient detail
- **Acceptance criteria:**
  - [ ] 1–100 ring, concerns list, breakdown, confidence
- **Verify:** Manual + a11y score announcement

---

## Phase 4 — Beauty scoring

### T-400 Beauty knowledge pack seed
- **Phase:** 4
- **Depends on:** T-000
- **Files:** `assets/knowledge/beauty_ingredients_v1.json`
- **Acceptance criteria:**
  - [ ] Schema-valid; includes fragrance + high-concern INCI
- **Verify:** Schema validation

### T-410 BeautyScoreEngine
- **Phase:** 4
- **Depends on:** T-400, T-310
- **Files:** `domain/scoring/BeautyScoreEngine.kt`
- **Acceptance criteria:**
  - [ ] Position weighting per SCORING.md
  - [ ] Golden tests
- **Verify:** VERIFICATION § Beauty scoring

### T-420 Category chooser UI
- **Phase:** 4
- **Depends on:** T-220, T-110
- **Files:** `ui` category_chooser
- **Acceptance criteria:**
  - [ ] Shown only on dual ambiguous hit
  - [ ] Routes with source=food|beauty
- **Verify:** Instrumented or manual with fixture override

---

## Phase 5 — History & polish

### T-500 Room history + cache
- **Phase:** 5
- **Depends on:** T-220
- **Files:** `data/local/*`
- **Acceptance criteria:**
  - [ ] History CRUD; product cache TTL
  - [ ] Offline history readable
- **Verify:** VERIFICATION § Offline

### T-510 Onboarding + settings + methodology
- **Phase:** 5
- **Depends on:** T-110
- **Files:** settings screens, DataStore flag
- **Acceptance criteria:**
  - [ ] First-launch privacy copy matches PRIVACY.md
  - [ ] Versions displayed
- **Verify:** Fresh install flow

### T-520 Visual polish + a11y pass
- **Phase:** 5
- **Depends on:** T-330, T-410
- **Acceptance criteria:**
  - [ ] Dark theme; reduce motion; TalkBack score
  - [ ] Microcopy from UI_UX applied sparingly
- **Verify:** VERIFICATION § A11y

---

## Phase 6 — Ship

### T-600 Release signing + GitHub Release workflow
- **Phase:** 6
- **Depends on:** T-520
- **Acceptance criteria:**
  - [ ] Tagged release produces APK artifact
  - [ ] Secrets not in git
- **Verify:** CI release dry-run

### T-610 F-Droid metadata + screenshots
- **Phase:** 6
- **Depends on:** T-520
- **Files:** `metadata/en-US/*`, images
- **Acceptance criteria:**
  - [ ] Descriptions match behavior
  - [ ] FDROID.md checklist complete
- **Verify:** Metadata lint if available

### T-620 Inclusion request
- **Phase:** 6
- **Depends on:** T-610, T-600
- **Acceptance criteria:**
  - [ ] Public source URL live
  - [ ] Inclusion issue/MR opened
- **Verify:** Human

---

## MVP exit criteria

All of T-100…T-520 essential paths done; T-600 optional for “usable APK”; T-610–620 for store.

Scan food or beauty → score 1–100 → concerns with why → local history → no Play Services.
