# Verification — AisleSpy

How to prove changes are correct. Link tasks in ROADMAP to sections below.

---

## Automated (when code exists)

### Commands
```bash
./gradlew assembleDebug
./gradlew test
./gradlew connectedDebugAndroidTest   # optional
```

### Unit tests required

| Area | Cases |
|------|--------|
| FoodScoreEngine | Clean NOVA1 high; NOVA4 + sev4 additives low; missing NOVA reweight; no ingredient data → partial; fiber ignored; nutriscore ignored |
| BeautyScoreEngine | Hazard at index 0 vs end; fragrance penalty; no ingredients → partial/no score |
| KnowledgePack.match | alias hit; name hit; no false positive on short token |
| CategoryResolver | food only; beauty only; both; neither; one error |
| DTO mappers | sample OFF JSON → Product |

### Fixtures
Store under `fixtures/` (see fixtures/README.md). Prefer real anonymized API responses with ODbL attribution.

---

## Golden barcodes (manual / integration)

| Barcode | DB | Expect |
|---------|-----|--------|
| `3017624010701` | OFF | Product loads; food score mid/low; NOVA/Nutri visible if present |
| (add OBF cosmetic EAN during Phase 4) | OBF | Beauty path |
| `0000000000000` | — | Not found empty state |
| Device offline | — | Network error on new lookup; history still opens |

Refresh expectations if OFF data changes; don’t assert exact score integers in brittle UI tests—assert bands or component presence where possible. Unit tests may freeze fixture JSON and exact scores.

---

## § Scanner

- [ ] Decode EAN-13 under normal indoor light
- [ ] Same code not spammed >1 request / 2s
- [ ] Manual entry path works with Play Services disabled / missing
- [ ] Permission deny → rationale + manual entry

## § API

- [ ] User-Agent present on requests (proxy or logging interceptor in debug)
- [ ] `fields=` used
- [ ] Found / not found / 500 mapped correctly

## § Knowledge

- [ ] Sample food tags match expected entry ids
- [ ] Invalid JSON fails at startup or load with clear error (dev)

## § Food scoring

- [ ] Engine matches SCORING.md tables within ±1 rounding on fixtures
- [ ] Concerns sorted by severity desc

## § Beauty scoring

- [ ] Position weight changes deduction
- [ ] Unknown ingredients do not auto-penalize

## § Offline

- [ ] Airplane mode: history readable
- [ ] Cached product within TTL still scores offline
- [ ] Expired cache + offline → network error

## § Privacy

- [ ] PCAPdroid / mitmproxy: only OFF/OBF (+ image hosts)
- [ ] No advertising IDs or analytics hosts
- [ ] PRIVACY.md matches observed behavior

## § A11y

- [ ] Score content description: “Score N out of 100, …”
- [ ] All icon buttons have descriptions
- [ ] Large font doesn’t break result layout critically

## § F-Droid readiness

- [ ] `assembleRelease` FOSS deps only
- [ ] `./gradlew :app:dependencies` reviewed for proprietary artifacts
- [ ] Metadata descriptions accurate

---

## Definition of done (release candidate)

1. MVP user journey works on emulator and one physical device  
2. Unit tests green  
3. Privacy network audit clean  
4. Works on AOSP/emulator **without** Google Play Services  
5. Disclaimer visible on result screen  
