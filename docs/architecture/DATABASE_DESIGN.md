# Database design

## Room ledger

| Record | Fields and constraints |
|---|---|
| Vehicle | Stable ID, kind, display label, optional catalog key, optional production year and calendar. Preserve the selected label if bundled catalog data later changes. Support several rows while the pilot UI works with one active vehicle. |
| Entry | Stable ID, vehicle foreign key, category, occurrence date, creation instant, optional notes, optional amount for maintenance-only entries, optional station name, and full-tank indicator flag (schema v5). Fuel requires a positive amount and a positive volume stored in whole milliliters. |
| Odometer reading | Stable ID, vehicle foreign key, occurrence date, whole kilometers, and optional unique entry reference. Support a standalone reading or one linked reading per entry. |
| Reminder | Stable ID, vehicle foreign key, title, exactly one due date or kilometer threshold, and active, completed, or canceled state. Record enough notification state to avoid repeatedly posting the same due reminder. |
| Recovery metadata | Local backup state and last acknowledged cloud revision. Keep credentials in separate Keystore-protected storage. |

- Index ledger and readings by vehicle and occurrence date. Use foreign keys, allowed categories and status values, and validated amount, date, and mileage ranges.
- Read or modify an entry and its linked reading in one transaction. A repeated submission from the same form uses the same entry ID. Reject an incompatible duplicate. Deleting a linked entry removes its reading and recalculates derived values.
- Accept same-day readings in any order. Check all affected readings against earlier and later days. The highest reading on the latest day is the displayed odometer. If a reading conflicts, offer an explicit expense-only save.
- Store integer legacy rials and convert explicitly for تومان display. Keep dates as Gregorian ISO civil dates and creation instants in UTC. Compute Jalali month bounds from Tehran civil dates. Test input normalization, exact totals, Nowruz and leap Esfand.
- Use one ledger for spending and maintenance history. Count each entry once and omit unpriced service records from the spending sum.
- Export schemas from the first database version. Add explicit tested migrations. If opening or migrating the database fails, surface the error and preserve the original database for support and recovery. [Room migrations](https://developer.android.com/training/data-storage/room/migrating-db-versions.html)

## Portable snapshots

The snapshot envelope has a format version, export instant, explicit currency unit, and the vehicle, entry, reading, and reminder collections. Use a stable ordering for predictable validation. Preserve IDs and dates. Exclude session material and device-only metadata. Validate version, size, fields, references, and mileage order before inserting anything. Confirm an empty ledger within the same transaction as the restore.

### Portable snapshot format v1

The standalone format uses UTF-8 JSON. Every listed key is required. A field marked nullable uses an explicit JSON `null` for absent data. Monetary, mileage, volume, and numeric timestamp fields are base-10 JSON integer tokens decoded directly to signed 64-bit integers. Consumers must use a parser that preserves 64-bit precision. Production years are integers in 1–9999 and come with a calendar.

| Object | Fields |
|---|---|
| Envelope | `formatVersion` = 1, `exportedAt` = canonical UTC ISO instant, `currencyUnit` = `legacy_irr`, and arrays `vehicles`, `entries`, `readings`, `reminders`. |
| Vehicle | `id`, `kind` (`car`), `displayLabel`, nullable `catalogKey`, nullable `productionYear`, nullable `productionYearCalendar` (`solar_hijri` or `gregorian`). |
| Entry | `id`, `vehicleId`, `category`, `occurredOn`, `createdAtEpochMillis`, nullable `notes`, nullable `amountRials`, nullable `fuelVolumeMl`. |
| Reading | `id`, `vehicleId`, `occurredOn`, `kilometers`, nullable `entryId`. |
| Reminder | `id`, `vehicleId`, `title`, nullable `dueOn`, nullable `dueKilometers`, `state` (`active`, `completed`, or `canceled`). |

The encoder writes compact JSON with fixed key order and sorts each array by ID. Decode accepts different array and key order and returns rows ordered by ID. The same rows and export instant yield the same bytes. Original Gregorian civil dates, including those outside Jalali display coverage, and the text in accepted records are preserved.

IDs use canonical lowercase UUID text and are unique within each collection. References must resolve within the file. Linked readings match their entry's vehicle and date, with at most one reading per entry. Every reading on a later date must meet or exceed the maximum of all earlier dates for that vehicle. Same-day readings may differ in either order. Entry category, positive amount and fuel-volume rules match the ledger. Mileage and mileage thresholds are nonnegative. Reminder titles and vehicle labels must be nonblank. Each reminder has exactly one threshold. Civil dates use valid Gregorian `YYYY-MM-DD` with years 0001–9999.

The inclusive cap is 4 MiB (4,194,304 bytes), checked before UTF-8 decoding and JSON parsing. Maximum collection lengths are 100 vehicles, 20,000 entries, 20,000 readings, and 5,000 reminders. The decoder also caps JSON at three container levels, 350,000 values, and 16 keys per object before building the JSON tree. M4.5 must bound file reads to the same byte limit plus one sentinel byte, including for providers that report unknown sizes. Ledgers exceeding these limits produce a validation error.

V1 rejects unsupported versions, unknown or duplicate fields at any level, malformed UTF-8, unpaired Unicode surrogates, and invalid values. Future formats require an explicit decoder and migration policy. The allowlist contains ledger records only, so device permissions, WorkManager state, notification opt-in and delivery markers, and credentials have no representation. The codec returns error categories without including file contents. The format is plaintext and carries no authenticity guarantee.

## PostgreSQL from M5

M5.1 initializes `accounts`, `pilot_invites`, `sessions`, `recovery_codes` and `auth_limits` with a versioned SQL migration. Migration history uses a checksum and a database transaction lock. The invite's unique account reference records redemption. A recovery verifier is unique and account-scoped. Recovery updates the credential version and revokes old sessions in the same account-row transaction as code rotation. The runtime role has no schema or invite issuance privileges. Throttle rows use HMAC-derived keys and explicit window expiry. [Migration](../../server/migrations/001_accounts.sql) · [Local roles and setup](../../server/README.md)

M5.2 adds `snapshot_slots` (one row per account, initialized on first access) and `snapshot_receipts` (account and attempt ID primary key). The slot stores the exact server-readable bytes, the digest and acknowledgement and expiry times. A slot keeps a new opaque revision when empty. Bounded receipts carry metadata only, including length and format, and commit in the same account-locked transaction as replacement. Expiry is evaluated on authorized requests under that lock. Account deletion and its operational deletion ledger remain in M6. [Snapshot migration](../../server/migrations/002_snapshots.sql) [API design](API_DESIGN.md) · [Security decision](ADR/0004-cloud-recovery-security.md)
