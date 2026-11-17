# Vehicle Ledger (دفتر خودرو) — Product Specification v2.0

**Document Status:** Baseline Takeover Specification (Verified against repository source on 2026-11-17)
**Target Distribution:** Cafe Bazaar (Iran — Persian-first, RTL, Offline-first Android)
**Package / Namespace:** `applicationId = org.mewlog.vehiclelog` (`release`) / `.debug` (`debug`); Kotlin `namespace = org.unishiasy.vehiclelog`
**Version Baseline:** `versionCode = 1`, `versionName = "0.1.0"`, `minSdk = 26`, `targetSdk = 36`, `compileSdk = 36`

---

## 1. Product Vision & Target Users

### Vision
Vehicle Ledger (**دفتر خودرو**) is a Persian-first, right-to-left (RTL), offline-first Android ledger that gives Iranian vehicle owners and professional drivers a single trustworthy place to track their vehicles, fuel purchases, maintenance & repair expenses, odometer readings, service reminders, monthly spending summaries, and portable local backups.

### Target Users
1. **Everyday Private Vehicle Owners (Primary):** Drivers with 1–3 household cars/motorcycles who want to know how much they spend monthly in Toman, when they last changed oil, and when insurance/service is due.
2. **Professional / Rideshare & Delivery Drivers (Primary):** High-mileage drivers (Snapp/Tapsi/intercity/cargo) who log frequent fuel stops, care about cost-per-month and mileage-based maintenance intervals, and work often in poor or zero connectivity.
3. **Device-Upgrading Users:** Users who want safe, offline file export/restore across phones without mandatory cloud accounts, login walls, or internet permissions.

---

## 2. Core Product Principles

| ID | Principle | Enforcement |
| :--- | :--- | :--- |
| **PR-01** | **Offline-First Forever** | 100% of core features (vehicles, entries, odometer, reminders, Jalali reports, JSON export/restore) MUST work with zero network permissions and zero Google Play Services dependencies in release builds. |
| **PR-02** | **Persian-First & RTL Native** | Natural Persian copy, Persian numerals (`۰-۹`), Solar Hijri (Jalali, Tehran civil date) primary dates, Vazirmatn typography, and explicit `\u2066...\u2069` / `\u2068...\u2069` bidi isolation on mixed numeric/Latin labels. |
| **PR-03** | **Exact Monetary Integrity** | Database stores integer **legacy rials** (`amount_rials: Long?`); UI displays and accepts **Toman** (`1 Toman = 10 Rials`) via `BigInteger`/`BigDecimal` exact math (`Money.kt`). Never store floats/doubles for money. |
| **PR-04** | **Locked Schema & Backup Continuity** | Existing Room v4 schema (`app/schemas/.../1..4.json`) and Backup Codec v1 JSON format (`version = 1`) MUST remain backward-compatible and strictly preserved unless an explicit versioned Room migration is approved and migration-tested. |
| **PR-05** | **No Dead-End UI or Mock Data** | Never ship unreachable UI, non-functional toggles, hardcoded AI/cloud stubs, or pre-populated fake records. |
| **PR-06** | **Accessibility & Real-World Readability** | Minimum `48dp × 48dp` touch targets, high WCAG AA contrast (`≥ 4.5:1`), 150% font-scale safety, TalkBack semantics & live regions, and safe window/IME insets across API 26–36. |

---

## 3. Scope Boundaries (v1.0 Cafe Bazaar Release vs Deferred)

