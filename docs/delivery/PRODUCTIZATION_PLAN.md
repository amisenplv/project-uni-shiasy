# Vehicle Ledger (دفتر خودرو) — Controlled Productization Plan

**Purpose:** Sequence safe, small, testable engineering slices to transform the verified Vehicle Ledger codebase into a Cafe Bazaar–ready v1.0 release without breaking Room v4 persistence, Backup v1 compatibility, monetary math, or offline-first reliability.
**Baseline Date:** 2026-11-17 (Post-Turn 4 Sprint 1; before P0 begins)

---

## Phase Overview & Priorities

| Priority | Theme | Goal | Target Exit Gate |
| :--- | :--- | :--- | :--- |
| **P0** | Correctness, Lint & Critical UX Blockers | Fix build/lint failure, accessibility/contrast violations, unreachable vehicle management, misleading mileage warning, accidental reminder cancel, and unsafe window insets. | `:app:lintDebug` = 0 errors, `:app:testDebugUnitTest` passes + new tests, zero dead-end UI flows. |
| **P1** | Product Completeness, Testability & Cafe Bazaar Readiness | Add process-death state restoration (`SavedStateHandle`), responsive 150%-font layouts, Jalali date picker + Today/Yesterday chips, live Toman formatting, standalone odometer UI, Entry Detail screen, History search/filter, Reminder edit/recurring, Robolectric UI/DB test harness, R8 release hardening, About/Privacy & backup reminder. | Full Robolectric CUJ & Room coverage; minified signed release APK cleanly verified; Bazaar checklist ready for physical QA. |
| **P2** | Differentiation & Efficiency Insights | Fuel efficiency (`L/100km`), cost-per-km metrics, descending spending breakdown, optional bundled Iranian vehicle catalog, optional CSV report export, guarded vehicle archive/delete. | Pure math + UI unit/Robolectric tests passing. |
| **P3** | Cafe Bazaar Launch & Post-v1.0 Cloud Decision | Execute physical device/TalkBack QA matrix, finalize Bazaar store metadata/screenshots, build signed release APK/AAB, explicitly decide fate of debug-only Cloud sync. | Physical QA signoff recorded; Store artifacts approved. |

---

## Detailed Work Breakdown (Small, Testable Slices)

---

### P0 — Correctness, Lint & Critical UX Blockers

#### Slice P0.1: Lint Zero-Error Baseline & Resource/KTX Hygiene
- **Objective:** Restore a green `:app:lintDebug` gate and eliminate `ContextCastToActivity` error and code-hygiene warnings without touching business logic.
- **Problem Solved:** `:app:lintDebug` currently fails on `CloudSettingsScreen.kt:68` (`ContextCastToActivity`), plus 3 `UseKtx` warnings and 10 `UnusedResources` warnings in `strings.xml`.
- **Requirement IDs:** `REL-001`, `SEC-004`
- **Affected Files:**
  - `app/src/main/java/org/unishiasy/vehiclelog/ui/settings/CloudSettingsScreen.kt` (replace `LocalContext.current as? Activity` with `LocalActivity.current`)
  - `app/src/main/java/org/unishiasy/vehiclelog/ui/settings/AppearancePreferences.kt`, `ui/home/HomeViewModel.kt`, `reminders/ReminderNotifications.kt` (`prefs.edit { ... }` KTX)
  - `app/src/main/res/values/strings.xml` (audit the 10 unused strings; keep any explicitly planned for P0/P1 or remove dead ones cleanly)
- **Dependencies:** None.
- **UX Impact:** Zero visible behavior change; clean debug/release lint pipeline.
- **Data / Schema / Backup Risks:** None.
- **Implementation Steps:** Fix `LocalActivity.current`, switch 3 `SharedPreferences.edit()` calls to `androidx.core.content.edit`, audit unused strings.
- **Verification & Tests:** Run `gradle :app:lintDebug :app:testDebugUnitTest`.
- **Acceptance Criteria:** `:app:lintDebug` passes with 0 errors and minimal/zero non-dependency warnings; 94+ unit tests pass.
- **Definition of Done:** Green lint + unit test run recorded in `AI_STUDIO_PROGRESS.md`.
- **Rollback Plan:** Revert single commit/edits cleanly.

