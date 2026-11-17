# Vehicle Ledger (دفتر خودرو) — Product Design System & UX Specification

**Grounded In:** `app/src/main/java/org/unishiasy/vehiclelog/ui/theme/{Color,Type,Shape,Theme}.kt` & `design/vehicle-ledger-concept-c-v2/`
**Brand Direction:** Warm utilitarian Iranian automotive notebook — calm mineral canvas, deep petroleum teal primary action, clean crisp paper cards, high-contrast tabular numbers, zero decorative clutter.

---

## 1. Visual Identity & Color System

### Palette Tokens (`VehicleLogColors` / `LocalVehicleLogSemanticColors`)

| Token Role | Light Mode | Dark Mode | Intended Usage |
| :--- | :--- | :--- | :--- |
| **Canvas (`background`)** | `#F3F6F8` | `#101D22` | App scaffold background (`CanvasLight` / `CanvasDark`) |
| **Surface (`surface`)** | `#FFFFFF` | `#192B31` | Cards, forms, sheets, top dock surface |
| **Primary Ink (`onSurface`)** | `#112D34` | `#EAF4F5` | Headings, primary copy, active odometer figures |
| **Muted Ink (`onSurfaceVariant`)** | `#4D6269` | `#AFC5C9` | Supporting descriptions, dates, field hints |
| **Action (`primary`)** | `#075A63` | `#A6DEDF` | Primary CTAs, selected tabs, monetary figures, active dock tint |
| **On Action (`onPrimary`)** | `#FFFFFF` | `#10383B` | Text/icons inside primary buttons and selected `FilterChip`s |
| **Wash (`surfaceVariant`)** | `#DBECEF` | `#254249` | Icon container wells, Day Rail pills, active vehicle tint |
| **Rule (`outline`)** | `#D2E0E3` | `#344C54` | Card hairline borders (`1.dp`), dividers |
| **Control Outline (`outlineVariant`)** | `#71858B` | `#809DA4` | Input borders, `OutlinedButton` strokes |
| **Warning Surface / Edge / Ink** | `#FFF0D3` / `#DDA133` / `#694414` | `#382A16` / `#DDA133` / `#F4D497` | Due reminder banners, plaintext backup callouts |
| **Danger Surface / Edge / Ink** | `#FAE9E7` / `#A12C30` / `#A12C30` | `#40262B` / `#FFB4AB` / `#FFB4AB` | Validation errors, destructive buttons/confirmations |
| **Success Surface / Edge / Ink** | `#E4F1E9` / `#2E7D32` / `#23533B` | `#1C352D` / `#2E7D32` / `#A6E1B7` | Save/backup confirmation banners |

### Strict Color & Contrast Rules
1. **Never hardcode `Color.White`, `Color.Black`, or hex literals in Composables.** Always read `MaterialTheme.colorScheme.*` or `LocalVehicleLogSemanticColors.current.*`. (Known fix pending in `Color.kt:173,177,181`.)
2. **Never place light/white `onPrimary` text onto `warningEdge` (`#DDA133`).** Due/warning badges MUST use dark ink (`VehicleLogColors.InkLight` `#112D34` or `semanticColors.onWarning` `#694414` on amber surface) so contrast exceeds `4.5:1` in both Light and Dark themes (`HomeScreen.kt:512` P0 fix).
3. **Category Accent Restraint:** Category icons inside `32.dp–36.dp` `wash` wells keep `semanticColors.action` tint; avoid rainbow card backgrounds so warning/error semantics retain urgency.

---

## 2. Typography & RTL / Bidi Standards

### Font Family & Scale (`Type.kt`)
- **Typeface:** Bundled local `res/font/vazirmatn.ttf` (`VazirmatnFontFamily`, `LocaleList("fa")`).
- **Key Roles:**
  - `displayMedium` (`26.sp Bold`, `34.sp` line height): Odometer readout, Monthly total Toman.
  - `headlineLarge` (`22.sp Bold`): App Bar title (`دفتر خودرو`), screen headers.
  - `titleMedium` (`16.sp Bold`): Section headings, card titles, primary numeric amounts.
  - `bodyLarge` (`16.sp Normal`, `25.sp` line height): Form inputs, primary labels.
  - `bodyMedium` (`14.sp Normal`, `22.sp` line height): Secondary descriptions, dates, supporting hints.
  - `labelSmall` (`12.sp Bold`): Status badges, bottom dock captions (`maxLines = 1` with `fontScale` headroom).

