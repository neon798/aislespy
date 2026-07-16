# UI / UX specification — AisleSpy

Implement screens by **stable IDs**. Do not invent routes without updating this file.

**Design system:** Material 3, Jetpack Compose  
**Brand:** Mischievous-helpful spy; scores stay serious  
**Tagline:** What’s really in the aisle.

---

## Visual language

### Color

| Role | Guidance |
|------|----------|
| Seed / primary | Deep teal (`#0F6B6B` or Material seed close to teal) |
| Secondary / accent | Amber (`#F5A524`) for CTAs / scan reticle accents |
| Score Excellent ≥75 | Green |
| Score Ok 50–74 | Yellow/amber |
| Score Poor 25–49 | Orange |
| Score Bad ≤24 | Red |
| Surfaces | Material 3 light + dark themes |

Score colors must meet WCAG AA against their label container (use on-score surface if needed).

### Typography

- Hero score: large numeric (displayLarge / ~57sp)
- Product name: titleLarge
- Concern why: bodyMedium
- Microcopy: bodySmall, slightly muted

### Motion

- Score ring animates from 0 → value once on success (~600–800ms)
- Respect system “reduce motion”: jump to final value
- Light haptic on successful decode (optional)

### Microcopy bank (optional flair)

| Context | Copy |
|---------|------|
| Loading product | “Running recon…” |
| Concerns section title | “Suspect ingredients” |
| No concerns | “Clean dossier—nothing flagged in our pack.” |
| Scan success (snackbar optional) | “Mission complete” |
| Network error | “Lost contact—check your connection.” |
| Not found | “This barcode isn’t in the open databases yet.” |

Never let flair replace the numeric score or severity.

---

## Navigation graph

```
scan                          (start destination)
├── manual_entry
│     └── result/{barcode}?source=auto
├── result/{barcode}?source=auto|food|beauty
│     ├── ingredient_detail/{concernId}
│     └── category_chooser/{barcode}   // when both DBs hit
├── history
│     └── result/{barcode}?source=auto
└── settings
      ├── methodology
      ├── privacy          // render PRIVACY.md summary or WebView/local markdown
      └── licenses
```

### Route table

| Screen ID | Route pattern | Arguments |
|-----------|---------------|-----------|
| `scan` | `scan` | — |
| `manual_entry` | `manual` | — |
| `result` | `result/{barcode}?source={source}` | barcode: String; source: auto\|food\|beauty |
| `category_chooser` | `choose/{barcode}` | barcode |
| `ingredient_detail` | `ingredient/{concernId}` | concernId |
| `history` | `history` | — |
| `settings` | `settings` | — |
| `methodology` | `settings/methodology` | — |
| `privacy` | `settings/privacy` | — |
| `licenses` | `settings/licenses` | — |

Use a bottom bar or top actions on `scan` for History + Settings. Result is a stack push.

---

## Screen: `scan`

### Purpose
Capture barcode quickly; show recent missions.

### Layout
1. Full-bleed camera preview
2. Center viewfinder reticle (rounded rect, amber corners)
3. Top bar: app name “AisleSpy”, Settings icon
4. Bottom sheet / panel:
   - Button: “Enter barcode”
   - Horizontal chips: recent history (up to 10)
   - Link/button: “History”
5. Permission empty state when camera denied

### State: `ScanUiState`
See DOMAIN_MODELS.md.

### Actions
| Action | Result |
|--------|--------|
| Barcode decoded | Debounce 2s same code; navigate `result/{code}?source=auto` |
| Enter barcode | navigate `manual` |
| Tap recent chip | navigate `result/{code}` |
| Settings | navigate `settings` |
| History | navigate `history` |
| Grant permission | re-open camera |

### Copy
- Permission rationale: “Camera access is only used to read product barcodes. Nothing is uploaded except the barcode lookup to Open Food Facts / Open Beauty Facts.”
- Denied: “Camera permission needed to scan. You can still enter a barcode manually.”

### A11y
- Reticle is decorative; announce “Camera ready, point at a barcode”
- Recent chips: “{name}, score {n}”

---

## Screen: `manual_entry`

