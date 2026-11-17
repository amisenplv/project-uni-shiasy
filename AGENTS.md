# Project instructions

This project is a Persian-first Android expense and maintenance ledger for professional drivers. The first distribution target is a signed Android APK for a closed pilot. [PLANS.md](PLANS.md) is the execution record.

## Scope and workflow

- The technical baseline is approved. Begin M1 only after the user explicitly approves it. Follow the milestone gates in [PLANS.md](PLANS.md).
- Keep each implementation task to half a day to two focused days when practical. Name the files, expected behavior, and checks before editing. Split larger tasks at a testable boundary.
- Check Git status and read relevant files before editing. Preserve unrelated changes. Keep diffs focused and write English identifiers and technical documentation.
- Explain the need for each production dependency and obtain approval before adding it. Obtain separate authorization for commits, publishing, deployment, provisioning, or destructive operations as applicable.
- Update milestone status and link evidence in [PLANS.md](PLANS.md). Report verification failures and remaining risks explicitly.

## Android and data conventions

- Use one Android module, `:app`, with Kotlin, Jetpack Compose Material 3, screen-level ViewModels with `StateFlow`, concrete repositories, and manual dependency wiring. Keep transaction and persistence logic outside composables.
- Use Room as the local source of truth. Export schemas and test migrations with populated prior databases. Preserve records on an open or migration failure. Keep app-private data and authentication material out of platform backups and logs.
- Use Persian copy, digits, and RTL layout in the app. Isolate mixed-direction values. Normalize numeric input, store money in integer legacy rials, and derive Jalali reporting periods using Tehran civil dates.
- Keep core logging available offline. Manual file backup requires driver action and safe file retention. Cloud recovery restores the last acknowledged snapshot and provides no live synchronization or cross-device merge.
- Keep signing keys and service credentials outside the repository. Apply explicit account ownership checks to every backend snapshot operation. See [architecture](docs/architecture/SYSTEM_ARCHITECTURE.md) and [data design](docs/architecture/DATABASE_DESIGN.md).

## Definition of Done

Pass the applicable automated tests and the milestone's physical Android checks before proceeding. Record commands, results, device and OS, APK build, and any blocking issue. A build, an emulator run, and a physical-device run are separate forms of evidence. [Test strategy](docs/delivery/TEST_STRATEGY.md)
