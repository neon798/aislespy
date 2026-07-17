# Scoring methodology — AisleSpy

**methodologyVersion:** `1.0.2`  
**Convention:** **100 = best** (healthier / fewer concerns), **1 = worst**.

This document is the source of truth. Code must implement these rules; formula changes require a version bump and an entry in [DECISIONS.md](DECISIONS.md).

---

## Disclaimer (required in UI + store)

> AisleSpy scores are **informational only**. They are not medical advice, an allergen guarantee, or a safety certification. Always read the physical label. Product data comes from community databases and may be incomplete or outdated.

---

## Color bands

| Total score | Band | Color token |
|-------------|------|-------------|
| 75–100 | Excellent | Green |
| 50–74 | Ok | Yellow |
| 25–49 | Poor | Orange |
| 1–24 | Bad | Red |

---

## Severity scale (ingredients / additives)

| Severity | Meaning | Typical deduction (food additives component) |
|----------|---------|-----------------------------------------------|
| 1 | Mild note / sensitive individuals | −2 |
| 2 | Minor concern / limited evidence debate | −4 |
| 3 | Moderate concern / notable caveats | −7 |
| 4 | Strong concern / restricted in some regions | −12 |
| 5 | Highest concern in our pack (ban/restrict / strong evidence flags) | −18 |

Deductions are applied inside component subscores, then weighted into the total.

---

## Food score (`FoodScoreEngine`)

### Components and base weights

| Component ID | Weight | Description |
|--------------|--------|-------------|
| `nutriscore` | 0.45 | Nutritional quality |
| `nova` | 0.25 | Ultra-processing |
| `additives` | 0.25 | Flagged additives from knowledge pack |
| `positives` | 0.05 | Small bonuses (capped) |

### Missing-data reweight

If a component’s inputs are missing, **remove its weight** and renormalize remaining weights to sum to 1.0.

Examples:
- No Nutri-Score, has NOVA + additives → weights on nova/additives/positives only.
- No additives matched and no additives tags at all → still compute additives subscore as 100 only if ingredients were analyzed; if no ingredient/additive data, drop `additives` component and lower confidence.

### Nutri-Score subscore (`nutriscore`) → 1–100

| Grade | Subscore |
|-------|----------|
| A | 95 |
| B | 80 |
| C | 60 |
| D | 40 |
| E | 20 |
| missing | component omitted |

If grade missing but `nutriscoreScore` (OFF numeric) present: map roughly  
`subscore = clamp(100 - (score + 15) * 3, 1, 100)` as a **best-effort** fallback (document in code comments; prefer grade). Tune only with DECISIONS entry.

### NOVA subscore (`nova`) → 1–100

| NOVA group | Subscore |
|------------|----------|
| 1 | 100 |
| 2 | 80 |
| 3 | 50 |
| 4 | 20 |
| missing | component omitted |

### Additives subscore (`additives`) → 1–100

1. Start at **100**.
2. Match `additives_tags` and ingredient text against food knowledge pack.
3. For each unique matched entry, deduct by severity table (once per entry id).
4. Soft floor: `max(subscore, 5)` before weight (avoid total collapse from many mild flags alone—severity 5 can still drag overall score down via weight).
5. Emit a `Concern` per match (severity ≥ 1).

### Positives subscore (`positives`) → 1–100

Start at **50** (neutral), then:

| Signal | Adjustment |
|--------|------------|
| Organic label tag (e.g. `en:organic`) | +20 |
| Fair-trade label | +10 |
| High fiber (≥ 6 g/100g if nutriments present) | +10 |
| Otherwise | 0 |

Clamp to 1–100.  
**Cap influence:** This component is only 5% weight so junk food cannot “bonus” into Excellent from labels alone.

### Total

```
total = round(sum(subscore_i * weight_i_normalized))
total = clamp(total, 1, 100)
```

### Food confidence

| Condition | Confidence |
|-----------|------------|
| Nutri-Score + NOVA present | High |
| Exactly one of Nutri-Score / NOVA present, or strong additive list | Medium |
| Mostly missing nutrition processing signals | Low |

---

## Beauty score (`BeautyScoreEngine`)

### Components and base weights

| Component ID | Weight | Description |
|--------------|--------|-------------|
| `hazards` | 0.70 | Matched INCI hazards × position weight |
| `allergens_fragrance` | 0.15 | Fragrance + listed allergens |
| `regulatory` | 0.15 | Pack entries flagged restricted/banned |

### Position weight

Ingredient lists are ordered approximately by concentration (INCI convention).

For match at 0-based index `i` in parsed list of length `n` (`n >= 1`):

```
positionWeight = 1.0 - 0.6 * (i / max(n - 1, 1))
```

So first ingredient ≈ 1.0, last ≈ 0.4.

If only free text and order unknown: `positionWeight = 0.7` for all matches; set confidence ≤ Medium.

