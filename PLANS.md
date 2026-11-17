# Android MVP execution plan

The solo-developer Android technical baseline is approved. **M1 and M2 are formally complete.** The developer approved a one-time exception for the signed `0.1.0` offline cold-launch check. M2 closed under the revised scope recorded below.

- [Product scope](docs/product/MVP_SCOPE.md) and [pilot metrics](docs/product/SUCCESS_METRICS.md)
- [Architecture](docs/architecture/SYSTEM_ARCHITECTURE.md), [data design](docs/architecture/DATABASE_DESIGN.md), and [API design](docs/architecture/API_DESIGN.md)
- [Roadmap](docs/delivery/ROADMAP.md), [backlog](docs/delivery/BACKLOG.md), [test strategy](docs/delivery/TEST_STRATEGY.md), [risks](docs/delivery/RISK_REGISTER.md), and [estimates](docs/delivery/COST_ESTIMATE.md)
- [Assumptions](docs/research/ASSUMPTIONS.md) and [open decisions](docs/research/OPEN_QUESTIONS.md)

All effort ranges are provisional and count focused engineering person-days. Business research time is recorded separately. Android paths below use `main/`, `test/`, and `androidTest/` for the corresponding `app/src/` source sets. Kotlin paths are relative to the source set's application package.

For each milestone, record actual time, test commands and results, physical device model and OS, APK version, blockers, and the completion decision. The next milestone opens only after the applicable automated and physical checks pass.

## M1. First signed APK

**Status:** Complete. The signed install and in-place upgrade passed, the developer confirmed signed `0.1.1` offline startup, and the isolated clean-source debug build passed. Signed `0.1.0` offline startup is recorded as **Waived — Accepted Residual Risk**. M2–M7 have been re-estimated provisionally. **Original provisional effort:** 3–5 days. Actual focused time was not fully tracked.

**Dependencies:** Build machine and phone, developer-controlled test signing key, and approval of initial production dependencies. `org.amirariana.vehiclelog` is provisional for controlled testing. Confirm the permanent application ID before the first public release. **Risk:** Toolchain incompatibility, blocked dependency downloads, unauthorized USB debugging, or signature and version mismatch.

- [x] Pin the JDK, SDK, wrapper, and dependencies in `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`, `gradlew`, `gradlew.bat`, `gradle/wrapper/`, `app/build.gradle.kts`, and `.gitignore`. A fresh local debug build succeeded with the pinned tools.
- [x] Add `main/AndroidManifest.xml`, `MainActivity.kt`, `ui/theme/Theme.kt`, and `main/res/values/strings.xml` for the Persian RTL Home shell.
- [x] Add the Compose launch test, explicit Android 12+ extraction policy, [local setup guide](docs/delivery/ANDROID_SETUP.md), and `scripts/check-android.sh`.
- [x] Build the debug and instrumented APKs, run lint, and inspect the merged manifest, packaged backup rules, and debug signatures.
- [x] Run the instrumented Compose launch smoke test unattended on the English-language Redmi Note 9S.
- [x] Manually check offline cold launch, rotation, and enlarged text on the physical phone. All three passed as reported by the user.
- [x] Set the provisional release ID and verify the adaptive launcher icon, debug APK, instrumented APK, and merged release manifest.
- [x] Install the verified new debug APK on the Redmi alongside the earlier debug package and pass the unattended Compose test against the new package.
- [x] Visually confirm the adaptive icon in the Redmi's MIUI launcher.
- [x] Prepare `scripts/release-apk.sh`, release-only Gradle version overrides, and the [release guide](docs/delivery/ANDROID_RELEASE.md). Verify preflight and manifest behavior without producing a release APK.
- [x] Obtain approval for the first signing trial and verify the developer-owned test keystore preflight.
- [x] Produce and independently verify the first signed APK for controlled testing.
- [x] Produce and independently verify the second signed APK with the same certificate and a higher version code. Confirm that version 1 is byte-for-byte unchanged.
- [x] Obtain approval for installation and verify the in-place upgrade on the Redmi.
- [x] Record the developer-reported signed `0.1.1` cold launch with airplane mode enabled and Wi-Fi disabled.
- [x] Record a debug build from an isolated, hashed clean-source snapshot as the pre-commit equivalent of the clean-checkout build.
- [x] Record the developer-approved one-time exception for signed `0.1.0` offline startup as **Waived — Accepted Residual Risk**.

**Automated:** `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleDebug`, `androidTest/AppLaunchTest.kt`, and `apksigner verify`.

**Physical Android:** Install debug and signed builds, launch in airplane mode, rotate, enlarge text, and install a higher-version signed release in place.

**Done:** All checks pass and the release procedure can be repeated. Record actual setup, coding, debugging, review, signing, and physical testing time. Re-estimate M2–M7 before starting M2.

**Evidence and completion notes (2026-11-07):**

- The build used Corretto JDK 17.0.21, Gradle 8.13, AGP 8.13.2, Kotlin and Compose compiler plugin 2.3.10, Compose BOM 2025.12.00 (Compose UI and Foundation 1.10.0, Material 3 1.4.0), and Activity Compose 1.12.2. It used Android Platform 36 revision 2 and Build Tools 36.0.0. The app targets API 36 and supports API 26 and later.
- Initially `JAVA_HOME` and `ANDROID_HOME` were unset. Android Studio bundled JDK 25.0.4, which Gradle 8.13 cannot run on. The installed SDK had API 37 and Build Tools 36.0.0. Used a verified portable JDK 17 and temporary API 36 SDK. AGP resolved the base platform after detecting a mismatched extension archive. The installed JDK and SDK were left untouched.
- Ran `./gradlew --version` and `./gradlew --no-daemon --console=plain --max-workers=4 :app:testDebugUnitTest :app:lintDebug :app:assembleDebug`. The combined Gradle run succeeded in 6m 50s. The unit-test task reported `NO-SOURCE`. Lint reported 0 errors and 8 warnings for newer dependencies, launcher icon, and Android 12+ backup rules. Packaging emitted a non-blocking debug-symbol stripping warning for an AndroidX native library.
- `app/build/outputs/apk/debug/app-debug.apk` passed `apksigner verify --verbose` with an APK v2 debug signature. Package `org.unishiasy.vehiclelog.debug`, version `0.1.0`, version code 1, launcher `org.unishiasy.vehiclelog.MainActivity`, and Persian label verified from the packaged manifest. SHA-256 `466278fbc4eff5726eb223139ddd30fc237a50ae743aefe678f4fb52bbd6e66f`.
- The application ID is provisional until the permanent ID is confirmed for release. Physical testing, the instrumented smoke test, release signing and update checks, and the post-M1 re-estimate are pending.

**Approved verification slice preflight (2026-11-07):**

- `JAVA_HOME`, `ANDROID_HOME`, and `ANDROID_SDK_ROOT` are unset. The only persistent JDK found was Android Studio's JDK 25.0.4. A persistent JDK 17 is required for the pinned build.
- The local SDK at `~/Android/Sdk` has base Platform 37, Build Tools 36.0.0, and Platform Tools. Install the base Android SDK Platform 36 package (`platforms;android-36`) into that SDK. Reuse the existing Build Tools and Platform Tools.
- Local ADB recognized an authorized USB-connected Redmi Note 9S running Android 12 (API 31). The device check was read-only. The physical instrumented launch test and manual smoke checks were not run.
- Read-only inspection confirmed that the merged debug manifest and existing APK declare `allowBackup="false"` and lack `dataExtractionRules`. The source rules file is absent. The APK checksum matches the first slice. The last lint report remains 0 errors and 8 warnings, including the missing Android 12+ backup rules and launcher icon, plus six dependency version notices.
- Stopped before app changes under the toolchain constraint. No new build, lint, JVM, or instrumented test run was performed. Once the persistent tools are available, the approved slice still needs the launch test, explicit backup rules, local setup documentation and checks, and physical-device evidence.

**API 37 experiment (2026-11-07):**

