# Domain models — AisleSpy

Canonical types for implementation. Names are Kotlin-oriented; adapt as needed but keep fields and meaning stable so UI and tests stay aligned.

---

## Enums

### `ProductCategory`
```text
Food
Beauty
```

### `SourceDb`
```text
OpenFoodFacts
OpenBeautyFacts
```

### `Confidence`
```text
High     // core inputs present (food: Nutri-Score + NOVA or strong ingredient data; beauty: ingredient list)
Medium   // some core inputs missing; reweighted
Low      // sparse data; score is rough
```

### `ScoreBand`
```text
Excellent  // >= 75  (green)
Ok         // 50–74  (yellow)
Poor       // 25–49  (orange)
Bad        // <= 24  (red)
```

Map from total score; used for color and TalkBack labels.

### `LookupOutcome` (repository level)
```text
Found(product: Product)
NeedsCategoryChoice(food: Product, beauty: Product)
NotFound(barcode: String)
NetworkError(message: String, barcode: String)
```

---

## Domain entities

### `Product`
| Field | Type | Notes |
|-------|------|--------|
| barcode | String | EAN/UPC digits |
| name | String | display name; fallback “Unknown product” |
| brands | String? | free-text brand string from OFF/OBF |
| brandsTags | List\<String\> | OFF/OBF `brands_tags` (e.g. `en:alpro`); used for ownership matching |
| imageUrl | String? | front image |
| category | ProductCategory | |
| sourceDb | SourceDb | |
| ingredientsText | String? | free text |
| ingredientsTags | List\<String\> | e.g. `en:sugar` |
| additivesTags | List\<String\> | food; e.g. `en:e322` |
| allergensTags | List\<String\> | |
| labelsTags | List\<String\> | organic, etc. |
| categoriesTags | List\<String\> | for heuristics |
| ingredientsAnalysisTags | List\<String\> | OFF analysis e.g. `en:vegan`, `en:non-vegan` |
| nutriscoreGrade | Char? | `a`–`e` lowercase |
| nutriscoreScore | Int? | raw OFF score if present |
| novaGroup | Int? | 1–4 |
| nutriments | Nutriments? | optional subset |

### Dietary flags (informational only — not scored)

Resolved on-device from OFF tags; never factored into `ScoreResult`.

### `DietaryStatus` (tri-state)
```text
Yes
No
Unknown
```

### `DietaryFlags`
| Field | Type | Resolution sketch |
|-------|------|-------------------|
| vegan | DietaryStatus | `en:vegan` (analysis or labels) → Yes; `en:non-vegan` → No; maybe/absent → Unknown |
| vegetarian | DietaryStatus | `en:vegetarian` (analysis or labels) → Yes; `en:non-vegetarian` → No; Yes vegan implies Yes vegetarian when vegetarian Unknown |
| dairyFree | DietaryStatus | allergen `en:milk` → No; labels `en:no-lactose` / `en:lactose-free` / `en:dairy-free` / `en:milk-free` → Yes; vegan Yes → Yes; else Unknown |

**UI (food only):** Yes → badge “Vegan” / “Vegetarian” / “Dairy-free”; No → “Not vegan” / “Not vegetarian” / “Contains dairy”; Unknown → no badge. Beauty: skip dietary badges.

### Values badges (informational only — not scored)

Certification labels from OFF/OBF `labels_tags` only. Never factored into `ScoreResult`. Organic scoring remains solely the food positives component (SCORING.md); the badge adds nothing.

### `ValuesBadge`
| Field | Type | Notes |
|-------|------|--------|
| id | String | Stable machine id |
| label | String | UI copy |

| id | label | Matching tags (lowercase; exact unless noted) |
|----|-------|-----------------------------------------------|
| fair-trade | Fair-trade | `en:fair-trade`, `en:fairtrade-international`, `en:max-havelaar`, `en:fairtrade` |
| organic-certified | Certified organic | `en:organic`, `en:eu-organic`, `en:usda-organic`, `en:ab-agriculture-biologique`, prefix `en:organic-` |
| cruelty-free | Cruelty-free | `en:cruelty-free`, `en:leaping-bunny`, `en:not-tested-on-animals`, `en:cruelty-free-international` |
| rainforest-alliance | Rainforest Alliance | `en:rainforest-alliance` |
| utz | UTZ certified | `en:utz-certified` |
| b-corp | B Corp | `en:b-corp`, `en:certified-b-corporation` |