#### Slice P0.2: Accessibility Contrast, Minimum 48dp Touch Targets & Material Deprecated APIs
- **Objective:** Bring every interactive element and semantic badge in compliance with WCAG AA contrast and Android 48dp touch target standards.
- **Problem Solved:**
  1. `HomeScreen.kt:512` renders white `onPrimary` text on `#DDA133` amber due badge (~2.25:1 contrast failure).
  2. `Color.kt:173,177,181` hardcodes `Color.White` instead of semantic token.
  3. `RemindersScreen.kt:600,622` uses `40dp` action buttons; `LedgerScreen.kt:665` uses `36dp` Delete button; `HomeScreen.kt:541` uses `40dp` `"مشاهده همه"`.
  4. `LedgerScreen.kt:339,345` uses deprecated `TabRow`/`tabIndicatorOffset`.
- **Requirement IDs:** `UX-001`, `UX-002`
- **Affected Files:**
  - `app/src/main/java/org/unishiasy/vehiclelog/ui/theme/Color.kt`
  - `app/src/main/java/org/unishiasy/vehiclelog/ui/home/HomeScreen.kt`
  - `app/src/main/java/org/unishiasy/vehiclelog/ui/ledger/LedgerScreen.kt`
  - `app/src/main/java/org/unishiasy/vehiclelog/ui/reminders/RemindersScreen.kt`
- **Dependencies:** P0.1.
- **UX Impact:** Legible due badge in Light & Dark mode; forgiving thumb tap targets across Home, History, and Reminders.
- **Data / Schema / Backup Risks:** None.
- **Implementation Steps:** Use `semanticColors.onWarning` / dark ink (`VehicleLogColors.InkLight`) on amber badges; replace `Color.White` in `Color.kt` with `VehicleLogColors.OnActionLight`; raise interactive touch bounds to `48.dp`; migrate `TabRow` to `PrimaryTabRow`.
- **Verification & Tests:** `compileDebugKotlin`, `VehicleLogThemeTest` expanded for contrast check, manual bounds audit.
- **Acceptance Criteria:** Zero `TabRow` deprecation warnings; all interactive controls `>= 48dp`; due badge contrast `>= 4.5:1`.
- **Definition of Done:** Build + lint + theme tests pass.
- **Rollback Plan:** Revert UI/theme styling changes.

#### Slice P0.3: Fix Vehicle Switcher & Management Routing + Missing Save Feedback + Double Insets
- **Objective:** Make `VehicleSwitcherSheet` and `VehicleList` reachable, display feedback after vehicle create/update on `HomeScreen`, and fix window inset double-padding on full-screen subviews.
- **Problem Solved:**
  1. `VehicleSwitcherSheet` in `HomeScreen.kt` has state `showVehicleSwitcher` that can never become `true`.
  2. Once ≥1 vehicle exists, `VehicleList` is unreachable; tapping «مدیریت خودروها» would open Add Vehicle instead of `VehicleList`.
  3. Creating/updating a vehicle transitions straight to `HomeScreen`, hiding `VehicleList`'s `vehicle_saved` message.
  4. `EntryFormScreen`, `MonthlyDashboardScreen`, `BackupSettingsScreen`, and `VehicleScreen` apply `safeDrawingPadding()` on raw `Surface`/`Column` while `HomeScreen`, `HistoryView`, and `RemindersScreen` use `Scaffold` (`contentWindowInsets`).
- **Requirement IDs:** `FR-003`, `UX-005`
- **Affected Files:**
  - `app/src/main/java/org/unishiasy/vehiclelog/ui/home/HomeScreen.kt`
  - `app/src/main/java/org/unishiasy/vehiclelog/ui/ledger/LedgerScreen.kt`
  - `app/src/main/java/org/unishiasy/vehiclelog/ui/ledger/MonthlyDashboardScreen.kt`
  - `app/src/main/java/org/unishiasy/vehiclelog/ui/settings/BackupSettingsScreen.kt`
  - `app/src/main/java/org/unishiasy/vehiclelog/ui/vehicle/VehicleScreen.kt`
  - `app/src/main/java/org/unishiasy/vehiclelog/ui/vehicle/VehicleViewModel.kt`