- Verified persistent Corretto JDK 17.0.21 at `/usr/lib/jvm/java-17-amazon-corretto` and used the installed base API 37 platform and Build Tools 36.0.0.
- Ran offline `:app:assembleDebug` in a scratch copy with `compileSdk = 37`, `targetSdk = 36`, the pinned AGP 8.13.2 and Gradle 8.13, and toolchain downloads disabled. The build passed in 1m 8s. APK metadata confirmed compile SDK 37 and target SDK 36. The debug signature verified. AGP emitted its unsupported compile SDK and SDK metadata version warnings.
- The project remains pinned to API 36, and the existing workspace APK checksum still matches. This experiment did not run device tests. A supported API 37 migration requires at least AGP 9.1.1 and Gradle 9.3.1. [AGP compatibility](https://developer.android.com/build/releases/agp-9-1-0-release-notes)

**Resumed verification slice preflight (2026-11-08):**

- Verified persistent Corretto JDK 17.0.21 and the base Android Platform 36 revision 2, Build Tools 36.0.0, and Platform Tools in `~/Android/Sdk`.
- ADB recognized the USB-connected Redmi Note 9S on Android 12 (API 31), with the system locale set to English. This was a read-only preflight. The instrumented test was not run.
- The Gradle Wrapper is present and pins 8.13 with its distribution checksum. The previous temporary distribution is gone and the local search found no installed or cached Gradle 8.13. Automatic approval review rejected fetching the pinned distribution because the user requires separate approval for toolchain downloads. A local Gradle 8.13 distribution or explicit authorization to fetch the pinned ZIP is required.
- The existing APK checksum still matches. Its packaged manifest has `allowBackup="false"` and no `dataExtractionRules`. The last completed lint run remains at 0 errors and 8 warnings. Stopped at preflight under the missing-tools constraint. No app files changed and no new build, lint, or instrumented test ran.

**Authorized Gradle download attempt (2026-11-08):**

- After explicit approval, attempted to fetch only the wrapper's configured official Gradle 8.13 ZIP into the ignored `.gradle/local-user-home` cache. The request returned HTTP 503 on all attempts, including one retry requesting cache revalidation. A separate header check reached the official redirect to GitHub and then timed out. No complete ZIP was received, so the pinned SHA-256 could not be verified and Gradle was not run.
- ADB again recognized the Redmi Note 9S on Android 12 (API 31). The earlier debug APK is installed on the phone. The app, wrapper configuration, and existing APK were preserved. Implementation, build, lint, and instrumented testing remain pending until a verified distribution is available.

**Verified local Gradle and verification-slice implementation (2026-11-08):**

- The user-provided `gradle-8.13-bin.zip` matched the wrapper pin and Gradle's published SHA-256 `20f1b1176237254a6fc204d8434196fa11a4cfb387567519c61556e8710aed78`. The wrapper JAR also matched its official checksum. Copied and verified the ZIP in the ignored `.gradle/local-user-home` cache. The unmodified wrapper reported Gradle 8.13 and Corretto JDK 17.0.21 from the persistent JDK path. [Gradle release checksums](https://gradle.org/release-checksums/)
- Added the Compose JUnit4 launch test for the real Persian Home activity and RTL direction. The new test libraries apply only to instrumentation. The existing production dependency pins and application ID are unchanged.
- The source manifest retains `allowBackup="false"` and now references the extraction rules. Source XML validation and a standalone AAPT2 compile, link, and APK inspection passed. The packaged probe contains nine exclusions in each of the cloud backup and device transfer sections. A fresh Gradle-merged app manifest and lint result remain pending.
- `bash -n scripts/check-android.sh` passed. A preflight with Android Studio JDK 25 correctly failed before invoking Gradle. With the persistent JDK 17 and SDK Platform 36, the offline `./scripts/check-android.sh --device` run recognized the sole USB-connected Redmi Note 9S, then failed during Gradle configuration because the AGP 8.13.2 plugin marker is absent from the offline cache. No build, lint, JVM test, or on-device test task ran. The previously built debug APK checksum still matches.
- Awaiting authorization to resolve the pinned build and instrumentation dependencies from the configured Gradle repositories. Once resolved, run the build and lint, inspect the merged app manifest, and run the launch test on the Redmi Note 9S (Android 12, API 31). The existing debug installation may require a signing compatibility check before installing a rebuilt APK.

**Approved dependency resolution and host verification (2026-11-08):**

- Confirmed Gradle 8.13, Corretto JDK 17.0.21, base Platform 36, and Build Tools 36.0.0. Resolved the pinned build and Compose JUnit4 instrumentation dependencies from the configured repositories using standard Gradle caching. No dependency or repository changes were needed.
- Ran `./scripts/check-android.sh --online`. Both APKs assembled on the first run. Lint rejected `./` as a subdirectory path for database and shared-preferences exclusions and flagged the missing legacy backup attribute. Changed whole-domain paths to `.` and set `android:fullBackupContent="false"`.
- Reran the host checks successfully in 47s. `:app:assembleDebug` and `:app:assembleDebugAndroidTest` passed. `:app:testDebugUnitTest` reported `NO-SOURCE`. `:app:lintDebug` passed with 0 errors and 8 warnings for dependency versions, the pinned target API, and the launcher icon. No backup warnings remain. The SDK metadata warning was non-blocking. [Lint report](app/build/reports/lint-results-debug.html)
- Inspected the fresh Gradle-merged manifest and packaged debug APK. They retain the debug package ID and set both legacy backup flags to false, with the resource reference resolving to all 18 whole-domain exclusions across cloud backup and device transfer. The test APK declares AndroidJUnitRunner and targets the unchanged debug package. Both APKs verified with APK Signature Scheme v2 and the same current debug certificate.
- Debug APK SHA-256 `9469a00a52dd1b5b19e7303e393ee0c43d4305a7d486226d7a90253a18ab2169`. Instrumented APK SHA-256 `aa85d1a6e6a87aff9fb95e1fb1dab6b872f2ba2fc0527e7cc0ceb3d412306054`. Current debug certificate SHA-256 `2937210215654d1944357664cd8d2a45963855ebcbbf0e4e780fbf645055a58c`.
- ADB recognized the Redmi Note 9S on Android 12 (API 31), with English system locale. The installed APK checksum matches the first M1 build, whose debug certificate SHA-256 is `535bf6cdd4b27ac3064fa85192401caf27b78154c13fb096bb1414cd9956135a`. Its certificate differs from both rebuilt APKs. The installed app was preserved. `:app:connectedDebugAndroidTest` and the manual checks remain pending a compatible debug key or approval to replace that debug installation.

**Approved debug replacement on the Redmi (2026-11-08):**

- Confirmed the sole authorized USB device was a Redmi Note 9S on Android 12 (API 31), locale `en-US`. Verified the exact installed package `org.unishiasy.vehiclelog.debug` and the original APK checksum before removal. Verified the rebuilt APK checksum and matching current debug signatures on both APKs.
- Ran `adb uninstall org.unishiasy.vehiclelog.debug` against the verified Redmi only. It returned `Success`. Ran `adb install app/build/outputs/apk/debug/app-debug.apk`. It failed with `INSTALL_FAILED_USER_RESTRICTED: Install canceled by user`.
- Confirmed the debug package is absent and the Redmi was asleep with its lock screen active. Requested that the phone be unlocked and the install prompt accepted. The Redmi did not become awake and unlocked during a 100-second wait, so the retry was not sent. `:app:connectedDebugAndroidTest` did not run. Resume with the verified rebuilt APK after the phone is awake and available for installation.

**Physical Compose verification on the Redmi (2026-11-08):**

- After the user unlocked the Redmi Note 9S, installed the rebuilt APK with `adb install --no-streaming app/build/outputs/apk/debug/app-debug.apk`. Installation returned `Success`. The installed APK matched SHA-256 `9469a00a52dd1b5b19e7303e393ee0c43d4305a7d486226d7a90253a18ab2169`.
- Ran `./gradlew :app:connectedDebugAndroidTest` with Gradle 8.13, JDK 17, and Platform 36 on the USB-connected Redmi Note 9S running Android 12 (API 31), locale `en-US`. The runner started one test, then stalled before the assertions. A debugger stack showed `ActivityScenario` waiting in `Instrumentation.startActivitySync`.
- An explicit foreground launch of the same `MainActivity` via `adb shell am start -W` returned `Status: ok` and unblocked the runner. `AppLaunchTest.homeDisplaysPersianTextInRightToLeftLayout` passed. The JUnit report records **1 test, 0 failures, 0 errors, 0 skipped**, with 290.059 seconds for the case including the stall. Gradle reported `BUILD SUCCESSFUL in 6m 14s`. This run verifies the Persian text and RTL layout after foreground launch. An unattended instrumented launch remains unverified. [Connected test report](app/build/reports/androidTests/connected/debug/index.html)
- Gradle removed the debug APK during its test cleanup. Reinstalled the same debug APK and verified its installed SHA-256 again. The debug app is present on the Redmi. The application ID, signing configuration, and dependency pins were unchanged.

**Unattended launch investigation on the Redmi (2026-11-09):**

- Reviewed the test, `MainActivity`, `AndroidJUnitRunner`, and the prior XML and debugger evidence. The original Compose rule entered `ActivityScenario` and waited in `Instrumentation.startActivitySync` before the test body.
- Reproduced the stall without a foreground assist on the awake, unlocked Redmi Note 9S (Android 12, API 31, MIUI V140, `en-US`). Logcat recorded the launch from the debug app UID and `ActivityStarterImpl: MIUILOG- Permission Denied Activity` for the exact `MainActivity`. The app's `MIUIOP(10021)` showed `ignore` with a recent rejection. Stopped only the stalled debug process after collecting evidence. The baseline Gradle run ended in 2m 44s and reported the interrupted instrumentation as `Process crashed`. No Compose assertion ran in that baseline.
- Changed `AppLaunchTest.kt` to use an empty Compose rule and launch the real launcher activity automatically through `UiAutomation` with `am start -W`. The test requires `Status: ok` and still checks both visible Persian strings and RTL layout direction. Production code and device permissions were unchanged. This test covers foreground launcher startup. It does not establish that MIUI permits background activity launches. [UiAutomation reference](https://developer.android.com/reference/android/app/UiAutomation)
- Ran `:app:connectedDebugAndroidTest :app:lintDebug` offline with the pinned tools. Logcat recorded the shell-originated launch and `MainActivity` displayed in 650 ms. The JUnit report records **1 test, 0 failures, 0 errors, 0 skipped**, with 0.873 seconds for the case. No manual launch was sent. Gradle reported `BUILD SUCCESSFUL in 1m 7s`, and lint reported 0 errors and 5 warnings. [Connected test report](app/build/reports/androidTests/connected/debug/index.html)
- Reinstalled the debug APK after Gradle's test cleanup and verified the installed SHA-256 `9469a00a52dd1b5b19e7303e393ee0c43d4305a7d486226d7a90253a18ab2169`. Manual offline launch, rotation, enlarged-text checks, and the release-signing and upgrade work remain pending.

**User-reported manual device checks (2026-11-09):**

The user reported these manual results on the Redmi Note 9S running Android 12. They are separate from the instrumented Compose test evidence.

| Manual check | User-reported result |
|---|---|
| Offline cold launch in airplane mode | Passed |
| Screen rotation | Passed |
| Enlarged system font and readable Persian UI | Passed |

The permanent release application ID requires explicit approval before the first public release.

**Provisional release identity and launcher icon (2026-11-09):**

- The approved provisional release ID is `org.amirariana.vehiclelog`. The `.debug` suffix produces `org.amirariana.vehiclelog.debug`. Kotlin namespace and source packages use `org.unishiasy.vehiclelog`. Explicitly confirm the permanent ID before the first public release. [ADR 0003](docs/architecture/ADR/0003-signed-apk-distribution.md)
- Added an adaptive car-and-ledger icon with foreground, background, and monochrome layers. Previewed circular and rounded-square masks locally. The app's minimum SDK is 26, so the icon uses the unversioned `mipmap-anydpi` directory.
- Ran offline `:app:assembleDebug`, `:app:lintDebug`, `:app:testDebugUnitTest`, `:app:assembleDebugAndroidTest`, and `:app:processReleaseMainManifest`. After moving the icon resource, an incremental resource link missed it. Rerunning the resource merge and packaging tasks rebuilt the debug APK successfully. Refreshed lint after removing the empty versioned directory. Final lint result was 0 errors and 4 warnings for the pinned target API and Gradle/Kotlin version notices. The SDK XML compatibility notice was non-blocking. The JVM test task reported `NO-SOURCE`.
- Verified both merged package IDs, the launcher label and exported `org.unishiasy.vehiclelog.MainActivity`, the packaged icon layers, and the debug APK signature. The instrumented APK has ID `org.amirariana.vehiclelog.debug.test` and targets the new debug package. Debug APK SHA-256 `d950578ff75e4d9cee1a3fde846c419f06da347e4a6e3350f7f4f27189ebf14e`.
- This slice produced debug APKs and a merged release manifest. Device verification followed after separate approval.

**New debug package verification on the Redmi (2026-11-09):**

- Confirmed the authorized USB device was a Redmi Note 9S running Android 12 (API 31), locale `en-US`. Before installation, AAPT2 verified app package `org.amirariana.vehiclelog.debug` (version `0.1.0`, version code 1) and test package `org.amirariana.vehiclelog.debug.test`, whose AndroidJUnitRunner targets the new app. Both APKs passed `apksigner verify --verbose --print-certs` with APK Signature Scheme v2 and certificate SHA-256 `2937210215654d1944357664cd8d2a45963855ebcbbf0e4e780fbf645055a58c`.
- Verified SHA-256 before installation and matched both installed APKs afterward. App APK `d950578ff75e4d9cee1a3fde846c419f06da347e4a6e3350f7f4f27189ebf14e`. Test APK `f56b67e42310b9d42faa8519e49ededb6375f28393daaa0317490b6caeb8b468`.
- Ran `./gradlew --offline -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true -Pandroid.experimental.testOptions.uninstallIncompatibleApks=false :app:connectedDebugAndroidTest` with the pinned local toolchain. Gradle installed the new debug app and its test companion and retained both. Gradle reported `BUILD SUCCESSFUL in 52s` with the existing non-blocking SDK XML compatibility warning.
- `AppLaunchTest.homeDisplaysPersianTextInRightToLeftLayout` passed without manual intervention. The XML report records **1 test, 0 failures, 0 errors, 0 skipped**, with 0.981 seconds for the test case. Logcat recorded the new package's activity displayed in 773 ms. The test verified the visible title «دفتر خودرو», the visible description «مدیریت هزینه‌ها و سرویس‌های خودرو», and RTL layout direction. [Connected test report](app/build/reports/androidTests/connected/debug/index.html)
- The earlier `org.unishiasy.vehiclelog.debug` installation retained APK SHA-256 `9469a00a52dd1b5b19e7303e393ee0c43d4305a7d486226d7a90253a18ab2169`, UID 10469, version code 1, and the same first-install and last-update timestamps. No uninstall, reinstall, or data operation targeted the earlier app.
- Android resolved the new package's launcher activity to `org.amirariana.vehiclelog.debug/org.unishiasy.vehiclelog.MainActivity`. The matching installed APK declares the Persian label and the adaptive icon with background, foreground, and monochrome layers.
- Visually checked the MIUI launcher on the same Redmi Note 9S (Android 12, API 31) using a cropped ADB screenshot. Of the two entries labeled «دفتر خودرو», the right-hand entry showed the new light car-and-ledger mark on a purple rounded-square background. The left-hand entry showed the Android robot on a teal grid. MIUI rejected the ADB tap with an `INJECT_EVENTS` permission error. Launcher appearance is verified visually, and activity registration is verified separately with the package manager.
- **User-reported manual pass:** The developer tapped the new «دفتر خودرو» icon in the Redmi Note 9S MIUI launcher and confirmed that the app opened successfully. This is separate from the Compose instrumentation result.

**Release signing infrastructure preparation (2026-11-09):**

- Kept the release build unsigned in Gradle and applied explicit version overrides to the release variant only. The [release workflow](docs/delivery/ANDROID_RELEASE.md) builds unsigned, aligns, signs through the SDK tool's interactive terminal prompt, then verifies alignment, app ID, versions, expected certificate SHA-256, and APK SHA-256 before publishing to a no-overwrite version directory.
- Inspected the installed JDK 17 `keytool` password reader and Build Tools 36.0.0 `apksigner` bytecode. Both can use Java's no-echo console. `apksigner` can fall back to standard input, so the script requires a terminal and disables echo during signing. The password never passes through Gradle, shell variables, or a password file. Terminal input capture and process-memory access remain outside this protection.
- `bash -n scripts/release-apk.sh`, `--help`, and `--check` passed with the persistent JDK 17 and SDK 36. Negative checks rejected missing arguments, invalid version values, unsupported JDK, missing SDK, malformed fingerprint, a missing or repository-local keystore path, an overly permissive file, and a non-interactive session. Isolated pseudo-terminal checks rejected an existing release path and a held lock before Gradle ran. A scratch-directory test confirmed that the no-clobber move preserved an existing destination.
- `./scripts/check-android.sh` passed offline in 50 seconds after enabling local IPC. The first attempt in the restricted sandbox failed before Gradle tasks with `Could not determine a usable wildcard IP for this machine`. Debug APK and instrumentation APK checks passed, the JVM test task returned `NO-SOURCE`, and lint reported 0 errors and 4 warnings. The SDK XML compatibility notice remains. Build Tools verified alignment and signature on the existing debug APK, whose checksum still matched the device-tested build. [Lint report](app/build/reports/lint-results-debug.html)
- Offline `:app:processReleaseManifestForPackage :app:processDebugManifestForPackage` with `-PvehiclelogReleaseVersionCode=42 -PvehiclelogReleaseVersionName=0.1.0-rc.1+local` passed in 18 seconds. The packaged release manifest contained `org.amirariana.vehiclelog`, code 42, and name `0.1.0-rc.1+local`. The debug manifest retained `org.amirariana.vehiclelog.debug`, code 1, and name `0.1.0`. A separate manifest-only run rejected an incomplete version override.
- No key or release APK was created, and no phone action was taken. The workflow is ready for an explicitly authorized signing trial. End-to-end signing, release artifact verification, and the physical upgrade remain pending. The permanent ID still requires approval before public release, and M2 remains gated on M1 completion and the re-estimate.

**First test APK signing preflight (2026-11-09):**

- The developer provided authenticated `keytool -list -v` output for the `vehiclelog-m1-test` alias and confirmed restoration of the encrypted backup and a matching fingerprint. The test certificate has a 3072-bit RSA public key, uses `SHA256withRSA`, and has SHA-256 `4da34964d2bd5de0bf98713e02b240fdb9438d18964d5d0c9efad248f1a8748a`.
- Resolved the test keystore outside the repository and confirmed it is readable, owned by `amir`, and mode `600`. `bash -n scripts/release-apk.sh` and `./scripts/release-apk.sh --check 1 0.1.0` passed with the local JDK 17 and SDK 36. `build/releases/1` was absent. Build Tools 36.0.0 `apksigner verify --print-certs app/build/outputs/apk/debug/app-debug.apk` confirmed that the current debug certificate SHA-256 `2937210215654d1944357664cd8d2a45963855ebcbbf0e4e780fbf645055a58c` differs from the test certificate.
- Signing was handed off to the developer's private terminal for provisional ID `org.amirariana.vehiclelog`, version code 1, and version name `0.1.0`. The expected path is `build/releases/1/vehiclelog-1-0.1.0.apk`. Signing, final APK checksum, independent artifact verification, and installation remain pending. No APK was built or signed during this preflight.

**First test APK verification and version 2 preflight (2026-11-09):**

- The developer produced `build/releases/1/vehiclelog-1-0.1.0.apk` in a private terminal. Build Tools 36.0.0 `zipalign -c -P 16 4` passed. `apksigner verify --verbose --print-certs` verified v2 and v3 and reported one signer with the expected test certificate SHA-256 `4da34964d2bd5de0bf98713e02b240fdb9438d18964d5d0c9efad248f1a8748a`.
- `aapt2 dump badging` confirmed provisional application ID `org.amirariana.vehiclelog`, version code 1, version name `0.1.0`, and launcher `org.unishiasy.vehiclelog.MainActivity`. The APK is non-debuggable and its metadata matches the generated release record. Independent `sha256sum` yielded `edded9c9cc0f6142dda0029b07187e24522ef065c32ef5cf9b7e9bf9470935d5`, which passed `sha256sum --check --strict SHA256SUMS`.
- Before version 2 signing, also recorded SHA-256 `8ff179c416966e8a51c69b0c5be9ed3945a72135fdea68741cf14da07674e71a` for the version 1 `SHA256SUMS` and `8eaeb9ffd19f93ca5e293895ae3f079bbcbdff4aa40ef3953fad7daa2a1a6e77` for its `release.txt`. Version 2 output path `build/releases/2` was absent.
- `./scripts/release-apk.sh --check 2 0.1.1` passed with the local JDK 17 and SDK 36. The developer received the version 2 command for the existing `vehiclelog-m1-test` alias in a private terminal. Version 2 signing and independent comparison remain pending. No device was contacted.

**Two signed test APKs verified (2026-11-09):**

- The developer completed the version 2 build. Independently checked both published APKs with Build Tools 36.0.0. Both passed `zipalign -c -P 16 4` and `apksigner verify --verbose --print-certs`, with v2 and v3 valid and one signer each. The shared test certificate SHA-256 is `4da34964d2bd5de0bf98713e02b240fdb9438d18964d5d0c9efad248f1a8748a`.
- `aapt2 dump badging` confirmed provisional ID `org.amirariana.vehiclelog`, launcher `org.unishiasy.vehiclelog.MainActivity`, version 1 (`0.1.0`), and the higher version code 2 (`0.1.1`). Both are non-debuggable, and their generated release records match the APK metadata and signer.
- Independent `sha256sum` results matched both published `SHA256SUMS` files and passed `sha256sum --check --strict SHA256SUMS`.

| Version code | Version name | APK | SHA-256 |
| --- | --- | --- | --- |
| 1 | `0.1.0` | `build/releases/1/vehiclelog-1-0.1.0.apk` | `edded9c9cc0f6142dda0029b07187e24522ef065c32ef5cf9b7e9bf9470935d5` |
| 2 | `0.1.1` | `build/releases/2/vehiclelog-2-0.1.1.apk` | `1761b660c8bfd33c7c688189e5e87d3c9f953da6c556ca10a4451b3330fc310c` |

- Rechecked the version 1 APK and both companion files against their pre-build SHA-256 values. All three matched byte-for-byte. Version 2 was published to the previously unused directory, and no existing release artifact was overwritten.
- Both APKs are ready for a separately authorized physical in-place upgrade test. No device was contacted.

**Authorized physical release installation and upgrade (2026-11-09):**

- Reverified both unchanged release artifacts with `sha256sum --check --strict` against the hashes above. Build Tools 36.0.0 `apksigner verify --verbose --print-certs` confirmed one signer each and valid v2 and v3 signatures. `aapt2 dump badging` confirmed `org.amirariana.vehiclelog`, launcher `org.unishiasy.vehiclelog.MainActivity`, and the requested versions. `zipalign -c -P 16 4` passed for both files.
- Confirmed the authorized USB device via `adb -d get-serialno` and `adb -s "$SERIAL" shell getprop`. It was a Redmi Note 9S (`curtana`) running Android 12 (API 31), awake and unlocked with Android user 0 active. `adb -s "$SERIAL" shell pm list packages -u --user 0` showed the release package was absent. Took both debug package baselines using `pm path --user 0`, `dumpsys package`, and on-device `sha256sum` for each base APK.
- `adb -s "$SERIAL" install --no-streaming --user 0 build/releases/1/vehiclelog-1-0.1.0.apk` returned `Success`. `adb -s "$SERIAL" install -r --no-streaming --user 0 build/releases/2/vehiclelog-2-0.1.1.apk` then returned `Success`. Each installed base APK, located with `pm path --user 0` and copied with `adb pull` for verification, matched the corresponding local SHA-256. Running `apksigner verify --verbose --print-certs` on each installed copy independently verified the same test certificate SHA-256 `4da34964d2bd5de0bf98713e02b240fdb9438d18964d5d0c9efad248f1a8748a`, one signer, and valid v2 and v3 signatures. `aapt2 dump badging` confirmed the installed package and version metadata.
- For each version, `adb -s "$SERIAL" shell am start -W --user 0 -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -n org.amirariana.vehiclelog/org.unishiasy.vehiclelog.MainActivity` returned `Status: ok` and `LaunchState: COLD`. `dumpsys activity activities` confirmed the expected main activity was resumed after each launch. Airplane mode was off during this test.

| Installed version | Release UID | `firstInstallTime` (device local) | `lastUpdateTime` (device local) | Cold launch `TotalTime` |
| --- | ---: | --- | --- | ---: |
| 1 (`0.1.0`) | 10472 | 2026-11-09 12:15:33 | 2026-11-09 12:15:33 | 594 ms |
| 2 (`0.1.1`) | 10472 | 2026-11-09 12:15:33 | 2026-11-09 12:16:09 | 333 ms |

- Both debug packages remained installed at version code 1 and version name `0.1.0`. Their installed base APK checksums, UIDs, and timestamps matched before and after the release upgrade.

| Debug package | UID | `firstInstallTime` and `lastUpdateTime` (device local) | Installed base APK SHA-256 |
| --- | ---: | --- | --- |
| `org.unishiasy.vehiclelog.debug` | 10469 | 2026-11-09 10:48:21 | `9469a00a52dd1b5b19e7303e393ee0c43d4305a7d486226d7a90253a18ab2169` |
| `org.amirariana.vehiclelog.debug` | 10470 | 2026-11-09 11:18:19 | `d950578ff75e4d9cee1a3fde846c419f06da347e4a6e3350f7f4f27189ebf14e` |

- The in-place upgrade passed. Version 2 remains installed. No package was uninstalled or cleared, and a post-test checksum check confirmed both local APK artifacts were unchanged.

**Developer-reported manual pass (2026-11-09):** The installed signed release `0.1.1` (version code 2) cold-launched on the Redmi Note 9S with airplane mode enabled and Wi-Fi disabled. The developer observed the Persian title, description, and RTL Home. This was a manual check separate from the earlier ADB launch and Compose instrumentation.

**Final milestone audit and roadmap re-estimate (2026-11-09):**

| M1 acceptance area | Evidence type | Assessment |
| --- | --- | --- |
| Pinned toolchain and debug build | Build verification | The pinned JDK 17, SDK 36, Gradle 8.13, AGP 8.13.2, and Kotlin 2.3.10 built the debug and test APKs. The isolated debug build below satisfies the pre-commit clean-source alternative. There is no committed source revision to check out. [Backlog](docs/delivery/BACKLOG.md) |
| Automated quality checks | Automated host checks | Latest debug lint report has 0 errors and 4 warnings. The warnings cover the pinned target and dependency versions. The SDK XML notice was non-blocking. The JVM test task reported `NO-SOURCE`. The shell has no business-logic unit suite, so the task provides no JVM test coverage. M2 requires actual tests for the new behavior. |
| Persian RTL Home | Automated physical instrumentation | One unattended Compose test passed on the new debug package, with 0 failures, 0 errors, and 0 skipped on the English-language Redmi Note 9S running Android 12. It asserted both Persian strings and RTL direction. The launcher-based test does not establish background-launch behavior. [Connected test report](app/build/reports/androidTests/connected/debug/index.html) |
| Manual smoke and launcher | Developer report and visual inspection | The developer reported offline cold launch, rotation, and enlarged-text readability on the earlier debug package, and a successful icon tap on the new debug package. The adaptive icon was separately observed in MIUI. |
| Backup policy | Build and artifact inspection | The recorded debug manifest and packaged rules exclude app data from platform backup and transfer. Populated-ledger backup and restore behavior belongs to later milestones. |
| Signing and release identity | Build and artifact verification, plus developer report | The documented workflow produced two APKs with matching test certificates, valid v2 and v3 signatures, correct alignment, and recorded checksums. The developer reported a successful encrypted test-key backup restore. The permanent ID and long-lived distribution key remain later distribution decisions. |
| Signed install, upgrade, and startup | Scripted physical-device verification | Both versions installed and cold-launched with airplane mode off. In-place upgrade preserved ID, certificate, UID, and first-install time. Both debug packages matched their baselines. |
| Signed offline cold launch | Developer-reported manual pass and separately approved exception | The developer confirmed offline cold startup into Persian RTL Home on signed `0.1.1`. Signed `0.1.0` offline startup is **Waived — Accepted Residual Risk**. This particular signed artifact was never directly observed launching offline. [Test strategy](docs/delivery/TEST_STRATEGY.md) and [release runbook](docs/delivery/ANDROID_RELEASE.md#future-two-version-redmi-upgrade-check) |
| Effort review and closeout | Planning evidence | M2–M7 ranges are revised in this plan, the roadmap, and the cost estimate. M1 has command and device timings but no complete focused-time log. Weekly developer capacity is still an assumption. |

**Clean-source debug build and lower-version offline disposition (2026-11-09):**

- The backlog calls for a clean-checkout build. With no committed revision, the execution plan allows a recorded clean-source equivalent. An isolated copy of the current source and build inputs exercises the same build gate without project outputs or local configuration. This demonstrates a clean-source build on the pinned local toolchain. A fresh-machine dependency bootstrap, Git revision provenance, and byte-for-byte rebuilds remain outside this evidence.
- Used `mktemp -d /tmp/vehiclelog-m1-clean.XXXXXX` and enumerated inputs with `LC_ALL=C git ls-files --cached --others --exclude-standard -z | sort -zu`. Reviewed the list against explicit source, Gradle, wrapper, script, and documentation path patterns, rejected symlinks and non-regular files, and transferred the 43 files using `tar --null -T "$scratch/inputs.nul" -cf - | tar -xf - -C "$scratch/source"`. The snapshot omitted Git metadata, project caches, build outputs, `local.properties`, keys, and credentials. `xargs -0 -r sha256sum --` produced a sorted file hash manifest, checked before and after the build. Its SHA-256 was `0d486b58c5802ddea78a954cb55c89c9b9f3633d777b11bad7a18c56aad25f7b`. The 18-file build-input subset (Android sources and Gradle configuration and wrapper files) had manifest SHA-256 `2bd989f5ac5b5c9469f0f467840dd8787c89997a856e63f8a3ed56d84445c6cc`.
- Used JDK 17 at `/usr/lib/jvm/java-17-amazon-corretto`, SDK 36 and Build Tools 36.0.0 under `$HOME/Android/Sdk`, and Gradle Wrapper 8.13. Copied only the existing `caches/modules-2` dependency cache and `wrapper/dists/gradle-8.13-bin` distribution into a separate temporary Gradle user home, excluding lock and GC files. Used empty temporary Android and Java user homes. The unchanged debug build used Gradle's default disposable debug signing in isolation. No existing signing keys or credentials were used.

```bash
cd "$scratch/source"
export JAVA_HOME=/usr/lib/jvm/java-17-amazon-corretto
export ANDROID_HOME="$HOME/Android/Sdk" ANDROID_SDK_ROOT="$HOME/Android/Sdk"
export GRADLE_USER_HOME="$scratch/gradle-home" ANDROID_USER_HOME="$scratch/android-user"
env -u ANDROID_SDK_HOME -u ANDROID_PREFS_ROOT -u JAVA_TOOL_OPTIONS -u _JAVA_OPTIONS \
    -u JDK_JAVA_OPTIONS -u JAVA_OPTS -u GRADLE_OPTS \
    ./gradlew --offline --no-daemon --no-build-cache --console=plain --max-workers=4 \
    "-Dorg.gradle.java.home=$JAVA_HOME" \
    "-Dorg.gradle.jvmargs=-Xmx2g -Dfile.encoding=UTF-8 -Duser.home=$scratch/java-user" \
    -Pandroid.builder.sdkDownload=false -Porg.gradle.java.installations.auto-download=false \
    :app:assembleDebug
```

- Gradle reported `BUILD SUCCESSFUL in 2m 12s` with 37 actionable tasks executed. Non-blocking notices covered SDK XML metadata and an AndroidX native library packaged without symbol stripping. `aapt2 dump badging` verified debug package `org.amirariana.vehiclelog.debug`, code 1, name `0.1.0`, and launcher `org.unishiasy.vehiclelog.MainActivity`. The temporary APK SHA-256 was `7915636d17fdffd74ede32b937eaca51040f1184f69b5bf66cb92492ed022803`. All copied inputs matched their starting hashes. The temporary snapshot, debug APK, and isolated build state were removed. No unrelated tests or device operations ran.
- **Signed `0.1.0` offline cold launch · Waived — Accepted Residual Risk.** The developer explicitly approved this one-time exception. Signed `0.1.0` installed and cold-launched with airplane mode off, then upgraded in place to signed `0.1.1`. The developer manually confirmed the retained `0.1.1` cold-launched offline. Read-only comparison of the 132 ZIP entries in each APK found identical uncompressed code and resources. Only `AndroidManifest.xml` and three `META-INF` signing entries differed. The decoded manifest differed only in `versionCode` and `versionName`. Reinstalling the lower version solely for this check offers limited additional value. This particular signed `0.1.0` artifact was never directly observed launching offline. The exception applies only to this artifact. The offline launch requirement remains in force for future releases.
- **Closure decision.** M1 is formally complete. The acceptance record includes passing verification, developer-reported manual results, and the explicitly accepted exception. `org.amirariana.vehiclelog` remains provisional. M2 is next. Confirm weekly capacity and obtain separate approval before starting it.

## M2. Room and vehicle setup

**Status:** Complete under revised MVP scope (2026-11-10). M2.1–M2.4 passed their applicable gates. M2.5b and M2.5c implementation and focused automated interaction checks passed. The product owner accepts residual risks for the two outstanding visual/accessibility checks listed below. Production catalog activation is deferred. Business recruitment is deferred by product decision and is not an M2 exit criterion under this revision. **Provisional effort:** 6–9 days.

**Dependencies:** M1 gate and re-estimate, Room and KSP approval. A sourced catalog is deferred rather than an M2 dependency. **Risk:** Missing schema export, malformed catalog data, or a save interrupted by process death.

- [x] Add `data/local/VehicleDatabase.kt`, `VehicleEntity.kt`, and `VehicleDao.kt`. Pin Room and KSP, export schema v1, and compile the focused DAO tests.
- [x] Run `VehicleDaoTest` on an Android device.
- [x] Add `data/VehicleRepository.kt`, vehicle input validation, and application-scoped `AppContainer.kt` wiring for validated writes and typed save outcomes.
- [x] Add `ui/vehicle/VehicleScreen.kt`, `VehicleViewModel.kt`, and Persian strings. Wire `MainActivity.kt` for the observable vehicle list and manual add/edit flow.
- [x] Run the focused `VehicleSetupTest` on a usable device.
- [x] Verify offline manual add/edit, larger-font readability, and persistence after force-stop and reboot. The developer reported these manual passes.
- [x] M2.5b · Add the versioned offline catalog loader, deterministic search, fictional JVM fixture, and focused tests.
- [x] M2.5c · Verify the optional catalog picker and manual fallback without shipping catalog data.
- [~] Production catalog activation deferred. No redistributable source is approved. The picker remains disabled in normal builds. Activation requires an approved dataset, recorded provenance, and physical-device verification of the activated build.
- [~] Business recruitment deferred by product decision. No businesses are reported as recruited or interviewed. [Research protocol](docs/delivery/ROADMAP.md#business-validation-track)

**Automated:** `test/VehicleCatalogTest.kt`, `androidTest/VehicleDaoTest.kt`, and `VehicleSetupTest.kt`.

**Physical Android:** Developer-reported passes cover offline mixed-script add/edit, force-stop/relaunch, and reboot. Focused catalog interaction tests passed on the Redmi Note 9S.

**Done under revised scope:** Manual vehicle setup and offline Room persistence are verified. Catalog engine, disabled-by-default selection integration, and focused interaction behavior are verified. Production catalog and business recruitment are explicitly deferred.

**M2.2 storage foundation evidence:**

- Verified Room runtime and compiler `2.8.4` with KSP `2.3.6` before editing the repository. An isolated Android project ran `./gradlew --no-daemon --console=plain --max-workers=4 "-Dorg.gradle.java.home=$JAVA_HOME" -Pandroid.builder.sdkDownload=false -Porg.gradle.java.installations.auto-download=false :app:compileDebugKotlin` against the existing repositories and pinned Kotlin `2.3.10`, AGP `8.13.2`, Gradle `8.13`, JDK 17, and SDK 36. The first probe used a one-segment Android namespace and stopped at manifest merge. After correcting the probe namespace, compilation and schema export succeeded in 2m 11s. Room runtime includes the Kotlin APIs used here. [Room release notes](https://developer.android.com/jetpack/androidx/releases/room) and [KSP 2.3.6](https://github.com/google/ksp/releases/tag/2.3.6)
- In the repository, ran `./gradlew --offline --no-daemon --console=plain --max-workers=4 "-Dorg.gradle.java.home=$JAVA_HOME" -Pandroid.builder.sdkDownload=false -Porg.gradle.java.installations.auto-download=false :app:compileDebugAndroidTestKotlin` with the pinned local environment. The first run exposed KSP's argument-provider parsing failure on the workspace path with spaces. The key/value overload preserved the path, and the same command then succeeded in 1m 17s. The existing SDK XML format notice remained non-blocking.
- KSP generated the database implementation and exported [schema v1](app/schemas/org.unishiasy.vehiclelog.data.local.VehicleDatabase/1.json). Inspection confirmed one application table, `vehicles`, with the approved six columns and a non-generated, non-null text primary key.
- The four instrumented cases cover insert, lookup and deterministic list order, duplicate-key rejection with the original row intact, update and missing-ID results, and persistence after closing and reopening a uniquely named file-backed database. The test class compiled. **Device tests not run.** `adb devices -l` listed no device. Run only `VehicleDaoTest` once a usable device is available. M2.2 has no app wiring or vehicle UI.
- **M2.2 device verification recheck:** `"$HOME/Android/Sdk/platform-tools/adb" devices -l` returned an empty device list. Device details could not be confirmed. All four cases remain pending with zero executed. No Gradle test task ran, so there are no new test failures or warnings to report.
- **M2.2 physical verification (2026-11-09):** ADB detected an authorized USB Redmi Note 9S (`curtana`), Android 12 (API 31), awake and unlocked. With Corretto JDK 17, SDK 36, Gradle 8.13, the existing ignored dependency cache, and `ANDROID_SERIAL` set to the verified USB device, ran:

  ```bash
  timeout --signal=INT --kill-after=20s 240s ./gradlew --offline --no-daemon --console=plain --max-workers=4 \
      "-Dorg.gradle.java.home=$JAVA_HOME" \
      -Pandroid.builder.sdkDownload=false -Porg.gradle.java.installations.auto-download=false \
      -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true \
      -Pandroid.experimental.testOptions.uninstallIncompatibleApks=false \
      -Pandroid.testInstrumentationRunnerArguments.class=org.unishiasy.vehiclelog.data.local.VehicleDaoTest \
      :app:connectedDebugAndroidTest
  ```

  Gradle reported `BUILD SUCCESSFUL in 1m 55s`. The JUnit XML contains only `VehicleDaoTest` with **4 passed, 0 failures, 0 errors, 0 skipped**. Insert and lookup returned the saved rows in deterministic order. Duplicate insert preserved the original row. Update affected one existing row and zero missing rows. The file-backed database retained its row after close and reopen. Non-blocking build warnings covered SDK XML metadata version 4 and packaging `libandroidx.graphics.path.so` without stripping. The debug APK SHA-256 was `3ac01fba2753aa5591e4842015c112cac40fe6e0e8caf61688fe085a1a30fba8` and the test APK SHA-256 was `1b15f89eea04ff00101ac766f578b1cd87b9c1786d2224010b290737412e02c0`. [Connected test report](app/build/reports/androidTests/connected/debug/index.html)

**M2.3 repository and application wiring evidence (2026-11-09):**

- The repository generates a canonical UUID on create and preserves the supplied ID on update. It distinguishes created, updated, invalid input, missing vehicle, and storage failure. Validation runs before any write. A zero-row update returns missing vehicle, and coroutine cancellation propagates. Reads delegate to the existing DAO.
- Label trimming preserves Persian text and joiners. Vehicle kind is `car`. Year and calendar are optional as a pair. When present, the year is four digits after normalization from Persian, Arabic-Indic, and Latin digits, and year zero is rejected. Calendar is explicitly `solar_hijri` or `gregorian`, with no conversion. Historical and future year bounds remain a product decision. Nonblank catalog keys are stored as opaque metadata pending a sourced catalog.
- The manifest registers `VehicleLedgerApplication`. Its lazy container shares one app-private Room database and one repository per application process. No destructive migration policy is configured. The existing Compose Home has no repository consumer in this slice.
- Ran the following with Corretto JDK 17, Android SDK 36, and the existing local Gradle cache. JUnit `4.13.2`, already used by instrumentation, was added to the JVM test configuration only.

  ```bash
  ./gradlew --offline --no-daemon --console=plain --max-workers=4 \
      "-Dorg.gradle.java.home=$JAVA_HOME" \
      -Pandroid.builder.sdkDownload=false -Porg.gradle.java.installations.auto-download=false \
      :app:testDebugUnitTest --tests org.unishiasy.vehiclelog.data.VehicleInputTest \
      --tests org.unishiasy.vehiclelog.data.VehicleRepositoryTest :app:compileReleaseKotlin
  ```

  Gradle succeeded in 1m 11s. Debug and release Kotlin compiled. The XML report shows **12 JVM tests passed, 0 failures, 0 errors, 0 skipped** across seven input and five repository tests. The SDK XML metadata notice remained non-blocking. Merged debug and release manifests point to the new application class and retain their existing application IDs. The DAO, entity, database declaration, exported v1 schema, and signed APKs matched their pre-run SHA-256 hashes. [JVM test report](app/build/reports/tests/testDebugUnitTest/index.html)
- Repository tests use a fake DAO. This slice did not run Android startup or repository integration on a device. The previous four Room DAO device passes still apply to the unchanged storage layer.

**M2.4 manual vehicle UI evidence (2026-11-09):**

- The existing Persian RTL entry point uses an activity-scoped `VehicleViewModel` with lifecycle-aware state collection. The screen displays observable saved vehicles, loading, empty, and load-error states. Manual add/edit preserves the existing ID on update and retains the draft on validation, missing vehicle, or storage failure. Save disables form actions while the repository writes and shows confirmation after a successful result. Existing validation accepts mixed-script names and supported digit forms, and the optional year requires an explicit calendar.
- Used the pinned JDK 17, SDK 36, Gradle 8.13 and local cache to run `./gradlew --offline --no-daemon --console=plain --max-workers=4 "-Dorg.gradle.java.home=$JAVA_HOME" -Pandroid.builder.sdkDownload=false -Porg.gradle.java.installations.auto-download=false :app:compileDebugAndroidTestKotlin`. Gradle compiled debug Kotlin and instrumentation Kotlin in 52 seconds with the existing non-blocking SDK XML metadata notice. Room v1 schema, repository and validation source, and both signed release APK checksums matched their pre-run values.
- The two `VehicleSetupTest` cases compiled and use an isolated in-memory Room database. They cover manual add/edit with the original ID and normalized year, and Persian name, calendar, and year errors with draft retention at 150% font scale. No device was available during the compilation pass. The subsequent focused physical run is recorded below.
- **M2.4 physical automation (2026-11-09):** ADB detected an authorized USB Redmi Note 9S (`curtana`), Android 12 (API 31), awake and unlocked. With Corretto JDK 17, SDK 36, Gradle 8.13, the existing local dependency cache, and `ANDROID_SERIAL` set to the verified USB device, ran:

  ```bash
  timeout --signal=INT --kill-after=20s 240s ./gradlew --offline --no-daemon --console=plain --max-workers=4 \
      "-Dorg.gradle.java.home=$JAVA_HOME" \
      -Pandroid.builder.sdkDownload=false -Porg.gradle.java.installations.auto-download=false \
      -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true \
      -Pandroid.experimental.testOptions.uninstallIncompatibleApks=false \
      -Pandroid.testInstrumentationRunnerArguments.class=org.unishiasy.vehiclelog.ui.vehicle.VehicleSetupTest \
      :app:connectedDebugAndroidTest
  ```

  Gradle succeeded in 2m 17s. The JUnit XML contains only `VehicleSetupTest` with **2 passed, 0 failures, 0 errors, 0 skipped**. The suite took 9.977s. Add/edit with the original ID passed in 3.990s, and Persian validation and draft retention at 150% font scale passed in 3.620s. Both used isolated in-memory Room storage. Non-blocking warnings covered SDK XML metadata version 4 and packaging `libandroidx.graphics.path.so` without stripping. Debug APK SHA-256 `4c48444cbf765b1f435081a3a8d7f8a8629233d4d8b78d4dc26d6da0b59e8652`. Test APK SHA-256 `4cf842888eb936ec2125fc9486c3933d0add4fa01d73a495e05b757159d15066`. [Connected test report](app/build/reports/androidTests/connected/debug/index.html)

**M2.4 developer-reported manual passes (2026-11-09):**

On the Redmi Note 9S using the current debug application (`org.amirariana.vehiclelog.debug`), the developer confirmed:

- The Persian RTL list and form were readable at normal and 150% system font size.
- The flow worked offline with airplane mode enabled and Wi-Fi disabled.
- Invalid year input showed a Persian validation error and retained the form draft.
- Vehicle creation succeeded with mixed Persian/Latin text, Persian year digits, and an explicit calendar.
- Editing the saved vehicle did not create a duplicate.
- The edited details persisted after Android force-stop and relaunch. The vehicle name, year, and calendar also persisted after device reboot.

**Acceptance decision:** M2.4 is complete.

**M2.5b offline catalog foundation evidence (2026-11-09):**

- The UTF-8 JSON document uses `schemaVersion: 1` and an `entries` array. Each entry requires a unique stable `key` matching `[a-z0-9][a-z0-9._-]*` and nonblank `manufacturer` and `model` strings. `variant` is an optional nonblank string or `null`. `aliases` is an optional array of nonblank strings and defaults to empty. Invalid JSON, unsupported versions, invalid records, and duplicate entry keys reject the load. The developer approved the cached `kotlinx-serialization-json:1.8.1` runtime dependency. The loader reads JSON through a local `Reader` using the library's JSON tree API. [Parser API](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json/parse-to-json-element.html)
- Search folds case with `Locale.ROOT`, maps Arabic yeh, alef maksura, and kaf to Persian equivalents, maps Persian and Arabic-Indic digits to ASCII, collapses whitespace, and removes ZWNJ from the search representation. Every query term must match a substring in the labels or aliases. Empty queries return all entries, and all results are ordered by stable key. Original labels, aliases, and keys are retained.
- The two fictional entries live only in `app/src/test/resources/vehicle_catalog_fixture.json`. The application has no catalog asset or catalog UI in this slice. Bama and Divar were not accessed or used. The next step is to approve and document a redistributable source before bundling real entries.
- With Corretto JDK 17, SDK 36, and the existing local Gradle 8.13 cache, ran:

  ```bash
  ./gradlew --offline --no-daemon --console=plain --max-workers=4 \
      "-Dorg.gradle.java.home=$JAVA_HOME" \
      -Pandroid.builder.sdkDownload=false -Porg.gradle.java.installations.auto-download=false \
      :app:testDebugUnitTest --tests org.unishiasy.vehiclelog.data.catalog.VehicleCatalogTest
  ```

  Gradle succeeded in 1m 30s and compiled debug Kotlin. The XML report contains only `VehicleCatalogTest`, with **7 passed, 0 failures, 0 errors, 0 skipped**. The existing SDK XML metadata version 4 warning remained non-blocking. The Room v1 schema SHA-256 was unchanged (`2ad530048d1d406b429972154426484de08fb2453f160c7617a7ad02d299369d`), and both signed APK checksums matched their pre-run values. No physical-device checks were run for this isolated slice. [JVM test report](app/build/reports/tests/testDebugUnitTest/index.html)

**M2.5c safe catalog selection evidence (2026-11-10):**

- Normal debug and release use the same disabled `approvedCatalogAsset` setting in `AppContainer`. After approval of a redistributable source, add the asset to `app/src/main/assets/vehicle_catalog.json` and explicitly set the path to `"vehicle_catalog.json"`. The container opens only that bundled UTF-8 asset on an I/O dispatcher and passes the validated, nonempty catalog to the ViewModel. A missing, unreadable, invalid, or empty asset leaves the form manual-only. Test fixtures remain in test source sets.
- The optional Persian picker searches labels and aliases. Selection copies the original manufacturer, model, and optional variant into the editable name and saves the stable key through the existing repository. The picker leaves the year and calendar alone. Dismissing it preserves the draft and errors. Existing records use their stored labels and keys even when catalog entries disappear or are renamed. Editing a name manually keeps its existing catalog key, and choosing another entry replaces it.
- With Corretto JDK 17, SDK 36, and the existing local Gradle 8.13 cache, ran:

  ```bash
  ./gradlew --offline --no-daemon --console=plain --max-workers=4 \
      "-Dorg.gradle.java.home=$JAVA_HOME" \
      -Pandroid.builder.sdkDownload=false -Porg.gradle.java.installations.auto-download=false \
      :app:testDebugUnitTest --tests 'org.unishiasy.vehiclelog.data.catalog.*' :app:compileDebugAndroidTestKotlin
  ```

  Gradle succeeded in 52s. Only `VehicleCatalogTest` and `VehicleCatalogAvailabilityTest` ran, with **10 passed, 0 failures, 0 errors, 0 skipped**. Debug and Compose test Kotlin compiled. The SDK XML metadata version 4 warning was non-blocking. [JVM test report](app/build/reports/tests/testDebugUnitTest/index.html)

- On the authorized USB Redmi Note 9S (`curtana`), Android 12 (API 31), used the same pinned environment and set `ANDROID_SERIAL` to the verified device. Ran:

  ```bash
  timeout --signal=INT --kill-after=20s 240s ./gradlew --offline --no-daemon --console=plain --max-workers=4 \
      "-Dorg.gradle.java.home=$JAVA_HOME" \
      -Pandroid.builder.sdkDownload=false -Porg.gradle.java.installations.auto-download=false \
      -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true \
      -Pandroid.experimental.testOptions.uninstallIncompatibleApks=false \
      -Pandroid.testInstrumentationRunnerArguments.class=org.unishiasy.vehiclelog.ui.vehicle.VehicleSetupTest \
      :app:connectedDebugAndroidTest
  ```

  Gradle succeeded in 2m 36s. The XML report contains only `VehicleSetupTest`, with **5 passed, 0 failures, 0 errors, 0 skipped** in 19.518s. The actual debug activity showed the manual form without a catalog control. Isolated tests verified mixed-script search and selection at 150% font scale, no-match and dismiss fallback, draft and validation retention, saving the key with the label, and editing a saved vehicle whose key is absent from the supplied catalog. Manual add/edit and validation also passed. Warnings covered SDK XML metadata version 4 and packaging `libandroidx.graphics.path.so` without stripping. Debug APK SHA-256 `29a3cbec67b195afab0721acf9b516c782bd0b814137a43385e255698ba18b22`. Test APK SHA-256 `6d42a646875a20e9c7b8cc144c166c77ea84f44461097366813f4244b8548cb5`. [Connected test report](app/build/reports/androidTests/connected/debug/index.html)
- The debug APK contained no `assets/` entries. Room v1 schema and both signed release APK SHA-256 checksums matched their pre-run values. This slice did not build a release APK or run a separate visual review. Approved-asset content, activated build verification, and the remaining M2 gate are pending.

**M2.5 visual acceptance checklist and product-owner disposition (2026-11-10):**

Use the Redmi Note 9S at normal and 150% system font size for both configurations. Record device, Android version, build or fixture, font size, and pass or failure per check. The M2.4 manual results apply to the earlier manual form. The M2.5c automated results above establish behavior, including selected scenarios at an injected 150% font scale.

Normal debug build with catalog disabled

- [ ] Open the list and tap «افزودن خودرو». Verify headings, field labels, and form content align from the right. Confirm that the catalog button is absent. Check that labels, fields, and actions remain readable and reachable by scrolling with the keyboard open.
- [ ] Save a test vehicle named «خودروی آزمایشی VX» with year «۱۴۰۲» and calendar «شمسی». Verify the mixed-script name and year summary read correctly in the list. Reopen it, edit the name, save, and confirm the existing row changes without a duplicate.
- [ ] Try saving an empty name. Then enter a valid name and year «۱۴۰» and try saving without a calendar. Select «شمسی» and save again. Verify each Persian error is readable beside the relevant field or calendar controls, the entered text stays intact, and there is no success message until the inputs are corrected and saved.
- [ ] With TalkBack, check that the vehicle row, name and year fields, calendar choices and selected state, and save and cancel actions are announced in a sensible order. Check that validation feedback can be heard and the controls can be tapped reliably without hitting a neighboring control.

Isolated test-only catalog configuration

Use the injection seam in `VehicleSetupTest` with an isolated database. The existing instrumentation supplies synthetic entries and seed rows for automation. A human-interactive session needs a test-only harness or a paused instrumented session. The normal debug launcher does not expose that configuration.

- [ ] Open «انتخاب خودرو از فهرست مدل‌ها». Search for «بادبادک», then enter `kite بادبادك ٢0۳`. Check the right-aligned dialog, readable mixed-script result, and reachable result and return action with the keyboard open. Select «آزمایشی خیال‌گرد بادبادک ۲۰۳ VX» and confirm that the editable name is filled while the year and calendar stay unchanged. Save and reopen the vehicle.
- [ ] With a name, year, calendar, and visible validation error already in the form, open the picker and search for «ناموجود». Verify the Persian no-results message is fully readable and offers manual entry. Dismiss via «بازگشت به فرم», then repeat using Android Back after the keyboard closes. Verify the name, year, calendar, and existing error are still present after each dismissal.
- [ ] Open a seeded vehicle with stored name «نام ذخیره‌شده VX» and a key absent from the injected catalog. Verify the stored name appears in the list and form. Edit it manually, save, and reopen the same row with the new name.
- [ ] With TalkBack, verify the picker title, search label, no-results feedback, result, and return action are announced. Verify focus stays inside the open dialog and returns to the form on dismissal. Check that each target is easy to tap and that text and controls remain visible at both font sizes.

**M2.5 device acceptance evidence (2026-11-10):**

- Redmi Note 9S (`curtana`), Android 12 (API 31), portrait, 1080 × 2400 at 440 dpi, dark theme. The system font scale stayed at 1.0. MIUI denied the ADB write with `WRITE_SETTINGS` and shell Back injection with `INJECT_EVENTS`. The enlarged run applied a test-only 1.5 Compose override. No accessibility settings changed. The active accessibility service list did not contain TalkBack.
- The two new methods in `VehicleSetupTest` use an in-memory Room database, including the manual form and the seeded vehicle whose catalog key is missing. The test-only picker is supplied by ViewModel injection. The normal launcher remains catalog-free, as established by the earlier `normalActivityShowsManualFormWithoutCatalog` test on the same debug APK. No vehicle writes targeted the normal database.
- With Corretto JDK 17, Android SDK 36, the existing Gradle 8.13 dependency cache, and `ANDROID_SERIAL` targeting the Redmi, ran only these methods:

  ```bash
  suite=org.unishiasy.vehiclelog.ui.vehicle.VehicleSetupTest
  filter="$suite#reviewsManualFormOnDevice,$suite#reviewsCatalogPickerOnDevice"
  ./gradlew --offline --no-daemon --console=plain --max-workers=4 \
      "-Dorg.gradle.java.home=$JAVA_HOME" \
      -Pandroid.builder.sdkDownload=false -Porg.gradle.java.installations.auto-download=false \
      -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true \
      -Pandroid.experimental.testOptions.uninstallIncompatibleApks=false \
      "-Pandroid.testInstrumentationRunnerArguments.class=$filter" :app:connectedDebugAndroidTest
  "$ANDROID_HOME/platform-tools/adb" shell am instrument -w -r -e class "$filter" \
      -e acceptanceFontScale 1.5 org.amirariana.vehiclelog.debug.test/androidx.test.runner.AndroidJUnitRunner
  ```

- Initial instrumentation timed out on a shell-derived keyboard visibility signal. Device screenshots subsequently showed the real IME open. The shell-injected Back attempt failed, consistent with MIUI's explicit permission denial. The focused retry uses the app window's IME insets and app-owned instrumentation key events. [Initial keyboard timeouts](app/build/reports/m25-acceptance/initial-ime-timeouts.xml) · [Initial Back failure](app/build/reports/m25-acceptance/initial-back-failure.xml)
- Final results were **2 passed, 0 failures, 0 errors, 0 skipped** at the system's 100% font setting and **2 passed** at the injected 150% form setting. The picker dialog reported an effective scale of **1.0 in both runs**. Gradle succeeded in 1m 22s. The 100% suite took 18.54s and the injected run took 17.142s. The app and test APK SHA-256 values were `29a3cbec67b195afab0721acf9b516c782bd0b814137a43385e255698ba18b22` and `973bf1fb3b8b23b072b2b4aff282d7c91381ad179fff86edfa0050f5703f82d2`. Room v1 and both signed APK checksums matched the baseline. [100% test XML](app/build/reports/m25-acceptance/100-insets-results.xml) · [Injected run](app/build/reports/m25-acceptance/150-insets-instrumentation.txt) · [Dialog scale evidence](app/build/reports/m25-acceptance/insets/1.5/catalog-font-scale.txt)

| Checklist item | Result | Method and evidence |
|---|---|---|
| Normal launcher has no catalog picker | PASS | Earlier physical `normalActivityShowsManualFormWithoutCatalog` assertion on the same debug APK. The isolated manual flow also asserts absence. |
| Persian RTL list and add/edit form, mixed Persian/Latin label, visible errors and controls | PASS | Screenshot inspection at system 100% and injected form 150%. Headings and labels align from the right, the mixed label is readable, and the wrapped year error has no obvious clipping or overlap. Spot check in dark theme on this device. [100% form](app/build/reports/m25-acceptance/manual-1.0.png) · [Injected 150% form](app/build/reports/m25-acceptance/manual-1.5.png) |
| Form fields, calendar, and actions reachable with keyboard open | PASS | `reviewsManualFormOnDevice` focused the fields and confirmed save and cancel after scrolling while the IME was visible. Screenshots show the numeric keyboard and controls. |
| Create with Persian year and Solar Hijri, edit the same row, preserve draft on invalid input | PASS | `reviewsManualFormOnDevice` checked empty name, missing calendar, incomplete year, no premature save, and the same stored ID and year after editing. |
| Picker search, readable result and no-results message, selection and storage | PASS | `reviewsCatalogPickerOnDevice` matched Persian-only and mixed-script queries, checked no results, selected with the IME open, preserved the calendar and year, then asserted the original label and stable key in Room. Screenshot inspection at effective 100% showed readable wrapping and reachable controls. [Picker screenshots](app/build/reports/m25-acceptance/catalog-1.0.png) |
| Explicit picker return and Android Back after IME dismissal | PASS | `reviewsCatalogPickerOnDevice` checked name, year, calendar, and existing error after each route. Window insets reported the IME hidden after the first Back while the picker was still present. The second Back closed the picker. |
| Saved vehicle with a missing catalog entry | PASS | The same test displayed the stored label and saved an edit while preserving the original ID and key. The edit form was visually inspected. |
| Basic control labels, clickable semantics, calendar selected state, and sampled targets | PASS | Compose text, click, and selection assertions. Save, cancel, Solar Hijri, and picker return each measured at least 48 dp in Compose touch bounds. Result selection used a touch event. This is a sampled check. |
| Real system 150% and enlarged picker layout | Waived — Accepted Residual Risk | Product owner explicitly chose not to perform this check now. It was NOT performed. MIUI blocked the font setting write. An injected 1.5 form override passed, but the picker dialog retained effective scale 1.0. [Picker with enlarged form behind it](app/build/reports/m25-acceptance/catalog-1.5.png) |
| TalkBack navigation order, announcements, focus containment and restoration, and exhaustive touch usability | Waived — Accepted Residual Risk | Product owner explicitly chose not to perform this review now. It was NOT performed. TalkBack was inactive. Compose semantics and sampled hit bounds do not establish the screen-reader experience. |

No production UI defect was confirmed in the exercised configurations. The first-run failures were in test observation and shell permissions. The real 150% system-font check and TalkBack review were NOT performed. The product owner accepts their associated residual risks for the current manual-entry MVP scope. These are not passes and do not satisfy either check for a future changed UI or catalog-enabled build without separate review.

**M2 closure:** M2 is formally complete under the revised MVP scope. Verified automated results, developer-reported M2.4 manual results, and the explicitly waived checks above are recorded separately. Production catalog activation remains deferred until a redistributable dataset is approved, provenance is recorded, and the activated build passes device verification. Business recruitment is deferred and was not completed. The two waived checks remain residual risks, not passes.

## M3. Ledger, odometer, and dashboard

**Status:** CLOSED WITH ACCEPTED RESIDUAL RISKS (2026-11-11). M3.1 through M3.6 implementation and recorded automated acceptance are complete. **Provisional effort:** 10–15 days for M3.

**Dependencies:** M2 revised-scope gate and the approved money and date contracts below. **Risk:** Duplicate or partial writes, stale edits, and incorrect monthly grouping.

### M3.1 · Expense and service entry storage

- Room 1→2 adds `entries` and its `(vehicleId, occurredOn)` index. `vehicleId` references `vehicles.id` with `ON DELETE RESTRICT`. The migration leaves the `vehicles` table intact and has no destructive fallback. [Room v2 schema](app/schemas/org.unishiasy.vehiclelog.data.local.VehicleDatabase/2.json)
- Entry IDs are caller-supplied UUIDs. Required columns are `id`, `vehicleId`, `category`, `occurredOn`, and `createdAtEpochMillis`. `notes`, `amountRials`, and `fuelVolumeMl` are nullable. There are no SQL defaults. Updates preserve the creation instant and report a missing row. Reads sort by occurrence date descending, creation instant descending, then ID ascending.
- `fuel` requires a positive amount and volume. `oil_change`, `repair`, and `parts` allow an unpriced service. `insurance`, `wash`, and `other` require a positive amount. Non-fuel records have null volume. `LedgerRepository` validates these rules and Gregorian dates, normalizes blank notes to null, and converts liters exactly to whole milliliters with at most three decimal places. Room's entity annotations have no `CHECK` option, so the repository is the write boundary for these value rules. SQLite enforces the primary key, foreign key, and required columns.
- The migration test builds a populated v1 database from the exported schema. It checks all six vehicle fields across three rows, including nullable values, Persian and mixed-script labels, and both production-year calendars. It verifies entry writes on the migrated database and checks all records again after reopening.
- [x] M3.1 physical gate · `LedgerDaoTest` and `LedgerMigrationTest` passed on the Redmi Note 9S.

#### M3.1 verification · 2026-11-10

Used the pinned toolchain with Corretto JDK 17, Android SDK and Build Tools 36, and the existing offline Gradle cache. The final build succeeded in 1m 52s. The focused JVM task ran four `EntryInputTest` cases on the first build and was up to date on the final build. All four passed with zero failures, errors, or skips. The initial sandboxed Gradle invocation failed to start its file-lock service. The first local-socket build compiled the app and ran the JVM tests, then hit a migration test compilation error. The final build passed after explicit Room database cleanup and a typed fixture bind array. [JVM XML](app/build/test-results/testDebugUnitTest/TEST-org.unishiasy.vehiclelog.data.EntryInputTest.xml) · [Final Gradle log](app/build/reports/m31/gradle.log) · [Initial build log](app/build/reports/m31/initial-gradle.log)

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-amazon-corretto
export ANDROID_HOME="$HOME/Android/Sdk"
export GRADLE_USER_HOME="$PWD/.gradle/local-user-home"
./gradlew --offline --no-daemon --console=plain --max-workers=4 \
    "-Dorg.gradle.java.home=$JAVA_HOME" \
    -Pandroid.builder.sdkDownload=false -Porg.gradle.java.installations.auto-download=false \
    :app:testDebugUnitTest --tests org.unishiasy.vehiclelog.data.EntryInputTest \
    :app:assembleDebug :app:assembleDebugAndroidTest
```

Emulator `Medium_Phone_API_37.0` (`sdk_gphone64_x86_64`, Android 17/API 37), with the debug app and test APKs. `LedgerDaoTest` passed all six cases and `LedgerMigrationTest` passed its populated-v1 case. Runner result was `OK (7 tests)` in 1.37s. It confirmed foreign-key enforcement for inserts, edits and vehicle deletion, duplicate insert rejection, deterministic reads, missing updates, exact integer storage, validation on both write paths, and preservation of vehicle and entry values on reopen. [Instrumentation output](app/build/reports/m31/emulator-instrumentation.txt) · [Device and APK hashes](app/build/reports/m31/emulator-build.txt)

```bash
adb="$ANDROID_HOME/platform-tools/adb"
serial=emulator-5556
package=org.unishiasy.vehiclelog.data.local
"$adb" -s "$serial" install -r app/build/outputs/apk/debug/app-debug.apk
"$adb" -s "$serial" install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
"$adb" -s "$serial" shell am instrument -w -r \
    -e class "$package.LedgerDaoTest,$package.LedgerMigrationTest" \
    org.amirariana.vehiclelog.debug.test/androidx.test.runner.AndroidJUnitRunner
```

Physical Redmi Note 9S (`curtana`, Android 12/API 31). Repeated the two `adb install -r` commands and the same class-filtered `am instrument` command above with `serial` set to the connected Redmi's USB serial. Both installations succeeded. The debug app and test APK SHA-256 values matched the emulator run. `LedgerDaoTest` passed all six cases and `LedgerMigrationTest` passed its populated-v1 case. The runner reported `OK (7 tests)` in 0.358s. Room opened the migrated fixture, preserved all six fields in each of the three vehicle rows, and reopened the database with those vehicles and the new entry intact. The DAO tests used in-memory databases and the migration test used a separate file-backed fixture. [Physical instrumentation output](app/build/reports/m31/redmi-instrumentation.txt) · [Physical device and APK hashes](app/build/reports/m31/redmi-build.txt)

The schema comparison found identical `vehicles` definitions in versions 1 and 2, verified every entry column and constraint declared by Room, and matched the migration table and index DDL to the v2 export. Room also validated the migrated schema when opening the fixture in both instrumented runs. The v1 export and both signed release APKs matched their baseline SHA-256 values. [Schema comparison and hashes](app/build/reports/m31/schema-review.txt)

### M3.2a · Odometer storage and linked operations

- Room 2→3 creates only `odometer_readings` and its indexes. It keeps 1→2 registered for chained upgrades. The table has a UUID text primary key, a required vehicle reference and Gregorian date, whole kilometers stored as a Kotlin `Long`, and a nullable entry reference. The vehicle FK restricts deletion, the entry FK cascades deletion, and both use `ON UPDATE NO ACTION`. The vehicle/date index supports timeline reads and the unique entry index permits one reading per entry and multiple standalone readings. There are no SQL defaults or destructive fallback. [Room v3 schema](app/schemas/org.unishiasy.vehiclelog.data.local.VehicleDatabase/3.json)
- The repository validates mileage and linked vehicle/date agreement. It checks the maximum on strictly earlier dates and the minimum on strictly later dates in the same transaction as the write, excluding the edited reading. Bounds are inclusive. Same-day readings can arrive in any order. Zero, `Long.MAX_VALUE`, and future Gregorian dates are allowed. Current mileage is the highest reading on the latest recorded day, or null for an empty timeline.
- Linked creation and edits are atomic. `Keep` follows a changed date and revalidates mileage. A vehicle change with an existing link requires `Set` or `Remove`. Edits preserve existing IDs and the original entry creation instant. Repeating a create with the same ID and normalized stored values succeeds without another insert. A conflicting duplicate is rejected. Removing a link retains the entry, and deleting an entry cascades only to its link. Database write errors escape the transaction before the repository maps them to results.
- [x] M3.2a physical gate · All 16 focused instrumentation cases passed on the Redmi Note 9S.

#### M3.2a verification · 2026-11-10

Corretto JDK 17, Android SDK and Build Tools 36, and the existing offline Gradle cache. The focused build succeeded in 1m 5s. `EntryInputTest` passed four cases with zero failures, errors, or skips. The debug app and instrumentation APK builds passed. [Gradle log](app/build/reports/m32a/gradle.log) · [JVM XML](app/build/test-results/testDebugUnitTest/TEST-org.unishiasy.vehiclelog.data.EntryInputTest.xml)

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-amazon-corretto
export ANDROID_HOME="$HOME/Android/Sdk"
export GRADLE_USER_HOME="$PWD/.gradle/local-user-home"
./gradlew --offline --no-daemon --console=plain --max-workers=4 \
    "-Dorg.gradle.java.home=$JAVA_HOME" \
    -Pandroid.builder.sdkDownload=false -Porg.gradle.java.installations.auto-download=false \
    :app:testDebugUnitTest --tests org.unishiasy.vehiclelog.data.EntryInputTest \
    :app:assembleDebug :app:assembleDebugAndroidTest
```

Physical Redmi Note 9S (`curtana`, Android 12/API 31). Both debug APK installations succeeded. The runner reported `OK (16 tests)` in 0.918s. `OdometerOperationsTest` passed 8 cases, `LedgerMigrationTest` passed 2, and `LedgerDaoTest` passed 6. [Physical instrumentation output](app/build/reports/m32a/redmi-instrumentation.txt) · [Device and APK hashes](app/build/reports/m32a/redmi-build.txt)

```bash
adb="$ANDROID_HOME/platform-tools/adb"
# Set serial to the USB serial of the Redmi listed by adb devices.
package=org.unishiasy.vehiclelog.data.local
"$adb" -s "$serial" install -r app/build/outputs/apk/debug/app-debug.apk
"$adb" -s "$serial" install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
"$adb" -s "$serial" shell am instrument -w -r \
    -e class "$package.OdometerOperationsTest,$package.LedgerMigrationTest,$package.LedgerDaoTest" \
    org.amirariana.vehiclelog.debug.test/androidx.test.runner.AndroidJUnitRunner
```

- **Migration evidence.** Populated 2→3 preserved every field on three vehicle and three entry rows after upgrade and reopen. Migrated table and index definitions matched a fresh v3 database. A standalone reading and a linked reading persisted after reopen. The 1→2→3 fixture also preserved all three vehicle rows. The v1 and v2 exports and signed release APKs matched their baseline SHA-256 values. [Schema review and hashes](app/build/reports/m32a/schema-review.txt)
- **Rollback evidence.** Test-only SQLite triggers rejected the reading insert and update after observing the pending entry write. The failed creation left neither row. The failed edit restored both original rows and current mileage. A separate test submitted two incompatible writes concurrently and observed one save and one mileage conflict.

### M3.2b · Exact money and Jalali date adapters

- The money adapter accepts Persian, Arabic-Indic, and Latin digits, either decimal mark, and correctly grouped thousands using either separator consistently. It converts one fractional toman digit directly to whole rials with overflow checking. Blank input is distinct from invalid input and maps to null for the existing repository category rules. The formatter uses Persian digits, drops a zero fraction, and labels a null amount `مبلغ ثبت نشده`.
- **Verified Jalali range:** 1300–1498 inclusive (Gregorian `1921-03-21` through `2120-03-20`). The converter uses the Calendar Center's 1300 Farvardin anchor and published leap years. Its calculated year starts were checked against the separately transcribed Gregorian Farvardin column, and the JVM test checked every valid day in both directions plus every month-end rejection. The last published year is leap. Years 1499 and 1500 remain outside coverage because the authoritative source ends at 1498. Valid stored Gregorian dates outside the range get an explicit `میلادی` label. [University of Tehran Calendar Center table, archived](https://web.archive.org/web/20230712052459if_/https://calendar.ut.ac.ir/Fa/News/Data/Doc/KabiseShamsi1206-1498-new.pdf)
- The default-date adapter samples an injected clock once and returns the civil date in `Asia/Tehran`. Draft callers retain that date. Entry creation time remains a separate UTC instant. Device defaults depend on the Android time-zone database. [IANA Iran time-zone update](https://www.iana.org/time-zones/releases/2022b)

#### M3.2b verification · 2026-11-10

The focused offline build succeeded in 34s with the pinned Corretto JDK 17 and Android SDK 36. `MoneyTest` passed 5 cases and `JalaliDatesTest` passed 6, with zero failures, errors, or skips. The Jalali suite covered 72,684 supported days, all requested Nowruz and Esfand fixtures, and the March and July Tehran midnight boundaries. Both debug APK builds passed. [Gradle log](app/build/reports/m32b/gradle.log) · [Money JVM XML](app/build/test-results/testDebugUnitTest/TEST-org.unishiasy.vehiclelog.domain.MoneyTest.xml) · [Jalali JVM XML](app/build/test-results/testDebugUnitTest/TEST-org.unishiasy.vehiclelog.domain.JalaliDatesTest.xml)

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-amazon-corretto
export ANDROID_HOME="$HOME/Android/Sdk"
export GRADLE_USER_HOME="$PWD/.gradle/local-user-home"
./gradlew --offline --no-daemon --console=plain --max-workers=4 \
    "-Dorg.gradle.java.home=$JAVA_HOME" \
    -Pandroid.builder.sdkDownload=false -Porg.gradle.java.installations.auto-download=false \
    :app:testDebugUnitTest \
    --tests org.unishiasy.vehiclelog.domain.MoneyTest \
    --tests org.unishiasy.vehiclelog.domain.JalaliDatesTest \
    :app:assembleDebug :app:assembleDebugAndroidTest
```

Physical Redmi Note 9S (Android 12/API 31). Both in-place debug APK installs succeeded. `JalaliDatesDeviceTest` returned `OK (2 tests)` in 0.075s, covering the bidirectional boundary fixtures, Gregorian fallback, and both Tehran midnight cases. API 26 was unavailable. ADB listed the Redmi and the only installed emulator image was API 37. All three Room exports and both signed release APKs matched their baseline SHA-256 hashes. [Device output](app/build/reports/m32b/redmi-instrumentation.txt) · [Device and APK hashes](app/build/reports/m32b/redmi-build.txt) · [Baseline checksums](app/build/reports/m32b/baseline-checksums.txt)

```bash
adb="$ANDROID_HOME/platform-tools/adb"
# Set serial to the USB serial of the Redmi listed by adb devices.
"$adb" -s "$serial" install -r app/build/outputs/apk/debug/app-debug.apk
"$adb" -s "$serial" install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
"$adb" -s "$serial" shell am instrument -w -r \
    -e class org.unishiasy.vehiclelog.domain.JalaliDatesDeviceTest \
    org.amirariana.vehiclelog.debug.test/androidx.test.runner.AndroidJUnitRunner
```

### M3.3a · Persian RTL Quick Add and Edit Entry

- Each saved vehicle has an explicit ledger action. Its ledger shows the selected vehicle, Quick Add, and up to five recent entries that open the shared edit form. The existing vehicle setup remains available. The activity-scoped ledger ViewModel retains the selected route and draft during configuration changes.
- The form uses the approved money, calendar, fuel, and mileage contracts. It captures one draft UUID and clock instant for retries. The date editor uses Persian numeric Jalali formatting. Existing out-of-range dates display with a Gregorian label and are retained when the replacement field is blank. Fuel volume is required only for fuel. The most recent mileage appears separately from the blank new reading.
- Entry edits use the existing repository transaction. A blank edit mileage retains the existing link, an edited value updates it, and removal requires an explicit action. A mileage conflict offers correction or an explicit save without mileage. The draft remains available on failure, and success appears after the repository reports a commit. M3.2b independent re-review was skipped by product decision.

#### M3.3a verification · 2026-11-10

Corretto JDK 17, Android SDK and Build Tools 36, and the existing offline Gradle cache. The first focused build succeeded in 59s. All 14 JVM tests passed with zero failures, errors, or skips. `EntryDraftTest` passed 3 cases, `MoneyTest` passed 5, and `JalaliDatesTest` passed 6. The final harness rebuild succeeded in 45s with the JVM result up to date. Both debug APKs built successfully. [First focused build](app/build/reports/m33a/first-focused-build.log) · [Final build](app/build/reports/m33a/gradle.log) · [Draft JVM XML](app/build/test-results/testDebugUnitTest/TEST-org.unishiasy.vehiclelog.ui.ledger.EntryDraftTest.xml)

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-amazon-corretto
export ANDROID_HOME="$HOME/Android/Sdk"
export GRADLE_USER_HOME="$PWD/.gradle/local-user-home"
./gradlew --offline --no-daemon --console=plain --max-workers=4 \
    "-Dorg.gradle.java.home=$JAVA_HOME" \
    -Pandroid.builder.sdkDownload=false -Porg.gradle.java.installations.auto-download=false \
    :app:testDebugUnitTest \
    --tests org.unishiasy.vehiclelog.ui.ledger.EntryDraftTest \
    --tests org.unishiasy.vehiclelog.domain.MoneyTest \
    --tests org.unishiasy.vehiclelog.domain.JalaliDatesTest \
    :app:assembleDebug :app:assembleDebugAndroidTest
```

**Automated on the physical Redmi Note 9S (Android 12/API 31).** The first run stalled launching via ActivityScenario on MIUI and was stopped. The retry reused the project’s shell launch approach. It passed 6 cases in 24.782s, covering exact fuel creation and duplicate prevention, field errors and draft retention across activity recreation, recent-entry editing with Gregorian fallback and linked reading intents, storage failure and retry, explicit mileage-conflict fallback, and the production vehicle launcher regression. The enlarged test injected Compose font scale 1.5 and checked that the save button was visible and tappable while Android reported the IME visible. The system font setting was 1.0. [First run](app/build/reports/m33a/first-redmi-instrumentation.txt) · [Final runner output](app/build/reports/m33a/redmi-instrumentation.txt) · [Device and APK hashes](app/build/reports/m33a/redmi-build.txt)

```bash
adb="$ANDROID_HOME/platform-tools/adb"
# Set serial to the Redmi USB serial listed by adb devices.
app=org.amirariana.vehiclelog.debug
runner="$app.test/androidx.test.runner.AndroidJUnitRunner"
suite=org.unishiasy.vehiclelog.ui.ledger.LedgerEntryUiTest
vehicle=org.unishiasy.vehiclelog.ui.vehicle.VehicleSetupTest
"$adb" -s "$serial" install -r app/build/outputs/apk/debug/app-debug.apk
"$adb" -s "$serial" install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
filter="$suite#createsFuelForSelectedVehicleWithExactValuesAndOneCommit"
filter+=",$suite#retainsDraftAndErrorsAcrossActivityRecreationWithEnlargedText"
filter+=",$suite#editsFromRecentAndKeepsSetsAndExplicitlyRemovesLinkedReading"
filter+=",$suite#storageFailureKeepsDraftAndRetryUsesSameIdentity"
filter+=",$suite#mileageConflictAllowsExplicitSaveWithoutReading"
filter+=",$vehicle#normalActivityShowsManualFormWithoutCatalog"
"$adb" -s "$serial" shell am instrument -w -r -e class "$filter" "$runner"
```

**Offline persistence.** The instrumented write phase saved through the form into a separate file-backed Room fixture. After `am force-stop`, `pidof` confirmed the debug process was absent. A fresh read phase reopened the entry in the form and verified 120001 rials, Gregorian `2026-03-21`, 32450 mL, and a 1200 km linked reading. The two phases each passed one case, in 5.23s and 2.487s. The packaged debug app declares no internet permission. The phone's airplane mode was off, so this is local persistence evidence on a network-incapable app. [Write](app/build/reports/m33a/offline-write.txt) · [Force-stop evidence](app/build/reports/m33a/offline-fixture.txt) · [Read](app/build/reports/m33a/offline-read.txt) · [APK permissions](app/build/reports/m33a/debug-permissions.txt)

```bash
fixture="m33a-offline-$(date -u +%s).db"
test_case="$suite#savesAndReopensFileBackedEntryAcrossForceStop"
"$adb" -s "$serial" shell am instrument -w -r -e class "$test_case" \
    -e ledgerPersistencePhase write -e ledgerPersistenceName "$fixture" "$runner"
"$adb" -s "$serial" shell am force-stop "$app"
! "$adb" -s "$serial" shell pidof "$app"
"$adb" -s "$serial" shell am instrument -w -r -e class "$test_case" \
    -e ledgerPersistencePhase read -e ledgerPersistenceName "$fixture" "$runner"
```

**Visual review.** The captured dark-theme form at injected 1.5× had readable wrapped errors, mixed-script notes, and a visible save action in the sampled viewport. Actual system-wide 150% and TalkBack remain unverified. Room schema exports and signed release APKs matched their baseline SHA-256 values. [Screenshot](app/build/reports/m33a/enlarged-form.png) · [Baseline checksums](app/build/reports/m33a/baseline-checksums.txt)

### M3.3b · History browsing and safe deletion

- Complete per-vehicle history browsing with `LazyColumn` and stable entry IDs (`key = { it.id }`), removing the 5-entry limit.
- Deterministic ordering: `occurredOn DESC, createdAtEpochMillis DESC, id ASC`.
- Each history card displays Persian category label, Jalali date, exact toman amount (or "مبلغ ثبت نشده" for unpriced service), fuel volume with liters unit, linked odometer reading with kilometers unit, optional notes, and Edit and Delete actions.
- Edit action reuses the existing M3.3a `EntryForm`.
- Safe deletion is scoped by entry UUID and vehicle ID (`DELETE FROM entries WHERE id = :id AND vehicleId = :vehicleId`). Confirmed deletion cascades to linked odometer reading via SQLite `FOREIGN KEY ... ON DELETE CASCADE` while preserving standalone readings and the vehicle.
- Persian confirmation dialog (`AlertDialog`) with clear explanation that deleting an entry also removes its linked odometer reading.
- Deletion is available both from the history card and from within the edit form (`ledgerDeleteEntry`). Duplicate deletion requests are guarded with ViewModel `deleting` flag and disabled UI buttons.
- Deletion returns typed results (`Saved`, `NotFound`, `ConstraintViolation`, `StorageFailure`).
- History list updates reactively upon creation, edit, or deletion.

#### M3.3b verification · 2026-11-10

Corretto JDK 17, Android SDK and Build Tools 36, and Gradle offline mode. All unit tests passed (`testDebugUnitTest` 26 executed tasks, zero failures). Physical Redmi Note 9S (Android 12/API 31):
- `LedgerDaoTest`: 3 focused instrumentation tests passed in 0.843s (`historyQueryReturnsMoreThanFiveEntriesWithDeterministicOrderAndLinkedOdometer`, `deleteEntryCascadesToLinkedReadingAndPreservesStandaloneReadingAndVehicle`, `deleteScopedToVehicleRejectsMismatchedVehicleAndMissingEntry`).
- `LedgerEntryUiTest`: 8 UI instrumentation tests passed in 30.205s (`displaysCompleteHistoryBeyondFiveEntriesInDeterministicOrder`, `deletesEntryFromHistoryWithConfirmationAndCascadesLinkedReading`, `deletesEntryFromEditFormWithConfirmation`, and 5 regression tests).
- Offline persistence: File-backed Room fixture write, force-stop, and read passed across process termination.
- Room schemas v1, v2, v3 and signed release APKs 0.1.0 and 0.1.1 matched their baseline SHA-256 hashes byte-for-byte.

```bash
# Physical device instrumentation commands
adb="$ANDROID_HOME/platform-tools/adb"
serial=4c1ad625
runner="org.amirariana.vehiclelog.debug.test/androidx.test.runner.AndroidJUnitRunner"
"$adb" -s "$serial" shell am instrument -w -r \
    -e class "org.unishiasy.vehiclelog.data.local.LedgerDaoTest#historyQueryReturnsMoreThanFiveEntriesWithDeterministicOrderAndLinkedOdometer,org.unishiasy.vehiclelog.data.local.LedgerDaoTest#deleteEntryCascadesToLinkedReadingAndPreservesStandaloneReadingAndVehicle,org.unishiasy.vehiclelog.data.local.LedgerDaoTest#deleteScopedToVehicleRejectsMismatchedVehicleAndMissingEntry" \
    "$runner"

suite="org.unishiasy.vehiclelog.ui.ledger.LedgerEntryUiTest"
filter="$suite#createsFuelForSelectedVehicleWithExactValuesAndOneCommit"
filter+=",$suite#retainsDraftAndErrorsAcrossActivityRecreationWithEnlargedText"
filter+=",$suite#editsFromRecentAndKeepsSetsAndExplicitlyRemovesLinkedReading"
filter+=",$suite#storageFailureKeepsDraftAndRetryUsesSameIdentity"
filter+=",$suite#mileageConflictAllowsExplicitSaveWithoutReading"
filter+=",$suite#displaysCompleteHistoryBeyondFiveEntriesInDeterministicOrder"
filter+=",$suite#deletesEntryFromHistoryWithConfirmationAndCascadesLinkedReading"
filter+=",$suite#deletesEntryFromEditFormWithConfirmation"
"$adb" -s "$serial" shell am instrument -w -r -e class "$filter" "$runner"
```

### M3.4a · Monthly reporting calculation foundation

- The repository observes the existing per-vehicle Room query and calculates the report off the UI thread. It groups by the Jalali year and month of the stored Gregorian occurrence date, with newest months first and category keys in stable order. Empty history emits an empty report.
- Every assigned entry counts once. Only non-null amounts contribute to the sum. A month with only unpriced entries has a nonzero entry count and zero rials. Monthly counts and amounts are derived from the category subtotals.
- Totals use exact `BigInteger` whole rials. The optional `Long` conversion returns null outside the signed 64-bit range. Existing dates outside Jalali years 1300–1498 remain in the report as unassigned entries with their original values and an explicit coverage reason. Malformed stored dates get a distinct reason. Neither group contributes to an assigned Jalali month.
- Home can consume the report flow. Its presentation must handle zero and wider-than-`Long` totals and surface unassigned entries. The report reads and recomputes the selected vehicle's full history on each Room emission. Large-history performance has no benchmark in this slice.

#### M3.4a verification · 2026-11-10

Corretto JDK 17, Android SDK and Build Tools 36, and the offline Gradle cache. The build succeeded in 50s and produced both debug APKs. `MonthlyReportingTest` passed 7 cases and `JalaliDatesTest` passed 6, with zero failures, errors, or skips. Coverage includes adjacent Jalali months within one Gregorian month, leap Esfand and Nowruz, overlapping vehicles, category reconciliation, unpriced and empty history, exact overflow totals, and both ends of the supported calendar range. [Gradle log](app/build/reports/m34a/gradle.log) · [Reporting JVM XML](app/build/test-results/testDebugUnitTest/TEST-org.unishiasy.vehiclelog.data.MonthlyReportingTest.xml) · [Calendar JVM XML](app/build/test-results/testDebugUnitTest/TEST-org.unishiasy.vehiclelog.domain.JalaliDatesTest.xml)

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-amazon-corretto
export ANDROID_HOME="$HOME/Android/Sdk"
export GRADLE_USER_HOME="$PWD/.gradle/local-user-home"
./gradlew --offline --no-daemon --console=plain --max-workers=4 \
    "-Dorg.gradle.java.home=$JAVA_HOME" \
    -Pandroid.builder.sdkDownload=false -Porg.gradle.java.installations.auto-download=false \
    :app:testDebugUnitTest \
    --tests org.unishiasy.vehiclelog.data.MonthlyReportingTest \
    --tests org.unishiasy.vehiclelog.domain.JalaliDatesTest \
    :app:assembleDebug :app:assembleDebugAndroidTest
```

Physical Redmi Note 9S (Android 12/API 31). Both debug APK installations succeeded. `LedgerDaoTest#monthlyReportObservesVehicleScopedRoomChanges` passed in 0.246s. The in-memory Room fixture emitted an empty report for the selected vehicle despite an overlapping entry for another vehicle, then emitted the correct month and 123-rial total after a write for the selected vehicle. Room schema exports and signed release APKs matched their baseline SHA-256 hashes. [Device output](app/build/reports/m34a/redmi-instrumentation.txt) · [Device and debug APK hashes](app/build/reports/m34a/redmi-build.txt) · [Baseline checksums](app/build/reports/m34a/baseline-checksums.txt)

```bash
adb="$ANDROID_HOME/platform-tools/adb"
# Set serial to the Redmi USB serial listed by adb devices.
"$adb" -s "$serial" install -r app/build/outputs/apk/debug/app-debug.apk
"$adb" -s "$serial" install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
"$adb" -s "$serial" shell am instrument -w -r \
    -e class 'org.unishiasy.vehiclelog.data.local.LedgerDaoTest#monthlyReportObservesVehicleScopedRoomChanges' \
    org.amirariana.vehiclelog.debug.test/androidx.test.runner.AndroidJUnitRunner
```

### M3.4b · Persian RTL monthly expense dashboard

- From a saved vehicle's ledger, open **گزارش ماهانه**. The screen shows the selected vehicle, the current Tehran Jalali month, and months with data. An empty current month shows zero and stays selected. The activity-scoped ledger ViewModel retains month selection across report updates and configuration recreation. Returning from the dashboard restores the vehicle's existing ledger route.
- The dashboard reads the per-vehicle report and displays exact Persian toman totals, entry counts, unpriced counts, and category subtotals in ledger order. The reporting model adds unpriced counts to the same snapshots as the existing totals. Formatting divides `BigInteger` rials by 10 and renders the remainder directly.
- A separate section preserves the original Gregorian values and labels unsupported dates and malformed values separately. These entries remain outside the month totals. The report screen performs no writes. Loading, empty, and retry states are localized.

#### M3.4b verification · 2026-11-10

Corretto JDK 17, Android SDK and Build Tools 36, and the existing offline Gradle cache. The focused build succeeded in 45s. `MonthlyDashboardStateTest` passed 4 cases, `MoneyTest` passed 6, and `MonthlyReportingTest` passed 7, with zero failures, errors, or skips. Both debug APKs built successfully. [Gradle log](app/build/reports/m34b/gradle.log) · [Dashboard JVM XML](app/build/test-results/testDebugUnitTest/TEST-org.unishiasy.vehiclelog.ui.ledger.MonthlyDashboardStateTest.xml) · [Money JVM XML](app/build/test-results/testDebugUnitTest/TEST-org.unishiasy.vehiclelog.domain.MoneyTest.xml) · [Reporting JVM XML](app/build/test-results/testDebugUnitTest/TEST-org.unishiasy.vehiclelog.data.MonthlyReportingTest.xml)

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-amazon-corretto
export ANDROID_HOME="$HOME/Android/Sdk"
export GRADLE_USER_HOME="$PWD/.gradle/local-user-home"
./gradlew --offline --no-daemon --console=plain --max-workers=4 \
    "-Dorg.gradle.java.home=$JAVA_HOME" \
    -Pandroid.builder.sdkDownload=false -Porg.gradle.java.installations.auto-download=false \
    :app:testDebugUnitTest \
    --tests org.unishiasy.vehiclelog.ui.ledger.MonthlyDashboardStateTest \
    --tests org.unishiasy.vehiclelog.domain.MoneyTest \
    --tests org.unishiasy.vehiclelog.data.MonthlyReportingTest \
    :app:assembleDebug :app:assembleDebugAndroidTest
```

**Physical Redmi Note 9S (Android 12/API 31).** The first run failed at the shell-launch harness because the phone was asleep and locked. After the phone was unlocked, the same six checks passed in 21.728s. Three dashboard cases covered the selected vehicle, Tehran month default, empty and unpriced-only months, switching, exact totals and subtotals, both unassigned reasons, reactive updates, and activity recreation. Three regression cases covered fuel creation, editing with linked readings, and complete History. The supplemental API 37 emulator attempt failed in Espresso startup with `NoSuchMethodException` on `InputManager.getInstance` before UI assertions. [Final Redmi output](app/build/reports/m34b/redmi-instrumentation.txt) · [Debug APK hashes](app/build/reports/m34b/redmi-build.txt) · [First Redmi run](app/build/reports/m34b/first-redmi-instrumentation.txt) · [Emulator attempt](app/build/reports/m34b/emulator-instrumentation.txt)

```bash
adb="$ANDROID_HOME/platform-tools/adb"
# Set serial to the unlocked Redmi USB serial listed by adb devices.
"$adb" -s "$serial" install -r app/build/outputs/apk/debug/app-debug.apk
"$adb" -s "$serial" install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
suite=org.unishiasy.vehiclelog.ui.ledger.LedgerEntryUiTest
filter="$suite#dashboardDefaultsToEmptyTehranMonthForSelectedVehicleAndSwitchesMonths"
filter+=",$suite#dashboardDisplaysExactWideTotalSubtotalsAndBothUnassignedReasons"
filter+=",$suite#dashboardKeepsSelectedMonthAcrossReactiveUpdateAndActivityRecreation"
filter+=",$suite#createsFuelForSelectedVehicleWithExactValuesAndOneCommit"
filter+=",$suite#editsFromRecentAndKeepsSetsAndExplicitlyRemovesLinkedReading"
filter+=",$suite#displaysCompleteHistoryBeyondFiveEntriesInDeterministicOrder"
"$adb" -s "$serial" shell am instrument -w -r -e class "$filter" \
    org.amirariana.vehiclelog.debug.test/androidx.test.runner.AndroidJUnitRunner
```

**Exact display evidence.** The device showed `۱٬۸۴۴٬۶۷۴٬۴۰۷٬۳۷۰٬۹۵۵٬۱۶۱٫۴ تومان` for two `Long.MAX_VALUE`-rial expenses. Both category subtotals retained their fractional toman digit and the unpriced category showed zero. The test found the original `2120-03-21` and `2026-02-30` values with distinct labels and read them back intact from Room.

**Visual and accessibility evidence.** The wide-total screenshot was visually reviewed at an injected Compose 1.5× scale. The number fit and the unit wrapped onto a second line. The system font scale stayed at 1.0. System-wide 150% text, TalkBack, and the error retry path were not exercised on the device. Room schema exports and signed release APKs matched their baseline SHA-256 hashes. [Redmi screenshot](app/build/reports/m34b/redmi-wide-total.png) · [Baseline checksums](app/build/reports/m34b/baseline-checksums.txt)

### M3.6 · Minimal Home Hub and direct navigation

- Added `ui/home/HomeScreen.kt` and `HomeViewModel.kt`. Wired navigation in `MainActivity.kt`.
- Shows saved vehicles with in-memory explicit vehicle switching and selected vehicle indicator.
- Observes latest recorded mileage for selected vehicle via `LedgerRepository.observeCurrentMileage()`. Displays Persian digits with `کیلومتر` unit or empty message if no reading exists.
- Direct navigation actions: Quick Add (pre-populates fresh draft for selected vehicle), complete History, and Monthly Dashboard.
- When no vehicle exists, preserves the existing `VehicleScreen` vehicle-setup experience.
- Activity recreation preserves in-memory vehicle selection state without persistent active-vehicle writes.
- Back navigation returns cleanly from EntryForm, MonthlyDashboard, and History to Home.

#### M3.6 verification · 2026-11-10

- **Discrepancy resolution:** The M3.5 report statement mentioning `120001000L` rials for `12,000.1` toman was confirmed to be a documentation typographical error only. The actual test code in `LedgerEntryUiTest.kt:509` and production conversion in `Money.toRials()` correctly asserted and produced `120001L` rials.
- **JVM Tests:** `HomeStateTest` passed 6 cases with zero failures, errors, or skips (`BUILD SUCCESSFUL in 16s`).
- **Physical Device:** Redmi Note 9S (Android 12/API 31). `HomeScreenTest` passed 6 cases (`OK (6 tests)` in 13.751s) covering zero vehicles setup experience, single vehicle layout and mileage, multiple vehicles with explicit switching and mileage updates, direct navigation to Quick Add, History, and Dashboard, activity recreation preserving selection, and Quick Add / Dashboard regressions. `AppLaunchTest` passed in 1.856s, and `m35CompleteEndToEndAcceptanceJourney` passed in 10.991s.
- **Data safety:** Room schemas v1/v2/v3 and signed release APKs 0.1.0/0.1.1 match baseline SHA-256 hashes byte-for-byte.

### M3 acceptance and closure disposition

On 2026-11-11 the product owner closed M3 with accepted residual risks based on the recorded automated acceptance, including focused instrumentation on the Redmi Note 9S (Android 12/API 31). The remaining manual acceptance work and verification of the provisional targets are explicitly deferred. All M3 gates have a completed or deferred disposition. [Test strategy](docs/delivery/TEST_STRATEGY.md)

- [x] **Storage and migration.** `LedgerDaoTest` and `LedgerMigrationTest` passed 7 physical cases for Room 1→2. The Room 3 gate passed 16 physical cases across those suites and `OdometerOperationsTest`. Populated upgrades preserved vehicle and entry data. Coverage includes atomic writes, retries, mileage ordering and backdating. [M3.1 evidence](#m31-verification--2026-11-10) · [M3.2a evidence](#m32a-verification--2026-11-10)
- [x] **Money, calendar, and reporting.** Focused JVM and Redmi checks covered exact conversion, Jalali boundaries, per-vehicle monthly reconciliation, and exact totals beyond `Long`. [Adapter evidence](#m32b-verification--2026-11-10) · [Reporting evidence](#m34a-verification--2026-11-10) · [Dashboard evidence](#m34b-verification--2026-11-10)
- [x] **Ledger and Home interactions.** Focused Redmi checks covered creation, correction, confirmed deletion, linked mileage, dashboard reconciliation, and Home navigation. The file-backed fixture passed across force-stop and reopen. The recorded M3.5 instrumented journey passed in 10.991s, and the M3.6 Home suite passed all 6 cases. [Quick Add evidence](#m33a-verification--2026-11-10) · [History evidence](#m33b-verification--2026-11-10) · [Journey rerun and Home evidence](#m36-verification--2026-11-10)
- **DEFERRED / NOT VERIFIED · Manual airplane-mode end-to-end walkthrough.** The product owner deferred physical observation of logging fuel and oil service, backdating a reading, editing and deleting entries, and reconciling totals by hand with airplane mode on. The M3.3a file-backed fixture verified persistence across force-stop and reopen with airplane mode off. [Walkthrough completion criteria](docs/delivery/BACKLOG.md#manual-airplane-mode-walkthrough)
- **DEFERRED / NOT VERIFIED · Human Quick Add study.** The product owner deferred human entry-time measurements and verification of both provisional targets, a median under 15 seconds and at least 80% first-attempt completion. The recorded 10.991s is automated journey runtime. [Study completion criteria](docs/delivery/BACKLOG.md#human-quick-add-study) · [Pilot success metrics](docs/product/SUCCESS_METRICS.md)

**Product scope decision:** The outstanding business customer discovery interviews are deferred to future product validation and removed from the M3 exit criteria. No business recruitment or interviews are reported complete. Preserve the original recruitment, interview, and synthesis requirements in the [business validation track](docs/delivery/ROADMAP.md#business-validation-track) and [future product validation backlog](docs/delivery/BACKLOG.md#future-product-validation). The M4 engineering deliverable remains the local prototype with reminders and manual recovery.

**Accepted residual risks:**

- Physical TalkBack and actual system-wide 150% font were unverified for M3. Compose semantics and injected 1.5× checks provide partial coverage.
- Large-history performance remains unbenchmarked. Monthly reporting reads and recomputes the selected vehicle's full history on Room emissions.
- The Jalali converter is verified for years 1300–1498. Reports keep unsupported Gregorian dates and malformed stored values visible as unassigned with distinct labels. API 26 calendar device verification remains outstanding because that environment was unavailable.
- The dashboard's recoverable error retry path was not exercised on the physical device.

## M4. Local prototype

**Status:** Development complete. M4.1–M4.5 accepted. M4.6 signed APK verification, signed migration, and emulator acceptance passed. The product owner approved proceeding to M5. M4 physical-device release acceptance is **DEFERRED / NOT VERIFIED** and remains open. Public release readiness is unestablished. **Provisional effort:** 8–13 days.

**Dependencies:** M3 gate and approval of WorkManager and Kotlin serialization when needed. **Risk:** Delayed notifications, interrupted exports, corrupt imports, or destructive restore.

- [ ] Add `data/local/ReminderEntity.kt`, `ReminderDao.kt`, `data/ReminderRepository.kt`, `reminders/ReminderWorker.kt`, `NotificationChannels.kt`, `ui/reminders/RemindersScreen.kt`, and `RemindersViewModel.kt`. Update the manifest, Room migration, and Home. Due date and mileage states are correct. Date notification opt-in and denial both leave Home reminders usable.
- [ ] Add `data/backup/BackupSnapshot.kt`, `BackupCodec.kt`, `BackupRepository.kt`, `ui/settings/SettingsScreen.kt`, `SettingsViewModel.kt`, and `main/res/xml/backup_rules.xml` and `data_extraction_rules.xml`. A driver explicitly exports a versioned file and restores it transactionally into an empty ledger. Explain that the file must be retained by the driver and covers only the records present at export.
- [ ] Update `docs/delivery/ANDROID_RELEASE.md` and this plan with the prototype upgrade and recovery evidence. The original parallel research synthesis is tracked in the [future product validation backlog](docs/delivery/BACKLOG.md#future-product-validation).

**Automated:** `test/ReminderRulesTest.kt`, `BackupCodecTest.kt`, `androidTest/ReminderWorkerTest.kt`, `BackupRestoreTest.kt`, and populated migration coverage from M3.

**Physical Android:** Test reminders with permission denied and granted, export, create a later record, and restore into an empty test installation. Verify only the earlier records are in the file and try restoring into a populated test ledger. Upgrade the signed M3 build in place.

**Done:** Signed local prototype works offline end to end and a retained manual file recovers its ledger.

**Evidence and completion notes:** Pending.

### M4.1 · Reminder storage and due rules

**Notification-state decision before schema freeze.** Room stores reminder lifecycle only. Future notification work can store a durable delivery marker keyed by reminder UUID in an app-private no-backup file using atomic writes. This state belongs to one installation and is excluded from portable snapshots and platform backup. Restore keeps reminder IDs, thresholds and lifecycle and starts with fresh device-local delivery state. The future worker should recheck active/due state and opt-in, serialize work for the same reminder, post using a stable notification tag, then persist the marker. A retry after a crash between posting and persisting replaces any still-active notification. A notification dismissed during that crash window could be posted again. This matches best-effort delivery and the one-active-notification requirement without a Room table.

**Signed upgrade baseline.** The two local signed artifacts are controlled releases 0.1.0 and 0.1.1 (version codes 1 and 2), both for `org.amirariana.vehiclelog` and signed by the same certificate. They predate M3. No eligible signed M3 upgrade baseline was found in the workspace. M4 release acceptance still needs a release-owner-approved signed M3 baseline.

**Status:** M4.1 complete (2026-11-11). M4.2 can use the reminder repository. Its date-based due display must refresh on resume or a Tehran civil date change because the Room flow emits on data changes.

**Storage and migration.** Room v4 adds `reminders` with UUID text primary key, indexed vehicle FK (`ON UPDATE NO ACTION ON DELETE RESTRICT`), nonblank title, nullable Gregorian ISO date and whole-kilometer thresholds, and active/completed/canceled state. No SQL defaults. Repository input validation and SQLite insert/update triggers enforce one threshold and reject invalid dates, negative mileage, and unsupported states. Fresh production databases install the same triggers via the Room creation callback. The triggers are runtime DDL and Room's schema export describes the table, FK, and index. The earlier migrations remain registered. The API scopes reads and lifecycle transitions by vehicle and starts new records active.

**Verification.** Corretto JDK 17 and Android SDK/Build Tools 36. The sandboxed Gradle startup failed to open its local lock socket. The first approved run passed all 5 JVM tests (0.147s, zero failures, errors, or skips) but exposed a test-fixture compile error. After fixing the fixture, the identical command succeeded in 55s, with the JVM task up to date and both debug APKs built. [JVM XML](app/build/test-results/testDebugUnitTest/TEST-org.unishiasy.vehiclelog.data.ReminderRulesTest.xml) · [Gradle log](app/build/reports/m41/gradle.log)

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-amazon-corretto
export ANDROID_HOME="$HOME/Android/Sdk"
export GRADLE_USER_HOME="$PWD/.gradle/local-user-home"
./gradlew --offline --no-daemon --console=plain --max-workers=4 \
    :app:testDebugUnitTest --tests '*ReminderRulesTest' :app:assembleDebugAndroidTest :app:assembleDebug
```

Physical Redmi Note 9S (`curtana`, Android 12/API 31). Both debug APK installs succeeded. The class-filtered run returned **OK (5 tests)** in 1.966s. The populated v3 fixture preserved three vehicle rows, a fuel entry, a linked reading, and a standalone reading on reopen, and a new reminder persisted. The migrated schema, including the reminder triggers, matched fresh v4. The prior v1 and v2 migration fixtures passed through v4. The other device cases verified vehicle isolation, lifecycle transitions and SQLite rejection of invalid rows, missing vehicle references, and deletion of a referenced vehicle. [Device runner output](app/build/reports/m41/redmi-instrumentation.txt)

```bash
adb="$HOME/Android/Sdk/platform-tools/adb"
serial=4c1ad625
"$adb" -s "$serial" install -r app/build/outputs/apk/debug/app-debug.apk
"$adb" -s "$serial" install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
"$adb" -s "$serial" shell am instrument -w -r \
    -e class org.unishiasy.vehiclelog.data.local.LedgerMigrationTest \
    org.amirariana.vehiclelog.debug.test/androidx.test.runner.AndroidJUnitRunner
```

The debug app and test APK SHA-256 digests were `ae13c8c19512f97007406d6bdec6a9f6fd1a5ebd7a8e6c40391489df9a2c0461` and `aaba04b229252bc75dc6cbe0b19436c2689b2975e3da4868a9b179c861823cd5`. Pre/post SHA-256 checks passed for all three historical Room exports and both signed APKs. A JSON comparison confirmed the three v3 entity definitions are identical in v4. Signed upgrade verification remains an M4 release gate.

### M4.2 · Reminder UI and Home

**Status:** Complete (2026-11-11). Home shows due reminders for the selected vehicle and opens its Persian RTL reminder screen. Drivers can create a Jalali date or whole-kilometer reminder and complete or cancel an active reminder. The screen distinguishes pending, due, completed, and canceled items, explains the missing-mileage case, and preserves draft fields and UUID through a failed save or activity recreation. The ViewModel guards in-flight writes. Room v4 and the signed artifacts passed pre/post SHA-256 checks.

The Home and reminder screens observe vehicle-scoped Room rows and derived mileage while resumed. Observation restarts on resume, vehicle switch, or load retry. Date evaluation wakes at the next Tehran midnight, with an hourly upper bound on the delay to account for wall-clock changes. The injected clock was advanced in device tests without changing the phone clock.

**Verification.** Corretto JDK 17 and Android SDK 36. The first build passed all 8 focused JVM tests (3 draft and date timer cases, 5 existing rule cases) with zero failures, errors, or skips. It built the debug app in 57s. Repeating the command with `:app:assembleDebugAndroidTest` appended built both APKs in 48s with the JVM results up to date.

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-amazon-corretto
export ANDROID_HOME="$HOME/Android/Sdk"
export GRADLE_USER_HOME="$PWD/.gradle/local-user-home"
./gradlew --offline --no-daemon --console=plain --max-workers=4 \
    :app:testDebugUnitTest --tests '*ReminderDraftTest' --tests '*ReminderRulesTest' :app:assembleDebug
```

Physical Redmi Note 9S (Android 12/API 31). Both debug APK installs succeeded. The Compose runner reported **OK (9 tests)** in 38.031s. Three new cases exercised creation and validation of both thresholds, missing and changed mileage, vehicle isolation, completion and cancellation, failed-save draft retention through recreation and repeated-save protection, and both resume and foreground-midnight transitions. Six existing Home cases passed, covering vehicle setup entry, switching, activity recreation, Quick Add, History and Monthly Dashboard. [Device output](app/build/reports/m42/redmi-instrumentation.txt)

```bash
adb="$HOME/Android/Sdk/platform-tools/adb"
serial=4c1ad625
"$adb" -s "$serial" install -r app/build/outputs/apk/debug/app-debug.apk
"$adb" -s "$serial" install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
"$adb" -s "$serial" shell am instrument -w -r \
    -e class org.unishiasy.vehiclelog.ui.home.HomeScreenTest \
    org.amirariana.vehiclelog.debug.test/androidx.test.runner.AndroidJUnitRunner
```

Physical TalkBack and system-wide enlarged text remain unverified. The retained draft covers configuration recreation. M4.3 can build on the reminder UI once WorkManager is approved. The signed M3 baseline remains a separate M4 release gate.

### M4.3 · Opt-in date reminder notifications

**Status:** Complete (2026-11-11). M4.4 can proceed. The signed M3 baseline remains an M4 release gate.

AndroidX `work-runtime-ktx:2.11.2` is the sole added direct production dependency. The product owner approved WorkManager for this slice. This stable version supports minSdk 26, and the AAR metadata check passed against compileSdk 36. [AndroidX WorkManager release notes](https://developer.android.com/jetpack/androidx/releases/work#2.11.2)

The reminder screen offers an explicit installation-wide opt-in. Android 13 and later request `POST_NOTIFICATIONS` after opt-in. The app checks both app-level and channel settings and offers a route to Android notification settings when blocked. The Persian date-reminder channel uses default importance. Home continues to derive due status from Room independently of notification settings.

WorkManager keeps one unique periodic reconciliation request with an hourly interval and queues a one-time reconciliation after opt-in, permission response, Activity resume, or creation of a date reminder while opted in. Work reads the current Room state and Tehran civil date at execution. Completion and cancellation share the reconciliation lock and remove the active notification after the lifecycle write. Reconciliation removes active notifications that no longer correspond to a due row. Timing is best effort and can slip with OS scheduling, battery restrictions or force-stop. The merged debug manifest has no exact-alarm permission.

Delivery uses a stable tag based on the reminder UUID. Atomic per-UUID marker files live under `noBackupFilesDir/reminder-delivery` and stay outside the future portable snapshot. The opt-in preference is installation-local as well. The worker posts first and persists the marker second. A crash in between can cause a retry to replace an active notification, or post it again if it was already dismissed. Exactly-once delivery is therefore unavailable. Turning notifications off removes active notifications and retains delivery markers.

**Build verification.** Corretto JDK 17 and Android SDK 36. The initial online AAR metadata check passed in 55s. The first offline assembly lacked a WorkManager transitive JAR. One online debug assembly populated the cache and passed in 2m 55s. The focused offline build then passed in 3m 8s. `ReminderRulesTest` and `ReminderDraftTest` passed all 8 JVM cases with zero failures, errors, or skips. After revising the device permission fixture to use actual OS settings, the same command passed in 1m, with JVM results up to date. [Gradle log](app/build/reports/m43/gradle.log)

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-amazon-corretto
export ANDROID_HOME="$HOME/Android/Sdk"
export GRADLE_USER_HOME="$PWD/.gradle/local-user-home"
./gradlew --no-daemon --console=plain --max-workers=4 :app:checkDebugAarMetadata
./gradlew --no-daemon --console=plain --max-workers=4 :app:assembleDebug
./gradlew --offline --no-daemon --console=plain --max-workers=4 \
    :app:testDebugUnitTest --tests '*ReminderRulesTest' --tests '*ReminderDraftTest' \
    :app:assembleDebug :app:assembleDebugAndroidTest
```

**API 37 emulator.** After `pm revoke` and `pm grant` for the debug package's `POST_NOTIFICATIONS` runtime permission, the final `systemNotificationSettingControlsDelivery` test passed in each state (0.186s denied and 0.121s granted). Denied left the due item available in-app, with neither a posted notification nor a delivery marker. Granted posted a notification and persisted a marker. A two-phase check passed before and after an explicit `am force-stop` (0.103s and 0.046s). `pidof` confirmed the app process was absent between runs. The second process read the persisted marker and did not repost the dismissed notification. The permission dialog itself was not manually exercised. [Denied](app/build/reports/m43/emulator-final-denied.txt) · [Granted](app/build/reports/m43/emulator-final-granted.txt) · [Restart write](app/build/reports/m43/emulator-restart-write.txt) · [Restart read](app/build/reports/m43/emulator-restart-read.txt)

The debug app and final test APK SHA-256 digests were `859a45cf122d2143a3733d28ebebf0ad2df4a90e63d06dbcb789503e4590b42e` and `e335b9fef32de99922de7f2fe103d3df742034547efcd522bf2748523a7ccae4`. Room exports v1–v4 and both signed APKs matched their pre-change checksums.

**Physical Redmi Note 9S (Android 12/API 31).** Debug app and test APK in-place installs succeeded. The first three-case run passed delayed reconciliation and scheduling but failed to establish the blocked state because MIUI did not apply the shell AppOps toggle to the notification manager. The product owner switched the debug app's **Show notifications** setting off and then back on. The final blocked run passed 1 case in 0.123s. The final enabled run passed 3 cases in 0.616s, covering actual notification posting/removal, overdue delivery, duplicate reconciliation, pending and terminal exclusion, stale cleanup and unique WorkManager scheduling. In both permission states the due reminder remained available in-app. The two force-stop phases passed in 0.137s and 0.080s. The debug process was absent between phases, and the original enabled setting was restored. [Blocked](app/build/reports/m43/redmi-denied.txt) · [Enabled and scheduling](app/build/reports/m43/redmi-granted.txt) · [Restart write](app/build/reports/m43/redmi-restart-write.txt) · [Restart read](app/build/reports/m43/redmi-restart-read.txt)

```bash
adb="$HOME/Android/Sdk/platform-tools/adb"
serial=4c1ad625
app=org.amirariana.vehiclelog.debug
runner="$app.test/androidx.test.runner.AndroidJUnitRunner"
worker=org.unishiasy.vehiclelog.reminders.ReminderWorkerTest
scheduler=org.unishiasy.vehiclelog.reminders.ReminderSchedulingTest
"$adb" -s "$serial" install -r app/build/outputs/apk/debug/app-debug.apk
"$adb" -s "$serial" install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
# With MIUI Show notifications off for the debug app.
"$adb" -s "$serial" shell am instrument -w -r \
    -e class "$worker#systemNotificationSettingControlsDelivery" \
    -e notificationPermissionExpectation denied "$runner"
# With MIUI Show notifications restored to on.
"$adb" -s "$serial" shell am instrument -w -r \
    -e class "$worker#systemNotificationSettingControlsDelivery,$worker#delayedReconciliationAndRestartSkipPreviouslyDeliveredAndTerminalReminders,$scheduler" \
    -e notificationPermissionExpectation granted "$runner"
fixture="m43-restart-$(date +%s)"
for phase in write read; do
    "$adb" -s "$serial" shell am instrument -w -r \
        -e class "$worker#deliveryMarkerSurvivesProcessRestart" \
        -e notificationRestartPhase "$phase" -e notificationRestartFixture "$fixture" "$runner"
    if [ "$phase" = write ]; then
        "$adb" -s "$serial" shell am force-stop "$app"
        if "$adb" -s "$serial" shell pidof "$app"; then exit 1; fi
    fi
done
```

On `emulator-5566`, the same debug APK installs and single-method permission command were used with `pm revoke` or `pm grant` before the respective expectation. The same two-phase force-stop command was used for the marker check.

### Historical M4.4 codec record · 2026-11-11

This prior verification record describes a different draft wire format. The current implementation and verification appear in [M4.4](#m44--versioned-portable-snapshot-codec). Confirm whether any files from the earlier draft need support before enabling file import in M4.5.

Implemented the portable, versioned JSON snapshot data model and strict validation codec foundation for Vehicle Ledger (دفتر خودرو), without introducing new production dependencies or modifying Room schemas:

1. **Snapshot data model (`BackupSnapshot.kt`):**
   - Format version: `1` (`BACKUP_FORMAT_VERSION`).
   - Creation timestamp: `createdAtEpochMillis` (Long, strictly positive).
   - Monetary unit: `"IRR_LEGACY_RIAL"` (`BACKUP_MONETARY_UNIT_LEGACY_RIAL`).
   - Domain collections: `vehicles`, `entries`, `odometerReadings`, and `reminders` covering the complete Room v4 schema.
   - Lossless bidirectional conversion between `BackupSnapshot` and Room v4 entity classes (`toRoomEntities()` and `BackupSnapshot.fromRoomEntities(...)`).

2. **Strict limits and validation policy (`BackupCodec.kt`):**
   - Payload byte size limit: 5 MiB (`MAX_SNAPSHOT_BYTES = 5 * 1024 * 1024L`).
   - Collection size limits: 50 vehicles, 50,000 entries, 50,000 readings, 10,000 reminders.
   - Strict unknown field rejection at root level and within every record object (rejects silent injection, device-local notification markers, and scheduling metadata).
   - Deterministic JSON encoding: deterministic collection sorting (vehicles by `id ASC`; entries by `occurredOn DESC, createdAtEpochMillis DESC, id ASC`; readings by `occurredOn ASC, kilometers ASC, id ASC`; reminders by `id ASC`) and deterministic object key ordering.
   - Strict data integrity validation:
     - Unambiguous versions and monetary unit enforcement.
     - Duplicate identifier rejection across all entity collections.
     - Referential integrity checks on foreign keys (`entry.vehicleId`, `reading.vehicleId`, `reminder.vehicleId`, `reading.entryId`).
     - Linked odometer reading cross-entity consistency: verified entry reference, single-link enforcement, and matching vehicle and occurrence dates.
     - Vehicle-scoped chronological mileage sequence enforcement across distinct dates ($\max(D_1) \le \min(D_2)$ for $D_1 < D_2$).
     - Category-specific amount and fuel volume validation: `fuel` requires positive amount and fuel volume; `insurance`, `wash`, and `other` require positive amount and null fuel volume; `oil_change`, `repair`, and `parts` allow null or positive amount with null fuel volume.
     - Reminder constraints: nonblank title, exactly one threshold (`dueOn` date or `dueKilometers >= 0`), valid civil occurrence date, valid lifecycle states (`active`, `completed`, `canceled`).
     - Vehicle year and calendar pairing with allowed calendar validation (`solar_hijri`, `gregorian`).
     - Structured typed `SnapshotValidationError` reporting without exposing user notes or sensitive data.

3. **Build and test verification:**
   - Corretto JDK 17 and Android SDK 36 on Debian Linux.
   - Ran offline JVM unit tests via Gradle:
     `ANDROID_HOME=/home/amir/Android/Sdk JAVA_HOME=/usr/lib/jvm/java-17-amazon-corretto ./gradlew --offline --no-daemon -g .gradle/local-user-home --console=plain --max-workers=4 :app:testDebugUnitTest --tests '*BackupCodecTest*'`
   - Results: **23 passing tests**, 0 failures, 0 errors, 0 skipped (`BUILD SUCCESSFUL in 45s`, test suite time: 0.432s).
   - Hand-authored fixtures verified full Room v4 coverage, round-trip lossless fidelity, large 64-bit numeric handling (`Long.MAX_VALUE`), empty snapshot handling, deterministic encoding identity, byte and collection limits, unknown field rejection, foreign key errors, inconsistent linked readings, mileage decrease conflicts, invalid civil dates, invalid category rules, reminder threshold violations, and vehicle year/calendar validation.
   - Room schema files (v1, v2, v3, v4) and local signed release APKs (0.1.0, 0.1.1) verified unchanged with byte-for-byte matching SHA-256 checksums.

### M4.4 · Versioned portable snapshot codec

**Status:** Complete (2026-11-14). Ready for M4.5 file handling and atomic restore. [Format v1 schema and validation contract](docs/architecture/DATABASE_DESIGN.md#portable-snapshot-format-v1)

The pure codec uses the existing Kotlin JSON runtime. It encodes all four Room v4 record types with exact whole-rial integers, a UTC export instant, and deterministic field and row ordering. The 4 MiB inclusive limit applies before decoding and parsing. The format caps each collection and rejects unknown or duplicate keys and unsupported versions. Errors report categories without including source data. Device-local notification state has no fields in the format.

**Verification.** Corretto JDK 17 and Android SDK 36. The focused JVM command succeeded in 43s. `BackupCodecTest` passed all 11 tests in 0.879s with zero failures, errors, or skips. Coverage includes full and empty round trips, exact 64-bit values, canonical ordering, byte and collection boundaries, malformed and ambiguous JSON, unknown fields, references, lifecycle, and cross-day mileage ordering. This pure JVM slice used no Android device tests. Pre/post SHA-256 checks matched all four Room schema exports and both signed APKs. [Gradle log](app/build/reports/m44/gradle.log) · [JVM XML](app/build/test-results/testDebugUnitTest/TEST-org.unishiasy.vehiclelog.data.backup.BackupCodecTest.xml)

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-amazon-corretto
export ANDROID_HOME="$HOME/Android/Sdk"
export GRADLE_USER_HOME="$PWD/.gradle/local-user-home"
./gradlew --offline --no-daemon --console=plain --max-workers=4 \
    :app:testDebugUnitTest --tests '*BackupCodecTest'
```

**Remaining limitations.** M4.5 must enforce bounded provider reads before invoking the codec, handle rejection of over-limit ledgers, and verify transactional restore and the physical file workflow. Portable files are plaintext and unauthenticated. The earlier draft used different v1 keys, currency token, and limits. Current v1 rejects those payloads. If any such files need support, define an explicit legacy reader before enabling import. Future versions require an explicit decoder, and cross-platform consumers need exact 64-bit numeric parsing. The signed M3 upgrade baseline remains a separate M4 release gate.

### M4.5 · Manual file export and atomic restore

**Status:** Complete (2026-11-14). Ready for M4.6 implementation and acceptance planning. The signed M3 upgrade baseline remains a separate release gate.

Persian Settings is reachable from Home and the empty vehicle setup screen. Export uses Android's Create Document picker with an editable suggested JSON filename. It reads all four collections in one Room transaction, caps each query at its codec limit plus one row, then encodes snapshot v1 and writes and closes the selected document off the main thread. The screen explains plaintext handling, retention, and the need to export again after changes. The app uses temporary document URI access.

Restore uses Open Document. It checks that the ledger is empty before reading, reads at most 4 MiB plus one sentinel byte, closes the stream, and validates the entire payload with the accepted codec. It rechecks all four tables in the insertion transaction and inserts vehicles, entries, readings, and reminders in that order. Room invalidation refreshes observers after commit. Restore uses aborting inserts. Notification reconciliation is locked across the restore transaction and cleanup. After commit it cancels old active reminder notifications, clears device-local markers, and requests scheduling with the existing opt-in. A cleanup exception after commit has a distinct restored-with-warning result.

The packaged manifest keeps `allowBackup=false` and `fullBackupContent=false`. Packaged extraction rules exclude all app data domains for both Android cloud backup and device transfer. The debug APK declares no internet or broad storage access. Pre/post SHA-256 checks matched Room exports v1–v4 and both signed APKs. Workspace inspection found no retained JSON file from the earlier incompatible snapshot draft. [Manifest](app/build/reports/m45/packaged-manifest.txt) · [Extraction rules](app/build/reports/m45/packaged-data-extraction-rules.txt) · [Checksums](app/build/reports/m45/preserved-hashes.txt)

**JVM and build evidence.** Corretto JDK 17 and Android SDK 36. The initial main-only debug build succeeded in 2m 4s. Two intermediate instrumentation builds exposed fixture wiring compile errors during development. The final focused command built both debug APKs in **1m 31s** and passed all **14 JVM tests** with zero failures, errors, or skips. `BackupCodecTest` passed 11 in 0.804s and `BackupInputTest` passed 3 in 0.034s. [Final Gradle log](app/build/reports/m45/gradle.log)

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-amazon-corretto
export ANDROID_HOME="$HOME/Android/Sdk"
export GRADLE_USER_HOME="$PWD/.gradle/local-user-home"
./gradlew --offline --no-daemon --console=plain --max-workers=4 :app:assembleDebug
./gradlew --offline --no-daemon --console=plain --max-workers=4 \
    :app:testDebugUnitTest --tests '*BackupCodecTest' --tests '*BackupInputTest' \
    :app:assembleDebug :app:assembleDebugAndroidTest
```

**Physical Redmi Note 9S (Android 12/API 31).** Both debug APK installs succeeded. `BackupRestoreTest` passed **8 cases in 0.676s** using isolated in-memory Room databases. It verified exact row and integer preservation, a concurrent writer, the transactional emptiness recheck, invalid and oversized inputs, stream cancellation and failures, and notification cleanup only after commit. An abort trigger on the final table left the ledger empty, retained the existing marker, and skipped reconciliation. `BackupDocumentPickerTest` passed **1 case in 6.489s**. Through actual Redmi DocumentsUI it saved a uniquely named file in Downloads, inserted a later entry in the source fixture, opened the file into a separate empty fixture, verified that only the exported entry appeared, rejected a second restore, and verified export picker cancellation. Test-created files and fixture state were cleaned up without resetting the installed ledger. [Repository instrumentation](app/build/reports/m45/redmi-BackupRestoreTest.txt) · [DocumentsUI instrumentation](app/build/reports/m45/redmi-BackupDocumentPickerTest.txt)

```bash
adb="$HOME/Android/Sdk/platform-tools/adb"
serial=4c1ad625
runner=org.amirariana.vehiclelog.debug.test/androidx.test.runner.AndroidJUnitRunner
"$adb" -s "$serial" install -r app/build/outputs/apk/debug/app-debug.apk
"$adb" -s "$serial" install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
for suite in BackupRestoreTest BackupDocumentPickerTest; do
    "$adb" -s "$serial" shell am instrument -w -r \
        -e class "org.unishiasy.vehiclelog.data.backup.$suite" "$runner"
done
```

The app debug SHA-256 was `b3d80f92f6a3da165bb7e66e326b7654ad412215ff34e96813f3c08a3e0e6432` and test debug SHA-256 was `90446c091d82c0017b9cd8656e30e75458f861a41b7491129da146770afb0dd7`. Provider read and write failures were exercised with injected streams. Providers may leave a partial or empty destination after a failed export. A process death between database commit and notification cleanup can leave old markers for that restore. Absolute delivery timing and exactly-once posting remain outside the reminder guarantees. Actual system font scaling, TalkBack, and cross-provider picker behavior were outside the exercised device scenarios.

### M4.6 · Signed local prototype acceptance

**Status:** Signed artifacts verified and isolated emulator acceptance exercised on 2026-11-15. M4 remains **OPEN**. The release gate requires an offline cold launch of signed M4 and an in-place upgrade from the populated, reconstructed signed M3 test baseline (Room v3) on an isolated physical device, using the same application ID and signing certificate and a strictly higher M4 version code. Verify preserved vehicle and entry fields, linked and standalone odometer rows, and monthly reports. Use a safely controlled package instance or device. [Release procedure and artifact audit](docs/delivery/ANDROID_RELEASE.md#m46-release-readiness-audit)

The repository has no commits or remotes. The Git object store retains M3 closeout tree `189f4788853494afbad51d8a5c80f1d7e4dd7903`, with M3.6 Home and monthly reporting, the three Room v3 entities, schemas v1–v3, and the pinned Gradle wrapper and release script. It differs from tree `26ced12d0601c46e0dca1c3f8cb6cc99b6e3edd0` only in M3 closure documentation. The recovered tree compiled offline as a release variant in **1m 39s**, with 17 tasks executed. Its generated v3 export retained SHA-256 `1b4b988c391ab24b10839862355a7ce60bd2e2e7b04c7d3cd0e8e68144e62958`, matching the historical export in this workspace. The 98-file source archive is preserved under `.backups/vehicle-ledger-M3-source-189f4788853494afbad51d8a5c80f1d7e4dd7903.tar.gz`, SHA-256 `71cbb11b0a4f28c887a19be0b08f9d4bfe7b721b17b1073df68c23bc1c11893c`. [Compile log](app/build/reports/m46/m3-source-compile.log)

```bash
tree=189f4788853494afbad51d8a5c80f1d7e4dd7903
audit_dir=$(mktemp -d /tmp/vehicle-ledger-m46-source.XXXXXX)
git archive "$tree" | tar -x -C "$audit_dir"
export JAVA_HOME=/usr/lib/jvm/java-17-amazon-corretto
export ANDROID_HOME="$HOME/Android/Sdk"
export GRADLE_USER_HOME="$PWD/.gradle/local-user-home"
(
    cd "$audit_dir"
    ./gradlew --offline --no-daemon --console=plain --max-workers=4 \
        "-Dorg.gradle.java.home=$JAVA_HOME" -Pandroid.builder.sdkDownload=false \
        -Porg.gradle.java.installations.auto-download=false :app:compileReleaseKotlin
)
```

The only retained signed release APKs are the two M1 artifacts. The Redmi Note 9S (`curtana`, Android 12/API 31) has M1 version code 2 installed under `org.amirariana.vehiclelog`. Its pulled APK matched the retained M1 artifact byte-for-byte. The separate debug package remains installed. Phase 1 used read-only package queries and an APK copy. It did not change either installation or inspect ledger contents. Pre/post checks matched both signed APKs and Room schema exports v1–v4.

**Reconstructed baseline approved. Preflight BLOCKED.** The product owner approved tree `189f4788853494afbad51d8a5c80f1d7e4dd7903` for a newly signed M3 test baseline, conditionally using version code 3 followed by M4 code 4. This would be the first signed APK produced from that recovered M3 source in this workflow. The archive and a fresh `git archive` matched on all 98 file paths, types, modes, and contents. The tar container bytes differ. Both source trees declare release ID `org.amirariana.vehiclelog`. The release script's `--check 3 0.2.0` and `--check 4 0.3.0` passed. Codes 3 and 4 are unused in the local release ledger, available APKs, and connected device. Any off-workspace allocations still require developer confirmation. Both retained signed APKs passed checksum verification and report certificate SHA-256 `4da34964d2bd5de0bf98713e02b240fdb9438d18964d5d0c9efad248f1a8748a`. Verification of the private keystore alias in the developer's terminal remains pending.

On the sole connected Redmi, user 0 still has the signed M1 code 2 and the separately signed debug code 1. APK copies verified both public signers. The debug package has `vehicle-ledger.db` and its WAL/SHM files. The release package is nondebuggable and `run-as` denied access, so its private data state remains unknown. Neither package is installed for user 10. A profile on the same phone isolates app data but shares the release package binary with user 0. The available emulator failed its acceleration check because `/dev/kvm` was unavailable and emulator evidence alone cannot satisfy the physical-device gate. No installation, clear, uninstall, or signing was performed. Use a dedicated physical Android device with the release ID absent across all users, then use a separate empty profile on that device or another dedicated test device for restore. [Commands and isolation procedure](docs/delivery/ANDROID_RELEASE.md#m46-reconstructed-baseline-preflight)

Phases 2 and 3 remain pending. No signed M4 APK, cold-launch result, or signed in-place upgrade result exists in this audit. The accepted M4.3 notification evidence on the Redmi and API 37 emulator and the accepted M4.5 atomic restore and actual DocumentsUI tests remain recorded above. Those checks were performed on debug artifacts. M3 deferred product validation and accepted residual risks retain their existing disposition.

**Emulator-first follow-up (2026-11-14).** The owner approved isolated emulator acceptance before the physical gate and directed that both existing Redmi installations remain untouched. The M1 record verifies the alias historically. The intended private keystore and the complete release-code allocations remain unconfirmed for M4.6. No APKs were built or signed. `sha256sum -c /tmp/m45-before.sha` again passed for both M1 APKs and all four Room schema exports. [Private verification and emulator procedure](docs/delivery/ANDROID_RELEASE.md#m46-emulator-first-follow-up)

A disposable API 37 AVD started using software emulation and a separate ADB server. The 55-second and 165-second boot probes both timed out without observing `sys.boot_completed=1`. The host lacks KVM. No emulator acceptance checks ran. The signed populated upgrade, offline launch, reminders and notification paths, and physical SAF recovery still require acceptance on an isolated target. The existing Redmi and existing AVD were untouched.

**Signed build handoff (2026-11-14).** The developer confirmed that `vehiclelog-m1-test` is a `PrivateKeyEntry` in the existing test keystore with certificate SHA-256 `4da34964d2bd5de0bf98713e02b240fdb9438d18964d5d0c9efad248f1a8748a`. The developer confirmed no off-ledger use of codes 3 and 4 and allocated `3 / 0.2.0` to the reconstructed M3 test baseline and `4 / 0.3.0` to the M4 test prototype. Both targets use `org.amirariana.vehiclelog`. The two release-script preflights passed. A fresh extraction of the approved M3 Git tree matched all 98 files and modes in the retained archive. The current M4 build inputs were captured separately. [Source checksums and private signing commands](docs/delivery/ANDROID_RELEASE.md#m46-approved-test-builds)

The offline unsigned `:app:assembleRelease` builds passed for M3 in 4m 30s and M4 in 4m 46s, each with 50 tasks executed. AAPT2 confirmed the package and allocated versions in both APKs. The captured source checksums, both signed M1 artifacts, and historical schemas v1–v4 passed after assembly. The developer received the commands for the private signer prompts. Independent verification requires the resulting signed APKs. No devices or emulators were contacted during the build handoff.

**Signed acceptance attempt (2026-11-15).** `bash build/m46/inputs.vA8Ful/verify-signed.sh` independently passed metadata, 16 KiB alignment, v2/v3 signatures, signer continuity, artifact checksums, source manifests, and historical APK and schema checksums. M3 SHA-256 `c07ff482a24df3dc72a17ee341e5247676a131396b7dcf05aa1f251c9d20db62`; M4 SHA-256 `c95be26d57ce0b0f3450944eed8ef22bdfbddbf737a7eb69557d1150c85c2f22`. The fresh API 37 emulator booted with KVM. The signed upgrade preserved Android app ID number and first-install time, two UI-created categories, the linked 1,500 km reading, and the exact Aban 1405 report of 1,434.6 toman. Both M4 cold launches ran with airplane mode, no Wi-Fi, no active default network, and no telephony data connection. Standalone reading and pre-upgrade UUID coverage remain unverified on the signed fixture.

ADB-driven UI acceptance on the disposable emulator exercised the date reminder due state and Home list, API 37 permission denial and grant, notification posting and removal on completion, and mileage reminder creation and cancellation. Actual SAF export produced plaintext JSON. Populated restore was rejected and the later 300.2 toman entry remained visible. In a separate, initially empty Android user, SAF restore succeeded. Re-exported business payloads matched exactly, including UUIDs, amounts, thresholds and links, with only the export timestamp differing. The later record was absent. This was an automated emulator interaction, with no manual physical-device run. [Commands, evidence and limitations](docs/delivery/ANDROID_RELEASE.md#m46-independent-signer-and-emulator-checks)

**Automated gap follow-up (2026-11-15).** On a new disposable API 37 AVD, the existing M4 debug binary migrated a seeded v3 database to v4. Every column matched across one vehicle, two entries, and linked and standalone readings, including UUIDs, exact rial amounts and dates. Both SQLite checks passed. The installed Play Store image refused root, and the signed app refused `run-as`, leaving direct signed verification of the seeded fixture outstanding. Debug restore through the real SAF picker cleared stale markers and posted the imported due reminder. After forced stop and cold relaunch, the marker persisted and the due reminder still appeared on Home, with no duplicate notification observed in eight seconds. The focused restore-order instrumentation passed one test. Signed M4 itself showed the file error and no success when a test DocumentsProvider injected an `EIO` write failure. That provider retained a 16-byte invalid JSON document. Both signed APKs, the earlier signed fixture, historical schemas and signed M1 releases were preserved. **M4 remains OPEN** pending the full signed fixture and physical-device acceptance. [Commands, exact results and limitations](docs/delivery/ANDROID_RELEASE.md#m46-automated-gap-follow-up)

**Signed fixture feasibility recheck.** Both signed APKs passed independent verification again. The only local emulator image and the signed M3 interfaces provide no supported way to seed and read known UUIDs and a standalone reading. Stopped before a new install or upgrade. The signed field-by-field gate and physical acceptance remain open. [Exact checks](docs/delivery/ANDROID_RELEASE.md#m46-signed-fixture-feasibility-recheck)

**Signed database integrity acceptance (2026-11-15) — PASS on emulator.** The fresh AOSP API 35 userdebug AVD verified adb UID 0 and ran the unmodified signed M3→M4 upgrade. Pre/post inventories are byte-identical across all business columns, including fixed UUIDs, linked and standalone readings, NULLs, dates, timestamps, and exact rials. Room v4 identity, empty reminders table and both validation triggers were verified. SQLite integrity and foreign-key checks passed. The app ID and first-install time stayed constant, and the offline M4 cold launch retained the exact report. M4 remains OPEN for physical-device acceptance. [Exact commands and evidence](docs/delivery/ANDROID_RELEASE.md#m46-signed-database-integrity-acceptance)

### M4 development handoff

The product owner formally deferred the separate M4 physical-device acceptance run and approved proceeding to M5 development. This decision leaves the physical release gate open. The rooted AOSP API 35 signed migration, offline cold launch, and API 37 emulator reminder, notification and SAF evidence retain their recorded scope. The deferred gate requires the populated signed upgrade, offline cold launch, reminder lifecycle, notifications denied and allowed where supported, and SAF export and restore on an isolated physical device. Preserve the existing Redmi installations. Record the device, OS, signed hashes, commands, and actual observations before closing the gate. Public release readiness remains unestablished. M3 deferrals and accepted residual risks retain their recorded status. [Release evidence and deferred gate](docs/delivery/ANDROID_RELEASE.md#m4-development-handoff) · [Backlog](docs/delivery/BACKLOG.md#deferred-m4-physical-release-acceptance)

### M4 physical acceptance preparation

The product owner designated only the user-0 release ledger disposable and retained per-command approval for destructive actions. The debug package and its data are excluded. The retained M3 and M4 signer verification script passed. ADB reported serial `4c1ad625` not found and device enumeration was empty. Current device identity, installed signer, package identities and profile state need live verification. No device state was changed and the physical gate remains **DEFERRED / NOT VERIFIED** and open. [Preparation and approval boundary](docs/delivery/ANDROID_RELEASE.md#m46-physical-acceptance-preflight)

On reconnect, the authorized Redmi and both installed packages matched the expected serial, versions, users and app UIDs. The copied release APK matched the retained M1 APK byte for byte and the M3/M4 signer. The owner explicitly approved the one-time user-0 release clear subject to preflight. The phone remained asleep behind secure keyguard after a wake request, so preparation paused before consuming the approved clear. [Live preflight evidence](docs/delivery/ANDROID_RELEASE.md#m46-physical-acceptance-preflight)

The next live attempt found the Redmi awake and unlocked with the expected user and package state. The installed release APK and both candidate checksums matched again. Android denied the explicitly authorized release-only clear with `SecurityException` (`CLEAR_APP_USER_DATA`, exit 255). Both packages retained their original version, UID and per-user data inode. The M3 install did not start. Physical acceptance remains open. [Exact command and outcome](docs/delivery/ANDROID_RELEASE.md#m46-physical-acceptance-preflight)

The owner later cleared the release app through its exact App Info screen. Android event logs recorded the user-0 release process killed due to clear data. The retained signed M3 installed and cold-launched with its empty-ledger screen. MIUI rejected ADB touch injection with `INJECT_EVENTS`, so the synthetic fixture requires manual interaction. M4 has not been installed or tested on the phone. Debug package metadata remained unchanged after the owner's clear.

At the owner's request to move on from the populated fixture, installed the latest retained signed offline release, M4 0.3.0 (code 4), fresh on Redmi user 0. The release package was absent in both users before installation. The installed APK matched the retained bytes, and the cold launch and force-stop/relaunch reached Persian Home. Debug metadata was unchanged. This smoke test leaves the populated physical migration, offline cold launch, reminders and SAF acceptance open. The newer M5.4 APK is the separate local-cloud debug build. [Smoke-test evidence](docs/delivery/ANDROID_RELEASE.md#m4-redmi-fresh-install-smoke-test)

### Android-only MVP completion plan

The product owner prioritized the offline Android MVP and deferred further server development and external hosting or deployment. The target for this plan is the existing M4 local scope. M1–M3 retain their closure decisions. M4 development is complete, and its physical-device release acceptance remains **DEFERRED / NOT VERIFIED** and open. M5.0–M5.4 implementation and synthetic evidence are retained. External staging, real-data processing, and public release remain unapproved.

**Required MVP behavior.** A signed Persian RTL Android build must provide manual setup for one active vehicle, offline expense logging and correction, maintenance history, odometer handling, Jalali monthly totals, date and mileage reminders with in-app due state, opt-in best-effort date notifications, and explicit manual SAF export and atomic restore into an empty ledger. The existing evidence shows those flows are implemented. The open mandatory gap is isolated physical acceptance of the signed release workflow. Any reproduced data loss, unsafe restore, crash, or UI, accessibility, or performance defect that prevents a required journey must be resolved and retested before completion. [MVP scope](docs/product/MVP_SCOPE.md#m4-local-prototype) · [M4 acceptance criteria](docs/delivery/BACKLOG.md#deferred-m4-physical-release-acceptance)

| Order | Android-only slice | Evidence and exit |
|---|---|---|
| 1 | **Physical signed acceptance.** Start with a read-only preflight for the retained signed M3 and M4 APKs and an isolated physical target. Preserve the existing Redmi installations. | Verify artifact hashes, package, signer and versions. With device authorization, upgrade a populated synthetic M3 fixture to M4 in place. Verify an airplane-mode cold launch, representative offline ledger use and persistence, reminder lifecycle and notification deny/grant where supported. Export through DocumentsUI, add a later source record, restore into a separate empty ledger, compare export-time records, and verify populated-target rejection. Record device, OS, build, observations and failures. If the target cannot be isolated, record the blocker and stop before installation. |
| 2 | **Targeted usability, accessibility, and performance review.** Follow the same critical workflows on the isolated device using realistic synthetic history. | Check Persian RTL and mixed-script values, keyboard reachability, save/edit/delete recovery, TalkBack focus and announcements, and actual system-wide 150% text on key screens. Sample cold start, save-to-confirmation, History, and Dashboard with a documented large fixture. Exercise the dashboard retry path and, if an API 26 target is available, run the outstanding calendar checks. Fix only observed issues that block a core journey or compromise data safety. Record observations and retest affected paths. |
| 3 | **Android closeout.** Validate the fixes that were actually needed and prepare the offline handoff. | Rerun affected Android regressions and repeat affected physical checks on the intended signed candidate. If code fixes require a new signed candidate, follow the existing signing and approval workflow. Record the gate disposition and remaining risks. Prepare short Persian instructions for install/update, manual backup retention, and empty-ledger recovery. |

The first proposed task is slice 1, the isolated physical acceptance runbook and artifact/target preflight. Existing emulator acceptance stays recorded as emulator evidence. Begin execution only on an authorized isolated target.

**Optional and previously deferred work.** Visual polish and optimization beyond observed core blockers can wait. The scoped review above does not reopen M2/M3 or turn prior waivers and deferrals into passes or mandatory thresholds. System-wide 150% text and TalkBack waivers at M2, M3 accessibility and performance gaps, API 26 calendar verification, the physical dashboard retry gap, the deferred M3 manual walkthrough and human Quick Add study, catalog activation, and business discovery retain their recorded dispositions until new evidence or a separate decision. The proposed offline smoke check does not substitute for the deferred M3 walkthrough or timing study. [M3 residual risks](#m3-acceptance-and-closure-disposition) · [Deferred validation](docs/delivery/BACKLOG.md#future-product-validation)

**Server and closed-pilot work deferred.** Pause backend changes, account deletion, deletion journals, backup reconciliation and drills, PostgreSQL provisioning, external hosting and deployment, and cloud accounts. Keep the existing M5 synthetic work and the unapproved M6.1 proposal. The separate M6 closed-pilot scope, including local erase controls and voluntary aggregate measurement, stays on its own track.

### Android offline MVP 0.3.1 release candidate

The product owner accepted and closed Android MVP Hardening Slice 1. The candidate includes the approved keyboard traversal and dismissal, compact ledger form spacing, contextual Persian action labels, and History loading and scroll handling. The owner permits the existing M5 implementation to remain packaged. Cloud activation, server work, hosting and external staging remain deferred.

The earlier API 35 UI runner recorded **28 passed, 1 skipped, 0 failed** across 29 cases. The suite summaries were 7 Vehicle, 9 Home and 13 Ledger. The Ledger summary includes the `savesAndReopensFileBackedEntryAcrossForceStop` assumption skip because that test requires separate write and read invocations. The runner's raw `-4` status is retained in `build/offline-rc5.U4tGuX/prior-ui-run.log`.

The snapshot v1 limits are **4 MiB inclusive**, **100 vehicles**, **20,000 ledger entries**, **20,000 odometer readings**, and **5,000 reminders**. The current codec, bounded SAF read, and [data design](docs/architecture/DATABASE_DESIGN.md#portable-snapshot-format-v1) agree. The codec and Room v1–v4 exports match the retained M4 inputs. [Build provenance, manifest checks and signing gate](docs/delivery/ANDROID_RELEASE.md#android-offline-mvp-031-release-candidate)

**Focused JVM and debug APK build.** Executed from the same captured source, with JDK 17, SDK 36, and the existing offline Gradle cache. The debug flag was explicitly off. Both debug APKs assembled successfully. The JVM XML reports record **42 passed, 0 failed, 0 errors, 0 skipped** across eight classes. The combined Gradle invocation completed in 2m 42s.

```bash
./gradlew --offline --no-daemon --console=plain --max-workers=4 \
    -Pandroid.builder.sdkDownload=false -Porg.gradle.java.installations.auto-download=false \
    -PvehiclelogLocalCloud=false :app:assembleDebug :app:assembleDebugAndroidTest \
    :app:testDebugUnitTest --tests '*EntryDraftTest' --tests '*HomeStateTest' \
    --tests '*VehicleInputTest' --tests '*ReminderDraftTest' --tests '*ReminderRulesTest' \
    --tests '*BackupCodecTest' --tests '*BackupInputTest' --tests '*MonthlyDashboardStateTest'
```

**Focused Android regression.** Executed `ANDROID_HOME="$HOME/Android/Sdk" bash build/offline-rc5.U4tGuX/device-regression.sh`. The harness created a fresh Android 15/API 35 AOSP x86_64 AVD at 946 × 2100 px and 420 dpi, used ADB server port 5098 and serial `emulator-5598`, and installed both cloud-disabled debug APKs in that fixture only. Airplane mode was on and Wi-Fi was off. Telephony radio power was 0, the data connection state was -1, and Android reported no active default network. The emulator's saved `mobile_data` preference read 1 after the disable command. See `build/offline-rc5.U4tGuX/device.ECSVmF/network-runtime.log`.

| Focus | Passed | Failed | Skipped |
|---|---:|---:|---:|
| Vehicle setup and keyboard reachability | 7 | 0 | 0 |
| Home navigation, mileage and reminder state | 8 | 1 | 0 |
| Ledger, History scroll retention, 1.5× Compose font and Dashboard | 13 | 0 | 1 |
| Offline file-backed ledger write and read after force-stop | 2 | 0 | 0 |
| Reminder scheduling and worker | 2 | 0 | 0 |
| Room snapshot export and atomic restore | 8 | 0 | 0 |
| Actual DocumentsUI SAF export and restore | 1 | 0 | 0 |
| Initial total | 41 | 1 | 1 |

The Ledger class skip was the expected two-phase persistence test. The separately invoked write and read phases passed with exact rials, Gregorian date, fuel and linked mileage preserved. The SAF case confirmed export-time data in an empty target, exact `Long.MAX_VALUE` rials, later-source-entry exclusion, populated-target rejection and picker cancellation. The Home failure was `multipleVehiclesSwitchesSelectionAndCurrentMileage` at line 197. The selector was present but its mileage field still rendered the empty-state string when the test expected 1,200 km. The test's wait at that point only covers the selector, which can arrive before the mileage Room flow. The initial run and raw statuses are retained in `build/offline-rc5.U4tGuX/device-regression.log` and `device.ECSVmF/`.

**Home retry.** The first retry attempt stopped at its port check before boot or test execution. Executed `ANDROID_HOME="$HOME/Android/Sdk" bash build/offline-rc5.U4tGuX/home-retry.sh` with dedicated port 5099 and fresh serial `emulator-5584`. The unchanged mileage case passed in isolation. The resulting **42 passes, 1 initial failure, 1 expected skip** count includes both attempts on that case and the executed persistence phases. Evidence is in `build/offline-rc5.U4tGuX/home-retry-5584.log` and `device.wuqJv7/home-retry.log`. The failure did not reproduce. The test's asynchronous assertion remains a source of flakiness.

**Disposition.** The product owner confirmed that code 5 was unused across the complete release ledger, approved `0.3.1` and signed the retained APK privately without rebuilding. Independent verification passed with `bash build/offline-rc5.U4tGuX/sign-retained-rc5.sh --verify`. The signed artifact is `build/releases/5/vehiclelog-5-0.3.1.apk` with SHA-256 `7c13b93e04826b49c3c75671e20949bfa30a6ec940c1f01825d0845ff94cc07f`. All 138 original APK entries match byte for byte, and the source and historical checksums passed. The package has no `INTERNET` permission or cleartext override. The signed APK is ready for controlled testing on an authorized isolated target. [Signing commands and artifact verification](docs/delivery/ANDROID_RELEASE.md#android-offline-mvp-031-release-candidate)

The initial Home test waited for the selector and asserted before the Room mileage result. Home can briefly display the empty-mileage text during this load. The unchanged retry passed, with no evidence of persisted mileage loss or a persistent wrong display. This signing verification did not install or run the signed RC5. The Redmi and existing user AVDs were outside the run. M4 physical acceptance remains **DEFERRED / NOT VERIFIED** and open. No physical TalkBack, system-wide 150% font, API 26 device or representative large-history performance result was established.

### Android Offline MVP development closeout

**PASS · Android Offline MVP DEVELOPMENT COMPLETE.** The six approved development criteria are supported by the evidence below. The reachability criterion uses the recorded IME and injected Compose 1.5× tests. System-wide font acceptance and signed physical runtime acceptance retain their separate dispositions.

| Criterion | Development evidence |
|---|---|
| 1. Reliable vehicle, expense and mileage persistence | Populated M3→M4 signed emulator upgrade preserved all vehicle, entry and linked and standalone reading fields. The 0.3.1 source-matched debug tests passed vehicle setup and both offline ledger persistence phases across force-stop, with exact rials, dates, fuel and linked mileage. [Signed migration](docs/delivery/ANDROID_RELEASE.md#m46-signed-database-integrity-acceptance) |
| 2. Jalali dates, exact money and monthly reporting | M3 JVM and Redmi tests covered calendar boundaries and exact conversion. The 0.3.1 dashboard tests passed exact wide totals and subtotals, Tehran month selection and updates. Jalali conversion is verified for years 1300–1498, with unsupported dates retained and identified. [M3 closure](#m3-acceptance-and-closure-disposition) |
| 3. Safe atomic manual backup and restore | Eight 0.3.1 Room backup cases and the actual DocumentsUI flow passed. Tests covered rollback after the final-table insert failed, populated-ledger rejection and exact recovery into an empty ledger. [Candidate regression evidence](#android-offline-mvp-031-release-candidate) |
| 4. No known reproducible P0 crash, ANR or core workflow blocker | No reproducible app blocker appears in the recorded offline runs. The first Home mileage assertion failed before the Room flow returned, and the unchanged retry passed. The earlier M5 replacement-emulator System UI ANR remains a recorded limitation of that fixture. The 0.3.1 offline document picker passed on its isolated emulator. |
| 5. Primary action reachability with keyboard and 150% text | `reviewsManualFormOnDevice` passed vehicle Save and Cancel checks while the IME was visible. `retainsDraftAndErrorsAcrossActivityRecreationWithEnlargedText` scrolled to and activated Quick Add Save with the IME visible at injected 1.5× Compose text. Actual system-wide 150% on signed RC5 remains unverified. |
| 6. Resolution of known MVP visual defects and broken animations | Hardening Slice 1 is accepted and closed. The captured changes address form spacing, keyboard traversal, contextual action labels and History list replacement during entry loading. The 0.3.1 test passed History scroll retention through opening and closing edit. No remaining broken animation is documented for the core MVP flows. No separate animation audit was performed. |

**Artifact and acceptance boundary.** Reverified signed RC5 `org.amirariana.vehiclelog`, code 5, version `0.3.1`, SHA-256 `7c13b93e04826b49c3c75671e20949bfa30a6ec940c1f01825d0845ff94cc07f`. The captured release flag disables cloud initialization and UI. The signed manifest has no `INTERNET` permission, network security config or cleartext override. All 138 original APK entries, 118 captured input checksums, historical APK checksums and Room v1–v4 exports matched. The initial emulator regression remains 41 passed, one failed and one skipped, with the Home retry recorded separately as a pass. No build, install or runtime test ran in this review. [Artifact evidence](docs/delivery/ANDROID_RELEASE.md#android-offline-mvp-031-release-candidate)

The [focused signed RC5 runtime check](#focused-redmi-m4-to-rc5-upgrade) subsequently passed on the Redmi. The separate M4 physical gate remains **DEFERRED / NOT VERIFIED** and open. Public release readiness is unestablished. Server changes, cloud deployment and M6.1 account deletion remain paused. [Physical release gate](docs/delivery/BACKLOG.md#deferred-m4-physical-release-acceptance)

### Focused Redmi M4-to-RC5 upgrade

**BLOCKED before installation.** Live preflight verified Redmi Note 9S `4c1ad625`, Android 12/API 31, active user 0, signed M4 `0.3.0` code 4 and the matching RC5 signer. Both app packages were installed only for user 0. The signed M4 release Home showed no registered vehicles. A populated baseline could therefore not be captured. The RC5 install, pre/post comparison and offline cold launch were not executed. No app data or connectivity settings were changed. Record one synthetic vehicle with a model year and two expenses in the same Jalali month, one with a linked odometer reading, before repeating the baseline. The separate M4 physical release gate remains **DEFERRED / NOT VERIFIED** and open. [Commands and evidence](docs/delivery/ANDROID_RELEASE.md#focused-redmi-m4-to-rc5-upgrade)

**Populated baseline follow-up · BLOCKED before installation.** Device, package, profiles and signing checks passed again. M4 Home showed `پژو ۲۰۶ تست`, Solar Hijri model year 1400 and current mileage 15,000 km. History showed the wash as expected on `1405/08/21` for 50,000 toman. Fuel showed 150,000 toman, 30 liters and linked mileage of 15,000 km, with an observed date of `1405/08/25` where the expected date was `1405/08/20`. Stopped at the mismatch before opening the dashboard or installing RC5. The offline cold launch and pre/post comparison remain unverified. [Follow-up evidence](docs/delivery/ANDROID_RELEASE.md#populated-redmi-baseline-follow-up)

**PASS after the owner accepted the observed fuel date.** The populated signed M4 `0.3.0` code 4 installation was upgraded in place to the exact signed RC5 `0.3.1` code 5 artifact. The installed APK matched SHA-256 `7c13b93e04826b49c3c75671e20949bfa30a6ec940c1f01825d0845ff94cc07f`. Release UID 10474, first-install timestamp and user-0 data inode remained identical. Debug package metadata matched before and after, and neither package was installed for user 10. Following force-stop, RC5 cold-launched offline and showed the same Persian RTL Home, vehicle and 15,000 km, both expenses with the exact baseline values, and the same Aban 1405 report totaling 200,000 toman. The device was already offline. Its connectivity settings were unchanged. The separate M4 physical release gate remains **DEFERRED / NOT VERIFIED** and open. [Commands and pre/post values](docs/delivery/ANDROID_RELEASE.md#populated-redmi-rc5-upgrade-pass)

### Cafe Bazaar product direction

**Product-owner decision · 2026-11-16.** Plan for a production-quality Persian-first Cafe Bazaar release after the visual product experience and optional cloud recovery are complete and approved. The Offline MVP development closeout, Hardening Slice 1 closure, and focused signed RC5 physical pass retain their original scope. M1–M3 stay closed. The separate M4 physical gate remains **DEFERRED / NOT VERIFIED**. M5 stays complete for isolated synthetic development. Server, hosting and cloud deployment remain paused, and M6.1 stays **DEFERRED / BLOCKED BY PRODUCT-OWNER PRIORITY**. No real-data pilot or public release is authorized.

The [staged roadmap](docs/delivery/ROADMAP.md#roadmap-from-rc5-to-cafe-bazaar) places **Product Visual Design & UX Polish** immediately after the functional MVP. The [production checklist](docs/delivery/BAZAAR_RELEASE_READINESS.md) records the verified Bazaar guidance, open console checks and independent product release gates, including a production signer and application-ID decision. The RC5 test signing key retains its existing test scope.

**Next slice · RC5 design discovery and one design proposal.** Use retained synthetic RC5 screens. Produce a dated screen/state inventory with a short ranked issue list, draft Compose tokens and component specs, and reference screens for Home, Quick Add and Settings/manual recovery. Tie each proposed change to an existing driver task. Record Persian type, RTL and responsive choices, approved appearance scope, and the evidence expected for sign-off. No production implementation is authorized by this plan. End at the owner's design review. After approval, apply the agreed tokens and designs to Home and Quick Add as the first implementation slice. [Approval and exit criteria](docs/delivery/ROADMAP.md#product-visual-design--ux-polish)

### Product Visual Design & UX Polish — Complete

**Product-owner approval & verification · 2026-11-16.** The comprehensive visual and UX audit, competitor analysis (Fuelio, Drivvo, Mechanist/مکانیک همراه, CarKeep), and design system specification ([VEHICLE_LEDGER_DESIGN_SYSTEM_AND_UX_AUDIT.md](file:///home/amir/.gemini/antigravity/brain/24a527bc-3dcd-49df-a31e-6cfe2d48e738/VEHICLE_LEDGER_DESIGN_SYSTEM_AND_UX_AUDIT.md)) were formally approved and implemented across all six phases:

1. **Slice A · Design System Foundations:** Created `VehicleLogColors` (Petrol Navy `#1E3A5F`, Fuel Green `#1B5E20`, Service Amber `#B45309`, Alert Red `#B91C1C`, Slate neutrals), `VehicleLogShapes` (4dp extraSmall, 6dp small, 8dp medium, 12dp large, 16dp extraLarge; completely eliminating generic 28dp pill shapes), and `VehicleLogTypography` (Persian font metrics, zero letter-spacing, RTL direction, no glyph clipping). Verified in `VehicleLogThemeTest`.
2. **Slice B · Home Hub & Multi-Vehicle Selector:** Redesigned Home with clean title hierarchy, 12dp rounded Hero Status card showcasing primary bold Persian mileage (`۱۲۰,۵۰۰ کیلومتر`), 52dp Navy primary Quick Add button, side-by-side secondary outlined cards (`تاریخچه هزینه‌ها و سرویس‌ها` and `گزارش ماهانه` saving 48dp vertical height), and alert-styled cards for due reminders.
3. **Slice C · Quick Add & Entry Form:** Implemented single-tap horizontally scrollable category chips (`سوخت`, `تعویض روغن`, `تعمیر`, `بیمه`, `کارواش`, `عوارض و جریمه`, `سایر`) inspired by Drivvo & Fuelio with single-line labels and proper padding. Preserved dropdown trigger and test tags (`ledgerCategory`, `ledgerCategory:${key}`). 8dp field borders and high-visibility error states.
4. **Slice D · History & Monthly Dashboard:** Upgraded HistoryEntryCards with 12dp corners, bold category and primary-accented Toman amount, inline liters/km badges, and 40dp action buttons (`ویرایش ثبت`, `حذف`). Upgraded Monthly Dashboard with 12dp card, bold `headlineLarge` total, and category-colored proportional spend bars.
5. **Slice E · Reminders & Settings:** Horizontal filter chips for date vs mileage threshold selection, due alert border tints, and structured OutlinedCards for SAF export and restore.
6. **Slice F · Comprehensive Verification & Authentic Visual Audit:**
   - **JVM Unit Tests:** 94 unit tests executed and passed (`./gradlew testDebugUnitTest --rerun-tasks`) with 0 failures in 27s.
   - **Isolated Android 15/API 35 Emulator Instrumentation Tests:**
     - `HomeScreenTest`: 9 passed, 0 failed.
     - `VehicleSetupTest`: 7 passed, 0 failed.
     - `LedgerEntryUiTest`: 14 passed (13 passed, 1 expected skip for force-stop), 0 failed.
     - `BackupDocumentPickerTest`: 1 passed (real DocumentsUI flow verified), 0 failed.
     - `ReminderSchedulingTest`: 1 passed, 0 failed.
     - `VisualAuditCaptureTest`: 4 passed, capturing all 13 authentic screenshots across 100% and 150% font scales into the artifacts directory.
   - **Purity & Isolation:** Room schema v4 pinned (no DB changes); snapshot wire format pinned (no JSON changes); Persian copy, digits, RTL directionality, and legacy integer Rial math strictly preserved. Physical device (`4c1ad625`) remained strictly untouched.

### Approved Concept C v2 Production Design Implementation — Complete

**Product-owner approval & verification · 2026-11-16.** The full approved Concept C v2 design specification (`design/vehicle-ledger-concept-c-v2/ANTIGRAVITY_IMPLEMENTATION.md`) was implemented across all presentation layers:

1. **Design Tokens & Theme Switching (Slice 1):**
   - Palette: Concept C Light (`#075A63` Action, `#F3F6F8` Canvas, `#FFFFFF` Surface, `#CAD5DE` Border, `#E6ECEF` Wash) and Dark (`#A6DEDF` Action, `#101D22` Canvas, `#16262C` Surface, `#2B3B42` Border, `#1B2D34` Wash). Status colors: Fuel Green `#2E7D32`, Amber Alert `#ED6C02`, Danger `#C62828`.
   - Shapes: `VehicleLogShapes` with explicit corner radii (`ButtonShape` 8dp, `SheetShape` 20dp, `NoticeShape` 8dp, `ControlShape` 8dp, `CardShape` 12dp).
   - Theme Selector in Settings: System Default / Light / Dark modes persisted with immediate composition update and draft preservation.
   - Reusable components in `CommonComponents.kt`: `AppBottomDock`, `NoticeBanner`, `StickyFormFooter`, `DisclosureRow`, `EntrySelectorSheet`, `VehicleSwitcherSheet`.

2. **Root Navigation, Vehicle Switcher & Home Hub (Slice 2):**
   - Persistent `AppBottomDock` across `HomeScreen`, `HistoryView`, and `RemindersScreen` with active tab indicators and 48dp+ tap targets.
   - Quick Add floating central action on the dock opening `EntrySelectorSheet` for direct intent routing (Fuel, Expense, Service).
   - Vehicle Switcher bottom sheet (`VehicleSwitcherSheet`) with checkmark indicator and quick link to vehicle management.
   - Multi-vehicle selection cards on Home retained for rapid vehicle toggling and test compatibility.

3. **Entry Form & Categories (Slice 3):**
   - Quick Add / Entry Form with single-tap category chips, formatted legacy Rial/Toman input, and validation state retention across configuration changes.
   - Preserved linked odometer input, validation banners, and full IME accessibility with Save button reachable in the scrollable view and under 150% font scaling.

4. **Notebook History & Monthly Report (Slice 4):**
   - Day-grouped History with 40dp day pills (`dayNumber`) and connecting vertical timeline rail.
   - Monthly Dashboard with category spend breakdown, proportional indicator bars, Jalali month picker, and failure/empty retry handling.

5. **Reminders, Settings & Manual Recovery (Slice 5):**
   - Reminders with due amber accent strips, remaining distance calculation chips, and status chips.
   - Settings with appearance chooser, plain-text export warning banner, and atomic empty-ledger SAF restore flow. Cloud settings remain properly gated behind `BuildConfig.LOCAL_CLOUD_ENABLED`.

6. **Automated Verification & Visual Audit (Slice 6):**
   - **JVM Unit Tests:** 94 unit tests passed in 5s (`./gradlew testDebugUnitTest`).
   - **Isolated Emulator Instrumentation Tests (port 5584, AVD test_ui_5584):** 35 tests executed across 4 test suites with 0 failures:
     - `HomeScreenTest`: 9 passed, 0 failed.
     - `LedgerEntryUiTest`: 14 passed, 0 failed.
     - `VehicleSetupTest`: 7 passed, 0 failed.
     - `VisualAuditCaptureTest`: 5 passed, 0 failed.
   - **Authentic Visual Audit:** 19 authentic device screenshots captured directly to the artifacts directory covering Light Mode, Dark Mode, and 150% font scale across all core screens (Home single/multi-vehicle, Entry form & validation, History & delete confirmation, Monthly dashboard, Reminders, Settings & backup, and empty state).
   - **Invariants Preserved:** Room database schema v4 strictly locked; backup snapshot v1 strictly locked; Persian digits, RTL layouts, and legacy integer Rial math untouched. Physical device (`4c1ad625`) remained completely untouched.


## M5. Optional cloud snapshot recovery

**Status as of 2026-11-16: M5 DEVELOPMENT COMPLETE FOR ISOLATED LOCAL TESTING.** M5.0–M5.4 are complete for the approved synthetic scope. The Android cloud flow is opt-in debug only and uses a loopback backend. Option A approval covers the limited-pilot design. External staging remains unapproved and unverified, a real-data pilot remains unauthorized and unverified, and public release readiness remains unestablished. The separate M4 physical release gate remains **DEFERRED / NOT VERIFIED** and open. **Provisional effort:** 13–20 days under the baseline estimate. Re-estimate if client-side encryption is approved.

**Scope.** Room remains authoritative. Offline logging and manual SAF export and restore remain available without an account. Optional invite-only cloud recovery stores one current acknowledged snapshot per account after an explicit upload and restores only into a completely empty ledger. Cloud sync, automatic or background upload, merge and notification delivery metadata are outside scope. Account deletion, separate local erasure and the operational deletion ledger and restore drill remain in M6.

| Slice | Dependency and acceptance |
|---|---|
| M5.0 Contract review | Drafted the security alternatives, account and snapshot API, lifecycle policy, restore rules and independent acceptance matrix. Option A is approved for the limited pilot. Retention and external operation still require separate approval. [Security decision](docs/architecture/ADR/0004-cloud-recovery-security.md) · [API design](docs/architecture/API_DESIGN.md) · [Test matrix](docs/delivery/TEST_STRATEGY.md#m50-cloud-recovery-test-matrix) |
| M5.1 Account backend | Complete locally. TypeScript/Fastify and isolated PostgreSQL 18.7 with synthetic accounts. Verified one-winner invite and recovery transactions, account isolation, revocation, throttling and failure handling. |
| M5.2 Snapshot API | Complete locally under the approved Option A scope. Byte-preserving account slots, revision-conditional upload and download, bounded replay receipts and synthetic-only expiry. The PostgreSQL integration and independent M4 codec comparison passed. |
| M5.3 Android account and upload | Complete for local synthetic development. Persian opt-in Settings, encrypted account binding and upload staging, explicit retry and revision review, and retained server acknowledgements. Focused JVM, backend and API 35 emulator evidence passed. |
| M5.4 Android restore and staging | Complete for isolated synthetic development. Revision-bound download and the existing validated, atomic empty-ledger restore passed on two independent AOSP installations. Physical and staging acceptance require separate evidence and the approvals below. [M5.4 results](#m54-explicit-cloud-download-and-atomic-recovery) |

### M5 operational and acceptance gates

| Open gate | Decision or evidence required |
|---|---|
| External staging authorization | Approve hosting provider and region, jurisdiction, budget, and reachability from target Iranian networks. Obtain separate authorization for synthetic-only external staging before provisioning. |
| Transport and storage | Verify HTTPS for the staging Android client and API, private database access, and encryption for server storage and operational backups. Option A allows the service to read snapshot contents. |
| Credentials and operational access | Approve named operators, least-privilege access, credential and key custody, rotation, privileged-action audit, and incident ownership. |
| Real-data consent and privacy | Approve permitted data, driver consent, and Persian privacy disclosures covering server-readable snapshots, expiry, backup retention, deletion, and the offline SAF option before processing real data. Real-data pilot acceptance requires separate authorization and evidence. |
| Snapshot retention and expiry | Approve active snapshot lifetime, expiry behavior, and a purge deadline. The one-hour local fixture and request-driven expiry are synthetic evidence. The proposed 90-day real-data lifetime and 24-hour purge deadline remain unapproved. |
| Operational backup and deletion | Approve encrypted off-host backup retention and custody, account and snapshot deletion procedures, and how deletion and expiry are reapplied before a restored server accepts traffic. M6 retains implementation and the off-host restore drill. |
| External staging and physical Android | Record staging and physical results separately. Verify exact snapshot recovery to a second physical installation, interrupted upload, expired sessions, and continued local logging offline. Exercise staging account isolation, revision conflicts, and lost acknowledgements. |

See the [security and cost decisions](docs/architecture/ADR/0004-cloud-recovery-security.md#approval-and-cost-gates) and [acceptance matrix](docs/delivery/TEST_STRATEGY.md#m50-cloud-recovery-test-matrix). The [M4 physical gate](docs/delivery/BACKLOG.md#deferred-m4-physical-release-acceptance) remains a separate release requirement.

**M5.0 evidence:** Reviewed project scope, the existing portable v1 codec and parser limits, bounded backup reads and the authoritative Room restore transaction and post-commit notification path. Documentation only. No builds, application tests, credentials or external provisioning.

### M5.1 local environment and dependency preflight

- Existing user-local Node.js **26.13.0** and npm **12.2.2** can run the TypeScript subset supported by native type stripping and the built-in test runner. Node's built-in Argon2id reproduced the RFC 9106 §5.3 public vector. This verifies primitive availability only.
- No project backend manifest, Fastify or PostgreSQL driver, PostgreSQL executables, or Docker/Podman installation was found. A read-only Debian APT simulation for `postgresql-18=18.6-3` and `postgresql-client-18=18.6-3` showed five new system packages and zero upgrades. No packages were installed.
- Proposed direct npm dependencies are `fastify@5.12.8` (MIT, HTTP handling, body bounds and request injection) and `pg@8.23.0` (MIT, parameterized PostgreSQL queries and transaction connections). Review the resolved transitive dependency licenses and advisories, pin the lockfile and track security updates. Use Node built-ins for crypto and testing. [Fastify releases](https://github.com/fastify/fastify/releases) · [node-postgres changelog](https://github.com/brianc/node-postgres/blob/master/CHANGELOG.md)
- Proposed PostgreSQL tooling is the Debian amd64 `postgresql-18`, `postgresql-client-18` and `libpq5` packages (PostgreSQL License, cached candidate `18.6-3`). After approval, recheck Debian for upstream 18.7 security fixes or the corresponding patched packages. Prefer project-local extraction into ignored build storage and run a disposable, user-owned cluster with a private Unix socket and TCP disabled. Keep the backend bound to loopback and use synthetic accounts only. Version-pin and recheck packages when refreshing local tooling. [PostgreSQL license](https://www.postgresql.org/about/licence/) · [PostgreSQL 18.7 release notes](https://www.postgresql.org/docs/release/18.7/)
- Implementation remains pending. The first migration would create account, invite, session, recovery-verifier and persistent throttle state with transactional migration tracking. Test concurrent invite/recovery redemption, session ownership and revocation, throttle persistence, and configuration and logging failures against real PostgreSQL. No backend integration tests or migrations ran in this preflight.
- M5.2 requires accepted M5.1 integration tests and a reusable account authorization and serialization boundary. The proposed 90-day snapshot expiry remains unapproved for real data. Snapshot retention and operational backup expiry are outside M5.1.

### M5.1 PostgreSQL security version gate

The product owner approved local use of `fastify@5.12.8`, `pg@8.23.0`, PostgreSQL 18 tooling and required libpq dependencies, and Node.js 26 built-in Argon2id. Installation and implementation are gated on a security-fixed PostgreSQL 18 build.

Refreshed signed amd64 package metadata for official Debian `forky`, `forky-updates`, `forky-backports` and `forky-security` into the ignored project directory `build/m51-apt.NqW82j`. The first sandboxed request failed DNS resolution. The network-enabled retry passed using these commands with the isolated APT configuration.

```bash
export APT_CONFIG="$PWD/build/m51-apt.NqW82j/apt.conf"
apt-get update
apt-cache policy postgresql-18 postgresql-client-18 libpq5
```

All three packages showed `Installed: (none)` and `Candidate: 18.6-3` from `forky/main`. Debian lists that build as vulnerable to issues fixed in `sid` at `18.7-1`, including CVE-2026-120154 and CVE-2026-120153. **STOP at preflight.** No packages or npm dependencies were installed. No npm lockfile, cluster, backend, migration, or integration tests were created or run. No system APT sources or services were changed. [Debian security tracker](https://security-tracker.debian.org/tracker/source-package/postgresql-18) · [PostgreSQL 18.7 release notes](https://www.postgresql.org/docs/release/18.7/)

Minimal next step is approval to use official Debian sid metadata in a separate project-local APT configuration for the matching `postgresql-18`, `postgresql-client-18` and `libpq5` packages at `18.7-1`. Verify signatures, package versions and host library compatibility before extracting into ignored project storage. If a fixed build reaches the configured suites first, recheck those suites. Then complete npm provenance and advisory checks before installation. M5.2 remains gated on passing the M5.1 PostgreSQL-backed tests and verifying the account authorization and transaction boundary.

### M5.1 implementation and local test evidence

After the product owner approved project-local Debian sid, the signed sid amd64 index offered exact `18.7-1` builds for `postgresql-18`, `postgresql-client-18` and `libpq5`. Downloaded those versions via APT and extracted them with `dpkg-deb -x` into `build/m51-sid.NWSfg7/root`. PostgreSQL, initdb, pg_ctl and psql each reported `18.7 (Debian 18.7-1)`. System APT configuration and services were untouched. [Debian security tracker](https://security-tracker.debian.org/tracker/source-package/postgresql-18)

Registry metadata reported the approved direct versions, MIT licenses, publisher repositories and SHA-512 integrity values. Resolved the exact lockfile with scripts disabled and optional dependencies omitted. The pre-install advisory audit returned zero known advisories. The install used the official npm registry and scripts remained disabled. Post-install verification confirmed 60 registry signatures and 24 attestations across the installed tree. These are point-in-time supply-chain checks. [Local setup](server/README.md)

```bash
export APT_CONFIG="$PWD/build/m51-sid.NWSfg7/apt.conf"
apt-get update
apt-cache policy postgresql-18 postgresql-client-18 libpq5
# From build/m51-sid.NWSfg7/debs:
apt-get download postgresql-18=18.7-1 postgresql-client-18=18.7-1 libpq5=18.7-1

# From build/m51-sid.NWSfg7/npm, using Node.js 26.13.0 and official registry:
npm install --package-lock-only --ignore-scripts --omit=optional --no-audit
npm ls --package-lock-only --all --omit=optional
npm audit --omit=optional --json
# From server after copying the checked manifest and lockfile:
npm ci --ignore-scripts --omit=optional --no-audit
npm audit signatures --omit=optional

# From project root:
export PATH="$HOME/.nvm/versions/node/v26.13.0/bin:$PATH"
export VEHICLE_PG_TOOLS="$PWD/build/m51-sid.NWSfg7/root"
bash server/test/local-postgres.sh test
```

The disposable cluster ran as host UID 1000 with TCP disabled, a private mode-0700 Unix socket, peer authentication and distinct migration owner, app and invite-operator database roles. The `vehicle_m51_test` database contained only synthetic accounts. The first sandboxed socket bind returned EPERM. The network-enabled local run passed the PostgreSQL startup check. The first integration execution passed 11 of 12 tests. The missing-config assertion invoked a URL-encoded path with spaces, so the child process never reached the app. Replaced URL pathname access with `fileURLToPath` and added a real loopback listener check. Final focused execution passed **13/13**, zero failures or skips, in 8.785 seconds, including PostgreSQL migrations and concurrency. TypeScript syntax and shell syntax checks passed. The integration script stopped the cluster and retained its disposable fixture path. A separate `bash server/test/local-postgres.sh dev` smoke test reached readiness, returned `404 route_missing` on loopback for `GET /v1/backup` and shut down its dev cluster after SIGINT. The initial preflight cluster was also stopped.

The account service bounds JSON and Argon2id work, applies persistent atomic HMAC-keyed auth limits, stores only account credential verifiers, and serializes account credential changes with session authorization. HTTP errors expose only stable categories and generated request IDs. Snapshot endpoints are absent. Verified retained signed M3 and M4 APK hashes against their recorded values. No Android, Room schema or signed artifact changes.

M5.2 requires separate implementation authorization and should reuse the verified account lock and in-transaction session recheck. Exact-byte snapshot validation, revision conflict handling, acknowledgements and replay receipts still require tests. Snapshot retention for real data, external TLS and storage encryption, operator and credential isolation, provider and jurisdiction approvals, auth resource benchmarking, and production housekeeping for expired sessions remain pending. M4 physical release acceptance remains deferred and open.

### M5.2 snapshot API and test evidence

Implemented authenticated snapshot metadata, explicit raw upload, and revision-bound download. Migration `002_snapshots.sql` adds account-owned slots and receipts. Upload uses the existing account lock and in-transaction session recheck, then commits the new revision and receipt atomically. The M4 v1 validator preserves numeric tokens as signed 64-bit integers and writes the original UTF-8 bytes. Tests include a byte-for-byte retained SAF export from the unmodified signed M4 emulator fixture and independently constructed variants. No packages were added. [API](docs/architecture/API_DESIGN.md#snapshot-requests) · [Local setup](server/README.md)

```bash
export PATH="$HOME/.nvm/versions/node/v26.13.0/bin:$PATH"
export VEHICLE_PG_TOOLS="$PWD/build/m51-sid.NWSfg7/root"
bash server/test/local-postgres.sh test
# The fixture invokes npm test, which runs:
# node --test --test-concurrency=1 test/accounts.test.ts test/snapshots.test.ts
```

Final run on Node.js 26.13.0 and PostgreSQL 18.7 passed **26/26**, 13 M5.1 regression and 13 M5.2 tests, zero failures or skips, in **14.139 seconds**. The earlier 24-test run also passed. The script stopped the disposable PostgreSQL fixture. The race test observed both writes waiting on the actual PostgreSQL account-row lock, then verified one accepted revision. A lost-acknowledgement test closed a real loopback connection before commit and confirmed the same acknowledgement through a different server instance. Logout and exact session-expiry boundary cases both won against waiting uploads. A PostgreSQL trigger forced receipt insertion to fail after the slot update and the transaction preserved the prior bytes and revision. Tests also covered account isolation, no implicit upload on read, input limits, receipt expiry, and per-account rate limiting.

A read-only JVM comparison used the existing compiled Android `BackupCodec`, Corretto 17 and cached Kotlin and serialization jars. The comparison harness is retained in ignored `build/m52-codec.zlGs9z`. Its `compare.mjs` ran with Node 26, the JVM executable and the cached app/stdlib/json/core classpath. Both implementations agreed on all **41** accept/reject cases, including the signed M4 export, UTF-8 and escaped text, duplicate keys, invalid references, signed 64-bit boundaries and canonical instants. This is sampled compatibility evidence. No Android sources, Room schemas or signed APKs were modified. The retained signed M3 and M4 checksums matched the release record.

The one-hour development lifetime is synthetic-only. There is no approved 90-day real-data retention setting, scheduled purge, external hosting or backup implementation. Stored receipts support retries for seven days. After that, a stale original revision still cannot write. Ingress concurrency is process-local and external deployment needs capacity and abuse review, TLS, encrypted storage and backups, policy approval and operational controls. M5.3 Android integration remains a separate slice. M4 physical release acceptance remains deferred and open.

### M5.3 optional Android account and explicit upload

The Persian RTL Settings flow offers invite registration, sign-in, one-time recovery code display, ledger binding, sign-out, and explicit send, retry or revision review. Android uses the approved M5.1/M5.2 endpoints, platform HTTP, existing Kotlin JSON and Android Keystore. M5.3 uses Room's unchanged M4 capture transaction and codec for SAF and cloud. It adds no dependencies, Room migrations or WorkManager jobs. Recovery code redemption and cloud download and restore remain outside this slice.

The encrypted, atomically replaced state in `noBackupFilesDir` contains the bearer, immutable account binding, last valid acknowledgement and at most one pending attempt with its original bytes, attempt ID and base revision. Passwords and recovery codes are memory-only. A session failure clears the local bearer while retaining the binding and the pending upload. A different account cannot upload a populated ledger bound to the previous account. The supported empty-ledger rebind is explicit and rechecks emptiness inside Room. Sign-out is committed locally before the best-effort server request. Private state never appears in SAF, Android cloud backup or device transfer. An unavailable key fails closed for cloud actions and leaves local ledger access available.

The client persists an attempt before calling PUT and accepts an acknowledgement only with the matching attempt, new revision, ETag, byte length and SHA-256. Status reads and process restart never replay it. Ambiguous outcomes keep the same bytes available for explicit retry, even after local changes. A rejected or superseded retry leads to explicit review. Review reads can present the currently observed revision for confirmation. They never turn pending data into a local upload acknowledgement. Approval of replacement captures a new snapshot and still requires the reviewed revision at the server.

**Network boundaries.** Build with `-PvehiclelogLocalCloud=true` to enable the feature in debug only. `app/src/debug/AndroidManifest.xml` supplies INTERNET and a debug network security configuration with cleartext allowed only to the exact literal `127.0.0.1`. The transport accepts only `http://127.0.0.1:8765` and fixed API routes, bypasses proxies and disables redirects. Use ADB reverse with an isolated emulator and a host loopback backend. The release manifest declares neither INTERNET nor the debug configuration, and its build flag stays false. Use synthetic data only. The trust boundary includes the emulator, ADB bridge and local host. Keystore evidence is from an emulator, with hardware backing and physical release acceptance still unverified. [Android network security configuration](https://developer.android.com/privacy-and-security/security-config) · [Android Keystore AES API](https://developer.android.com/reference/kotlin/android/security/keystore/KeyGenParameterSpec)

**Interoperability finding.** The Android HTTP stack sends a media type with its empty logout POST. The existing Fastify route rejected the header at parsing with HTTP 415, so local sign-out worked and remote revocation did not. The same logout route now ignores the media type only for an explicit zero-length request with no transfer encoding. Requests with bodies remain rejected. The real Android flow then observed HTTP 204, followed by invalid-session on protected access. No route was added.

**Exact test commands.** Executed with existing SDK platform tools and AndroidJUnitRunner. The standalone Android CLI was unavailable.

```bash
export ANDROID_HOME="$HOME/Android/Sdk"
export JAVA_HOME=/usr/lib/jvm/java-17-amazon-corretto
./gradlew --offline --no-daemon -g .gradle/local-user-home --console=plain --max-workers=4 \
  -PvehiclelogLocalCloud=true :app:testDebugUnitTest --tests '*CloudRepositoryTest*' \
  --tests '*BackupCodecTest*' --tests '*BackupInputTest*' --tests '*ReminderRulesTest*' \
  :app:assembleDebug :app:assembleDebugAndroidTest :app:processReleaseMainManifest

export PATH="$HOME/.nvm/versions/node/v26.13.0/bin:$PATH"
export VEHICLE_PG_TOOLS="$PWD/build/m51-sid.NWSfg7/root"
bash server/test/local-postgres.sh test
VEHICLE_HTTP_PORT=8875 bash server/test/local-postgres.sh dev

export M53_OUTPUT="$PWD/build/m53-android.K6aDH2/final"
# The proxy was run separately after creating the private output directory.
printf '%s\n' normal > "$M53_OUTPUT/proxy-mode"
python3 scripts/m53-loopback-fixture.py "$M53_OUTPUT" 8875

# In the device-run terminal:
export M53_DEVICE=emulator-5592 M53_ADB_PORT=5093
export VEHICLE_PG_SOCKET=/tmp/vehicle-m51.hcF7zE/socket
bash scripts/m53-local-device.sh
```

The process-death harness in `build/m53-android.K6aDH2` installed only freshly built debug and test APKs on the disposable API 35 AOSP emulator. It ran individual classes or methods with `adb -P 5093 -s emulator-5592 shell am instrument -w -r -e localCloudFixture true -e class <selector> org.amirariana.vehiclelog.debug.test/androidx.test.runner.AndroidJUnitRunner`. The cloud tests require this explicit fixture opt-in. Instrumentation received freshly issued synthetic invite inputs without echoing them. For process death it held the response after PostgreSQL returned 200, issued `shell am force-stop org.amirariana.vehiclelog.debug`, then reopened instrumentation separately for read, explicit retry and acknowledgement persistence. The proxy recorded only fixed routes and response codes. The offline regression run removed ADB forwarding, enabled airplane mode and disabled Wi-Fi and mobile data.

**JVM and backend evidence.** Focused JVM passed 29/29 with no failures or skips. Ten cloud repository tests use fakes for the API and store and cover persistence order, cancelled requests, expiry, conflicts, account switches and restart behavior. The other 19 exercise the existing backup codec, bounded reads and reminder rules. The final PostgreSQL 18.7 regression run passed 26/26 in 12.594 seconds after the logout fix. The first Gradle invocation hit a sandbox local-socket restriction and the offline retry with local socket access passed. Debug APK and instrumentation assembly and the release manifest check passed.

**Device runs.** The controlled force-stop phase is a harness interruption, reported as an expected process termination by AndroidJUnitRunner. A preceding device run confirmed the account and Room upload case, original pending attempt after restart, exact retry after a subsequent Room edit, and receipt persistence in another process. The initial Compose rerun needed an explicit wait for the asynchronous local status. The initial offline reminder run lacked the notification grant, which the final harness applies only to this disposable emulator.

**Final emulator run.** `bash scripts/m53-local-device.sh` exited 0. Seven cloud instrumentation cases passed and the separate force-stop fault injection completed as intended. The signed-out Settings journey ran at a 360 dp viewport and produced zero PUTs. The Room round trip sent the exact M4 bytes and verified local Keystore persistence, sign-out and server revocation. The restart phase sent zero PUTs before explicit retry. That retry confirmed the original digest and attempt after a later Room edit, and another process read the persisted acknowledgement. Disconnect before forwarding and disconnect after backend commit both retained the pending attempt without falsely marking success. The proxy counted five explicit PUTs, four backend 200 responses and no PUTs from Settings, restart reads or offline work. The offline SAF and reminder suite passed 13/13 with no skips, and the separate reminder restart-read passed 1/1. The final harness set the notification grant and existing reminder fixture arguments only on the disposable emulator.

Debug APK SHA-256 `55c52c6cd09d693d25cb8d62c176b081cfba635310556777bfed68163e1809c6`. Instrumentation APK SHA-256 `5178eb7dfa7304db6cbc6d39821d8bb43e0d4d1805ad73a1be13aa8f9e5db256`. Checksums matched the retained signed M4 APK and all Room schema exports. The disposable emulator, private ADB server, bridge and PostgreSQL cluster were stopped. Logs are retained under ignored `build/m53-android.K6aDH2`. No physical device was involved. UI cancellation and exact local expiry were covered with JVM fakes. Physical hardware-backed Keystore, physical accessibility, and external HTTPS, server data handling and deployment approval remain open. M5.4 and M4 physical acceptance remain separate gates.

### M5.4 explicit cloud download and atomic recovery

**Status:** Complete for isolated local synthetic development on 2026-11-15.

The optional local debug Settings flow accepts the existing password or one-use recovery code, and keeps new credentials and issued codes in memory. Review shows the acknowledged revision and its Tehran timestamp and expiry. Download starts only after confirmation. Users see that recovery requires an empty ledger and later local records are outside the selected backup. The signed production path and manual SAF remain independent of account state.

The client uses the existing exact-byte download endpoint with the selected ETag and current bearer. Network staging is private process memory, capped at 4 MiB, with response length, encoding, media type, format, revision, SHA-256 and full portable-v1 validation. It is discarded on all outcomes and after process death. After validation, metadata is checked again against the selected revision. The client serializes recovery with its own account operations and rechecks the local session, binding and expiry inside the shared SAF restore transaction, together with all four tables' emptiness and atomic inserts. Room flows refresh Home, mileage, History, reports and reminders. Markers reset and scheduling is reconciled after commit. The restored revision is kept separately from upload acknowledgement in the encrypted, backup-excluded local state.

**Exact commands.**

```bash
export ANDROID_HOME="$HOME/Android/Sdk" JAVA_HOME=/usr/lib/jvm/java-17-amazon-corretto
./gradlew --offline --no-daemon -g .gradle/local-user-home --console=plain --max-workers=4 \
  -PvehiclelogLocalCloud=true :app:testDebugUnitTest --tests '*CloudRepositoryTest*' \
  --tests '*BackupCodecTest*' --tests '*BackupInputTest*' \
  :app:assembleDebug :app:assembleDebugAndroidTest :app:processReleaseMainManifest

export PATH="$HOME/.nvm/versions/node/v26.13.0/bin:$PATH"
export VEHICLE_PG_TOOLS="$PWD/build/m51-sid.NWSfg7/root"
bash server/test/local-postgres.sh test
VEHICLE_HTTP_PORT=8876 bash server/test/local-postgres.sh dev
# Set VEHICLE_PG_SOCKET to the private socket reported by the dev fixture.
M54_BACKEND_PORT=8876 bash scripts/m54-local-device.sh
```

**JVM and PostgreSQL evidence.** Focused JVM passed **24/24**, including 10 fake-based upload/account regressions and 14 codec and bounded-input tests. The offline build assembled the debug and instrumentation APKs and processed the release manifest. The isolated PostgreSQL 18.7 regression suite passed **26/26**, with no failures or skips, in 25.061 seconds. It exercised real transactions for session recovery and revocation, account isolation, revision races, and exact-byte download. No backend contract or implementation change was required.

**Replacement-device and failure evidence.** The two fresh AOSP Android 15/API 35 installations used serials `emulator-5594` (source) and `emulator-5596` (replacement), private ADB port 5095 and the same disposable PostgreSQL server. The source confirmed one upload and added a later vehicle locally. The replacement signed in independently, kept its selected revision through Activity recreation, explicitly restored and reopened the ledger, and rejected another restore after population. All four tables matched the original fixture, including IDs, links, standalone mileage, dates, timestamps, null fields, `Long.MAX_VALUE` rials, and reminder lifecycle states. The later vehicle was absent. Derived mileage and the exact monthly total survived a process restart. A stale reminder delivery marker was removed after successful commit.

The proxy exercised interrupted, oversized and corrupt download responses. Instrumented tests separately rejected an unsupported snapshot, replaced revisions before and after the download, a locally changed account and session, expiry and server revocation. An SQLite trigger aborted insertion on the last table and all prior writes rolled back. A concurrent local writer remained intact and blocked restore. Failed paths preserved notification delivery state. Recovery-code redemption issued a new code and rejected reuse and the earlier bearer. Two intentional force-stops interrupted instrumentation: one before download delivery and one after Room commit and marker cleanup but before cloud receipt storage. The first left the ledger empty, and the second left the committed rows intact without a receipt and rejected a repeat restore. Neither restart resumed a transfer.

The first Compose test driver attempted to scroll a dialog confirmation button that was already visible. After fixing that test driver, `bash build/m54-android.mXZT77/resume.sh` restarted the same disposable replacement AVD and installed only the corrected instrumentation APK with `adb -P 5095 -s emulator-5596 install -r`. App data and the existing debug APK were retained. [Per-case device evidence](build/m54-android.mXZT77)

**Offline and combined emulator results.** Across the completed runs, **20 instrumentation cases passed**: nine local cloud cases, two offline persistence and logging cases on the replacement after its OS reboot, eight offline Room backup cases, and one actual DocumentsUI export/restore case. Two intentional process kills are separate. The replacement picker launches reached Android DocumentsUI. A System UI ANR dialog held input focus after the offline reboot, so the replacement-emulator end-to-end picker case did not pass. The picker passed on the source AVD after dismissing that emulator's boot-time System UI dialog and waiting for launcher focus. That run retained airplane mode on, Wi-Fi and mobile data off, and no ADB forwarding. The source test exported and reopened a real document, verified the export-time data, rejected a populated target and exercised cancellation. The maintained harness now runs the picker check offline before reboot. The initial dialog-scroll test-driver failure and picker retries remain in the saved evidence.

```bash
./gradlew --offline --no-daemon -g .gradle/local-user-home --console=plain --max-workers=4 \
  -PvehiclelogLocalCloud=true :app:assembleDebugAndroidTest
VEHICLE_PG_SOCKET=/tmp/vehicle-m51.JCLEeO/socket M54_BACKEND_PORT=8876 \
  bash build/m54-android.mXZT77/resume.sh
bash build/m54-android.mXZT77/offline-saf-rerun.sh
bash build/m54-android.mXZT77/offline-saf-diagnostic.sh
bash build/m54-android.mXZT77/offline-saf-source.sh
```

The bridge observed 12 explicit fixture GETs and three PUTs, including the test-triggered replacements. Its transfer count was unchanged across online restart-only phases and subsequent offline work. The debug APK SHA-256 was `1e638ab4962616d3080ccad0e1f0ee8446fee6faeb0a99135138768c5277222d`. Initial instrumentation APK SHA-256 was `4288885069d9e3d91fa070a93f17e1f9e38aeab64ae25b6f0db39528fda570c4`. Corrected instrumentation APK SHA-256 was `77da4d185bae82ff37f51d01ae82acbd4b5788a117c27a50895f64dabb22d69e`. The release merged manifest contained neither INTERNET nor the debug cleartext exception. Retained M1, reconstructed M3, signed M4, and Room v1–v4 checksums matched their originals. The disposable emulators, private ADB, bridge and PostgreSQL processes were stopped. The Redmi and existing AVDs were untouched.

**Remaining release gates.** The local debug route continues to use fixed IPv4 loopback via ADB forwarding and Option A still allows the server to read snapshots. External staging, HTTPS and vendor/operator custody, production retention/purge, encrypted operational backups, deletion handling and real-data consent remain open. Physical M5 replacement-device acceptance, physical accessibility and the separate deferred M4 physical release gate remain open. A crash between database commit and notification marker removal can still interrupt cleanup. A committed populated ledger blocks a second restore even if its cloud receipt was never saved. The final remote check and local commit are separate consistency boundaries.

## M6. Market-ready closed pilot APK

**Status:** Planned. **Provisional effort:** 9–14 days.

**M6.0 planning complete on 2026-11-16.** The [closed-pilot decision record](docs/delivery/M6_PILOT_DECISION.md) records hosting candidates, pending approvals, acceptance evidence, and the implementation sequence. External staging and real-data pilot are **BLOCKED**. The subsequent product-owner decision prioritizes the [Android-only MVP completion plan](#android-only-mvp-completion-plan).

**M6.1 DEFERRED / BLOCKED BY PRODUCT-OWNER PRIORITY.** The product owner withheld approval and postponed further server development and external deployment until after Android MVP completion. The [account deletion and reconciliation contract](docs/architecture/M6_ACCOUNT_DELETION.md) remains an unapproved proposal. Implementation, PostgreSQL tests, and the restore drill are unstarted for M6.1.

**Dependencies:** M5 operational and acceptance gates, host and APK distribution eligibility, available pilot devices and Iranian networks. **Risk:** Signing-key loss, failed sideload or migration, unreachable host, and incomplete privacy or restore controls.

- [ ] Update `server/src/auth.ts`, `server/src/backup.ts`, `ui/account/AccountScreen.kt`, and `ui/settings/SettingsScreen.kt`. Add `server/Dockerfile`, `server/compose.yaml`, `scripts/restore-server.sh`, and `docs/delivery/PILOT_RUNBOOK.md`. Local erase and cloud deletion are distinct controls. Cloud deletion revokes sessions and removes the active snapshot. Reconcile deletions before reopening a restored database.
- [ ] Update `scripts/release-apk.sh` and `docs/delivery/ANDROID_RELEASE.md`. A clean checkout produces a verified signed APK. Record revision, version, certificate fingerprint, checksum, tested distribution path, and support contact.
- [ ] Add `domain/PilotSummary.kt` and an opt-in aggregate export in Settings. Verify that exported counts support the metrics and omit amounts, notes, contact details, and location.

**Automated:** Full Android and server suites, `server/test/accountDeletion.test.ts`, `test/PilotSummaryTest.kt`, and signature, certificate, and checksum verification.

**Physical Android:** Download through the actual pilot path, install and upgrade over M4 records, and exercise backup, restore, deletion, and support intake on representative older and newer phones. Check access from at least two target mobile networks.

**Done:** Release checks and the off-host server restore drill pass. APK, privacy notice, instructions, and support path are ready for the invited cohort.

**Evidence and completion notes:** Pending.

## M7. Pilot readout

**Status:** Planned. **Provisional effort:** 5–8 days within the four-week observation window.

**Dependencies:** M6 gate and recruitable drivers. **Risk:** Recruitment bias, incomplete voluntary reports, and short-lived early use.

- [ ] Start with 5–10 drivers and expand toward 40–60 after checking installation and data safety. Update `PLANS.md`, `docs/product/SUCCESS_METRICS.md`, and `docs/delivery/RISK_REGISTER.md` with denominators, failures, activation, and repeat logging.
- [ ] Fix confirmed defects in affected modules and update `docs/delivery/BACKLOG.md`. Each distributed patch passes its regression tests and the signed upgrade check. Record the decision to continue, improve the driver loop, or pause before considering discovery development.

**Automated:** Affected regression suites and release verification for each pilot patch.

**Physical Android:** Reproduce reported device failures, verify the fix, and upgrade a populated installation before distributing any patch.

**Done:** Four weeks of evidence and a driver product decision are documented. Directory work requires a separate scope decision.

**Evidence and completion notes:** Pending.