**Order:** stable as listed above. **One badge per id.** Conservative matching only (no loose substring that could false-positive, e.g. `en:inorganic` ≠ organic).

**UI (food and beauty):** badge with `style = "values"` (gold/amber outline + leading ★). Replaces the plain T-330 “Organic” chip.

### Brand ownership (informational only — not scored)

Curated ownership pack + OFF/OBF `brands_tags`. **Never** factored into `ScoreResult` (PRODUCT.md excludes brand reputation from scoring; ADR-019). Separate schema from food/beauty severity packs—**no severity**.

### `BrandOwnership` (sealed; null = no badge)
```text
Corporate(parentDisplay, sources)   // sourced conglomerate match only
Independent(display, note?, sources) // verified-independent allowlist only
// null → no ownership badge (unlisted brand, ambiguity, or pack conflict)
```

### `BrandOwnershipEntry` / pack
| Field | Type | Notes |
|-------|------|--------|
| id | String | Unique slug across the pack |
| ownership | `conglomerate` \| `independent` | Entry kind |
| brandAliases | List\<String\> | Lowercase; exact match vs normalized `brands_tags` |
| parent / parentDisplay | String? | Conglomerate only (e.g. parentDisplay `"Danone"`) |
| display / note | String? | Independent only |
| sources | List\<String\> | ≥1 required; no EWG |

**Matching (conservative):** normalize tags/aliases (lowercase, trim); **exact token** match only—no loose substring. Most-specific (longest alias) wins. Brand on **both** lists → **null** (fail safe). Never infer independence from absence; never guess a parent.

**UI (food and beauty):**
- Corporate → neutral chip `Owned by <parentDisplay>` (`style = "ownership"`); a11y “Owned by Nestlé, ownership information”
- Independent → values-style gold-star chip `Independent` (`style = "values"`); a11y “Independent brand”
- null → no badge

### `Nutriments` (optional subset)
| Field | Type |
|-------|------|
| energyKcal100g | Double? |
| sugars100g | Double? |
| salt100g | Double? |
| saturatedFat100g | Double? |
| fiber100g | Double? |
| proteins100g | Double? |

### `MatchedIngredient`
Result of knowledge-pack matching before scoring.

| Field | Type |
|-------|------|
| entryId | String |
| displayName | String |
| severity | Int |
| why | String |
| sources | List\<String\> |
| matchedOn | String |
| listIndex | Int? |

### `Concern`
User-facing problem ingredient (after scoring pipeline).

| Field | Type | Notes |
|-------|------|--------|
| id | String | knowledge pack id |
| displayName | String | |
| severity | Int | 1–5 |
| shortWhy | String | one or two sentences |
| sources | List\<String\> | |
| positionHint | String? | e.g. “Near top of ingredient list” |
| matchedOn | String | debug/transparency |

### `ScoreComponent`
| Field | Type | Notes |
|-------|------|--------|
| id | String | food: `additives`, `nova`, `positives`; beauty: `hazards`, … |
| label | String | UI label |
| score | Int | 1–100 contribution subscore |
| weight | Float | weight used after reweight |
| detail | String? | e.g. “NOVA 4”, “2 flagged additives” |

### `ScoreResult`
| Field | Type | Notes |
|-------|------|--------|
| total | Int | |
| band | ScoreBand | |
| confidence | Confidence | |
| components | List\<ScoreComponent\> | |
| concerns | List\<Concern\> | |
| methodologyVersion | String | |
| summarySentence | String | Band × concern-count matrix (SCORING.md); never implies flags when concerns empty |
| driverSentence | String? | Optional “Main drags: …” for largest weighted losses; null if none > 5 pts |
| omittedComponents | List\<String\> | Human labels of components dropped for missing data, e.g. `NOVA (no data)` |

`summarySentence` examples (food, methodology 2.0.0):
- “Looking good—nothing flagged in our pack.” (high score, 0 concerns)
- “Middling score—mostly processing signals, not flagged ingredients.”
- “Low score—driven by heavy processing; see the breakdown.”
- “Several concerns—read carefully.”

