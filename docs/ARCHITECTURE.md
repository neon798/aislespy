# Architecture — AisleSpy

## Status

Specification for Phase 1+ implementation. No application code exists yet.

## Goals

- Clear separation: UI / domain / data
- Testable scoring without Android framework
- FOSS-only dependencies
- Easy F-Droid review (prefer simple DI)
- Client-only: no first-party backend

## High-level diagram

```
┌──────────────────────────────────────────────────────────┐
│  Presentation (Jetpack Compose)                          │
│  ui/scan  ui/result  ui/history  ui/ingredient  ui/settings│
│  ViewModels expose StateFlow<UiState>                      │
└───────────────────────────┬──────────────────────────────┘
                            │
┌───────────────────────────▼──────────────────────────────┐
│  Domain                                                  │
│  model/  scoring/FoodScoreEngine  scoring/BeautyScoreEngine│
│  scoring/ScoreEngine (interface)  lookup/CategoryResolver  │
└───────────────────────────┬──────────────────────────────┘
                            │
        ┌───────────────────┼────────────────────┐
        ▼                   ▼                    ▼
  data/remote/        data/knowledge/      data/local/
  OffApi, ObfApi      KnowledgePackLoader  Room: history,
  ProductRepository   match()              product cache
```

## Package layout (future)

```
app.aislespy/
├── MainActivity.kt
├── AisleSpyApp.kt                 # optional Application
├── di/
│   └── AppContainer.kt            # manual DI composition root
├── ui/
│   ├── theme/
│   │   ├── Color.kt
│   │   ├── Theme.kt
│   │   └── Type.kt
│   ├── navigation/
│   │   └── NavGraph.kt
│   ├── components/                # see COMPONENTS.md
│   ├── scan/
│   ├── result/
│   ├── history/
│   ├── ingredient/
│   └── settings/
├── domain/
│   ├── model/
│   │   ├── Product.kt
│   │   ├── ScoreResult.kt
│   │   ├── Concern.kt
│   │   └── …
│   └── scoring/
│       ├── ScoreEngine.kt
│       ├── FoodScoreEngine.kt
│       ├── BeautyScoreEngine.kt
│       └── CategoryResolver.kt
└── data/
    ├── remote/
    │   ├── OffApi.kt
    │   ├── ObfApi.kt
    │   ├── dto/
    │   └── ProductRepository.kt
    ├── local/
    │   ├── AisleSpyDatabase.kt
    │   ├── HistoryDao.kt
    │   ├── ProductCacheDao.kt
    │   └── entity/
    └── knowledge/
        ├── KnowledgePack.kt
        └── KnowledgePackLoader.kt
```

Application ID / namespace: **`app.aislespy`**

## Layer rules

| Layer | May depend on | Must not depend on |
|-------|---------------|--------------------|
| `ui` | domain, Android, Compose | raw OFF DTOs, Room entities |
| `domain` | Kotlin stdlib only (prefer) | Android, OkHttp, Room |
| `data` | domain models, Android as needed | Compose UI |

Map DTOs → domain in `data`. Map domain → UI models in ViewModel or a thin mapper in `ui`.

## Key flows

### Scan → score

```
1. Barcode detected (zxing-cpp) or manual entry
2. Navigate to result/{barcode}
3. ProductRepository.lookup(barcode)
   a. Check Room product cache (if fresh)
   b. Else parallel GET OFF + OBF
   c. CategoryResolver.resolve(off?, obf?)
   d. If both ambiguous → emit NeedsCategoryChoice
   e. Else map to domain Product
4. KnowledgePack.match(product)
5. FoodScoreEngine or BeautyScoreEngine.score(product, matches)
6. Persist HistoryEntry
7. Emit ResultUiState.Success
```

### Offline

- No network: if cache hit, show cached product + score; else `networkError`
- History always readable offline

## Networking

- HTTP client: Ktor (OkHttp engine) **or** Retrofit + OkHttp (pick one in Phase 1; record in DECISIONS.md)
- Mandatory custom User-Agent (see API_CONTRACTS.md)
- Timeouts: connect ~10s, request ~20s
- No certificate pinning required for MVP (optional later)

## Local storage

| Store | Contents |
|-------|----------|
| Room `history` | Recent scans for UI |
| Room `product_cache` | barcode → JSON/fields + fetchedAt (TTL e.g. 7 days) |
| DataStore | first-launch flag, theme preference |

## Dependency injection

```kotlin
// Conceptual — implement in Phase 1
class AppContainer(context: Context) {
  val httpClient: HttpClient
  val offApi: OffApi
  val obfApi: ObfApi
  val db: AisleSpyDatabase
  val knowledgePack: KnowledgePack
  val repository: ProductRepository
  val foodEngine: FoodScoreEngine
  val beautyEngine: BeautyScoreEngine
}
```

Pass container via `ViewModelFactory` / default factory. Avoid global singletons except Application-held container.

## Threading

- Main: Compose / UI state
- Default/IO: network, Room, knowledge pack load
- Scoring: pure CPU on Default dispatcher; keep engines side-effect free

## Configuration constants

Centralize in e.g. `domain/Config.kt` or `data/remote/ApiConfig.kt`:

- OFF base URL
- OBF base URL
- User-Agent template
- Cache TTL
- `methodologyVersion` (must match SCORING.md)

## Security & privacy architecture

- No account tokens
- No third-party analytics SDK
- Cleartext traffic disabled (`usesCleartextTraffic=false`)
- Camera permission only for scan flows
- Logcat: never log full personal data; barcode logging OK at debug only

## Testing seams

| Unit | How |
|------|-----|
| Score engines | Pure JVM unit tests + fixtures |
| CategoryResolver | Unit tests |
| KnowledgePack.match | Unit tests with sample JSON |
| ProductRepository | MockWebServer |
| ViewModels | Turbine / coroutines tests |
| UI | Optional Compose UI tests later |

## Related docs

- [DOMAIN_MODELS.md](DOMAIN_MODELS.md)
- [API_CONTRACTS.md](API_CONTRACTS.md)
- [SCORING.md](SCORING.md)
- [UI_UX.md](UI_UX.md)
