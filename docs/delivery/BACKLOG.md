# MVP backlog

Each row represents an epic, feature, user story, and reviewable implementation slice with its acceptance criterion. Break a slice exceeding two focused days into smaller testable changes. The exact planned files, test names, dependencies, and milestone checks are in [PLANS.md](../../PLANS.md).

P0 is essential to the named release. P1 is deferrable pilot work. P2 is post-MVP. P3 is exploratory. All tasks are pending and M1 awaits explicit approval.

| Epic and feature | Priority | Story and slice | Acceptance |
|---|---|---|---|
| Foundation · Build | P0 / M1 | Developer can build from the pinned toolchain. Add wrapper and build configuration. | Given a clean checkout and pinned tools, the debug APK builds. |
| Foundation · App shell | P0 / M1 | Driver can open a Persian RTL app. Add the shell and launch test. | Given an English system locale and airplane mode, the physical phone opens Persian RTL Home. |
| Foundation · Signing | P0 / M1 | Developer can repeat the APK release. Add release script and instructions. | Given two signed builds with the same ID and key, the higher version upgrades in place and its certificate and checksum are recorded. |
| Vehicle · Storage | P0 / M2 | Driver keeps vehicle details across restarts. Add schema, DAO, repository, and tests. | Given a saved vehicle, force-stop and reboot preserve it. |
| Vehicle · Catalog | P0 / M2 | Driver finds a model quickly. Bundle sourced aliases and normalize search. | Given mixed-script input, matching aliases return the same model family. |
| Vehicle · Manual entry | P0 / M2 | Driver can enter a missing model. Add setup and edit screens. | Given no catalog match, a manual vehicle can be saved and edited offline. |
| Ledger · Entry schema | P0 / M3 | Driver retains a vehicle when the ledger ships. Add and test migration. | Given a populated M2 database, migration preserves vehicle data. |
| Ledger · Mileage | P0 / M3 | Driver can log a valid older expense. Add atomic writes and ordering rules. | Given a conflicting reading, the transaction preserves existing data and offers an explicit expense-only retry. |
| Ledger · Quick add | P0 / M3 | Driver can quickly log fuel. Add amount, liters, date defaults, and save state. | Given repeated taps or a save retry, one valid entry appears and invalid input is retained. |
| Ledger · History | P0 / M3 | Driver can correct a record. Add edit and confirmed deletion. | Given an edit or deletion, history, mileage, and totals reflect the accepted change. |
| Ledger · Dashboard | P0 / M3 | Driver can see current Jalali month spending. Add the monthly aggregation and Home view. | Given entries around a month boundary, the total matches the ledger for the chosen Jalali month. |
| Reminders · Due state | P0 / M4 | Driver sees upcoming service. Add thresholds and due checks. | Given a due date or reached mileage threshold, Home shows it and completion or cancellation clears it. |
| Reminders · Device notification | P0 / M4 | Driver can opt in to date reminders. Add best-effort scheduling. | Given denied permission or delayed scheduling, Home remains accurate. A repeated job does not create another active notification for the same due item. |
| Recovery · Manual file | P0 / M4 | Driver can keep a portable copy. Add explicit file export and copy about retention. | Given a canceled export or provider write failure, the app avoids reporting success and the ledger stays intact. |
| Recovery · Local restore | P0 / M4 | Driver can recover a saved file. Validate and import atomically. | Given a valid file and an empty ledger, restore reproduces the exported records. Later records are absent. Invalid files and populated-ledger restores preserve the ledger. |
| Cloud · Accounts | P0 / M5 | Invited driver can secure a recovery account. Add invites, credentials, session revocation, and recovery. | Given a reused invite or recovery code, the server rejects it. A session cannot access another account's snapshot. |
| Cloud · Upload | P0 / M5 | Driver can choose when to back up. Add explicit upload and confirmed status. | Given an interrupted upload or stale revision, the app preserves both copies and reports the last acknowledged recovery point accurately. |
| Cloud · Restore | P0 / M5 | Driver can recover a snapshot onto an empty installation. Reuse the file codec. | Given a backup followed by a new local entry, the second installation restores only the acknowledged records. The source device remains usable offline. |
| Release · Data controls | P0 / M6 | Driver can export or erase data. Add distinct local and cloud deletion flows. | Given a cloud deletion, sessions and the active snapshot are revoked and a restore drill preserves the deletion. |
| Release · Distribution | P0 / M6 | Invited driver can install and update. Verify actual download path and support instructions. | Given a populated M4 installation, the distributed APK upgrades with records intact on physical devices. |
| Release · Measurement | P0 / M6 | Driver can share coarse pilot counts. Add an opt-in aggregate export. | The export excludes amount, notes, contact data, and location. |
| Pilot · Operate | P0 / M7 | Pilot operator can evaluate repeat use. Record denominators and triage issues. | Four weeks of results and any evidence gaps are recorded before a scope decision. |
| Discovery · Moderated list | P2 | Evaluate a limited-area directory after the driver readout. | Require usable driver evidence, consent or reusable sources, and a named steward before proposing implementation. |
| Discovery · Map and partner tools | P3 | Explore needs using interview results. | Require separate scope approval and evidence of listing demand, operating cost, and upkeep. |

## Future product validation

**Deferred and unscheduled.** The product owner deferred recruitment at M2 closure and the customer discovery interviews at M3 closeout review. Recruitment, interviews, and the dependent synthesis remain incomplete. Keep research notes separate from code and test data. [Original research protocol](ROADMAP.md#business-validation-track)

| Epic and feature | Priority and original window | Story and slice | Acceptance |
|---|---|---|---|
| Research · Business recruitment | P1 / originally M2 | Pilot operator can reach local shops. Recruit a small set. | Count 5–10 targeted distinct businesses, responses, refusals, and any participation shortfall. |
| Research · Interviews | P1 / originally M3 | Pilot operator learns how details are maintained. Interview recruited shops. | Capture discovery channel, source of truth, correction ownership, consent expectations, and refusals. |
| Research · Synthesis | P1 / originally M4 | Pilot operator can assess business feasibility. Summarize participation and upkeep. | Findings include refusals, gaps, willingness, and evidence needed to test discovery later. |

### Manual airplane-mode walkthrough

**DEFERRED / NOT VERIFIED.** On a physical Android device in airplane mode with Wi-Fi disabled, log fuel and oil service, backdate an odometer reading, edit and delete entries, and reconcile displayed monthly totals by hand against the expected ledger. Record the device, OS, build, connectivity settings, and observed result for each step, including discrepancies.

### Human Quick Add study

**DEFERRED / NOT VERIFIED.** Observe 10 attempts across at least five drivers entering a familiar fuel expense. Measure from opening Quick Add to confirmed save. Record retries and incomplete attempts. Report the median observed completion time with its sample size and the first-attempt completion count and rate across all attempts. Record whether the median is strictly under 15 seconds and whether first-attempt completion is at least 80%. [Pilot success metrics](../product/SUCCESS_METRICS.md)

## Deferred M4 physical release acceptance

**DEFERRED / NOT VERIFIED.** The product owner approved proceeding to M5 development while this gate remains open. On an isolated physical device, verify a populated signed M3→M4 upgrade, offline cold launch, reminder lifecycle and notification denial and grant where supported, SAF export, restore into a separate empty ledger, and rejection on a populated ledger. Add a record after export and verify that it remains on the source and is absent from the restored ledger. Record device, OS, APK hashes, commands, and observed results. Preserve the existing Redmi installations. [Signed emulator evidence and handoff](ANDROID_RELEASE.md#m4-development-handoff)
