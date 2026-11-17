# Vehicle Ledger (دفتر خودرو) — Cafe Bazaar Release Gap Analysis

**Assessment Date:** 2026-11-17
**Target Store:** Cafe Bazaar (Iran)
**Current App Baseline:** `applicationId = org.mewlog.vehiclelog`, `versionCode = 1`, `versionName = "0.1.0"`, `minSdk = 26`, `targetSdk = 36`

> **Verification Discipline:** Repository facts below are directly verified against Gradle, Manifest, Kotlin source, and build/lint runs. External Cafe Bazaar console rules that cannot be queried from this offline build environment are explicitly tagged `[Pending verification — live Cafe Bazaar developer console check required]`.

---

## Summary Verdict: NOT YET READY FOR CAFE BAZAAR SUBMISSION

The offline-first Room v4 ledger core, Jalali date engine, exact rial/Toman arithmetic, SAF JSON backup codec, and adaptive Persian RTL theme are strong engineering foundations. However, **critical release, lint, UX correctness, test-execution, and distribution-config gaps remain** before cutting a public `v1.0` APK.

---

## Category-by-Category Gap Audit

| # | Area | Current Verified State | Severity | Required Action Before Release |
| :--- | :--- | :--- | :--- | :--- |
| **1** | **Package Name (`applicationId`) Consistency** | `app/build.gradle.kts` & `scripts/release-apk.sh` updated to **`org.mewlog.vehiclelog`** per owner decision (`namespace = "org.unishiasy.vehiclelog"`). | **Resolved (Frozen)** | Keep `org.mewlog.vehiclelog` permanently locked prior to and after Cafe Bazaar publication. |
| **2** | **Versioning & Changelog** | `versionCode = 1`, `versionName = "0.1.0"`. | **Low** | Bump to `versionCode >= 2` / `versionName = "1.0.0"` for public v1.0 release after passing all P0/P1/P3 gates. |
| **3** | **Android Lint Baseline (`:app:lintDebug`)** | **FAILING** (`CloudSettingsScreen.kt:68` `ContextCastToActivity` error + 3 `UseKtx` + 10 `UnusedResources` + 10 outdated dependency warnings). | **P0 Blocker** | Fix `LocalActivity.current` error, KTX calls, and unused string audit in **Slice P0.1** so `:app:lintDebug` passes with 0 errors. |
| **4** | **R8 / ProGuard Minification & Shrinking** | `release` buildType sets `isMinifyEnabled = false`; no `proguard-rules.pro` exists (`app/build.gradle.kts:31-37`). | **P1 High** | Enable `isMinifyEnabled = true` + `isShrinkResources = true` with verified ProGuard/R8 rules (`kotlinx.serialization`, Room, WorkManager) in **Slice P1.7**; reduces APK size and prevents trivial decompilation. |
| **5** | **JDK / Release Build Reproducibility** | Host container runs JDK 21; `scripts/release-apk.sh` demands JDK 17 (`JAVA_HOME` check fails in JDK 21-only environments). `gradle.properties` JVM flags should be verified for clean CI/release builds. | **Medium** | Update `scripts/release-apk.sh` (with owner approval) or document JDK 17/21 compatibility so release APK builds cleanly outside hardcoded single-JDK paths. |
| **6** | **Signing Keys & Keystore Custody** | Debug signing uses repo `debug.keystore`; release signing requires external env vars (`VEHICLE_LOG_KEYSTORE_PATH`, etc. in `release-apk.sh` / `build.gradle.kts`). | **Critical (Ops)** | Keep release keystore strictly outside repo, backed up in encrypted vault with `keytool` SHA-1/SHA-256 recorded. Confirm Cafe Bazaar App Signing / self-signed APK policy `[Pending verification — live Cafe Bazaar developer console check required]`. |
| **7** | **Target SDK & Cafe Bazaar Store Policies** | `compileSdk = 36`, `targetSdk = 36`, `minSdk = 26`. Zero payment SDKs, zero ad SDKs, zero analytics, zero Play Services dependencies (`play-creator-track-policy` compliant). | **Low (Check Console)** | `[Pending verification — live Cafe Bazaar developer console check required]`: Confirm Cafe Bazaar's current required `targetSdk`, APK vs AAB upload requirement, Persian app-store metadata rules, and whether `Bazaar In-App Billing` or update checker is desired (not needed for free offline utility). |
| **8** | **Permissions & Manifest Hygiene** | Release `AndroidManifest.xml` requests ONLY `POST_NOTIFICATIONS` (optional runtime). `INTERNET` + cleartext `127.0.0.1` live strictly in `app/src/debug/AndroidManifest.xml`. `android:allowBackup="false"` + restrictive `data_extraction_rules.xml`. | **Pass (Verify Release Merge)** | Inspect merged `release` manifest after **P1.7** (`assembleRelease`) to verify zero `INTERNET` or `usesCleartextTraffic` leaks into release APK. |
| **9** | **Privacy Policy & In-App Disclosures** | `backup_export_explanation` warns on plaintext JSON in Settings, but **no dedicated About / Privacy Policy / Version screen exists**, and no explicit privacy link/page exists for store listing. | **P1 High** | Add In-App About / Offline Privacy screen (**P1.7**) and prepare public Persian Privacy Policy URL for Cafe Bazaar store form (even though zero data is collected offline, stores commonly require a clear privacy statement). |
| **10** | **Database Safety & Backup Continuity** | Room v4 exported schemas (`1..4.json`), atomic transactions, `fallbackToDestructiveMigration` absent. Backup v1 JSON strictly validated & capped at 10 MiB. | **Pass (Add Restore UX Polish)** | Preserve Room v4 & Backup v1 strictly. Add last-backup timestamp + stale-backup nudge (**P1.7**) so users don't forget to export before factory reset. |
| **11** | **Core UX Completeness & Daily Bugs** | Unreachable `VehicleSwitcherSheet` & `VehicleList`; false `reminder_missing_mileage` warning when mileage exists; accidental single-tap reminder cancel; missing standalone odometer UI; missing History filters & Record Detail view; Fuel row font-scale risk; `Home` due badge white-on-amber contrast failure; `36dp`/`40dp` tap targets. | **P0 / P1 High** | Resolve all P0 items (**P0.2–P0.4**) and P1 workflow items (**P1.2–P1.6**) before release cut. |
| **12** | **Crash / State Recovery (Process Death)** | Activity-scoped ViewModels lose navigation destination & unsaved drafts on background process death. | **P1 High** | Implement `SavedStateHandle` / bundle persistence for routes & active drafts (**P1.2**), keeping cloud recovery secrets excluded. |
| **13** | **Automated Test Execution Gap** | 94 pure JVM unit tests run & pass (`testDebugUnitTest`). All 17 Room/Compose/WorkManager tests sit in `androidTest/` and **cannot execute** in AI Studio (no emulator/ADB). Zero Robolectric tests exist yet. | **P1 High** | Add Robolectric + Compose UI JVM test harness (**P1.1**) so UI, Room migrations, and WorkManager run continuously without requiring an emulator. |
| **14** | **Localization, Font & Persian RTL Polish** | `Vazirmatn` bundled locally (`res/font/vazirmatn.ttf`), `supportsRtl="true"`, Persian strings & digits applied. Minor inconsistencies: `Home` recent liter uses `Locale.US` `.` instead of `٫`; some screens mix `safeDrawingPadding()` and `Scaffold`. | **P0 / P1 Medium** | Standardize `Scaffold` insets (**P0.3**) and decimal formatting (`Product Design System` §2). |
| **15** | **App Icon & Store Visual Assets** | Custom vector adaptive icon (`ic_launcher_foreground.xml` + `@color/ic_launcher_background` + monochrome) exists. | **Low (Visual Review)** | Review adaptive icon rendering across circle/squircle/rounded-square launcher masks on real devices; prepare 512×512 store icon, feature graphic, and localized RTL screenshots for Cafe Bazaar (**P3.1**). |
| **16** | **Device Compatibility Matrix** | Supports `minSdk = 26` (Android 8.0 Oreo) through `targetSdk = 36` (Android 16); pure Kotlin/Compose + SQLite (bundled Room/AndroidX, no custom native `.so` ABIs limiting arm64/armeabi-v7a/x86_64). | **Medium (Unverified on Hardware)** | Execute physical QA matrix across Android 8–15, small `320dp–360dp` screens, dark/light theme, TalkBack, and 150% font scale (**P3.1**). |
| **17** | **Debug-Only `cloud/` Source Footprint** | `data/cloud/` + `CloudSettingsScreen` remain compiled into APK (gated by `BuildConfig.LOCAL_CLOUD_ENABLED = false`). | **Low (Keep Guarded)** | Confirm with Product Owner whether to retain gated `cloud/` code for future v1.1 or strip unused cloud UI strings/flows from release cleanly (`SEC-004`). |

---

## Pre-Submission Exit Checklist (Go / No-Go)

- [ ] **P0 Complete:** `:app:lintDebug` = 0 errors; contrast & 48dp touch targets compliant; vehicle management/switcher & reminder/entry bugs fixed.
- [ ] **P1 Complete:** Robolectric UI/DB tests passing; `SavedStateHandle` process-death recovery tested; Jalali picker, live Toman formatting, standalone odometer UI, History filter/detail, Reminder edit/confirm, About/Privacy & backup timestamp implemented; R8 minified `assembleRelease` passes with zero `INTERNET` permission.
- [ ] **P3 Physical QA Complete:** Real device TalkBack, 150% font scale, dark mode, SAF export/reinstall/restore, and notification deep-link walkthrough signed off.
- [ ] **Cafe Bazaar Console Confirmed:** `[Pending verification — live Cafe Bazaar developer console check required]` items verified (signing, AAB/APK format, privacy URL, store listing assets).