- **Dependencies:** P0.2.
- **UX Impact:** Clean vehicle switcher chip/card trigger on Home, reachable Manage Vehicles screen, visible save confirmation snackbar/banner, consistent edge-to-edge inset behavior.
- **Data / Schema / Backup Risks:** None.
- **Implementation Steps:** Add explicit switcher button on Home's selected vehicle header; add `managingVehicles` UI flag in `LedgerHost`/`VehicleViewModel` so `VehicleList` can be opened from `VehicleSwitcherSheet` («مدیریت خودروها»); surface transient vehicle save banner/snackbar on Home; standardize sub-screens onto `Scaffold` with `consumeWindowInsets`.
- **Verification & Tests:** Update/compile `androidTest` compile check (`VehicleSetupTest`, `HomeScreenTest`) + new Robolectric routing test.
- **Acceptance Criteria:** Existing `testTag`s (`homeSelectedVehicleCard`, `homeVehicleSelector`, `vehicleAddButton`) preserved; switcher opens sheet; «مدیریت خودروها» opens `VehicleList`; back returns cleanly.
- **Definition of Done:** Tests + `lintDebug` + `compileDebugAndroidTestKotlin` pass.
- **Rollback Plan:** Revert routing/insets diff.

#### Slice P0.4: Reminder Critical Bug Fixes (`reminder_missing_mileage` & Accidental Cancel) + Entry Form Defaults & Direct Recent Click
- **Objective:** Fix three high-impact daily-use bugs before feature expansion.
- **Problem Solved:**
  1. `RemindersScreen.kt:367-373` unconditionally displays «تا زمانی که کیلومتری برای این خودرو ثبت نشود...» whenever `Mileage` tab is selected—even when `list?.mileage != null`!
  2. `RemindersScreen.kt:619` cancels active reminders irreversibly on single tap with zero confirmation dialog.
  3. `LedgerScreen.kt:747` defaults `detailsExpanded = true` on new entries, adding clutter; `HomeScreen.kt:573` tapping a recent entry opens History list instead of that specific entry.
- **Requirement IDs:** `FR-015`, `FR-031`, `FR-032`, `UX-007`
- **Affected Files:**
  - `app/src/main/java/org/unishiasy/vehiclelog/ui/reminders/RemindersScreen.kt`
  - `app/src/main/java/org/unishiasy/vehiclelog/ui/reminders/RemindersViewModel.kt`
  - `app/src/main/java/org/unishiasy/vehiclelog/ui/ledger/LedgerScreen.kt`
  - `app/src/main/java/org/unishiasy/vehiclelog/ui/ledger/LedgerViewModel.kt`
  - `app/src/main/java/org/unishiasy/vehiclelog/ui/home/HomeScreen.kt`
  - `app/src/main/res/values/strings.xml`
- **Dependencies:** P0.3.
- **UX Impact:** Eliminates false warnings on mileage reminders, prevents accidental reminder deletion, streamlines quick expense entry, makes Home's Recent Entries directly actionable.
- **Data / Schema / Backup Risks:** None (read/UI & state guard only).
- **Implementation Steps:** Gate `reminder_missing_mileage` on `list?.mileage == null`; add `pendingCancelId` + `AlertDialog` confirmation in `RemindersScreen`; default `detailsExpanded` to `draft.editing && (!draft.mileage.isBlank() || !draft.notes.isBlank()) || state.fieldErrors.containsKey(EntryField.Mileage)` (and auto-expand if `MileageConflict` occurs); wire `RecentEntryRow` tap to open entry directly (`ledger.openEntryFromHome(...)`).
- **Verification & Tests:** Unit + Robolectric tests for `RemindersScreen` conditional hint, cancel confirmation dialog, and `EntryForm` initial/error accordion state.
- **Acceptance Criteria:** Hint hidden when mileage exists; cancel requires confirm; accordion collapses for clean new entry and expands on mileage conflict.
- **Definition of Done:** All tests & lint pass.
- **Rollback Plan:** Revert UI/VM slice cleanly.

---

### P1 — Core Product Completeness, Testability & Cafe Bazaar Hardening

