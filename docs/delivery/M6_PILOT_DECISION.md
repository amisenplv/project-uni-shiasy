# M6.0 closed-pilot decision record

Prepared 2026-11-16. **Planning complete. Product-owner decisions pending.**

**External staging: BLOCKED. Real-data pilot: BLOCKED.** Option A is approved for the limited-pilot design. The running service can read snapshots. M5.0–M5.4 are accepted for isolated synthetic testing. External staging requires explicit authorization and provider eligibility. A real-data pilot additionally requires policy approval, operational controls, and the acceptance evidence below. Public release readiness remains unestablished. The [M4 physical release gate](BACKLOG.md#deferred-m4-physical-release-acceptance) remains **DEFERRED / NOT VERIFIED** and open.

## Hosting candidates

Public documentation establishes product availability. Capacity at order time, account eligibility, payment continuity, and reachability of the selected hostnames from Iranian mobile networks remain unverified.

| Candidate | Architecture and solo maintenance | Recurring cost categories and eligibility risks |
|---|---|---|
| Hetzner Cloud, Falkenstein, Germany | One VM for the API and self-managed PostgreSQL on a Unix socket with no public database listener. Use an HTTPS reverse proxy with ACME renewal, encrypted database storage under operator-approved custody, and separately encrypted off-host PostgreSQL backups. The operator owns OS and database patching, monitoring, backup verification, and recovery. A VM outage affects both services. [Locations](https://docs.hetzner.com/cloud/general/locations/) · [Data protection](https://docs.hetzner.com/general/company-and-policy/data-protection-at-hetzner/) | VM, storage, bandwidth and IP where billed, DNS, off-host backup storage and restore traffic, monitoring, and operator time. Verify account onboarding, permitted customer and end-user locations and workload, recurring payment eligibility, and account recovery before selection. [Payment methods](https://docs.hetzner.com/general/billing-and-account-management/billing-at-hetzner/payment-overview/) · [Terms](https://www.hetzner.com/pdf/en/AGB_en.pdf) |
| DigitalOcean, Frankfurt FRA1, Germany | API Droplet plus Managed PostgreSQL in the same VPC. Restrict database access to the API and verify private TLS connectivity. Use ACME at the API edge. The provider manages database maintenance and offers backup/PITR. Confirm encryption at rest, backup retention, and key custody for the chosen plan. Keep a separately encrypted off-host export and independently recoverable deletion records. The operator still owns app, host, access, and restore procedures. [Regions](https://docs.digitalocean.com/platform/regional-availability/) · [Managed databases](https://docs.digitalocean.com/products/databases/) | API compute, managed database and storage, backup/PITR and off-host copies, restore traffic, DNS, transfer, monitoring, and operator time. U.S. provider terms and trade controls make eligibility for this specific workload and intended users a gating question. Verify account ownership, permitted access, accepted recurring payment, and account recovery. [Terms](https://www.digitalocean.com/legal/terms-of-service-agreement) · [Payment methods](https://docs.digitalocean.com/platform/billing/manage-payment-methods/) |

Start eligibility review with Hetzner Falkenstein for the one-host staging topology already in the roadmap. Consider DigitalOcean FRA1 if managed database maintenance fits the approved budget and eligibility is confirmed. German hosting still depends on international routing from Iran. For each candidate, test DNS, TLS, APK download, and authenticated API transfers on the actual hostnames after approval. Routing disruption, filtering, or loss of international access can affect either candidate. Confirm that the named operator can also access the control plane and recover the account. Verify ACME issuance and renewal against the selected DNS and ingress configuration. [ACME challenge types](https://letsencrypt.org/docs/challenge-types/)

Obtain provider confirmation and appropriate compliance review for the account holder, intended users, workload, payment, and data locations. Applicability of U.S. cloud-service authorizations depends on the service and use. Hosting region alone does not establish eligibility. [OFAC cloud-service guidance](https://ofac.treasury.gov/faqs/1088)

## Product-owner approvals

Every row below is pending. Record the decision, approver, date, and evidence reference for each.

| Decision | Recommended starting point |
|---|---|
| Provider and region | Review Hetzner Falkenstein first. Approve data and backup jurisdictions, provider eligibility, and actual target-network results before pilot use. |
| Monthly budget | Owner sets an all-in ceiling and currency after an eligible quote. Include taxes, off-host recovery storage and transfer, DNS, monitoring, and expected operator time. No spend is authorized. |
| Operations and custody | Name the accountable operator, backup/key custodian, and incident owner. Document MFA and access recovery, least-privilege roles, separate app, migration, backup and operator credentials, rotation, and privileged-access audit. Keep the APK signing key developer-controlled. |
| Staging and real data | Approve synthetic-only staging as a distinct decision with separate credentials and database. Defer real-data authorization until controls, notices, and acceptance evidence are reviewed. |
| Persian notice and consent | Obtain opt-in consent before the first real upload. Explain snapshot contents, server readability, hosting and backup locations, retention and deletion limits, manual SAF, and offline logging without an account. Use Persian RTL copy and accessible controls. |
| Snapshot lifecycle | ADR proposal is 90 days from acknowledgement, read blocked at expiry, purge within 24 hours, and no extension on reads or idempotent retries. Approve or revise before implementation against real data. |
| Operational backup lifecycle | ADR proposal is encrypted off-host copies and provider PITR with a 14-day maximum. Approve a separately durable minimal deletion record that outlives the oldest recoverable backup plus restore lag. Approve recovery objectives and audit evidence. |
| Incident response | Name who pauses access, revokes sessions, rotates credentials and keys, communicates with drivers, and authorizes reopening after recovery. |
| Distribution and device evidence | Use an invite-controlled HTTPS download of the verified signed APK with Persian install, update, checksum, and support instructions. Approve an isolated older and newer physical Android device and two target mobile networks, for example MCI and Irancell if used by the intended cohort. |

## Release and recovery acceptance matrix

All rows below are open for the stated release environment. Record build and SHA-256, device and Android version, carrier where relevant, date, observed result, and redacted evidence. Preserve prior local and emulator results as recorded in the [test strategy](TEST_STRATEGY.md).

| Gate | Required evidence |
|---|---|
| M4 physical prerequisite | On an isolated physical target, verify the populated signed M3→M4 upgrade and data preservation, signed offline cold launch, reminder lifecycle and notification denial/grant, actual SAF export/restore to an empty ledger, and rejection into a populated ledger. Follow the [deferred gate](BACKLOG.md#deferred-m4-physical-release-acceptance) and [signed artifact record](ANDROID_RELEASE.md#m4-development-handoff). |
| M6 signed installation and upgrade | On representative older and newer phones, download through the actual invite path. Record application ID, signer, versions, checksum, and install result. Upgrade a populated M4 fixture in place and compare IDs, amounts, dates, readings, reminders, and report totals. Exercise Persian support instructions. |
| Offline, reminders, and manual SAF | Cold-launch in airplane mode with Wi-Fi and mobile data off. Log locally. Verify reminder creation, due handling, completion and cancellation, plus notification denial/grant where supported. Export via DocumentsUI, add a later record, restore into a separate empty installation, compare snapshot-time records, and verify populated-target rejection. Keep the source-emulator SAF pass and replacement-emulator picker failure distinct. |
| Cloud recovery and sessions | With synthetic accounts on approved staging, explicitly upload and acknowledge a revision, then restore it on a second physical installation with exact field equality and no later source record. Verify interrupted upload and retry, stale revision rejection, revoked and expired bearer rejection, and zero automatic transfers on restart. Record notification cleanup and preserve the documented crash window after Room commit. |
| Separate erase and deletion | Verify local erase leaves the cloud account and snapshot available. Verify cloud deletion requires fresh credentials, revokes sessions, removes the active snapshot and account access, blocks concurrent or stale writes, and preserves local records. |
| Off-host operational recovery | Restore encrypted PostgreSQL and snapshot bytes into an isolated environment with the API closed to traffic. Compare surviving payloads and revisions. Reapply deletions and expiry from the independently retained record. Verify no deleted account or snapshot returns, pre-restore sessions and stale upload revisions fail, and obsolete invites or codes are invalidated when their state cannot be verified. Record elapsed recovery time, recovery point, and backup-retention evidence. Reopen only after checks pass. |
| Two Iranian mobile networks | On each approved carrier using mobile data directly, record DNS and TLS outcome, signed APK download and checksum, authentication, upload and replacement-device download, and failures or retries. Use the actual pilot endpoints and document test location and time at coarse precision. Provider-region documentation alone supplies no reachability evidence. |

## Implementation sequence

The product owner deferred server work and external deployment. M6.1 is **DEFERRED / BLOCKED BY PRODUCT-OWNER PRIORITY**. The sequence below records the original M6 proposal for later review. The active priority is the [Android-only MVP completion plan](../../PLANS.md#android-only-mvp-completion-plan).

| Slice | Boundary and dependency |
|---|---|
| 1. Local data controls and deletion reconciliation | **Recommended first implementation slice after separate approval.** Use synthetic fixtures on local PostgreSQL. Start with account deletion under the existing account lock, revocation, active snapshot removal, and durable reconciliation tests for an older backup. Confirm deletion-record ownership, containment and semantics before implementation. Add and verify distinct local erase and cloud deletion controls within this slice. External hosting and real data are unnecessary. |
| 2. Synthetic release and recovery rehearsal | Prepare the existing M6 packaging and restore runbook. Rehearse encrypted backup and restoration locally, signed install and upgrade on isolated physical targets when separately authorized, and opt-in aggregate counts that omit amounts, notes, contacts, and location. Sign and distribute artifacts only under existing developer-controlled approvals. This slice can use synthetic data throughout. |
| 3. Approved synthetic external staging | Requires explicit provider, region, budget, credential and operator approval. Verify HTTPS, encrypted storage and off-host backups, the restore drill, the actual signed distribution and physical recovery paths, and both Iranian networks. |
| 4. Closed-pilot release decision | Requires the remaining M4 physical gate, M5 staging and physical evidence, M6 exit criteria, and explicit approval for real-data handling, consent, retention, deletion and support readiness. |

[Roadmap](ROADMAP.md) · [M6 execution plan](../../PLANS.md#m6-market-ready-closed-pilot-apk) · [Security and retention proposals](../architecture/ADR/0004-cloud-recovery-security.md#approval-and-cost-gates)
