# Knowledge packs

Curated ingredient and additive metadata for AisleSpy scoring explanations.

## Layout

| Path | Purpose |
|------|---------|
| `schema/` | JSON Schema for entries |
| `examples/` | Tiny sample packs (not production-complete) |
| *(future)* `app/src/main/assets/knowledge/` | Runtime packs loaded by the app |

## Rules

1. Follow [docs/KNOWLEDGE_PACK.md](../docs/KNOWLEDGE_PACK.md).
2. Only open, citable sources ([docs/DATA_SOURCES.md](../docs/DATA_SOURCES.md)).
3. **Do not** scrape EWG or other proprietary databases.
4. Every entry needs balanced `why` text—no fear-mongering.
5. Bump pack `version` when shipping meaningful changes.

## Production packs (not in Phase 0)

Phase 3–4 will add:

- `food_additives_v1.json` (~50–100 entries)
- `beauty_ingredients_v1.json` (high-concern INCI + fragrance)

Start from `examples/*_sample.json` and expand.
