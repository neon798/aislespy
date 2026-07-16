# Shared Compose components — AisleSpy

Reusable UI building blocks. Implement under `ui/components/`. Keep presentational (no repository calls).

---

## `ScoreRing`

**Purpose:** Hero 1–100 score.

| Prop | Type | Notes |
|------|------|--------|
| value | Int | 1–100 |
| band | ScoreBand | colors the arc |
| label | String | e.g. “Poor” |
| animated | Boolean | default true |
| modifier | Modifier | |

**Behavior:** Circular track + progress arc; center shows value; optional label below. TalkBack: “Score {value} out of 100, {label}”.

---

## `ProductHeader`

| Prop | Type |
|------|------|
| name | String |
| brand | String? |
| imageUrl | String? |
| category | ProductCategory |
| barcode | String? |

Image with Coil; placeholder if null/error. Category as small AssistChip.

---

## `ConcernCard`

| Prop | Type |
|------|------|
| concern | ConcernUi |
| onClick | () → Unit |

Row/card: severity chip, name, one-line why, optional position hint, chevron. Entire card clickable.

**Severity chip colors:** 1–2 neutral/amber, 3 orange, 4–5 red.

---

## `ConfidenceBadge`

| Prop | Type |
|------|------|
| confidence | Confidence |

Maps High/Medium/Low to chip colors and labels:
- High → “High confidence”
- Medium → “Partial data”
- Low → “Low confidence”

---

## `ScoreBreakdownList`

| Prop | Type |
|------|------|
| components | List\<ScoreComponentUi\> |

Expandable section; each row: label, optional detail, mini score or linear indicator.

---

## `BadgeRow`

| Prop | Type |
|------|------|
| badges | List\<BadgeUi\> |

Horizontal scroll of chips (Nutri-Score, NOVA, Organic…).

---

## `HistoryRow`

| Prop | Type |
|------|------|
| item | HistoryItemUi |
| onClick | () → Unit |

List item for history and scan recents.

---

## `CameraPermissionPane`

| Prop | Type |
|------|------|
| onRequest | () → Unit |
| onManualEntry | () → Unit |

Shown when permission not granted.

---

## `LoadingRecon`

Centered ProgressIndicator + text “Running recon…”

---

## `DisclaimerFooter`

One-line informational disclaimer + TextButton “How we score” → methodology.

---

## `EmptyMissions`

Illustration/text for empty history.

---

## Theme tokens

Expose in `ui/theme/Color.kt`:

```text
scoreExcellent
scoreOk
scorePoor
scoreBad
brandTeal
brandAmber
```

Use Material 3 `ColorScheme` for surfaces; score colors are semantic extras.

---

## Related

- [UI_UX.md](UI_UX.md)
- [DOMAIN_MODELS.md](DOMAIN_MODELS.md)