| Area | v1.0 Offline Release Scope | Post-v1.0 / Deferred Scope |
| :--- | :--- | :--- |
| **Data Storage** | Local Room SQLite v4 (`allowBackup="false"`, excluded from Android cloud/D2D auto-backup). | Encrypted backup passphrase / optional self-hosted snapshot sync. |
| **Cloud Recovery** | **Disabled in release (`LOCAL_CLOUD_ENABLED = false`)**. Localhost-only loopback client remains debug/test-only. | Production HTTPS backend snapshot recovery (requires dedicated security audit, `INTERNET` permission disclosure, and backend ops). |
| **Vehicle Catalog** | Manual vehicle name + optional Jalali/Gregorian production year (`approvedCatalogAsset = null`). | Bundled verified Iranian vehicle catalog JSON (`assets/` + picker). |
| **Categories & Analytics** | 7 fixed categories (`fuel`, `oil_change`, `repair`, `insurance`, `wash`, `parts`, `other`), Jalali monthly category totals, Fuel L/100km & cost/km (planned P2). | Custom user categories, multi-vehicle comparison charts, CSV/PDF reports. |
| **Permissions** | Zero internet permission in release; runtime `POST_NOTIFICATIONS` (API 33+) only if user enables reminder notifications. | None added without strict user benefit & Bazaar compliance review. |

---

## 4. Personas & Top User Journeys

1. **First-Run Onboarding (0 Vehicles):** Launch -> Empty state card + Restore Backup option -> Add Vehicle (`خودرو جدید`) -> Return directly to Home dashboard for that vehicle with confirmation feedback.
2. **10-Second Fuel / Expense Logging:** From Home (`ثبت سوخت` / `ثبت هزینه +` or bottom dock `ثبت جدید`) -> Pre-selected category -> Type Toman amount (live formatted), optional liters -> Pick Today/Yesterday or Jalali calendar date -> Optional mileage/notes disclosure -> Save -> Immediate return to Home/History with updated odometer & totals.
3. **Review, Edit, or Delete Past Entry:** Open History tab or tap a Recent Entry card on Home -> Filter by month/category or search notes -> View Entry Detail -> Edit or Confirm Delete -> Balances and current odometer recalculate cleanly.
4. **Update Odometer Without an Expense:** Log or manage standalone odometer readings so mileage-based service reminders stay accurate even without recording an expense.
5. **Service Reminder Lifecycle:** Create date-based or mileage-based reminder -> Receive due notification (if enabled) and Home warning banner -> Edit reminder, Mark Complete (optionally schedule next interval), or Confirm Cancel.
6. **Monthly Financial Review:** Open Report tab (`گزارش ماهانه`) -> Switch Jalali months -> Review total Toman, entry counts, category breakdown, fuel efficiency, and any unassigned invalid-date records.
7. **Safe Phone Switch / Backup:** Open Settings (`تنظیمات`) -> Export plaintext JSON via Android SAF (`CreateDocument`) with timestamp hint -> On new install, Restore JSON via SAF (`OpenDocument`) into an empty ledger.

---

## 5. Feature Inventory & Verified Current Status

