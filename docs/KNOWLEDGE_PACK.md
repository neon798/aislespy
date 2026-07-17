# Knowledge packs — AisleSpy

Curated ingredient/additive metadata shipped with the app. Used to attach **severity**, **why**, and **sources** to products—and to drive part of the score (see SCORING.md).

---

## Locations

| Phase | Path |
|-------|------|
| Now (docs) | `knowledge/schema/`, `knowledge/examples/` |
| Runtime (Phase 3+) | `app/src/main/assets/knowledge/food_additives_v1.json` |
| Runtime (Phase 4+) | `app/src/main/assets/knowledge/beauty_ingredients_v1.json` |
| Runtime (ADR-019) | `app/src/main/assets/knowledge/brand_ownership_v1.json` |

Pack files are JSON arrays or `{ "version": "1.0.0", "entries": [ ... ] }`. Prefer wrapped form:

```json
{
  "version": "1.0.0",
  "domain": "food",
  "entries": [ ]
}
```

---

## Entry schema (common)

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| id | string | yes | Stable slug, e.g. `e211`, `butylparaben` |
| names | string[] | yes | Display / match names (lowercase-friendly) |
| aliases | string[] | no | OFF-style tags e.g. `en:e211` |
| domain | `food` \| `beauty` | yes | |
| severity | int 1–5 | yes | See SCORING.md |
| categories | string[] | no | `preservative`, `fragrance`, `restricted`, `banned`, … |
| title | string | yes | UI title |
| why | string | yes | Plain language, 1–3 sentences |
| sources | string[] | yes | Short citations |

JSON Schema files: [knowledge/schema/](../knowledge/schema/).

---

## Brand ownership pack (no severity)

Separate from food/beauty ingredient packs. **Does not participate in scoring** (ADR-019). Schema: [brand_ownership.schema.json](../knowledge/schema/brand_ownership.schema.json).

Wrapped form:

```json
{
  "version": "1.0.0",
  "domain": "brand_ownership",
  "entries": [ ]
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| id | string | yes | Unique slug |
| ownership | `conglomerate` \| `independent` | yes | Entry kind |
| brandAliases | string[] | yes | Exact match vs normalized OFF/OBF `brands_tags` |
| parent | string | conglomerate | Parent company slug |
| parentDisplay | string | conglomerate | UI name e.g. `Nestlé` |
| display | string | independent | Brand display name |
| note | string | no | Short note e.g. `Family-owned` |
| sources | string[] | yes | ≥1 citation (portfolio / report / encyclopedic — no EWG) |

**No severity field.** Conservative resolution: badge only on positive pack match; unmatched brands show nothing; dual-list conflict → no badge.

---

## Matching algorithm

Input: `additives_tags`, `ingredients_tags`, `allergens_tags`, `ingredients_text`, optional ordered ingredient names.

```
normalize(s) = lowercase, trim, collapse spaces, strip punctuation except hyphens
tag_normalize(t) = lowercase as-is (keep en: prefix)

For each pack entry:
  if any alias in tags (exact): match
  else if any normalize(name) equals normalize(token) for tokens from text/tags: match
  else if any name is contained as whole word in ingredients_text: match (careful with short names)

Prefer longer name matches when overlapping.
Each entry id matches at most once per product.
Record listIndex when ordered list available.
```

Do **not** fuzzy-match aggressively (false positives are worse than misses).

---

## Seed strategy

### Food v1 (~50–100 entries)

Prioritize commonly flagged additives with public concern discussions:

- Certain colors (e.g. some azo dyes)
- Nitrites/nitrates in meat products
- Selected sweeteners (note controversy level carefully—severity 1–3)
- BHA/BHT
- Sodium nitrite
- Carrageenan (moderate—document uncertainty)
- MSG (low severity note—avoid fear hype)
- Sulfites

Every entry needs a balanced `why` (function + concern + context).

### Beauty v1

- Fragrance / parfum
- Methylisothiazolinone / methylchloroisothiazolinone
- Certain parabens (butyl-, propyl-)
- BHA/BHT
- Formaldehyde releasers (e.g. DMDM hydantoin) if cited
- Specific UV filters under ED evaluation (only with regulatory source)
- Coal tar dyes where restricted

Use CosIng / EU lists for `restricted` / `banned` categories.

---

## Versioning

- Pack `version` string independent of methodologyVersion but both shown in Settings.
- Breaking id renames require migration notes in changelog.

---

## Editing guidelines

See [CONTRIBUTING.md](../CONTRIBUTING.md) and [DATA_SOURCES.md](DATA_SOURCES.md).

---

## Related

- [SCORING.md](SCORING.md)
- Examples: [knowledge/examples/](../knowledge/examples/)