`driverSentence` example:
- “Main drags: ultra-processing (NOVA 4), flagged ingredients (2 flagged additives).”
### `HistoryEntry`
| Field | Type |
|-------|------|
| barcode | String |
| name | String |
| score | Int |
| category | ProductCategory |
| scannedAtEpochMs | Long |
| thumbnailUrl | String? |

---

## UI models

Keep UI models immutable. ViewModels map domain → UI.

### `HistoryItemUi`
| Field | Type |
|-------|------|
| barcode | String |
| name | String |
| score | Int |
| band | ScoreBand |
| category | ProductCategory |
| scannedAtLabel | String |
| thumbnailUrl | String? |

### `ProductHeaderUi`
| Field | Type |
|-------|------|
| name | String |
| brand | String? |
| imageUrl | String? |
| category | ProductCategory |
| barcode | String |

### `ScoreUi`
| Field | Type |
|-------|------|
| value | Int |
| band | ScoreBand |
| label | String |
| confidence | Confidence |
| confidenceLabel | String |
| summarySentence | String |
| driverSentence | String? |

### `ScoreComponentUi`
| Field | Type | Notes |
|-------|------|--------|
| id | String | |
| label | String | |
| score | Int | |
| detail | String? | |
| weight | Float | Normalized weight after reweight; UI shows e.g. “45% of score” |

### `ConcernUi`
| Field | Type |
|-------|------|
| id | String |
| name | String |
| severity | Int |
| shortWhy | String |
| positionHint | String? |

### `BadgeUi`
| Field | Type |
|-------|------|
| id | String |
| label | String |
| style | String |

Examples: `NOVA 4`, values badges (`Certified organic`, `Fair-trade`, … with `style = "values"`).

**Note (ADR-018 / methodology 2.0.0):** Nutri-Score is **not** a primary result badge. The Nutri-Score grade lives on the nutrition screen via [NutritionUi], not on the badges row. NOVA remains a primary badge when present.

### `NutritionUi`
Display-only nutrition payload for the `nutrition/{barcode}` screen. **Never** fed into score engines (ADR-018).

| Field | Type | Notes |
|-------|------|--------|
| nutriScoreGrade | Char? | `a`–`e` when present |
| energyKcal100g | Double? | per 100 g |
| sugars100g | Double? | |
| salt100g | Double? | |
| saturatedFat100g | Double? | |
| fiber100g | Double? | |
| proteins100g | Double? | |
| hasData | Boolean | true if grade or any nutriment field is present |

Held in an activity-scoped `NutritionStore` (same pattern as `ConcernDetailStore`) after a food success load.

---

## Screen state models

### `ScanUiState`
```text
permission: CameraPermission  // Denied | Rationale | Granted
cameraActive: Boolean
lastError: String?
recent: List<HistoryItemUi>   // max 10
```

### `ResultUiState` (sealed)
```text
Loading(barcode)
Success(
  product: ProductHeaderUi,
  score: ScoreUi,
  breakdown: List<ScoreComponentUi>,
  omittedComponents: List<String>,  // e.g. "NOVA (no data)" → muted reweight rows
  concerns: List<ConcernUi>,
  badges: List<BadgeUi>,
  disclaimerVisible: Boolean = true
)
Partial(...)                   // same as Success but confidence Low/Medium emphasized
NotFound(barcode, contributeFoodUrl, contributeBeautyUrl)
NetworkError(barcode, message)
NeedsCategoryChoice(barcode, foodName, beautyName)
```

### `HistoryUiState`
```text
items: List<HistoryItemUi>
empty: Boolean
```

### `IngredientDetailUiState`
```text
id, name, severity, fullWhy, sources, positionHint?
```

### `SettingsUiState`
```text
appVersion: String
methodologyVersion: String
knowledgePackVersion: String
privacySummary: String
```

---

## Mapping notes

1. OFF/OBF DTOs never reach Compose directly.
2. Sort `concerns` by `severity` descending, then name.
3. Clamp `total` to `1..100`.
4. Empty product name → `"Unknown product"`.
5. Image URL may be HTTP from older data—prefer HTTPS upgrade if scheme is http.

---

## Related

- [SCORING.md](SCORING.md)
- [UI_UX.md](UI_UX.md)
- [KNOWLEDGE_PACK.md](KNOWLEDGE_PACK.md)