| Feature | Current Code Status | Evidence | Gaps to Close |
| :--- | :--- | :--- | :--- |
| **Multi-vehicle create & edit** | Implemented | `VehicleRepository`, `VehicleViewModel`, `VehicleScreen.kt` | `VehicleList` unreachable once ≥1 vehicle exists; save confirmation lost on transition to Home; no delete/archive UI (`RESTRICT` FK). |
| **Active vehicle persistence** | Implemented (Turn 4) | `SelectedVehiclePreferences`, `HomeViewModel.kt` | `VehicleSwitcherSheet` in `CommonComponents.kt` is wired to `showVehicleSwitcher`, which is never toggled `true` (`HomeScreen.kt:93-103`). |
| **7-category ledger CRUD** | Implemented | `LedgerRepository`, `LedgerDao`, `LedgerViewModel`, `LedgerScreen.kt` | `detailsExpanded` defaults `true`; no Jalali date picker or Today/Yesterday chips; no live Toman separator; Fuel paired `Row` clips at large font scale; History tap opens Edit directly without Detail screen; no History search/filter; Delete `TextButton` height `36dp`. |
| **Odometer reading CRUD & conflict detection** | Data/Repository implemented; Partial UI | `OdometerDao`, `LedgerRepository.kt`, `EntrySaveResult.MileageConflict` | **Standalone odometer reading CRUD (`createStandaloneReading` / `update` / `delete`) has ZERO UI entry point.** |
| **Jalali monthly spending report** | Implemented | `MonthlyReporting.kt`, `MonthlyDashboardScreen.kt` | Ordered by fixed enum instead of descending amount; no L/100km or cost/km efficiency metrics; uses `safeDrawingPadding` without `Scaffold`. |
| **Date & mileage reminders + WorkManager notifications** | Implemented | `ReminderRepository`, `ReminderRules`, `ReminderWorker`, `RemindersScreen.kt` | Misleading `reminder_missing_mileage` note shown whenever Mileage chip selected even if `list.mileage != null`; no Edit active reminder; one-tap Cancel has no confirmation dialog; action buttons `40dp` (`< 48dp`); no recurring interval helper. |
| **Manual SAF JSON backup & restore** | Implemented | `BackupCodec`, `BackupRepository`, `BackupSettingsScreen.kt` | No last-backup timestamp / stale-backup nudge; no App Info / Privacy / Backup Guide section. |
| **Theme & Appearance** | Implemented | `AppearancePreferences`, `Theme.kt`, `Color.kt` | `HomeScreen` due badge uses `onPrimary` (`#FFFFFF`) over `#DDA133` amber (fails WCAG contrast); `Color.kt` uses raw `Color.White`. |
| **Debug-only Local Cloud Sync/Recovery** | Debug-only (`LOCAL_CLOUD_ENABLED`) | `CloudRepository`, `CloudSettingsScreen.kt` | `CloudSettingsScreen.kt:68` fails `:app:lintDebug` (`ContextCastToActivity`). Must stay stripped from release. |

---

## 6. Functional Requirements (`FR-xxx`)

### Vehicles (`FR-001` .. `FR-006`)
- **`FR-001` First-Run Onboarding:** When `vehicles.isEmpty()`, the app shall display a clear Persian onboarding view with primary CTA «افزودن خودرو» and secondary CTA «بازیابی از فایل پشتیبان».
  - *Acceptance Criteria:* Fresh install shows zero-vehicle card; creating a valid vehicle transitions to `HomeScreen` for that vehicle and displays confirmation feedback.
  - *Verification:* Robolectric UI test on `VehicleScreen` + `LedgerHost`.
- **`FR-002` Create & Edit Vehicle:** Users shall create or edit a vehicle with required `displayLabel` (normalized whitespace) and optional paired `productionYear` + `productionYearCalendar` (`solar_hijri` 1300–1450 / `gregorian` 1920–2100).
  - *Verification:* `VehicleInputTest`, `VehicleRepositoryTest`, Robolectric form test.
- **`FR-003` Multi-Vehicle Switcher & Management Access:** Users with ≥1 vehicle shall be able to switch the active vehicle cleanly and reach a Vehicle Management view (`لیست خودروها`) to inspect, edit, or add vehicles without losing their place.
  - *Acceptance Criteria:* Tapping the vehicle header/switcher opens `VehicleSwitcherSheet`; «مدیریت خودروها» opens `VehicleList` rather than forcing «افزودن خودرو»; selected vehicle persists across cold restart (`SelectedVehiclePreferences`).
  - *Verification:* Robolectric `HomeScreen` + `LedgerHost` navigation test.
- **`FR-004` Safe Vehicle Deletion Policy (Deferred / Guarded for P2):** If vehicle deletion is added, it shall warn explicitly when linked entries/readings/reminders exist and never leave orphan rows (`ON DELETE RESTRICT` or explicit transactional cascade under user confirmation).

### Ledger & Odometer (`FR-010` .. `FR-019`)
- **`FR-010` Expense & Fuel Entry Creation/Editing:** Users shall create/edit entries across 7 categories (`fuel`, `oil_change`, `repair`, `insurance`, `wash`, `parts`, `other`), optional Toman amount (`null` = unpriced, or `> 0`), required Jalali occurred date (`YYYY/MM/DD`), optional fuel liters (`fuel`, `> 0`, stored in mL), optional linked odometer km (`>= 0`), and optional notes.
  - *Verification:* `EntryInputTest`, `EntryDraftTest`, Robolectric `LedgerViewModel` & Room tests.