#### Slice P1.1: Robolectric JVM Test Harness Setup (Enabling Real UI + Room + WorkManager Verification Here)
- **Objective:** Add Robolectric + Compose JVM UI test + Room migration test execution under `app/src/test/` so every UI, Room, and WorkManager slice can be genuinely executed in AI Studio (where no ADB/emulator exists).
- **Problem Solved:** Currently 17 critical test files live in `androidTest/` and compile-only without runtime execution in CI/AI Studio.
- **Requirement IDs:** `REL-001`
- **Affected Files:**
  - `gradle/libs.versions.toml`, `app/build.gradle.kts` *(requires dependency review via `dependency-advisor` and Product Owner approval per `AGENTS.md`)*
  - `app/src/test/java/org/unishiasy/vehiclelog/ui/AppJourneyRobolectricTest.kt`
  - `app/src/test/java/org/unishiasy/vehiclelog/data/local/VehicleDatabaseRobolectricTest.kt`
- **Dependencies:** P0 complete + explicit owner approval for adding `robolectric` & `androidx.compose.ui:ui-test-junit4` as `testImplementation` dependencies (zero production APK impact!).
- **UX Impact:** None directly; unlocks verified automated regression safety for every P1/P2 change.
- **Data / Schema / Backup Risks:** None (`testImplementation` only).
- **Implementation Steps:** Validate dependencies with `dependency-advisor`; obtain approval; configure `testOptions.unitTests.isIncludeAndroidResources = true`; port smoke/Room/CUJ coverage into fast Robolectric JVM tests.
- **Verification & Tests:** `gradle :app:testDebugUnitTest` executes both pure JUnit and Robolectric Compose/Room tests.
- **Acceptance Criteria:** Robolectric Room v1->v4 migration check and Compose Home/Ledger/Reminders smoke CUJs execute and pass on JVM.
- **Definition of Done:** `testDebugUnitTest` passes with Robolectric suites active.
- **Rollback Plan:** Revert Gradle test dependency edits.

#### Slice P1.2: Process-Death & Configuration Navigation/Draft Restoration (`SavedStateHandle`)
- **Objective:** Replace fragile Activity-only `MutableStateFlow` route/draft state so user screens, selected tabs, and unsaved entry/vehicle/reminder drafts survive Android background process kill.
- **Problem Solved:** OS background kill currently resets the user back to `HomeScreen` and discards partially typed expense/reminder forms.
- **Requirement IDs:** `UX-004`
- **Affected Files:**
  - `MainActivity.kt`, `AppContainer.kt`
  - `LedgerHost`, `LedgerViewModel.kt`, `VehicleViewModel.kt`, `RemindersViewModel.kt`
  - `EntryDraft.kt`, `ReminderDraft.kt` (`Parcelable` / JSON-serialized state keys)
- **Dependencies:** P1.1 (Robolectric `ActivityScenario` recreation tests).
- **UX Impact:** Seamless resumption after switching to calculator/SMS/camera while logging an expense.
- **Data / Schema / Backup Risks:** None; **strictly exclude** `CloudSettingsViewModel.recovery` secret state from `SavedStateHandle` (per `AGENTS.md` rule).
- **Implementation Steps:** Introduce lightweight `AbstractSavedStateViewModelFactory` wiring in `MainActivity` or explicit state bundle saving for active tab (`home`/`history`/`dashboard`/`reminders`/`settings`/`vehicles`) and active draft (`EntryDraft`, `VehicleDraft`, `ReminderDraft`). Verify `CloudSettingsViewModel` stays out of `SavedStateHandle`.
- **Verification & Tests:** Robolectric `ActivityController` destroy/recreate test preserving active tab + partially edited `EntryDraft`.
- **Acceptance Criteria:** Rotating or recreating `MainActivity` preserves tab, active vehicle, and form text without duplicating saves.
- **Definition of Done:** Recreation tests pass.
- **Rollback Plan:** Revert ViewModel factory / state saving changes.

#### Slice P1.3: Responsive 150%-Font Form Layouts, Live Toman Formatting & Jalali Date Picker
- **Objective:** Upgrade `EntryFormScreen` and `ReminderForm` into truly effortless Persian data-entry experiences.
- **Problem Solved:**
  1. Fuel paired `Row { Amount; Volume }` clips labels at 150% font scale or `320dp` width.
  2. Toman typing lacks live thousand separators (`250000` -> `۲۵۰٬۰۰۰`) so large numbers are error-prone.
  3. Dates require manual `YYYY/MM/DD` typing without a Jalali calendar or Today/Yesterday quick chips.
