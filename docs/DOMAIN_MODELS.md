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
| brands | String? | |
| imageUrl | String? | front image |
| category | ProductCategory | |
| sourceDb | SourceDb | |
| ingredientsText | String? | free text |
| ingredientsTags | List\<String\> | e.g. `en:sugar` |
| additivesTags | List\<String\> | food; e.g. `en:e322` |
| allergensTags | List\<String\> | |
| labelsTags | List\<String\> | organic, etc. |
| categoriesTags | List\<String\> | for heuristics |
| nutriscoreGrade | Char? | `a`–`e` lowercase |
| nutriscoreScore | Int? | raw OFF score if present |
| novaGroup | Int? | 1–4 |
| nutriments | Nutriments? | optional subset |

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
| id | String | `nutriscore`, `nova`, `additives`, `hazards`, … |
| label | String | UI label |
| score | Int | 1–100 contribution subscore |
| weight | Float | weight used after reweight |
| detail | String? | e.g. “Nutri-Score D” |

### `ScoreResult`
| Field | Type |
|-------|------|
| total | Int |
| band | ScoreBand |
| confidence | Confidence |
| components | List\<ScoreComponent\> |
| concerns | List\<Concern\> |
| methodologyVersion | String |
| summarySentence | String |

`summarySentence` examples:
- “Solid pick—few flags in our pack.”
- “Mixed—watch the additives.”
- “Lots of suspect ingredients—read carefully.”

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

### `ScoreComponentUi`
| Field | Type |
|-------|------|
| id | String |
| label | String |
| score | Int |
| detail | String? |

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

Examples: `Nutri-Score C`, `NOVA 4`, `Organic`.

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