- **`FR-011` Jalali Date Picker & Quick Date Chips:** `EntryFormScreen` and `ReminderForm` shall provide quick chips («امروز», «دیروز» for entries; «امروز» / +1/+3/+6 month presets or Jalali calendar modal) alongside manual `YYYY/MM/DD` typing.
  - *Acceptance Criteria:* Selecting «امروز» or picking a Jalali date updates `EntryDraft.date` with normalized Persian `YYYY/MM/DD` string and clears any `EntryField.Date` error.
  - *Verification:* Unit test for Jalali picker date mapping + Robolectric UI test.
- **`FR-012` Live Toman Formatting & Numeric Normalization:** Toman numeric input shall accept Persian (`۰-۹`), Arabic-Indic (`٠-٩`), and ASCII digits, normalize separators, and display clean thousand-separated visual formatting without corrupting cursor position or `Money.parseTomanToRials`.
  - *Verification:* `MoneyTest` + Robolectric visual transformation / input test.
- **`FR-013` Linked Odometer Chronology Conflict Handling:** When saving an entry whose linked odometer violates chronological ordering relative to adjacent dated readings, the ViewModel shall keep the draft open, display a specific Persian explanation, and allow either fixing mileage/date or «ذخیره بدون کیلومتر» (`saveWithoutMileage()`).
  - *Verification:* `OdometerOperationsTest` (migrated/mirrored to Robolectric) + `LedgerViewModelTest`.
- **`FR-014` Standalone Odometer Log UI:** Users shall be able to add, edit, and delete standalone odometer readings (`entryId = null`) directly from Home's Odometer panel and History, backed by `LedgerRepository.createStandaloneReading` / `updateStandaloneReading` / `deleteStandaloneReading`.
  - *Acceptance Criteria:* Tapping «ثبت کیلومتر فعلی» opens a compact dialog/sheet prefilled with date «امروز», validates mileage chronology, persists to `odometer_readings`, and updates Home's odometer panel + due mileage reminders immediately.
  - *Verification:* Robolectric UI + Room DAO test for standalone reading add/edit/delete & chronological rejection.
- **`FR-015` Entry Detail View & Confirmed Delete:** Tapping an entry in `HomeScreen` or `HistoryView` shall open an explicit Read-Only Entry Detail sheet/view showing category, formatted Toman cost, fuel liters, odometer, Jalali date, and notes, with «ویرایش» and «حذف» (confirmation dialog).
  - *Verification:* Robolectric `LedgerHost` + `HistoryView` test.

### History & Monthly Reporting (`FR-020` .. `FR-024`)
- **`FR-020` Searchable & Filterable History:** `HistoryView` shall support filtering by category chip («همه», «سوخت», etc.), filtering by Jalali month, and searching entry notes/titles, with clear empty-filter recovery («پاک کردن فیلترها»).
  - *Verification:* Robolectric filter state + `HistoryView` UI test.
- **`FR-021` Monthly Jalali Report & Category Ranking:** `MonthlyDashboardScreen` shall display total Toman spent (`BigInteger` safe), priced/unpriced entry counts, categories ordered by spending descending (stable tie-breaker), percentage share bars, and unassigned malformed/out-of-range date warnings (`ReportDateIssue`).
  - *Verification:* `MonthlyReportingTest`, `MonthlyDashboardStateTest`, Robolectric dashboard test.
- **`FR-022` Fuel Efficiency & Cost-per-km Metrics (P2):** When a vehicle has ≥2 fuel entries with linked odometer and volume within a month/period, compute and clearly label estimated `L/100km` and average Toman/km, gracefully hidden or marked insufficient-data when <2 readings exist.
  - *Verification:* Pure JVM unit tests with edge cases (missing volume, non-monotonic odometer, single fill-up, zero distance).

