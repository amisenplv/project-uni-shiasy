# Test strategy

Each milestone requires passing automated checks and testing on a physical Android device. Record the command, result, APK build, device model and OS, manual scenarios, and any unresolved defect in [PLANS.md](../../PLANS.md). An emulator can supplement device coverage. Pilot distribution requires a signed upgrade over a populated release installation.

| Milestone | Required automated evidence | Required physical-device evidence |
|---|---|---|
| M1 | Build, lint, JVM tests, Persian Compose launch test, APK signature verification | Install, cold-launch offline, rotate, enlarge text, and install the higher-version signed build in place |
| M2 | Catalog normalization, Room vehicle constraints, vehicle setup flow, exported schema | Manual and catalog entry, mixed script, force-stop, reboot, and edit |
| M3 | Exact money and month totals, Jalali fixtures, atomic ledger, idempotence, mileage ordering, populated migration | Log, correct, delete, backdate, and reconcile totals offline. Observe entry time |
| M4 | Due rule and repeat-job tests, snapshot validation, atomic restore, populated migration | Deny and grant notifications, verify due state, retain an export, restore into an empty installation, and test signed upgrade |
| M5 | Real PostgreSQL auth and ownership, retry, stale revision, restore, and client failure tests | Cloud recovery to a second installation, interrupted upload, expired session, and continued local logging offline |
| M6 | Full Android and API suites, account deletion and restore reconciliation, signing and checksum | Actual download path, signed update over M4 data, privacy flows, and representative older and newer devices. Verify two pilot mobile networks and the server restore drill |
| M7 | Affected regression tests and signing verification for each distributed fix | Reproduce and retest reported issues and upgrade a populated installation for each distributed fix |