### Mandatory Directionality & Digit Rules
1. **Layout Direction:** Full RTL (`android:supportsRtl="true"`, `fa` primary locale).
2. **User-Entered Text:** Apply `TextDirection.ContentOrRtl` on vehicle names, reminder titles, and notes so English model names render cleanly inside RTL layouts.
3. **Numeric Fields:** Use `TextDirection.Ltr` in `OutlinedTextField` for numeric/date editing (so `1404/02/15` edits naturally left-to-right).
4. **Mixed RTL+Number Isolation:** Whenever embedding numbers, km, liters, or Gregorian dates inside Persian sentences, wrap numeric spans in Unicode LRI/PDI (`\u2066...\u2069`) and dynamic mixed titles in FSI/PDI (`\u2068...\u2069`).
5. **Persian Digits & Formatting:**
   - All rendered counts, years, Toman amounts, and Jalali dates use Persian digits (`۰-۹`), thousand separator (`٬` / comma-safe visual transform), and Persian decimal separator `٫` (`U+066B`).
   - Standardize fuel volume rendering across Home & History using shared helper (avoid `HomeScreen.kt`'s `Locale.US` decimal `.` mismatch).

---

## 3. Spacing, Shapes & Layout Architecture

### Spacing Grid (`8.dp` System, Compact Outer Margin `20.dp`)
- Screen horizontal padding: `20.dp`
- Vertical section spacing: `16.dp`
- Card internal padding: `14.dp`–`16.dp` (`12.dp` compact row cards)
- Button / control spacing: `8.dp`–`10.dp`
- Bottom scroll buffer above dock: `24.dp`–`32.dp` (plus `Scaffold` `paddingValues`)

### Shape Scale (`Shape.kt`)
- Status badges & small icon tiles: `6.dp` (`VehicleLogShapes.small`)
- Inputs, FilterChips, Secondary/Primary Buttons: `8.dp`–`10.dp` (`VehicleLogShapes.medium` / `ButtonShape`)
- Feature cards (Odometer card, Monthly summary, Reminder cards): `12.dp` (`VehicleLogShapes.large`)
- Modal Bottom Sheets: `16.dp` top corners (`SheetShape`)

### Window Insets Rule
- **Every full-screen view MUST use `Scaffold`** (with `bottomBar` on top-level tabs and `topBar`/header inside body) and apply `.padding(paddingValues).consumeWindowInsets(paddingValues)` plus `.imePadding()` on scrollable forms—**never mix `safeDrawingPadding()` with `Scaffold` padding**.

### Responsive & Large-Font Layout Rules (`150%` Font Scale & `320dp–360dp` Widths)
- Detect `LocalDensity.current.fontScale >= 1.35f` or `BoxWithConstraints { maxWidth < 360.dp }`:
  - Stack paired form fields (`Amount` + `Fuel Volume`) vertically (`Column`) instead of side-by-side `Row`.
  - Wrap action chip strips (`ThemeMode`, `History Category Filters`) with `FlowRow` or horizontal scrollable rows without truncation.
  - Ensure card titles and trailing Toman amounts use `Modifier.weight(1f)` + `softWrap = true` cleanly instead of overlapping.

---

## 4. Navigation Architecture & Component Patterns

### Top-Level Destination Model
- **Bottom Dock (`AppBottomDock`):** Visible ONLY on top-level vehicle destinations (`Home`, `History`, `Reminders`) when `>= 1` vehicle exists and no modal/form/detail flow is open.
  - **Tabs:** `خانه` (`HOME`), `تاریخچه` (`HISTORY`), `یادآورها` (`REMINDERS`)
  - **Contextual Primary Dock Action (`+ ثبت جدید`):**
    - On `HOME` or `HISTORY`: opens `EntrySelectorSheet` (Fuel / Oil & Service / General Expense / Standalone Odometer).
    - On `REMINDERS`: opens/focuses Reminder creation form (`reminderAddTrigger`).
- **Secondary / Stack Destinations (Dock Hidden, Explicit Back Button + `BackHandler`):**
  - `EntryFormScreen` (Create / Edit expense)
  - `EntryDetailSheet` (P1 — Read-only view of any entry with Edit / Delete)
  - `MonthlyDashboardScreen` (Accessible from Notebook `گزارش ماهانه` tab or Home report card)
  - `VehicleScreen` (`VehicleList` manage mode + `VehicleForm` add/edit mode)
  - `BackupSettingsScreen` (Accessible via Settings gear icon from Home / Empty State; includes Appearance, Notifications, Backup/Restore, About & Privacy)

### Key UX Component Standards
1. **Touch Targets:** Minimum `48.dp` height on every `Button`, `OutlinedButton`, `TextButton`, `IconButton`, `FilterChip`, and clickable banner.
2. **Primary vs Secondary Hierarchy:** One primary filled CTA (`primary`) per visual region; secondary actions use `OutlinedButton` with `outlineVariant` stroke; destructive actions use `semanticColors.onDanger` / `error` with an **explicit confirmation `AlertDialog`** (required for both entry deletion and reminder cancellation!).
3. **Vehicle Selection & Management (`HomeScreen`):**
   - Selected Vehicle card header provides a clear switcher affordance opening `VehicleSwitcherSheet` when tapped, plus quick «مدیریت خودروها» link into `VehicleList`.
   - When `vehicles.size > 1`, inline radio switcher may remain or defer cleanly to `VehicleSwitcherSheet` (verify against `HomeScreenTest` `testTag` expectations before altering `homeVehicleSelector`).
4. **Input & Validation Ergonomics:**
   - **Amount (Toman):** `KeyboardType.Decimal`/`Number`, live spaced visual formatting (`VisualTransformation`), helper clarifying unit («تومان»).
   - **Date (Jalali):** Quick chips («امروز»، «دیروز») + calendar button opening pure-Kotlin `JalaliDatePickerDialog` + direct `YYYY/MM/DD` input fallback.
   - **Mileage & Notes Disclosure:** Collapsed by default on new entries (`detailsExpanded = false`); auto-expanded when editing existing non-empty mileage/notes OR when `MileageConflict`/mileage error occurs.
   - **Conditional Hints:** Only show `reminder_missing_mileage` when `vehicleMileage == null`.
5. **Standard States:**
   - **Loading:** Inline localized status with `LiveRegionMode.Polite` (no blocking spinner overlays for instant local Room reads).
   - **Empty:** Contextual icon well (`wash`) + plain explanation + direct recovery CTA (e.g., Add Vehicle, Add Entry, Clear History Filters).
   - **Error / Conflict:** Surface semantic `dangerSurface` / `warningSurface` card with actionable recovery (`تلاش مجدد` or «ذخیره بدون کیلومتر»).
   - **Success Feedback:** Auto-shown polite banner/snackbar after Vehicle save, Entry save/delete, Reminder create/complete/cancel, or SAF backup export/restore.

---

## 5. Accessibility Checklist (TalkBack & Motor)

- Every `IconButton` has non-null localized `contentDescription` (decorative icons inside labeled buttons pass `contentDescription = null`).
- Section titles carry `Modifier.semantics { heading() }`.
- Async validation, load failures, and operation results declare `Modifier.semantics { liveRegion = LiveRegionMode.Polite }`.
- Selectable options (`homeVehicle:{id}`, `CalendarOption`, month items) expose `Role.RadioButton` or `selected` semantics.
- Existing Compose `testTag` contracts (`homeScreen`, `homeQuickAdd`, `homeAddExpense`, `homeSelectedVehicleCard`, `homeOpenHistory`, `homeOpenDashboard`, `homeOpenReminders`, `homeOpenSettings`, `ledgerHistoryList`, `ledgerRecent:{id}`, `ledgerDelete:{id}`, `remindersScreen`, `reminderSave`, `backupSettings`, `backupExport`, `backupRestore`) MUST be preserved cleanly across all upgrades.
