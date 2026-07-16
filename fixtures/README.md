# API fixtures

Place saved Open Food Facts / Open Beauty Facts JSON responses here for unit and repository tests **once implementation starts**.

## Suggested files (Phase 2+)

```
fixtures/
  off_nutella_3017624010701.json
  off_not_found.json
  obf_sample_cosmetic.json
```

## Rules

- Prefer real API responses with a reduced `fields=` set.
- Product data is © Open Food Facts / Open Beauty Facts contributors (ODbL).
- Do not commit personal data.
- Note barcode and fetch date in a short header comment is not possible in pure JSON—document in test names instead.

## Fetch example

```bash
curl -A "AisleSpy/0.0-dev (fixture-capture; https://github.com/example/aislespy)" \
  "https://world.openfoodfacts.org/api/v2/product/3017624010701?fields=code,product_name,brands,image_front_url,nutriscore_grade,nutriscore_score,nova_group,additives_tags,ingredients_text,ingredients_tags,allergens_tags,labels_tags,categories_tags,nutriments" \
  -o fixtures/off_nutella_3017624010701.json
```
