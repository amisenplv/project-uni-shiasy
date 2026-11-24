# Vehicle Ledger UI Redesign & Modernization Design Document

**Date:** 2026-11-23  
**Status:** Complete — Ready for Approval  
**Scope:** Complete visual, interaction, and data modernization of `:app` based on `new_desain/` (Fuelio reference)

---

## 1. Intent and Why
Professional drivers need a vehicle ledger that is fast, legible at a glance, and pleasant to use in varied in-cab lighting conditions. The current UI is functionally complete and compliant with offline and Jalali conventions, but its visual presentation is utilitarian.

This project modernizes the entire interface to match the refined dark slate and lavender visual language captured in `new_desain/`. It introduces clear visual hierarchy, floating pill navigation, grouped monthly ledger cards with rich metadata (station, unit price, full-tank indicator), vehicle summary cards, structured database storage for stations and full-tank flags, and quick driver utilities such as a Trip Cost Calculator.

---

## 2. Goals, Non-Goals, and Anti-Goals

### Goals
1. **Design System & Theme**:
   - Establish a refined dark palette: Deep Slate background (`#0E0E13`), Surface Card container (`#1A1824`), Elevated Surface (`#262335`), and Lavender/Lilac primary accent (`#9D85F2`).
   - High-contrast Persian typography with consistent Persian digits (`۰۱۲۳۴۵۶۷۸۹`) and RTL layout.
   - Reusable UI primitives: Floating pill dock, rounded metric cards, circular icon badges, and status pills.
   - Retain theme settings flexibility while setting dark mode as the signature default.
2. **Navigation Shell**:
   - Floating rounded bottom pill navigation dock with items:
     - **Home (خانه)**: Vehicle glance, consumption metrics, active reminders.
     - **Ledger Log (دفترچه سوابق)**: Grouped fill-up and maintenance timeline.
     - **Reminders (یادآورها)**: Due service reminders and interval tracker.
     - **Tools (ابزارها)**: Hub containing Trip Calculator, Vehicle Profile, and Reports.
3. **Home Screen**:
   - Vehicle selector pill header displaying active vehicle name and current odometer reading.
   - Key metrics card: Average fuel consumption (L/100km) and latest fuel price in Toman.
   - Quick action chips and urgent service reminders alert.
4. **Mileage & Expense Log**:
   - Entries grouped by Jalali month (e.g., آبان ۱۴۰۵).
   - Rich log cards displaying category icon, Jalali date, odometer, total in Toman, quantity/rate breakdown, station name, and `باک پر` (Full Tank) badge.
5. **Entry Forms & Sheets**:
   - Polished sheet screen with top action bar (`انصراف` / `ذخیره`).
   - Grouped input cards for Date & Odometer (integrated with Jalali wheel picker), Fuel breakdown (liters, rate, total with auto-calculation), Station name, and Full Tank toggle.
6. **Data Storage & Migration (Option B)**:
   - Room migration from version 4 to 5, adding `stationName` (TEXT nullable) and `isFullTank` (INTEGER boolean NOT NULL default 0) to `entries`.
   - Update backup serializers (`BackupSnapshot`, `BackupCodec`) and migration verification tests.
7. **Driver Tools**:
   - **Trip Cost Calculator (محاسبه‌گر هزینه سفر)**: Instant calculation of trip fuel needed and cost in Toman given distance and current consumption/price.

### Non-Goals
- Adding external GPS maps, station lookups, or third-party web services (the app is strictly offline-first).
- Implementing multi-user accounts, cloud subscription paywalls, or commercial tracking features seen in the reference app.

### Anti-Goals
- Cluttering the screen with low-contrast text or touch targets smaller than 48dp.
- Inconsistent numeral systems (mixing Latin and Persian digits within the same screen).
- Breaking existing offline local data safety or offline test guarantees.

---

## 3. Constraints
- **Android & Compose**: Single `:app` module, Jetpack Compose Material 3, screen-level ViewModels with `StateFlow`, manual dependency wiring.
- **RTL & Persian**: Pure Persian copy, Persian digits normalized across input and display, Tehran civil dates for Jalali calendar calculations.
- **Money**: Handled in integer legacy rials internally, formatted in Toman with proper thousands separators (`۱۲۰٬۰۰۰ تومان`).
- **Tests**: All automated tests (JUnit, Robolectric, Room migration tests, and instrumentation) must pass.