The product owner closed M3 with accepted residual risks on 2026-11-11. The M3 manual airplane-mode walkthrough, human entry timing, and verification of both Quick Add targets are **DEFERRED / NOT VERIFIED**. The table preserves the original M3 check descriptions. [M3 closure evidence](../../PLANS.md#m3-acceptance-and-closure-disposition) · [Future product validation backlog](BACKLOG.md#future-product-validation)

For M4.6, the signed emulator exercise supplements the physical-device gate. Preserve the upgraded fixture and restore its export in a separate empty test installation or Android user. Use a pre-upgrade fixture with both reading types and capture stable IDs before the update for full migration coverage. [M4.6 signed acceptance evidence](ANDROID_RELEASE.md#m46-independent-signer-and-emulator-checks)

The M4.6 follow-up verified field equality in a debug Room migration and stale-marker handling in a debug SAF restore and focused instrumentation. The provider fault test exercised the real SAF picker in signed M4 with a separate test provider. Record the app result and any provider-retained document independently. The subsequent AOSP API 35 run passed the seeded migration on both unmodified signed APKs, with exact field equality and an offline M4 cold launch. The physical-device gate remains outstanding. [Signed database integrity acceptance](ANDROID_RELEASE.md#m46-signed-database-integrity-acceptance) [M4.6 gap follow-up](ANDROID_RELEASE.md#m46-automated-gap-follow-up)

The product owner deferred the M4 physical run and approved the M5 development handoff. The M4 physical checks in the table remain **DEFERRED / NOT VERIFIED** and still apply to release acceptance. The signed emulator runs support development completion. M5 staging and physical checks retain their own gate. [Deferred M4 physical release acceptance](BACKLOG.md#deferred-m4-physical-release-acceptance)

## Recovery acceptance cases

- Export, then add a local entry and restore into an empty test installation. The restored records match the file at export time. The UI explains that the user must export and retain a file manually, and that manual export is not automatic backup.
- Back up to the server, add an entry, and restore on a second installation. The later entry remains on the originating device. The restored ledger ends at the last acknowledged snapshot. The UI explains that cloud recovery is not live sync or a merge.
- Repeat a successful upload after losing the response. Confirm one accepted revision and the same acknowledgement. Submit an outdated revision and confirm the server and local records remain intact.
- Reject a malformed, unsupported, oversized, or foreign-account snapshot. A failed restore leaves the ledger intact. A valid file also cannot overwrite a populated ledger.
- Exercise a database-open or migration failure and verify that the app preserves the original data. Check platform backup exclusions for the database and session material.

## M5.0 cloud recovery test matrix

These are planned checks. Use synthetic fixtures and independent accounts or sessions as indicated. See the [M5 API contract](../architecture/API_DESIGN.md) for response codes.

| Layer | Independent scenario | Required result |
|---|---|---|
| Real PostgreSQL / HTTP | Accounts A and B upload distinct records. Use A's session with B's revision, attempt ID and guessed account identifier. | B's metadata and bytes are inaccessible. A's slot is unaffected by B's values. Ownership comes from the session. |
| Real PostgreSQL / HTTP | Race two registrations with the same invite. Try an expired invite and invalid sign-in details. Saturate auth limits against known and unknown usernames. | One account consumes the invite. Invalid credentials share the same response category. Work stays bounded and throttling expires. |
| Real PostgreSQL / HTTP | Test a logout-revoked session, a session revoked by recovery, and a naturally expired session independently. Race recovery against an in-flight PUT. | Each invalid session loses subsequent access. A PUT that lost the serialization race cannot commit. |
| Real PostgreSQL / HTTP | Redeem one recovery code twice, then try expired and rotated-out codes. Inject loss of the successful recovery response. | Only one redemption commits. The new password works after response loss and can rotate the unread code. |
| Real PostgreSQL / HTTP | Drop the successful PUT response after commit. Replay the original attempt and body. Replay with a changed body, and replay after another device supersedes it. | Current retry returns the same revision/time without renewing retention. Changed input and superseded attempts return distinct conflicts. |
| Real PostgreSQL / HTTP | Submit two independent attempts concurrently against the same populated revision, then repeat with an empty slot. Expire a slot and reuse its old revision. | One commit per round. No old revision succeeds against the new empty slot. A mismatched download revision never returns replacement bytes. |
| HTTP and codec conformance | Send malformed UTF-8, duplicate or unknown fields, invalid references, incompatible version/currency and signed 64-bit boundary values. | A rejects invalid and incompatible payloads before replacement. B validates framing server-side and rejects corrupt or incompatible inner data at client restore. Exact integer fields survive. |
| HTTP and Android I/O | Test exact byte limit, limit plus one with and without declared length, and collection or nesting overflow. Interrupt a PUT before and after commit and interrupt the response body during GET. | Correct limits apply, partial input never becomes current, ambiguous PUT follows the retry contract, and partial GET leaves no import or usable staged file. |
| HTTP / codec | Upload valid noncanonical key ordering and whitespace with a large exact amount. Download and compare raw bytes and SHA-256. For B, compare both envelope and decrypted inner bytes. | Byte comparisons match and no numeric value passes through a floating-point representation. |
| Android JVM / Room | Restore into an empty ledger, a ledger populated before download, and a ledger that gains a record just before commit. Inject a failed insert, expired session, account switch and invalid digest independently. | Only the empty valid case commits. Rejected cases preserve existing rows. Notification reconciliation follows successful commit only. |
| Physical Android with approved staging | Upload, add a later record, recover the account on a replacement installation and restore. Test old sessions and codes. For B, try a missing and incorrect key separately. | Only snapshot-time records return, exact totals and IDs match, old credentials fail, and B requires the correct independent key. Record physical and staging evidence separately. |
| Android JVM / device with request counter | Cold launch, reconnect, restart after a pending attempt, run reminder work, edit the ledger and use manual SAF offline. | Zero upload requests until an explicit upload or retry action. Existing local flows remain usable while signed out or offline. |
| Operational lifecycle / M6 integration | Expire a slot, race deletion and upload, and restore an older encrypted operational backup. | Expired and deleted payloads remain inaccessible. Reconcile deletions, expiry, sessions and pre-restore revisions before traffic resumes. Verify the approved backup purge window. |

## M5.1 local account evidence

The focused integration suite passed 13/13 on a disposable PostgreSQL 18.7 database with no TCP listener. It exercised concurrent migration, invite and recovery redemption, tenant scoping, session expiry and revocation, persistent throttling, malformed bodies, safe errors and startup configuration. One HTTP test used a real loopback listener. Later local snapshot and Android evidence appears below. [Exact commands and results](../../PLANS.md#m51-implementation-and-local-test-evidence)

## M5.2 local snapshot evidence

The isolated suite passed 26/26, including 13 snapshot tests and 13 account regressions. Same-base uploads and session revocation used real PostgreSQL lock contention. A lost response and an oversize chunked request used loopback HTTP. The receipt-failure rollback used a real PostgreSQL trigger. The signed M4 SAF fixture and 41-case comparison with the Android JVM codec support format compatibility. Later local Android evidence appears below. Physical acceptance remains open. [Exact commands and results](../../PLANS.md#m52-snapshot-api-and-test-evidence)

## M5.3 local Android upload evidence

- **Focused JVM:** 29 passed, including ten cloud repository cases with fake transport and storage, 14 M4 codec and input cases, and five reminder rule cases.
- **PostgreSQL:** 26 passed after the existing logout route accepted Android's typed zero-length POST. Request bodies remain rejected.
- **Disposable API 35 emulator:** seven cloud instrumentation cases passed against the real loopback backend. The separate intentional force-stop occurred after PostgreSQL committed. Verified no upload on Settings open or pending-attempt restart, exact retry after a later Room edit, saved acknowledgement after another restart, and disconnects before and after backend commit. The Settings flow was exercised at 360 dp.
- **Offline emulator:** 13 manual SAF and reminder cases plus the separate reminder restart-read passed. ADB forwarding was removed, airplane mode enabled and Wi-Fi and mobile data disabled. Physical-device and production transport verification remain open. [Commands, limitations and evidence](../../PLANS.md#m53-optional-android-account-and-explicit-upload)

## M5.4 local Android recovery evidence

- **JVM and PostgreSQL:** 24 focused Android JVM and 26 real PostgreSQL integration cases passed. The JVM subset combines fake-based account/upload regressions with codec and input-limit tests.
- **Independent API 35 installations:** Nine cloud cases passed, including acknowledged upload followed by exact replacement-device restore, rejection of another restore, lost connectivity, oversize, corrupt and unsupported data, changed revision, revoked and expired sessions, account switching, and recovery code rotation. A local write that won the race was preserved. A SQLite trigger aborted the last table and all earlier inserts rolled back. Only committed restore reset notification markers.
- **Process and device restart:** The harness force-stopped before body delivery and separately after Room commit and marker reset but before saving the receipt. The first left the ledger empty. The second preserved all rows and blocked another restore. No restart-only flow transferred a snapshot. Two more cases passed after an offline reboot, including exact restored fields and a new local entry.
- **Manual file regression:** Eight offline Room backup cases passed on the replacement. The DocumentsUI end-to-end case passed offline on the source AVD. The replacement picker did not pass after reboot because a System UI ANR held input focus over DocumentsUI. The maintained harness schedules the picker before the offline reboot.
- **Total:** 20 emulator instrumentation cases passed across focused runs. Two intentional force-stops and the failed test-driver and picker attempts are recorded separately.
- Physical replacement-device and external staging acceptance remain open. A hard process death immediately after Room commit can still interrupt notification-marker cleanup. [Exact commands, artifact hashes and results](../../PLANS.md#m54-explicit-cloud-download-and-atomic-recovery)

## Coverage and release policy

Use JVM tests for deterministic validation, amount conversion, reminder thresholds, and summary calculations. Use instrumented tests for Room migrations, device calendar behavior, and a few critical Compose journeys. Use isolated PostgreSQL for API authorization and concurrency. Manually verify TalkBack, RTL focus and mixed-direction values, date and number input, notification permission, poor connectivity, and APK distribution. Stop at a failed milestone gate and record the blocking defect.

## Android offline MVP candidate checks

The historical Slice 1 UI run covered 29 cases and recorded 28 passes and one assumption skip. Count the AndroidJUnitRunner per-test statuses. Its `OK` summary includes the skipped persistence case. Exercise that case with separate write and read invocations and force-stop between them.

Use the cloud-disabled debug APK and a disposable, disconnected emulator for focused Room and Compose regressions. The debug manifest still declares `INTERNET`. Inspect the actual release APK for network permissions, network configuration, backup exclusions and exported components. The unsigned release audit and debug instrumentation are separate from signed distribution and physical-device evidence. The M4 physical gate remains deferred and open. [Commands and evidence](../../PLANS.md#android-offline-mvp-031-release-candidate) · [Release audit](ANDROID_RELEASE.md#android-offline-mvp-031-release-candidate)

The 0.3.1 run recorded 42 passing JVM tests. API 35 instrumentation recorded 41 passes, one Home mileage assertion failure and one expected persistence assumption skip in the initial run. Both separately invoked persistence phases passed. The unchanged mileage test passed in a fresh isolated retry. History scroll retention, keyboard reachability, injected 1.5× Compose text, reminder work, transactional restore and the actual system document picker passed in this debug instrumentation. [Case counts, commands and limitations](../../PLANS.md#android-offline-mvp-031-release-candidate)
