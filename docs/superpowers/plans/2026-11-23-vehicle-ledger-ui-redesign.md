# Vehicle Ledger UI Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Modernize the Vehicle Ledger Android app's visual style and user experience based on the `new_desain/` (Fuelio reference) dark slate and lavender design language, adding a floating pill navigation dock, rich monthly ledger cards, Room v5 migration for station and full-tank flags, and an in-cab Trip Cost Calculator.

**Architecture:** Incrementally update the Jetpack Compose Material 3 UI layer with new theme tokens, shared primitives (`CommonComponents.kt`), and screen composables while maintaining strict offline-first Room data persistence, Persian RTL layout, and Jalali date normalization. Migrate Room from v4 to v5 to persist station names and full-tank flags.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Room, StateFlow, JUnit4, Robolectric.

**Spec:** [docs/superpowers/specs/2026-11-23-vehicle-ledger-ui-redesign-design.md](file:///home/amir/Documents/ChatGPT/Project%20UNI%20Shiasy/docs/superpowers/specs/2026-11-23-vehicle-ledger-ui-redesign-design.md)

## Global Constraints

- Android single module `:app` with Kotlin and Jetpack Compose Material 3.
- Persian copy, Persian digits (`۰۱۲۳۴۵۶۷۸۹`), and RTL layout across all screens.
- Money stored in integer legacy rials and displayed in Toman.
- Tehran civil dates for Jalali calendar reporting periods.
- Offline-first Room source of truth; all automated tests must pass.
- Minimum touch target >= 48dp; compatible with 320dp small screens.

## Review Focus

1. **Room v4 -> v5 migration on existing populated database**: Pre-existing v4 entries with null station and 0 full-tank flag must load without crash or data loss.
2. **Fuel consumption with missing full-tank records**: Average L/100km calculation must safely return null / empty state without division-by-zero when odometer delta is <= 0 or fewer than 2 full-tank entries exist.
3. **Trip Cost Calculator with zero or invalid inputs**: Zero distance or zero rate must display 0 Toman safely without NaN or crashes.
4. **Floating Pill Dock overlaying screen content**: Bottom content in Home, History, and Reminders must have adequate bottom content padding so the floating dock does not obscure buttons or entries.
5. **320dp screen text scaling (150% font scale)**: Card layouts and floating pill tabs must wrap or scale gracefully without clipping Persian labels.

---

### Task 1: Room Database Schema Migration (v4 -> v5) & Backup Codec

**Files:**
- Modify: `app/src/main/java/org/unishiasy/vehiclelog/data/local/EntryEntity.kt`
- Modify: `app/src/main/java/org/unishiasy/vehiclelog/data/local/AppDatabase.kt`
- Modify: `app/src/main/java/org/unishiasy/vehiclelog/data/local/RoomBackupSnapshot.kt`
- Modify: `app/src/main/java/org/unishiasy/vehiclelog/data/local/BackupCodec.kt`
- Modify: `app/src/main/java/org/unishiasy/vehiclelog/data/EntryInput.kt`
- Test: `app/src/test/java/org/unishiasy/vehiclelog/data/local/Migration4To5Test.kt`
- Test: `app/src/test/java/org/unishiasy/vehiclelog/data/local/BackupCodecTest.kt`

**Interfaces:**
- Consumes: Existing `EntryEntity`, `AppDatabase`, `BackupSnapshot`
- Produces:
  - `EntryEntity(id, vehicleId, category, occurredOn, createdAtEpochMillis, notes, amountRials, fuelVolumeMl, stationName: String? = null, isFullTank: Boolean = false)`
  - `MIGRATION_4_5: Migration`

- [ ] **Step 1: Write failing migration and backup codec tests**
Write `Migration4To5Test.kt` testing SQLite migration from schema 4 to 5 with populated sample entries. Write `BackupCodecTest.kt` testing serialization/deserialization of `stationName` and `isFullTank`.

- [ ] **Step 2: Run tests to verify they fail**
Run: `./gradlew testDebugUnitTest --tests "org.unishiasy.vehiclelog.data.local.Migration4To5Test"`
Expected: FAIL (compilation error or missing schema migration).

- [ ] **Step 3: Implement migration and entity updates**
Add `station_name TEXT NULL` and `is_full_tank INTEGER NOT NULL DEFAULT 0` to `entries`. Define `MIGRATION_4_5` in `AppDatabase.kt` bumping database version to 5. Update `EntryInput`, `RoomBackupSnapshot`, and `BackupCodec`.

- [ ] **Step 4: Run tests to verify they pass**
Run: `./gradlew testDebugUnitTest --tests "org.unishiasy.vehiclelog.data.local.*"`
Expected: PASS.

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/org/unishiasy/vehiclelog/data/ app/src/test/java/org/unishiasy/vehiclelog/data/
git commit -m "feat(data): migrate Room schema to v5 with station name and full-tank flag"
```

---

### Task 2: Design System Tokens & Shared Primitives

**Files:**
- Modify: `app/src/main/java/org/unishiasy/vehiclelog/ui/theme/Color.kt`
- Modify: `app/src/main/java/org/unishiasy/vehiclelog/ui/theme/Theme.kt`
- Modify: `app/src/main/java/org/unishiasy/vehiclelog/ui/components/CommonComponents.kt`
- Test: `app/src/test/java/org/unishiasy/vehiclelog/ui/components/FloatingDockTest.kt`

**Interfaces:**
- Consumes: Material 3 Theme tokens
- Produces:
  - `FloatingBottomDock(selectedTab: MainTab, onTabSelected: (MainTab) -> Unit, modifier: Modifier = Modifier)`
  - `MetricCard(title: String, value: String, subtitle: String? = null, icon: ImageVector, modifier: Modifier = Modifier)`
  - `CircularIconBadge(icon: ImageVector, backgroundColor: Color, iconColor: Color, modifier: Modifier = Modifier)`
  - `StatusPill(text: String, containerColor: Color, contentColor: Color, modifier: Modifier = Modifier)`

- [ ] **Step 1: Write failing Robolectric/Compose tests for shared components**
Write `FloatingDockTest.kt` testing tab selection callbacks, pill dock layout, and metric card text bindings.

- [ ] **Step 2: Run tests to verify they fail**
Run: `./gradlew testDebugUnitTest --tests "org.unishiasy.vehiclelog.ui.components.FloatingDockTest"`
Expected: FAIL (`FloatingBottomDock` undefined).

- [ ] **Step 3: Implement tokens and components**
Define dark slate (`#0E0E13`, `#1A1824`, `#262335`) and lavender (`#9D85F2`, `#B8A7FF`) in `Color.kt` and `Theme.kt`. Implement `FloatingBottomDock`, `MetricCard`, `CircularIconBadge`, and `StatusPill` in `CommonComponents.kt`.

- [ ] **Step 4: Run tests to verify they pass**
Run: `./gradlew testDebugUnitTest --tests "org.unishiasy.vehiclelog.ui.components.FloatingDockTest"`
Expected: PASS.

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/org/unishiasy/vehiclelog/ui/theme/ app/src/main/java/org/unishiasy/vehiclelog/ui/components/CommonComponents.kt app/src/test/java/org/unishiasy/vehiclelog/ui/components/
git commit -m "feat(ui): add dark slate theme tokens and shared dock and card primitives"
```

---

### Task 3: Fuel Consumption & Trip Cost Calculator Domain & UI

**Files:**
- Create: `app/src/main/java/org/unishiasy/vehiclelog/domain/FuelConsumptionCalculator.kt`
- Create: `app/src/main/java/org/unishiasy/vehiclelog/ui/tools/TripCalculatorScreen.kt`
- Test: `app/src/test/java/org/unishiasy/vehiclelog/domain/FuelConsumptionCalculatorTest.kt`
- Test: `app/src/test/java/org/unishiasy/vehiclelog/ui/tools/TripCalculatorScreenTest.kt`

**Interfaces:**
- Consumes: `EntryEntity`
- Produces:
  - `FuelConsumptionCalculator.calculateAverageL100Km(entries: List<EntryEntity>): Double?`
  - `TripCalculatorState(distanceKm: String, ratePerLiterToman: String, consumptionL100Km: String, totalFuelLiters: Double, totalCostToman: Long)`
  - `TripCalculatorScreen(state: TripCalculatorState, onDistanceChange: (String) -> Unit, ...)`

- [ ] **Step 1: Write failing unit test for consumption calculator & trip calculator UI**
Test calculation of consecutive full-tank fill-ups: 40L over 500km = 8.0 L/100km. Test zero distance, invalid rate, and trip cost formula in `TripCalculatorScreenTest.kt`.

- [ ] **Step 2: Run tests to verify they fail**
Run: `./gradlew testDebugUnitTest --tests "org.unishiasy.vehiclelog.domain.FuelConsumptionCalculatorTest"`
Expected: FAIL.

- [ ] **Step 3: Implement calculation logic and Composable**
Implement `calculateAverageL100Km` in `FuelConsumptionCalculator.kt` sorting by odometer and pairing consecutive full-tank events. Build `TripCalculatorScreen.kt` with Persian digit inputs, real-time formula computation, and calculated summary card.

- [ ] **Step 4: Run tests to verify they pass**
Run: `./gradlew testDebugUnitTest --tests "org.unishiasy.vehiclelog.domain.FuelConsumptionCalculatorTest"`
Run: `./gradlew testDebugUnitTest --tests "org.unishiasy.vehiclelog.ui.tools.TripCalculatorScreenTest"`
Expected: PASS.

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/org/unishiasy/vehiclelog/domain/FuelConsumptionCalculator.kt app/src/main/java/org/unishiasy/vehiclelog/ui/tools/ app/src/test/java/org/unishiasy/vehiclelog/domain/ app/src/test/java/org/unishiasy/vehiclelog/ui/tools/
git commit -m "feat(tools): add fuel consumption domain calculator and trip calculator screen"
```

---

### Task 4: Home Screen Redesign

**Files:**
- Modify: `app/src/main/java/org/unishiasy/vehiclelog/ui/home/HomeScreen.kt`
- Test: `app/src/test/java/org/unishiasy/vehiclelog/ui/home/HomeScreenRobolectricTest.kt`

**Interfaces:**
- Consumes: `MetricCard`, `CircularIconBadge`, `StatusPill`, `FuelConsumptionCalculator`
- Produces:
  - Updated `HomeScreen` with vehicle selector pill header, fuel & price metric cards, action chips, and active reminder alert cards.

- [ ] **Step 1: Write failing tests for new Home Screen sections**
Test presence of vehicle selector header (`پژو ۲۰۷ • ۶۱٬۶۴۱ کیلومتر`), consumption metric card, and reminder pill badges in `HomeScreenRobolectricTest.kt`.

- [ ] **Step 2: Run tests to verify they fail**
Run: `./gradlew testDebugUnitTest --tests "org.unishiasy.vehiclelog.ui.home.HomeScreenRobolectricTest"`
Expected: FAIL.

- [ ] **Step 3: Update HomeScreen composable**
Implement vehicle pill card at the top, consumption & fuel price cards using `MetricCard`, quick-action chips (`آمار کلی`, `گزارش ماهانه`), and due service alerts with rounded slate styling.

- [ ] **Step 4: Run tests to verify they pass**
Run: `./gradlew testDebugUnitTest --tests "org.unishiasy.vehiclelog.ui.home.HomeScreenRobolectricTest"`
Expected: PASS.

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/org/unishiasy/vehiclelog/ui/home/HomeScreen.kt app/src/test/java/org/unishiasy/vehiclelog/ui/home/
git commit -m "feat(ui): redesign Home screen with vehicle header, consumption cards, and quick chips"
```

---

### Task 5: Mileage & Expense Log Redesign with Monthly Grouping

**Files:**
- Modify: `app/src/main/java/org/unishiasy/vehiclelog/ui/ledger/LedgerScreen.kt`
- Modify: `app/src/main/java/org/unishiasy/vehiclelog/ui/ledger/EntryDetailSheet.kt`
- Modify: `app/src/main/java/org/unishiasy/vehiclelog/ui/ledger/EntryDraft.kt`
- Test: `app/src/test/java/org/unishiasy/vehiclelog/ui/ledger/LedgerScreenRobolectricTest.kt`

**Interfaces:**
- Consumes: `EntryEntity` (with `stationName`, `isFullTank`), `CommonComponents`
- Produces:
  - Monthly grouped ledger items (e.g., `آبان ۱۴۰۵`)
  - Rich log cards with station name, unit rate calculation, and `باک پر` status pill
  - Polished entry sheet with top action bar (`انصراف` / `ذخیره`) and full tank toggle

- [ ] **Step 1: Write failing tests for monthly grouping, station name, and full-tank pill**
Test that entries are displayed under Jalali month headers, show station pin icon and name, and show `باک پر` badge when `isFullTank == true`.

- [ ] **Step 2: Run tests to verify they fail**
Run: `./gradlew testDebugUnitTest --tests "org.unishiasy.vehiclelog.ui.ledger.LedgerScreenRobolectricTest"`
Expected: FAIL.

- [ ] **Step 3: Implement log cards and entry form sheet**
Update `LedgerScreen.kt` and `EntryDetailSheet.kt` with monthly group headers, circular category badges, quantity/rate formulas (`۴۰ لیتر ← ۳٬۰۰۰ تومان/لیتر`), station line, and full-tank badge. Add full-tank toggle and station name field to `EntryDraft.kt`.

- [ ] **Step 4: Run tests to verify they pass**
Run: `./gradlew testDebugUnitTest --tests "org.unishiasy.vehiclelog.ui.ledger.LedgerScreenRobolectricTest"`
Expected: PASS.

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/org/unishiasy/vehiclelog/ui/ledger/ app/src/test/java/org/unishiasy/vehiclelog/ui/ledger/
git commit -m "feat(ui): implement monthly grouped ledger cards with station and full-tank badges"
```

---

### Task 6: Tools Hub Screen & Floating Dock Navigation Integration

**Files:**
- Create: `app/src/main/java/org/unishiasy/vehiclelog/ui/tools/ToolsHubScreen.kt`
- Modify: `app/src/main/java/org/unishiasy/vehiclelog/MainActivity.kt`
- Modify: `app/src/main/java/org/unishiasy/vehiclelog/ui/ledger/LedgerViewModel.kt`
- Test: `app/src/test/java/org/unishiasy/vehiclelog/ui/AppJourneyRobolectricTest.kt`

**Interfaces:**
- Consumes: `FloatingBottomDock`, `TripCalculatorScreen`, `MonthlyDashboardScreen`
- Produces:
  - `MainTab`: `HOME`, `HISTORY`, `REMINDERS`, `TOOLS`
  - `ToolsHubScreen` with cards for Trip Calculator, Vehicle Management, and Monthly Reports

- [ ] **Step 1: Write failing navigation test for 4-tab floating dock and Tools hub**
In `AppJourneyRobolectricTest.kt`, test navigating to the 4th tab (`TOOLS`), opening the Trip Calculator, and returning.

- [ ] **Step 2: Run tests to verify they fail**
Run: `./gradlew testDebugUnitTest --tests "org.unishiasy.vehiclelog.ui.AppJourneyRobolectricTest"`
Expected: FAIL.

- [ ] **Step 3: Implement ToolsHubScreen and integrate 4-tab dock in MainActivity**
Add `TOOLS` to `MainTab`. Implement `ToolsHubScreen.kt` with clean navigation cards. Wire `FloatingBottomDock` at the scaffold bottom with content padding so content is never obscured.

- [ ] **Step 4: Run tests to verify they pass**
Run: `./gradlew testDebugUnitTest --tests "org.unishiasy.vehiclelog.ui.AppJourneyRobolectricTest"`
Expected: PASS.

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/org/unishiasy/vehiclelog/MainActivity.kt app/src/main/java/org/unishiasy/vehiclelog/ui/ app/src/test/java/org/unishiasy/vehiclelog/ui/
git commit -m "feat(ui): integrate 4-tab floating dock and tools hub screen"
```

---

### Task 7: Full Test Suite Verification & Lint Gate

**Files:**
- Modify: Existing tests across `test/` and `androidTest/` as needed for updated strings or test tags

- [ ] **Step 1: Run complete offline unit test suite**
Run: `./gradlew --offline testDebugUnitTest`
Expected: 100% PASS with 0 failures and 0 skips.

- [ ] **Step 2: Run lint check**
Run: `./gradlew --offline lintDebug`
Expected: 0 errors.

- [ ] **Step 3: Assemble debug and test APKs**
Run: `./gradlew --offline assembleDebug assembleDebugAndroidTest`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit and finalize execution log**
```bash
git add .
git commit -m "chore: verify redesign with full unit test suite and lint passes"
```