### Service Reminders & Notifications (`FR-030` .. `FR-035`)
- **`FR-030` Date & Mileage Reminders:** Users shall create active reminders with either a Jalali `dueOn` date or `dueKilometers` threshold (`ReminderRules.kt` exclusivity constraint).
  - *Verification:* `ReminderRulesTest`, `ReminderDraftTest`, Room trigger/constraint test.
- **`FR-031` Conditional Missing-Mileage Hint (Bug Fix):** The hint «تا زمانی که کیلومتری برای این خودرو ثبت نشود، موعد یادآورهای کیلومتری بررسی نمی‌شود» shall ONLY appear when the active vehicle's `currentMileage == null`, NEVER when mileage is already recorded.
  - *Verification:* Robolectric `RemindersScreen` test verifying visibility with `mileage = null` vs `mileage = 54000`.
- **`FR-032` Confirmed Cancel & Active Reminder Editing:** Canceling an active reminder shall require an explicit confirmation dialog («لغو یادآور؟»). Users shall also be able to edit an active reminder's title/threshold without deleting and recreating it (and optionally mark complete + schedule next interval).
  - *Verification:* Robolectric `RemindersViewModel` & `RemindersScreen` test.
- **`FR-033` Offline Notification Scheduling:** When notifications are enabled (with runtime `POST_NOTIFICATIONS` consent on Android 13+), `WorkManager` daily check evaluates due date & mileage reminders safely using `ReminderDeliveryStore` deduplication and opens `MainActivity` on tap.
  - *Verification:* `ReminderSchedulingTest` / `ReminderWorkerTest` (Robolectric WorkManager test).

### Backup, Settings & About (`FR-040` .. `FR-044`)
- **`FR-040` SAF JSON Export & Restore:** Export streams all vehicles, entries, odometer readings, and reminders as strict Backup v1 JSON (`BackupCodec`, 10 MiB import cap, strict schema & FK validation). Restore requires an empty ledger (`BackupOutcome.LedgerPopulated` guard), runs atomically inside `withTransaction`, and schedules/warns on notifications cleanly.
  - *Verification:* `BackupCodecTest`, `BackupInputTest`, Robolectric `BackupRestoreTest`.
- **`FR-041` Last-Backup Tracking & Stale Reminder:** App shall store the epoch timestamp of the last successful local export in local preferences and display it in Settings + a subtle non-blocking prompt when entries were added/modified since last backup or >14 days have elapsed.
  - *Verification:* Unit + Robolectric `BackupSettingsScreen` test.
- **`FR-042` About, Offline Privacy & Backup Help Screen:** Settings shall include an «درباره و حریم خصوصی» section displaying app version (`versionName`/`versionCode`), clear statement of 100% offline storage, plaintext backup warning, and data restore rules.
  - *Verification:* Robolectric `BackupSettingsScreen` test.

---

## 6. Non-Functional Requirements

### UX & Accessibility (`UX-001` .. `UX-007`)
- **`UX-001` Minimum Touch Target:** Every clickable button, chip, icon button, and row action MUST be `>= 48.dp` in touch height/size (`LedgerScreen.kt` Delete button & `RemindersScreen.kt` action buttons must be fixed from `36dp`/`40dp` to `48dp`).
- **`UX-002` Contrast Compliance:** All text/badge/icon pairs MUST satisfy WCAG AA (`>= 4.5:1` normal text, `>= 3.0:1` large text/UI components) in both Light and Dark themes (`HomeScreen` amber due badge `onPrimary` white text must change to dark ink `#112D34` or `onWarning`).
- **`UX-003` Responsive & 150% Font-Scale Layouts:** Paired horizontal rows (e.g., Fuel Amount + Volume, Home quick action buttons, Theme chips, Reminder action buttons) MUST wrap or stack vertically when `LocalDensity.current.fontScale >= 1.35f` or `maxWidth < 360.dp`.
- **`UX-004` Process-Death & Configuration-Change Continuity:** Screen route, active bottom tab, active vehicle ID, and open in-progress drafts (`EntryDraft`, `VehicleDraft`, `ReminderDraft`) MUST survive activity recreation (`onSaveInstanceState` / `SavedStateHandle`).
- **`UX-005` Navigation Consistency:** Avoid mixed `safeDrawingPadding()` and `Scaffold` insets (fix `EntryFormScreen`, `MonthlyDashboardScreen`, `BackupSettingsScreen`, `VehicleScreen`, `CloudSettingsScreen`); ensure predictive/hardware `BackHandler` returns cleanly along a predictable single hierarchy without trapped states.
- **`UX-006` Bidi & Mixed Text Safety:** Use `TextDirection.ContentOrRtl` for user-entered titles/notes and `\u2066...\u2069` isolate around pure numbers (`km`, `L`, dates) inside Persian strings.
- **`UX-007` Disclosure Accordion Default:** In `EntryForm`, optional Mileage & Notes disclosure (`detailsExpanded`) MUST default to `false` for new entries unless editing an existing entry with mileage/notes or a `MileageConflict`/validation error occurred.

