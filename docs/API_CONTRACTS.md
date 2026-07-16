# API contracts — Open Food Facts & Open Beauty Facts

AisleSpy does **not** host a product API. Clients call OFF/OBF directly.

Official docs: https://openfoodfacts.github.io/openfoodfacts-server/api/

---

## Base URLs

| Database | Base |
|----------|------|
| Open Food Facts | `https://world.openfoodfacts.org` |
| Open Beauty Facts | `https://world.openbeautyfacts.org` |

Use production hosts in release builds. (OFF also documents `.net` staging hosts for experiments—do not point production AisleSpy at staging.)

---

## User-Agent (required)

```
AisleSpy/<appVersion> (Android; https://github.com/<owner>/aislespy)
```

Example: `AisleSpy/0.1.0 (Android; https://github.com/example/aislespy)`

Set on every request. Update `<owner>` when the public repo URL is known (DECISIONS.md).

---

## Get product by barcode

### Food

```
GET https://world.openfoodfacts.org/api/v2/product/{barcode}
```

### Beauty

```
GET https://world.openbeautyfacts.org/api/v2/product/{barcode}
```

### Query: field filter

Always request a reduced field set:

```
fields=code,product_name,brands,image_front_url,image_front_small_url,
nutriscore_grade,nutriscore_score,nova_group,additives_tags,
ingredients_text,ingredients_tags,allergens_tags,labels_tags,
categories_tags,nutriments,ingredients_analysis_tags
```

Beauty may ignore nutriscore/nova/nutriments when absent; still safe to request.

Optional: `ingredients` (structured) if needed for order—prefer tags + text for MVP.

### Success response shape (conceptual)

```json
{
  "status": 1,
  "code": "3017624010701",
  "product": {
    "product_name": "...",
    "brands": "...",
    "image_front_url": "https://...",
    "nutriscore_grade": "e",
    "nova_group": 4,
    "additives_tags": ["en:e322", "..."],
    "ingredients_text": "...",
    "ingredients_tags": ["en:...", "..."],
    "allergens_tags": [],
    "labels_tags": [],
    "categories_tags": ["en:...", "..."],
    "nutriments": { }
  }
}
```

### Not found

- HTTP 200 with `status: 0` **or** missing product object — treat as not found for that DB.
- HTTP 404 — not found.

### Errors

| Situation | App mapping |
|-----------|-------------|
| HTTP 200 + status 1 + product | Found |
| HTTP 200 + status 0 | Not found (this DB) |
| HTTP 404 | Not found |
| HTTP 429 | NetworkError “Too many requests—try again shortly” |
| HTTP 5xx | NetworkError |
| Timeout / offline | NetworkError |
| Malformed JSON | NetworkError |

---

## Parallel lookup algorithm

```
function lookup(barcode):
  cached = cache.get(barcode)
  if cached fresh:
    return Found(cached.product)  // or NeedsCategoryChoice if stored as pair

  offJob  = async GET OFF
  obfJob  = async GET OBF
  off, obf = await both (each may be Found/NotFound/Error)

  if off is Error AND obf is Error:
    return NetworkError
  if off is Found AND obf is Found:
    if clearlyFood(off) AND NOT clearlyBeauty(obf): return Found(mapFood(off))
    if clearlyBeauty(obf) AND NOT clearlyFood(off): return Found(mapBeauty(obf))
    return NeedsCategoryChoice(mapFood(off), mapBeauty(obf))
  if off is Found: return Found(mapFood(off))
  if obf is Found: return Found(mapBeauty(obf))
  if either Error and neither Found:
    return NetworkError  // prefer error over false NotFound if one failed
  return NotFound(barcode)
```

### Heuristics: `clearlyFood` / `clearlyBeauty`

**Food signals:** nutriscore present, nova present, categories contain food-like tags (`en:plant-based-foods`, `en:snacks`, …), additives_tags non-empty with E-numbers.

**Beauty signals:** categories contain cosmetics tags (`en:skin-care`, `en:hair-care`, `en:makeup`, …), absence of nutriments with presence of cosmetic categories.

If unsure and both Found → **always** `NeedsCategoryChoice` (UI: category_chooser).

---

## Caching policy

| Item | TTL | Storage |
|------|-----|---------|
| Product payload | 7 days recommended | Room `product_cache` |
| History row | until user deletes | Room `history` |
| Knowledge pack | shipped in APK | assets |

Do not cache indefinitely without TTL—labels change.

---

## Courtesy / rate limits

- One lookup per user action (scan or submit)—no prefetch of arbitrary barcodes.
- Debounce scanner so the same code is not requested more than once per 2 seconds.
- No bulk export or scraping from the mobile client.
- Prefer `fields=` to reduce bandwidth.

---

## Contribute URLs (not found UX)

| DB | Example contribute / product URL |
|----|----------------------------------|
| Food | `https://world.openfoodfacts.org/cgi/product.pl?type=search&code={barcode}` or product page pattern |
| Beauty | `https://world.openbeautyfacts.org/cgi/product.pl?type=search&code={barcode}` |

Use official URL patterns current at implementation time; verify once in browser.

---

## Images

- Prefer `image_front_small_url` for lists; `image_front_url` for detail.
- Load via Coil; HTTPS only when possible.
- Failure → placeholder illustration.

---

## Test barcodes

| Barcode | Expectation |
|---------|-------------|
| `3017624010701` | Nutella — OFF hit, low/mid food score territory |
| Random invalid `0000000000000` | Not found both |
| Known cosmetic EAN in OBF | Beauty path (discover during fixtures phase) |

Save real JSON under `fixtures/` when implementing tests (respect ODbL attribution in README of fixtures).

---

## Related

- [DATA_SOURCES.md](DATA_SOURCES.md)
- [ARCHITECTURE.md](ARCHITECTURE.md)
- [DOMAIN_MODELS.md](DOMAIN_MODELS.md)