### Hazards subscore (`hazards`)

1. Start at **100**.
2. For each matched beauty knowledge entry with severity ≥ 1:
   - `deduction = baseDeduction(severity) * positionWeight`
   - baseDeduction: same table as food (2/4/7/12/18)
3. Sum deductions; `subscore = clamp(100 - sum, 1, 100)`.
4. Unknown ingredients: **no penalty** (no fear-padding). Optionally count `unratedCount` for UI transparency later.

### Allergens / fragrance subscore

1. Start at **100**.
2. If fragrance/parfum matched (`fragrance`, `parfum`, `aroma` tags/names): −25.
3. Each EU-listed allergen match from product allergens tags: −5 (cap total allergen deduction at −40).
4. Clamp 1–100.

### Regulatory subscore

1. Start at **100**.
2. Each knowledge entry with category including `restricted` or `banned`: −20 (severity 5) or −12 (severity 4).
3. Clamp 1–100.

### Beauty confidence

| Condition | Confidence |
|-----------|------------|
| Structured ingredient list with ≥ 3 items | High |
| Free-text ingredients only | Medium |
| Almost no ingredient data | Low — omit hazards or mark total Low |

If no ingredient data: do not invent a mid score; prefer `NotFound`-style partial UI or Low confidence with total based only on available signals, or show “Not enough data to score” (`ResultUiState` partial/empty score path). **Preferred MVP behavior:** if beauty product found but no ingredients → `Partial` with message “Found product, but no ingredients to score” and hide numeric score or show “—”.

---

## Summary sentences (food)

Band × concern-count matrix. **Zero concerns must not imply flagged ingredients exist.**

| Score range | 0 concerns | ≥ 1 concern |
|-------------|------------|-------------|
| ≥ 75 | “Looking good—nothing flagged in our pack.” | “Looking good—only minor flags below.” |
| 50–74 | “Middling score—mostly nutrition and processing, not flagged ingredients.” | “Mixed bag—check the notes below.” |
| 25–49 | “Low score—driven by nutrition or processing; see the breakdown.” | “Several concerns—read carefully.” |
| ≤ 24 | “Very low score—nutrition and processing look rough.” | “Lots of flags—you may want to skip.” |

### Summary sentences (beauty)

Same principle (0 matches → do not imply flags). Beauty tone retained:

| Score range | 0 concerns | ≥ 1 concern |
|-------------|------------|-------------|
| ≥ 75 | “Formula looks gentle—nothing flagged in our pack.” | “Formula looks gentle—only minor flags below.” |
| 50–74 | “Middling score—mostly formula signals, not flagged ingredients.” | “Mixed bag—check the notes below.” |
| 25–49 | “Low score—driven by hazards or other formula signals; see the breakdown.” | “Several suspect ingredients—read carefully.” |
| ≤ 24 | “Very low score—formula signals look rough.” | “Lots of flags—you may want to skip.” |

### Score drivers and omitted components (explanation only)

- **`driverSentence`:** optional one-liner of the largest weighted drags: for each component, loss = `(100 - subscore) * normalizedWeight`. List contributors with loss **> 5** points, highest first (e.g. “Main drags: nutrition (Nutri-Score D), ultra-processing (NOVA 4).”). Null when none exceed the threshold. Does not change the total.
- **`omittedComponents`:** human labels of components dropped for missing data (e.g. “NOVA (no data)”). UI shows them as muted breakdown rows (“NOVA — no data (score reweighted)”). Weights of remaining components are already renormalized per missing-data reweight above.

---

## Category resolution (pre-score)

See API_CONTRACTS for lookup. Scoring uses:

- Food product → `FoodScoreEngine`
- Beauty product → `BeautyScoreEngine`
- User choice when both databases return a hit

---

## What we deliberately do not score (v1)

- Environmental Green-Score as primary (optional badge only later)
- Personalized allergen profile beyond listing product allergens
- Dose / daily intake calculations
- Brand reputation
- Dietary flags (vegan / vegetarian / dairy-free) — shown as informational badges only; never factored into totals or components

---

## Versioning

| Version | Date | Notes |
|---------|------|-------|
| 1.0.0 | 2026-07-16 | Initial methodology |
| 1.0.1 | 2026-07-16 | Dietary flags shown, not scored — copy/version bump only, no formula change |
| 1.0.2 | 2026-07-16 | Explanation copy only, no formula change (ADR-015: concern-aware summaries, driverSentence, omittedComponents) |

Any weight or mapping change → bump semver (patch for copy, minor for weight tweaks, major for reinterpretation of scale).

---

## Related

- [KNOWLEDGE_PACK.md](KNOWLEDGE_PACK.md)
- [DOMAIN_MODELS.md](DOMAIN_MODELS.md)
- [VERIFICATION.md](VERIFICATION.md)
