# Data sources — AisleSpy

## Product databases

| Source | Role | License |
|--------|------|---------|
| [Open Food Facts](https://world.openfoodfacts.org/) | Food product facts by barcode | [ODbL](https://opendatacommons.org/licenses/odbl/) |
| [Open Beauty Facts](https://world.openbeautyfacts.org/) | Cosmetics / personal care by barcode | ODbL (same family as OFF) |

### Attribution (required)

In Settings → About and README:

> Product data © Open Food Facts / Open Beauty Facts contributors, available under the Open Database License. https://world.openfoodfacts.org https://world.openbeautyfacts.org

Individual product photos may have their own contributor credits on OFF/OBF; linking to the product page is sufficient for MVP.

### Allowed use

- On-demand barcode lookup for end users
- Local caching for performance and reduced API load
- Display of names, brands, images, ingredients, nutrition-related fields

### Not allowed / discouraged

- Redistributing a full database dump as if it were AisleSpy proprietary data without ODbL share-alike compliance
- Bulk scraping via the mobile app
- Removing attribution

---

## Knowledge packs (ingredient risk explanations)

AisleSpy-authored JSON packs (Apache-2.0 with the app) summarize **public** scientific/regulatory information for UX.

### Allowed source types for pack content

| Source type | Examples | Notes |
|-------------|----------|-------|
| Regulatory inventories | EU CosIng, EU additive lists | Prefer primary docs |
| Endocrine disruptor lists | [edlists.org](https://edlists.org/) EU lists | Cite list name |
| Authority opinions | EFSA summaries (public) | Plain-language paraphrase |
| OFF taxonomies | Additive tags, categories | Open |
| Peer-reviewed consensus | High-level, widely reported | No sensational single-blog claims |

### Forbidden for pack ingestion

| Source | Why |
|--------|-----|
| EWG Skin Deep / Food Scores bulk data | Proprietary / TOS risk—do not scrape or copy scores |
| Closed commercial “toxicity APIs” | Licensing + F-Droid complexity |
| Random social media lists without citations | Quality |

You may independently research the same chemicals that appear on proprietary sites **from open sources**, and write original short `why` text.

---

## Scoring methodology IP

The composite 1–100 formula is AisleSpy’s (documented in SCORING.md).  
Nutri-Score and NOVA are **existing public systems** displayed/used as inputs; credit them in methodology UI:

- Nutri-Score: public nutrient profiling system (Santé Publique France / scientific literature)
- NOVA: food processing classification (Monteiro et al. / public nutrition literature)

---

## Network allowlist (privacy)

| Host pattern | Purpose |
|--------------|---------|
| `world.openfoodfacts.org` | Food API |
| `world.openbeautyfacts.org` | Beauty API |
| `*.openfoodfacts.org` images / static as used by API image URLs | Photos |
| `*.openbeautyfacts.org` images | Photos |

Any new host requires PRIVACY.md + this file update **before** release.

---

## Third-party libraries

All runtime libraries must be FOSS (Apache-2.0, MIT, BSD, GPL-compatible as appropriate). Track in app’s open-source licenses screen (Phase 5).

Barcode: **zxing-cpp** (or maintained FOSS wrapper)—not Google ML Kit.

---

## Related

- [PRIVACY.md](../PRIVACY.md)
- [KNOWLEDGE_PACK.md](KNOWLEDGE_PACK.md)
- [API_CONTRACTS.md](API_CONTRACTS.md)
