# Vehicle Ledger · دفتر خودرو

دفتر خودرو برنامه‌ای فارسی و آفلاین برای ثبت هزینه‌ها، سرویس‌ها و کارکرد خودرو است.

Vehicle Ledger is a Persian-first Android app for drivers in Iran. It keeps vehicle records, fuel and service expenses, odometer readings, Jalali monthly reports, and reminders on the device. The app supports manual backup to a JSON file and transactional restore into an empty ledger.

## Build and run

Install JDK 17 and Android SDK Platform 36, Build Tools 36.0.0, and Platform Tools. The app supports Android 8.0 (API 26) and later. Open the repository root in Android Studio and select JDK 17 for Gradle, or run these commands from the repository root after setting `JAVA_HOME` and `ANDROID_HOME` for your machine.

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk` and uses application ID `org.amirariana.vehiclelog.debug`. For emulator and physical-device checks, including what the device test task installs, see the [Android setup guide](docs/delivery/ANDROID_SETUP.md) and [test strategy](docs/delivery/TEST_STRATEGY.md).

The default build needs no account or backend. The optional recovery service and Android cloud integration are for local tests with synthetic data. Use the [local backend guide](server/README.md) if you need to develop or test that integration.

## Repository map

| Path | What it contains |
| --- | --- |
| [app/](app/) | Kotlin and Jetpack Compose app, resources, and Android and JVM tests |
| [app/schemas/](app/schemas/) | Versioned Room schema exports and migration history |
| [server/](server/) | Local TypeScript, Fastify, and PostgreSQL recovery API and integration tests |
| [design/](design/) | Design prototypes and previews, including the [Concept C v2 specification](design/vehicle-ledger-concept-c-v2/ANTIGRAVITY_IMPLEMENTATION.md) |
| [docs/](docs/) | Architecture, product scope, roadmap, test strategy, and release procedures |

## Project handoff

Start with [HANDOFF.md](HANDOFF.md) and [AGENTS.md](AGENTS.md). Use [PLANS.md](PLANS.md) for implementation and test evidence, the [roadmap](docs/delivery/ROADMAP.md) for work sequencing, and the [database design](docs/architecture/DATABASE_DESIGN.md) for storage and migration decisions.

- **Offline Android MVP:** Development is complete. Signed RC5 (`0.3.1`, version code 5) passed a [focused physical upgrade and offline persistence check](PLANS.md#focused-redmi-m4-to-rc5-upgrade).
- **Current Android source:** Includes the [Concept C v2 redesign](PLANS.md#approved-concept-c-v2-production-design-implementation--complete) and its recorded JVM and emulator checks. The signed RC5 evidence applies to the earlier artifact. Follow the [release procedure](docs/delivery/ANDROID_RELEASE.md) to identify and verify a later candidate.
- **Release gates:** The separate signed M3-to-M4 physical acceptance remains deferred and open. Public release readiness is open. Track Cafe Bazaar and production signing requirements in the [release checklist](docs/delivery/BAZAAR_RELEASE_READINESS.md).
- **Cloud recovery:** M5 development was completed for isolated synthetic testing. External staging, hosting, real-data processing, and server-side account deletion are deferred. See the [M5 handoff](PLANS.md#m5-optional-cloud-snapshot-recovery) and [roadmap](docs/delivery/ROADMAP.md).

GitHub is the source handoff. If an exact historical APK, local evidence bundle, or signing continuity is required, arrange a separate private transfer with the project owner and verify artifact hashes against the [release ledger](docs/delivery/ANDROID_RELEASE.md). Keep signing credentials, local backups, and ledger data outside Git.
