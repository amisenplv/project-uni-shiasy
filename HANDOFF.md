# Vehicle Ledger (دفتر خودرو) · Chat handoff

## Resume here

**Concept C v2 redesign implemented as of 2026-11-16.** Start with the [repository README](README.md) for setup and scope. Use [PLANS.md](PLANS.md#approved-concept-c-v2-production-design-implementation--complete) as the execution record for the current Android design.

- **Design specification:** [Concept C v2 implementation brief](design/vehicle-ledger-concept-c-v2/ANTIGRAVITY_IMPLEMENTATION.md) and [prototype previews](design/vehicle-ledger-concept-c-v2/README.md).
- **Recorded verification:** The Concept C v2 run logged 94 passing JVM tests and 35 emulator instrumentation tests with zero failures. The execution record describes 19 screenshots from the emulator audit. The screenshot capture was retained locally. [Verification record](PLANS.md#approved-concept-c-v2-production-design-implementation--complete)
- **Integrity & Bounds:** Pinned Room v4 schema (no DB alterations); pinned snapshot wire format (no JSON codec alterations); Persian RTL, digits, and legacy integer Rial math preserved. Physical Redmi Note 9S (`4c1ad625`) remained untouched.
- **Next steps:** Follow the [Cafe Bazaar release checklist](docs/delivery/BAZAAR_RELEASE_READINESS.md). RC5 physical evidence covers the focused M4-to-RC5 upgrade. The distinct M4 physical gate and public release readiness remain open. Cloud deployment and account deletion remain deferred.

## Read first

- [AGENTS.md](AGENTS.md) defines the project workflow, architecture, approval rules, and evidence to record.
- [PLANS.md](PLANS.md) records the commands, results, exceptions, and milestone decisions. [ROADMAP.md](docs/delivery/ROADMAP.md) and [MVP_SCOPE.md](docs/product/MVP_SCOPE.md) reflect the revised product scope.
- [DATABASE_DESIGN.md](docs/architecture/DATABASE_DESIGN.md) defines ledger fields, integrity rules, migration expectations, and storage units. [SYSTEM_ARCHITECTURE.md](docs/architecture/SYSTEM_ARCHITECTURE.md) describes the offline design and recovery boundaries.
- [ANDROID_SETUP.md](docs/delivery/ANDROID_SETUP.md), [ANDROID_RELEASE.md](docs/delivery/ANDROID_RELEASE.md), and [TEST_STRATEGY.md](docs/delivery/TEST_STRATEGY.md) describe the toolchain and device verification procedures. [OPEN_QUESTIONS.md](docs/research/OPEN_QUESTIONS.md) tracks product decisions.

## Archived M2 handoff

The sections below record the earlier M2 closeout and its then-proposed M3 work. The current status is above.

### Work completed

| Slice | Implementation and evidence |
|---|---|
| M1 · Signed Android foundation | Built the Persian RTL shell and adaptive icon, pinned the Android toolchain, added the build and release scripts, and verified the packaged backup policy and unattended Compose launch. Signed controlled test builds `0.1.0` and `0.1.1` were verified and upgraded in place on the Redmi Note 9S. The developer confirmed offline cold launch for signed `0.1.1`. An isolated clean source debug build passed. Offline cold launch for signed `0.1.0` was explicitly waived with accepted residual risk. [M1 evidence](PLANS.md#m1-first-signed-apk) |
| M2.1–M2.3 · Vehicle storage and app wiring | Exported Room v1, added `VehicleEntity`, `VehicleDao`, the repository and input validation, and wired the app-wide Room instance through `AppContainer`. Four `VehicleDaoTest` cases passed on the Redmi. Twelve JVM input and repository tests passed. The repository generates stable vehicle IDs, preserves them on edits, and normalizes mixed digit input for vehicle years. |
| M2.4 · Manual vehicle UI | Implemented the Persian RTL list and add/edit flow with validation and draft retention. Two focused Compose tests passed on the Redmi. The developer separately reported readable M2.4 list/form text at normal and 150% system font size, offline mixed-script add/edit, validation with draft retention, and persistence after force-stop, relaunch, and reboot. |
| M2.5b · Offline catalog engine | Added the versioned JSON loader, validation, normalized Persian and mixed-script search, and a fictional JVM fixture. Seven focused JVM tests passed. |
| M2.5c · Optional picker | Added safe catalog injection, search, manual fallback, selection with a stable catalog key, and preservation of saved labels when a key is missing. Ten catalog JVM tests and five focused Compose tests passed. The normal debug launcher showed manual entry with the picker hidden. |
| M2.5 · Device acceptance follow-up | Added `reviewsManualFormOnDevice` and `reviewsCatalogPickerOnDevice` in `VehicleSetupTest`. Both passed on the Redmi at system 100% and again with an injected 150% form setting. Screenshots and assertions covered Persian RTL and mixed-script text, keyboard reachability, validation, picker results, dismissal with draft preservation, editing missing-key vehicles, and sampled touch targets. Test writes used isolated in-memory Room databases. The dialog's measured font scale was 1.0 in both runs. [Device evidence](PLANS.md#m2-room-and-vehicle-setup) |
| M2 · Scope closure | Updated PLANS.md, ROADMAP.md, and MVP_SCOPE.md to close M2 on offline manual setup, Room persistence, and the verified catalog foundation. Preserved the original catalog and business goals and recorded their deferrals and the two acceptance waivers. |

### M2 waivers and deferred work

- The real system-wide 150% font check for M2.5 and its TalkBack review were **not performed**. The product owner explicitly recorded both as **Waived — Accepted Residual Risk**. The earlier M2.4 developer report and the M2.5 injected font test are separate evidence. Actual enlarged picker layout, screen reader announcements, traversal, and dialog focus behavior remain unverified. The two waivers apply to the current closure.
- Production catalog activation requires an approved redistributable dataset, documented provenance, and physical-device verification of the activated build. The file-level `approvedCatalogAsset` setting in `AppContainer.kt` is null. The production launcher therefore offers manual entry. Synthetic fixtures are for tests.
- Business recruitment was deferred by the product owner and is incomplete. The original research track remains in the roadmap.

### M2 workspace and verification

- Single Android `:app` module using Kotlin, Compose Material 3, Room, KSP, and manual dependency wiring. Source namespace is `org.unishiasy.vehiclelog`. Controlled test application ID is `org.amirariana.vehiclelog`, with `.debug` on debug builds. Confirm the permanent application ID before public release.
- Pinned tools are Gradle 8.13, AGP 8.13.2, Kotlin 2.3.10, Corretto JDK 17, Android SDK and Build Tools 36, minSdk 26, Room 2.8.4, and KSP 2.3.6. Previous local runs used `/usr/lib/jvm/java-17-amazon-corretto`, `$HOME/Android/Sdk`, and the ignored offline Gradle cache `.gradle/local-user-home`. Use the setup guide for the exact flags.
- `VehicleDatabase` opens the app-private `vehicle-ledger.db`. Its only v1 table is `vehicles` with `id`, `kind`, `displayLabel`, `catalogKey`, `productionYear`, and `productionYearCalendar`. Keep the existing [Room v1 schema export](app/schemas/org.unishiasy.vehiclelog.data.local.VehicleDatabase/1.json) as migration history.
- Physical evidence came from a Redmi Note 9S, Android 12/API 31. MIUI denied ADB writes to the system font setting and shell key injection. The successful tests used app-owned instrumentation and window insets for Back and keyboard state. System font remained at 100%, and no accessibility settings changed.
- Local ignored evidence includes the [100% test XML](app/build/reports/m25-acceptance/100-insets-results.xml), [150% form override results](app/build/reports/m25-acceptance/150-insets-instrumentation.txt), [form screenshots](app/build/reports/m25-acceptance/manual-1.5.png), and [picker screenshots](app/build/reports/m25-acceptance/catalog-1.0.png). PLANS.md preserves the results and commands if generated reports are later cleared. The controlled signed APKs are under the local `build/releases/` directories. Their checksums matched the prior baseline after M2.5 testing.

### Earlier proposed slice · M3.1 Room storage foundation

Approval of this slice and the storage decisions below is the next step. The proposal is limited to ledger entry and odometer reading persistence. Ledger UI, dashboard, Jalali reporting, and reminders belong to later slices.

1. Propose final category keys and entry columns before committing to DDL. In particular, confirm which entry types may omit amount, how to represent positive fuel volume in whole milliliters, and whether optional notes and date fields need defaults. Use the documented integer legacy-rial amount, Gregorian ISO civil occurrence date, UTC creation instant, and whole-kilometer readings. Currency wording and authoritative Jalali boundary fixtures are separate open M3 decisions.
2. After approval, add `EntryEntity.kt`, `OdometerReadingEntity.kt`, `LedgerDao.kt`, and `Migrations.kt` under `data/local/`. Update `VehicleDatabase.kt` to v2, register the explicit 1→2 migration in the app's Room builder, and export the v2 schema. Preserve all existing vehicle rows and values through an additive migration. Use the documented vehicle and occurrence-date indexes and foreign keys. A reading can stand alone or reference one entry, with at most one linked reading per entry.
3. Prove migration from a populated v1 fixture and exercise the new tables. The focused DAO checks cover valid inserts, rejected foreign keys and duplicate reading links, standalone readings, deterministic ordering, exact rial storage, allowed null amounts, and rollback on a failing linked write. Review the exported schema and record the migration and DAO instrumentation results in PLANS.md. Risks include vehicle data loss on upgrade, orphaned or duplicated links, and half-completed writes. [M3.1 acceptance proposal](PLANS.md#m3-ledger-odometer-and-dashboard)

### Earlier M2 handoff prompt

> Continue Vehicle Ledger (دفتر خودرو) from HANDOFF.md in `/home/amir/Documents/ChatGPT/Project UNI Shiasy`. Read AGENTS.md and the M2 closure and M3 proposal in PLANS.md. M2 is formally complete under the revised scope. Help me settle the M3.1 category keys, entry columns, and fuel-volume representation. Wait for my explicit approval before implementing M3.1.