- **Requirement IDs:** `FR-011`, `FR-012`, `UX-003`
- **Affected Files:**
  - `app/src/main/java/org/unishiasy/vehiclelog/domain/Money.kt` (Toman display formatting helper)
  - `app/src/main/java/org/unishiasy/vehiclelog/ui/components/TomanVisualTransformation.kt` (new)
  - `app/src/main/java/org/unishiasy/vehiclelog/ui/components/JalaliDatePickerDialog.kt` (new pure-Kotlin Jalali calendar modal backed by `JalaliDates.kt`)
  - `app/src/main/java/org/unishiasy/vehiclelog/ui/ledger/LedgerScreen.kt`
  - `app/src/main/java/org/unishiasy/vehiclelog/ui/reminders/RemindersScreen.kt`
  - `app/src/main/res/values/strings.xml`
- **Dependencies:** P0.4, P1.1.
- **UX Impact:** Fast 1-tap Today/Yesterday/calendar selection, readable spaced Toman numerals with cursor-safe `OffsetMapping`, zero clipping at 150% font scale.
- **Data / Schema / Backup Risks:** Low — `VisualTransformation` affects presentation only; persisted `amountRials: Long` and `occurredOn: String` formats remain 100% unchanged.
- **Implementation Steps:** Implement `TomanVisualTransformation` (preserves numeric parsing); add adaptive `BoxWithConstraints` stacking when `fontScale >= 1.35f || maxWidth < 360.dp`; build lightweight pure-Compose Jalali calendar grid using existing `JalaliDates` civil math + «امروز» / «دیروز» chips.
- **Verification & Tests:** Unit tests for `OffsetMapping` cursor stability across Persian/ASCII/comma edge cases; Robolectric UI test at `fontScale = 1.5f` and date picker selection.
- **Acceptance Criteria:** Zero `ArithmeticException`/offset out-of-bounds on typing/deleting; Fuel form stacks cleanly at 150% font scale; Jalali picker selects valid dates across leap years (Esfand 29/30).
- **Definition of Done:** Unit + Robolectric tests + `lintDebug` pass.
- **Rollback Plan:** Revert new UI components without affecting DB/codec.

#### Slice P1.4: Standalone Odometer Logging UI
- **Objective:** Expose the already-implemented & tested `LedgerRepository` standalone odometer operations (`entryId = null`) in the UI.
- **Problem Solved:** Drivers cannot update mileage directly without faking an expense entry, breaking real-world mileage-based reminders and Home odometer accuracy.
- **Requirement IDs:** `FR-014`
- **Affected Files:**
  - `app/src/main/java/org/unishiasy/vehiclelog/ui/home/HomeScreen.kt` (add «به‌روزرسانی کیلومتر» CTA on Odometer panel)
  - `app/src/main/java/org/unishiasy/vehiclelog/ui/ledger/OdometerSheet.kt` (new modal/dialog supporting Add / Edit / Delete standalone readings + conflict error message)
  - `app/src/main/java/org/unishiasy/vehiclelog/ui/ledger/LedgerViewModel.kt` or `HomeViewModel.kt`
  - `app/src/main/res/values/strings.xml`
- **Dependencies:** P0.3, P1.3 (reuses Jalali date chips/picker).
- **UX Impact:** Drivers can keep odometer current in 2 taps directly from Home.
- **Data / Schema / Backup Risks:** Uses existing `LedgerRepository.createStandaloneReading`, `updateStandaloneReading`, `deleteStandaloneReading` and existing `odometer_readings` schema—no schema change.
- **Implementation Steps:** Wire standalone odometer draft/save/conflict handling; add list/edit affordance for standalone readings if needed; update Home mileage & due reminders reactively.
- **Verification & Tests:** Robolectric Room + UI test covering valid save, chronological conflict rejection, edit, and delete.
- **Acceptance Criteria:** Standalone odometer persists, updates `observeCurrentMileage`, triggers due mileage reminders when crossed, and shows conflict message on out-of-order date/km.
- **Definition of Done:** Tests + `lintDebug` pass.
- **Rollback Plan:** Remove UI trigger & sheet while keeping repository intact.