### Purpose
Type EAN/UPC when camera fails.

### Layout
- Text field (digits, length 8–14 typical)
- Helper text: “Enter the number under the barcode”
- Primary button: “Look up”
- Back

### Validation
- Non-empty digits only
- Enable button when length ≥ 8
- On submit: navigate `result/{barcode}?source=auto`

---

## Screen: `result`

### Purpose
One-glance score + explain concerns.

### Layout (Success)
1. Top app bar: back, product category chip (Food/Beauty)
2. `ProductHeader`: image, name, brand
3. **ScoreRing** hero + summary sentence + confidence chip
4. Expandable **breakdown** (Nutri-Score, NOVA, etc.)
5. Badge row
6. Section “Suspect ingredients” → list of `ConcernCard`
7. Footer disclaimer (one line + “Learn more” → methodology)

### States

| State | UI |
|-------|-----|
| Loading | Skeleton header + centered progress + “Running recon…” |
| Success | Full layout |
| Partial | Success layout + strong confidence warning banner |
| NotFound | Illustration, message, buttons “Add on Open Food Facts” / “Open Beauty Facts” (external links), “Scan another” |
| NetworkError | Message, Retry, “View history” |
| NeedsCategoryChoice | Navigate to or embed chooser |

### Actions
| Action | Result |
|--------|--------|
| Tap concern | `ingredient/{id}` |
| Retry | re-fetch |
| Scan another | pop to `scan` |

### A11y
- Score: “Score {n} out of 100, {band label}, confidence {level}”
- Concerns list heading announced

### Not found contribute
Open browser/custom tab to OFF/OBF contribute URLs (API_CONTRACTS.md).

---

## Screen: `category_chooser`

### Purpose
User picks Food vs Beauty when both DBs return data.

### Layout
- Title: “Which kind of product?”
- Subtitle: barcode
- Two cards: “Food — {foodName}” / “Beauty — {beautyName}”
- Cancel → pop

### Actions
Navigate `result/{barcode}?source=food` or `source=beauty`.

---

## Screen: `ingredient_detail`

### Purpose
Full explanation of one concern.

### Layout
- Title: display name
- Severity chips (1–5) with color
- Position hint if any
- Full why text
- Sources list
- Disclaimer line

### Actions
Back only (MVP).

---

## Screen: `history`

### Purpose
Local-only past scans.

### Layout
- LazyColumn of rows: thumbnail, name, score badge, date, category
- Empty: “No missions yet—scan something in the aisle.”
- App bar: Clear all (confirm dialog)

### Actions
| Action | Result |
|--------|--------|
| Tap row | `result/{barcode}` |
| Swipe delete | remove local row |
| Clear all | confirm → wipe history table |

---

## Screen: `settings`

### Rows
1. How scoring works → methodology
2. Privacy → privacy
3. Open-source licenses → licenses
4. Knowledge pack version (read-only text)
5. Methodology version (read-only)
6. App version
7. Attribution blurb (OFF/OBF)

### First launch
On first open of app (before or overlaying scan): dialog or full-screen pager:

**Title:** Welcome to AisleSpy  
**Body:** We don’t create accounts or run analytics. Scan history stays on this device. Looking up a product sends the barcode to Open Food Facts or Open Beauty Facts.  
**Buttons:** View privacy policy · Get started  

Store `onboarding_done` in DataStore.

---

## Screen: `methodology`

Plain-language summary of SCORING.md:
- 100 = better
- Food uses Nutri-Score, NOVA, additives
- Beauty uses ingredient hazards + position
- Link tone: transparent, not medical

---

## Shared empty / error patterns

| Pattern | Use |
|---------|-----|
| Inline error text | Form validation |
| Full-screen error | Result network failure |
| Snackbar | Non-fatal (e.g. history delete undo optional) |

---

## Responsive / device

- Phone portrait primary
- Camera screen: keep controls thumb-reachable
- Don’t require tablets for MVP

---

## Related

- [COMPONENTS.md](COMPONENTS.md)
- [DOMAIN_MODELS.md](DOMAIN_MODELS.md)
- [SCORING.md](SCORING.md)