---

## 4. Approaches Considered

### Approach 1: In-Place Incremental Redesign with Shared Design Tokens (Selected)
- **Description**: Update theme color tokens, build shared Compose primitives in `CommonComponents.kt`, execute Room v4->v5 migration for station and full-tank flags, and incrementally refactor `HomeScreen`, `LedgerScreen`, `RemindersScreen`, and `MonthlyDashboardScreen` while introducing the Calculator tool.
- **Pros**: Cleanest diffs, keeps all test suites continuously verifiable, fully supports the desired structured station and consumption data.
- **Cons**: Requires Room schema export and migration tests.

### Approach 2: Full Screen Rewrite in Parallel (`ui/v2`) (Rejected)
- **Description**: Build completely parallel screens and switch over at the end.
- **Why rejected**: Creates duplicate code, breaks CI/test feedback loops, high risk of regressions.

### Approach 3: Surface-Only Color Reskin (Rejected)
- **Description**: Only change color values without restructuring cards, navigation, or adding the calculator.
- **Why rejected**: Fails to deliver the agreed structural improvements from `new_desain`.

---

## 5. Architectural & Component Specifications

### 5.1 Design System Tokens (`ui/theme/` and `CommonComponents.kt`)
- `DarkSlateBackground`: `Color(0xFF0E0E13)`
- `DarkSlateSurface`: `Color(0xFF1A1824)`
- `DarkSlateSurfaceElevated`: `Color(0xFF262335)`
- `LavenderPrimary`: `Color(0xFF9D85F2)`
- `LavenderSecondary`: `Color(0xFFB8A7FF)`
- `TextPrimary`: `Color(0xFFF3F3F6)`
- `TextSecondary`: `Color(0xFFA09DB1)`
- `AccentGreen`: `Color(0xFF4EBA87)`
- `AccentAmber`: `Color(0xFFF5A623)`
- `AccentRed`: `Color(0xFFE55656)`

### 5.2 Navigation & Floating Pill Dock
- Floating pill bottom navigation bar with rounded pill shape (32dp corner radius), elevated above the bottom of the screen with margin.
- 4 primary destinations:
  - `HOME`: Home overview and quick actions.
  - `HISTORY`: Mileage and expense timeline with monthly grouping.
  - `REMINDERS`: Service reminders.
  - `TOOLS`: Hub for Trip Calculator, Vehicle Management, and Monthly Reports.

### 5.3 Database Schema Migration (Room v4 -> v5)
- Table `entries` gains:
  - `station_name TEXT NULL`
  - `is_full_tank INTEGER NOT NULL DEFAULT 0`
- Room schema export version 5 generated and tested against version 4 fixture.
- Backup snapshot codec upgraded to preserve `stationName` and `isFullTank` with backward-compatible import.

### 5.4 Consumption Calculation Model
- Consecutive full-tank calculation:
  - Identifies consecutive fuel fill-ups where `isFullTank == 1`.
  - Calculates fuel consumption: `(Total Fuel Volume (L) / Distance Delta (km)) * 100`.
  - Fallback: When fewer than two full-tank records exist, displays a placeholder message guiding the driver to log two full fill-ups.

### 5.5 Trip Calculator Component
- Located within the Tools screen or openable as a modal sheet.
- State: `distanceKm`, `fuelRatePerLiter`, `consumptionL100Km`.
- Pre-fills with active vehicle's average consumption and last recorded fuel price (or defaults if unavailable).
- Calculated values: `fuelNeededLiters = (distanceKm * consumptionL100Km) / 100`, `totalCostToman = fuelNeededLiters * fuelRatePerLiter`.

---

## 6. Calls Made Settleable by the Builder
1. **Theme Mode**: Set Dark Theme as the signature default for the app. Keep light theme support intact with high-contrast neutral tokens to preserve user settings compatibility.
2. **Tools Destination**: Implemented as a hub screen displaying cards for Calculator, Vehicle Profile, and Reports.
3. **Form Architecture**: Full-screen sheet with top action bar (`انصراف` / `ذخیره`) and grouped card sections, preserving keyboard insets handling and 320dp small screen scrolling safety.
4. **Icons & Animations**: Material Icons Rounded vector drawables and standard spring transitions for floating dock tab selection.