### Data Integrity & Recovery (`DATA-001` .. `DATA-006`)
- **`DATA-001` Exact Integer Money:** Store `Long` legacy rials (`> 0` for non-null entries); convert Toman <-> Rials via `Money.kt` (`* 10` / `/ 10` exact `BigInteger`).
- **`DATA-002` Jalali Civil Dates:** Use `JalaliDates.kt` with `Asia/Tehran` civil date semantics; supported report/picker year bounds clearly validated and surfaced if out-of-range (`ReportDateIssue`).
- **`DATA-003` Atomic Writes:** Entry + linked odometer reading updates, reminder completions, and JSON backup restores MUST execute in `VehicleDatabase.withTransaction`.
- **`DATA-004` Safe Migration & Open-Failure Preservation:** Exported Room schemas `1.json`..`4.json` must never be altered retroactively; `fallbackToDestructiveMigration` is strictly forbidden. Any future schema v5+ requires explicit user-approved migration + populated migration test.
- **`DATA-005` Restore Safety:** JSON restore MUST refuse overwriting a non-empty database (`BackupOutcome.LedgerPopulated`) and enforce the 10 MiB read cap + duplicate/missing FK integrity checks.
- **`DATA-006` Zero Platform Cloud Auto-Backup:** `android:allowBackup="false"` and restrictive `data_extraction_rules.xml` (`fullBackupContent` / `cloud-backup` / `device-transfer` excluded) remain enforced so sensitive vehicle/financial logs never leak silently to OS cloud backups.

### Security & Privacy (`SEC-001` .. `SEC-004`)
- **`SEC-001` Zero Unneeded Permissions in Release:** Release manifest MUST NOT request `INTERNET`, `ACCESS_NETWORK_STATE`, `READ_EXTERNAL_STORAGE`, `READ_MEDIA_*`, `CAMERA`, or location permissions. Only `POST_NOTIFICATIONS` (optional runtime) and WorkManager `RECEIVE_BOOT_COMPLETED` / `WAKE_LOCK` (via library manifest merge) may exist.
- **`SEC-002` Plaintext Backup Transparency:** Users must be visibly informed before/during export that JSON backups are unencrypted plaintext files (`backup_export_explanation`).
- **`SEC-003` No Credential or PII Logging:** Never log database values, financial amounts, vehicle names, backup contents, or local cloud credentials in `Log.*`.
- **`SEC-004` Debug-Only Cloud Isolation:** `BuildConfig.LOCAL_CLOUD_ENABLED`, cleartext `local_cloud_network.xml`, and `app/src/debug/AndroidManifest.xml` `INTERNET` permission MUST remain isolated to `debug` source set/buildType only.