#### Slice P1.5: Read-Only Record Detail Sheet + History Search, Category Filter & Month Grouping
- **Objective:** Make `HistoryView` genuinely useful as a multi-month vehicle notebook and separate inspecting a record from editing it.
- **Problem Solved:**
  1. Tapping a record immediately enters edit mode, risking accidental changes.
  2. `HistoryView` has no search, no category filter, and no Jalali month filter/grouping; redundant `"بازگشت به خودروها"` button sits at the bottom.
- **Requirement IDs:** `FR-015`, `FR-020`
- **Affected Files:**
  - `app/src/main/java/org/unishiasy/vehiclelog/ui/ledger/HistoryFilterState.kt` (new pure filter/grouping logic)
  - `app/src/main/java/org/unishiasy/vehiclelog/ui/ledger/EntryDetailSheet.kt` (new read-only detail sheet with Edit + Confirmed Delete)
  - `app/src/main/java/org/unishiasy/vehiclelog/ui/ledger/LedgerScreen.kt`
  - `app/src/main/java/org/unishiasy/vehiclelog/ui/ledger/LedgerViewModel.kt`
  - `app/src/main/res/values/strings.xml`
- **Dependencies:** P0.4.
- **UX Impact:** Scannable month headers on the day rail, instant filtering by category («سوخت»، «تعویض روغن»، ...) or keyword in notes, safe read-only detail inspection from both Home and History.
- **Data / Schema / Backup Risks:** None (in-memory derivation over vehicle's observed `HistoryEntry` list; zero DB changes).
- **Implementation Steps:** Create `HistoryFilterState` & unit-test Persian digit/month normalization; add compact search bar + horizontal category filter chips + Jalali month headers; add `EntryDetailSheet` triggered by card tap (preserving `ledgerRecent:{id}` click compatibility or updating `LedgerEntryUiTest` only if contract changes intentionally). *Note: preserve existing `testTag`s cleanly.*
- **Verification & Tests:** Pure unit tests on `HistoryFilterState` + Robolectric UI tests for filtering, empty-filter reset, and detail -> edit/delete flow.
- **Acceptance Criteria:** Filtering works offline with Persian/Arabic numerals in query; empty filter state offers reset button; detail sheet displays all non-empty fields clearly.
- **Definition of Done:** Unit + Robolectric tests + `lintDebug` pass.
- **Rollback Plan:** Revert filter/detail UI additions.

#### Slice P1.6: Reminder Editing & Recurring Next-Interval Prompt
- **Objective:** Allow drivers to edit active reminders and optionally schedule the next interval when completing a recurring service (e.g., oil change every 5,000 km or 6 months).
- **Problem Solved:** Completing or mistyping a reminder currently forces manual re-entry from scratch.
- **Requirement IDs:** `FR-032`, `FR-033`
- **Affected Files:**
  - `app/src/main/java/org/unishiasy/vehiclelog/data/ReminderDao.kt` & `ReminderRepository.kt` (check if active reminder update method exists or add safe SQL `UPDATE reminders SET title = ..., due_on = ..., due_kilometers = ..., updated_at = ... WHERE id = ... AND state = 'active'`, preserving schema v4)
  - `app/src/main/java/org/unishiasy/vehiclelog/ui/reminders/RemindersViewModel.kt`, `RemindersScreen.kt`, `ReminderDraft.kt`
  - `app/src/main/res/values/strings.xml`
- **Dependencies:** P0.4, P1.3.
- **UX Impact:** Edit button on active reminders; after marking «انجام شد», optional lightweight «ثبت دوره بعدی» action pre-populated with interval (+5,000 km or +6 months).
- **Data / Schema / Backup Risks:** Zero schema changes (`updated_at` and `due_on`/`due_kilometers` already exist). Validate `due_on XOR due_kilometers` strictly before DB write.
- **Implementation Steps:** Add `updateActiveReminder` in DAO/Repository + unit/Room tests; add edit mode to `ReminderForm` (collapsible/modal card); add post-complete recurring prompt.
- **Verification & Tests:** Robolectric Room DAO test (`updateActive` rejects completed/canceled reminders) + ViewModel/UI test.
- **Acceptance Criteria:** Editing active reminder preserves ID and notification schedule; non-active reminder edit returns `NoActiveReminder`.
- **Definition of Done:** Tests + `lintDebug` pass.
- **Rollback Plan:** Revert edit/recurring UI & DAO method.

#### Slice P1.7: Last-Backup Reminder, About / Privacy / Version Info & Cafe Bazaar Release Hardening (`R8` + Build Script)
- **Objective:** Close the final Cafe Bazaar publication gaps in Settings and Gradle release builds.
- **Problem Solved:**
  1. `app/build.gradle.kts` has `isMinifyEnabled = false` and no `proguard-rules.pro`.
  2. `scripts/release-apk.sh` requires JDK 17 (unavailable on JDK 21 hosts) and leaves backup/privacy disclosures buried.
  3. Users have no "Last Backup Date" visibility and no "About / Privacy / Version" screen in Settings.
- **Requirement IDs:** `FR-041`, `FR-042`, `PERF-003`, `SEC-001`, `SEC-002`, `REL-001`
- **Affected Files:**
  - `app/build.gradle.kts` (`isMinifyEnabled = true`, `isShrinkResources = true`, `proguard-rules.pro` for `release` — with explicit product-owner review!)
  - `app/proguard-rules.pro` (new — kotlinx.serialization, Room, WorkManager keep rules)
  - `app/src/main/java/org/unishiasy/vehiclelog/ui/settings/BackupPreferences.kt` (new — stores last export timestamp + stale check)
  - `app/src/main/java/org/unishiasy/vehiclelog/ui/settings/BackupSettingsScreen.kt` (last backup badge + About/Privacy/Offline card showing `BuildConfig.VERSION_NAME` & `VERSION_CODE`)
  - `app/src/main/java/org/unishiasy/vehiclelog/ui/home/HomeScreen.kt` (subtle dismissible stale-backup banner if entries > 0 and > 14 days since last export)
  - `app/src/main/res/values/strings.xml`
- **Dependencies:** P1.1.
- **UX Impact:** Builds driver trust with clear offline privacy commitment, visible version info, and timely backup reminders.
- **Data / Schema / Backup Risks:** Low — timestamp stored in `SharedPreferences` (`vehicle_log_backup_meta`). Verify R8 does not strip `BackupSnapshot` `@Serializable` serializers or Room `_Impl` generated classes!
- **Implementation Steps:** Create `proguard-rules.pro`, enable R8 on `release`, verify `gradle :app:assembleRelease` succeeds and inspect merged release `AndroidManifest.xml` for zero `INTERNET` permission; add last-export timestamp + About/Privacy card.
- **Verification & Tests:** `gradle :app:assembleRelease :app:lintDebug :app:testDebugUnitTest` + Robolectric BackupSettings test verifying timestamp update on `BackupOutcome.Exported`.
- **Acceptance Criteria:** Release APK compiles with R8 enabled; `aapt`/`apkanalyzer` or merged manifest confirms zero `INTERNET` permission; backup export updates visible Jalali last-backup date.
- **Definition of Done:** Minified release APK builds cleanly and passes all automated checks.
- **Rollback Plan:** Toggle `isMinifyEnabled = false` if any R8 blocker arises while troubleshooting keep rules.

---

### P2 — Differentiation & Analytical Value

#### Slice P2.1: Monthly Report Upgrades (Descending Sort + Fuel Efficiency `L/100km` & Cost/km)
- **Objective:** Sort category spending largest-first and surface actionable fuel consumption (`L/100km`) & cost-per-km stats when sequential odometer + fuel data exist.
- **Problem Solved:** Fixed enum sorting hides top cost drivers; fuel volume + odometer data sit unused in analytics.
- **Requirement IDs:** `FR-021`, `FR-022`
- **Affected Files:**
  - `app/src/main/java/org/unishiasy/vehiclelog/data/MonthlyReporting.kt`
  - `app/src/main/java/org/unishiasy/vehiclelog/data/FuelEfficiency.kt` (new pure domain calculator)
  - `app/src/main/java/org/unishiasy/vehiclelog/ui/ledger/MonthlyDashboardScreen.kt`
  - `app/src/main/res/values/strings.xml`
- **Dependencies:** P1.1.
- **UX Impact:** High real-world value for professional/daily Iranian drivers monitoring fuel economy & top expense categories.
- **Data / Schema / Backup Risks:** Zero DB/backup changes (computed read-only from existing `entries` + `odometer_readings`).
- **Implementation Steps:** Define explicit transparent algorithm (consecutive dated fuel fill-ups with monotonic linked odometer readings; reject negative/zero delta cleanly); add pure JVM unit tests + `MonthlyReportingTest` update + Dashboard UI card.
- **Verification & Tests:** Comprehensive `FuelEfficiencyTest` (normal, single entry, zero delta, gap/non-monotonic readings, Persian numeric formatting).
- **Acceptance Criteria:** Largest category shown first; L/100km matches exact reference calculations; gracefully omitted/explained when insufficient readings.
- **Definition of Done:** Unit + Robolectric tests pass.
- **Rollback Plan:** Revert analytics calculator/card.

#### Slice P2.2: Guarded Vehicle Archive/Delete & Optional Bundled Iranian Vehicle Catalog
- **Objective:** Address multi-year lifecycle management (retiring an old car safely) and fast onboarding via a verified bundled catalog of popular Iranian vehicles (if approved).
- **Problem Solved:** Users cannot remove a mistakenly created or sold vehicle; `VehicleCatalogLoader` infrastructure exists (`approvedCatalogAsset = null`) but ships empty.
- **Requirement IDs:** `FR-004`
- **Affected Files:** `VehicleDao.kt`, `VehicleRepository.kt`, `VehicleScreen.kt`, `AppContainer.kt`, `app/src/main/assets/` *(if catalog approved)*
- **Dependencies:** P0.3, P1.1 + **explicit Product Owner approval** (because deleting linked data impacts `ON DELETE RESTRICT` semantics or requires schema/cascade decision).
- **UX Impact:** Faster vehicle setup + clean garage management.
- **Data / Schema / Backup Risks:** Medium — transactional delete (`entries` + `odometer_readings` + `reminders` + `vehicle`) requires irreversible confirmation dialog & backup suggestion first.
- **Implementation Steps:** Decide Archive vs Delete-with-Backup-Prompt with Product Owner; implement & test thoroughly.

---

### P3 — Cafe Bazaar Pre-Launch QA & Cloud Decision

#### Slice P3.1: Physical Device QA, TalkBack Walkthrough & Cafe Bazaar Listing Readiness
- **Objective:** Close remaining physical verification & store submission items.
- **Tasks:**
  1. Run physical Android device test plan (`TEST_STRATEGY.md`), TalkBack RTL walkthrough, 150% font scale visual review, dark mode review, SAF export -> reinstall -> restore drill, and WorkManager due notification test on real device.
  2. Verify live Cafe Bazaar Developer Console requirements (`[Pending verification — live Cafe Bazaar developer console check required]`: targetSdk policy, APK vs AAB acceptance, Bazaar-specific release keystore management, data safety/privacy declaration).
  3. Confirm product owner decision on `applicationId` (`org.amirariana.vehiclelog` vs `org.unishiasy.vehiclelog`), Play/Bazaar signing key storage outside repo, and whether debug-only `cloud/` package remains debug-only or is removed/deferred.

---

## Explicit Decision & Approval Gates

| Gate | Trigger Before Execution | Requires Owner Decision On |
| :--- | :--- | :--- |
| **Gate G0 (Immediate)** | Before starting P0.1 | Approval of this `PRODUCTIZATION_PLAN.md` & permission to start P0.1–P0.4. |
| **Gate G1 (Test Dependencies)** | Before P1.1 | Approval to add `robolectric` & `ui-test-junit4` (`testImplementation` only) in `libs.versions.toml` / `app/build.gradle.kts`. |
| **Gate G2 (Release Config)** | Before P1.7 | Confirm release R8 minification (`isMinifyEnabled = true`) & finalize `applicationId` (`org.amirariana.vehiclelog`). |
| **Gate G3 (Data Lifecycle)** | Before P2.2 | Decide Vehicle Delete (transactional cascade with confirm) vs Soft-Archive (requires Room v5 migration), and whether to ship a bundled Iranian vehicle catalog JSON. |