### Performance & Reliability (`PERF-001` .. `PERF-003`, `REL-001` .. `REL-004`)
- **`PERF-001` Main-Thread Freedom:** All Room DAOs/transactions, JSON encoding/decoding, and SharedPreferences/file I/O stay off the UI thread (`Dispatchers.IO` / Room suspend).
- **`PERF-002` Unbounded List Efficiency:** `HistoryView` uses keyed `LazyColumn` (`key = { it.id }`); filtering/search runs via `remember` / Flow derivation without redundant allocations on scroll.
- **`PERF-003` Clean Build & Shrink:** Release APK enables R8 (`isMinifyEnabled = true`, `isShrinkResources = true`) with verified ProGuard/R8 keep rules for `kotlinx.serialization`, `Room`, and `WorkManager`.
- **`REL-001` Lint & Unit Gate:** `./gradlew` / `gradle :app:lintDebug :app:testDebugUnitTest :app:assembleRelease` MUST pass with 0 Lint errors and 0 test failures.
- **`REL-002` Physical Device Verification Gate:** Cafe Bazaar candidate APKs MUST pass the physical/emulator checklist (`TEST_STRATEGY.md` + TalkBack + 150% font scale + RTL + dark mode + process kill + WorkManager + offline airplane mode).

---

## 7. Data Model & Business Rules Summary (Verified)

- **Room Entities (`VehicleDatabase` v4):**
  - `vehicles (id, display_label, normalized_label, catalog_key, production_year, production_year_calendar, created_at)`
  - `entries (id, vehicle_id -> RESTRICT, category, occurred_on [YYYY-MM-DD Gregorian], amount_rials [Long?], fuel_volume_ml [Long?], notes, created_at, updated_at)`
  - `odometer_readings (id, vehicle_id -> RESTRICT, entry_id -> RESTRICT UNIQUE?, reading_date, kilometers [Long], created_at)`
  - `reminders (id, vehicle_id -> RESTRICT, title, due_on [YYYY-MM-DD?], due_kilometers [Long?], state [active|completed|canceled], created_at, updated_at, completed_at, canceled_at)` — SQLite CHECK enforces `due_on IS NOT NULL XOR due_kilometers IS NOT NULL` (`ReminderConstraints.kt`).
- **Odometer Monotonicity Rule (`LedgerRepository`):** Any reading on `reading_date` must satisfy `previous_date_reading <= kilometers <= next_date_reading`. Violations return `EntrySaveResult.MileageConflict` / `OdometerWriteResult.Conflict`.
- **Backup Format (`BackupSnapshot` v1):** Strict JSON (`StrictBackupJson`), version = 1, exports/validates all 4 tables atomically.

---

## 8. Localization, Typography & RTL Rules

- **Primary Locale:** Persian (`fa`, `supportsRtl="true"`, `res/font/vazirmatn.ttf`).
- **Strings:** 100% of user-visible labels stored in `app/src/main/res/values/strings.xml`. Unused legacy strings (10 identified by Lint) shall be audited and either wired cleanly or removed.
- **Digits:** Display uses Persian digits (`۰-۹`), decimal separator `٫` (`U+066B`); input normalization accepts `0-9`, `۰-۹`, `٠-٩`.
- **Numbers in RTL Sentences:** Wrap isolated numeric spans in `\u2066...\u2069` (LRI/PDI); wrap mixed user names/titles in `\u2068...\u2069` (FSI/PDI) or `TextDirection.ContentOrRtl`.

---

## 9. Release & Maintenance Verification Matrix

| Gate | Requirement IDs | Target Evidence |
| :--- | :--- | :--- |
| **Static & Build** | `REL-001`, `PERF-003`, `SEC-001` | `:app:lintDebug` (0 errors), `:app:assembleRelease` with R8 enabled & zero release `INTERNET` permission |
| **JVM & Robolectric Automated** | `FR-001..042`, `DATA-001..005`, `UX-001..007` | JUnit + Robolectric UI/Room/WorkManager suites under `app/src/test/` |
| **Pre-Publish Device QA** | `UX-001..006`, `REL-002` | Physical Android phone + emulator check: TalkBack RTL walkthrough, 150% font scaling, dark mode, SAF export/restore across clean install, process kill recovery, notification deep-link |
