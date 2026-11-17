# Delivery roadmap

The developer owns implementation, review, device testing, and release signing. Codex assists in focused slices. M1 and M2 are formally complete. The M2 closure applies the product-owner revised scope recorded in [PLANS.md](../../PLANS.md#m2-room-and-vehicle-setup). M2 catalog sourcing and business recruitment are deferred. Its real system 150% font and TalkBack checks are **Waived — Accepted Residual Risk**, were NOT performed, and were not passed. The signed `0.1.0` offline cold-launch check retains its separate one-time **Waived — Accepted Residual Risk** disposition. [M1 audit and acceptance evidence](../../PLANS.md#m1-first-signed-apk)

| Milestone | Deliverable | Provisional engineering person-days |
|---|---|---:|
| M1 | First signed APK and verified update on the developer's phone | 3–5 (original estimate) |
| M2 | Room persistence, manual vehicle setup, and optional offline catalog integration | 6–9 |
| M3 | Offline expense ledger, odometer, and dashboard | 10–15 |
| M4 | Local prototype with reminders and manual file recovery | 8–13 |
| M5 | Optional account-backed cloud snapshot recovery | 13–20 |
| M6 | Market-ready closed pilot APK and tested release and restore process | 9–14 |
| M7 | Four-week pilot readout and scoped iteration | 5–8 |

## M3 delivery and closure status

**M3 is CLOSED WITH ACCEPTED RESIDUAL RISKS as of 2026-11-11.** M3.1 through M3.6 implementation is complete, and focused JVM and Redmi Note 9S instrumentation results are recorded. The product owner deferred the manual airplane-mode walkthrough, human Quick Add timing, and verification of the median-under-15-seconds and at-least-80% first-attempt completion targets. These checks remain **DEFERRED / NOT VERIFIED**. Business customer discovery remains deferred. [Acceptance evidence and residual risks](../../PLANS.md#m3-acceptance-and-closure-disposition) · [Future product validation backlog](BACKLOG.md#future-product-validation)

The engineering sequence is M1 → M2 → M3 → M4 → M5 → M6 → M7. Record applicable automated results and physical-device evidence at each gate. The revised ranges reflect the M1 audit and assume one developer. Confirm weekly availability before approving a milestone. [Test gates](TEST_STRATEGY.md) and [effort assumptions](COST_ESTIMATE.md)

## M4 development handoff and M5 planning

M4 development is complete. M4.1–M4.5 are accepted, and M4.6 signed APK verification, signed migration and emulator acceptance passed. The product owner approved proceeding to M5 while M4 physical release acceptance remains **DEFERRED / NOT VERIFIED**. Public release readiness is unestablished. [M4 release gate](BACKLOG.md#deferred-m4-physical-release-acceptance)

## M5 local-development handoff and M6 planning

**M5 DEVELOPMENT COMPLETE FOR ISOLATED LOCAL TESTING as of 2026-11-16.** M5.0–M5.4 completed the approved synthetic scope. Option A is approved as a limited-pilot design. External staging is unapproved and unverified. Real-data pilot acceptance and public release readiness remain open. [Local test evidence](../../PLANS.md#m54-explicit-cloud-download-and-atomic-recovery) · [Open operational and acceptance gates](../../PLANS.md#m5-operational-and-acceptance-gates)

M6 remains the market-ready closed pilot APK. Its existing scope covers separate local erase and cloud account deletion, an off-host server restore drill with deletion reconciliation, a tested signed distribution and upgrade path, privacy and support materials, and voluntary aggregate pilot measurement. Dependencies remain M5 acceptance, an eligible host and distribution path, representative physical devices, and reachability from at least two target Iranian mobile networks. The deferred M4 physical gate still applies to release acceptance. [M6 plan and exit criteria](../../PLANS.md#m6-market-ready-closed-pilot-apk)

[M6.0 planning](M6_PILOT_DECISION.md) is complete. M6.1 is **DEFERRED / BLOCKED BY PRODUCT-OWNER PRIORITY**. Approval of the deletion contract is still pending. Further server development, hosting and deployment remain paused under the updated direction. The closed-pilot scope and release gates remain in place.

## Roadmap from RC5 to Cafe Bazaar

**Product direction approved on 2026-11-16.** The offline Android MVP is a completed engineering foundation. Its signed RC5 `0.3.1` (code 5) passed the focused populated M4→RC5 physical upgrade, offline cold launch and fixture persistence check on Redmi Note 9S. The separate M4 physical gate remains **DEFERRED / NOT VERIFIED**. M5 local cloud evidence applies to synthetic development. Production deployment, real-data authorization, pilot entry, and Bazaar release are open. The stages below sequence work after RC5 while preserving the M1–M7 record. [RC5 evidence](ANDROID_RELEASE.md#populated-redmi-rc5-upgrade-pass) · [Production checklist](BAZAAR_RELEASE_READINESS.md)

| Stage and status | Deliverable and dependencies | Can proceed without hosting? | Owner approvals and evidence to close |
|---|---|---|---|
| A · Product Visual Design & UX Polish · **OPEN** | Review RC5, approve a bounded Persian RTL design, then implement the agreed existing journeys using Compose Material 3. See milestone below. | Yes. Synthetic data. | Owner signs off on design before broad implementation and accepts the implemented screens against actual screenshots, visual regression results and accessibility checks. |
| B · Offline quality and physical acceptance · **OPEN** | Use the approved visual build for focused offline, accessibility, performance and signed update checks. Device and fixture prep can overlap A. Preserve the separate deferred physical M3→M4 gate. | Yes. Synthetic data. | Approve device matrix, isolated device use, fixtures and performance thresholds. Attach actual offline cold launch, populated signed upgrade, reminder lifecycle and notification checks, manual SAF restore and rejection, and accessibility and performance results. Record accepted residual risks explicitly. |
| C · Cloud production architecture and deployment · **DEFERRED** | Close the provider, region, eligibility, cost and custody decisions, then verify approved synthetic staging with HTTPS, private PostgreSQL, encrypted storage and backups and least-privilege access. Depends on M5 local evidence and a decision to lift the server pause. Coordinate with D before admitting real data. | Design and runbook only. Staging and network evidence require an approved host. | Approve the provider, budget, operator, access and separate staging scope. Attach deployed configuration, renewal and encrypted restore evidence, plus measured DNS, TLS and recovery results on the actual endpoints. |
| D · Security, privacy, account deletion and operational recovery · **DEFERRED** | Review app and server exposure, account/session recovery, consent, erasure, retention and reconciliation. M6.1 remains gated by the unapproved contract. Operational rehearsal uses the approved staging configuration from C. | Policy and review can be drafted locally. Local implementation and synthetic tests require separate approval. | Approve the production data model and permitted data, lifecycle and custody policies, and incident owner. Attach security findings and fixes, checked Persian disclosures, operator runbooks, deletion/expiry tests and proof an older backup cannot restore access to deleted data. |
| E · Closed pilot with real users · **OPEN / BLOCKED** | M6 release and M7 readout, using the authorized invite route, real device and carrier matrix, and consented user journeys. Depends on A–D and the existing M4, M5 and M6 release gates. | Protocol can be drafted locally. The intended cloud-recovery pilot requires approved infrastructure. | Approve cohort, consent, real-data scope, distribution path and support owner. Attach signed install/update and replacement-device recovery evidence, results from at least two target Iranian mobile networks, and a pilot readout with denominators, incidents and a go/no-go decision. |
| F · Cafe Bazaar submission readiness · **OPEN** | Resolve live store requirements, finalize signing continuity, prepare Persian help, support, privacy text and listing assets, and validate the intended store-delivered build. Research can start now. Submission follows the pilot decision. | Public research, assets and runbooks can proceed locally. A release with cloud recovery depends on C–E. | Approve permanent application ID, signer and key custody, listing, release build and submission. Attach current official requirements, applicable console checks and candidate metadata. Track Bazaar approval and public rollout authorization separately. |
| G · Post-launch support and maintenance · **OPEN** | Prepare support intake, incident response, recovery, vulnerability review and signed update procedures before launch. Operate them if rollout is approved. | Runbooks and local drills can proceed offline. Live support and cloud recovery rely on approved operations. | Name the maintenance and incident owners and approve the operating cadence. Attach rehearsal results and update continuity evidence. After launch, record incidents and corrective releases with their verification. |

### Product Visual Design & UX Polish

**First milestone after the functional offline MVP.** Use the actual RC5 screens as the baseline. Limit changes to how existing functionality is presented and used. Keep storage, calendar, money, snapshot and offline-first behavior in force. Cloud screens may be represented as labelled design states for later approvals.

1. **Discovery.** Inventory screens and states using current synthetic RC5 captures and the existing release evidence. Walk vehicle setup and presentation, Home/dashboard, Quick Add/edit, History/monthly reports, reminders, and Settings/manual recovery. Record issues by affected action, device/viewport, text size and severity. Use existing screenshots where they establish the baseline. Capture missing states only on an authorized target in the follow-up task.
2. **Design proposal.** Present one restrained identity and hierarchy tailored to frequent logging, with Persian typography and number treatment, RTL focus and layout rules, reusable Compose Material 3 color, type, spacing, shape and component specs, responsive examples and light/dark appearance choices. Show Home, Quick Add and backup/recovery references. Map the same system to vehicle, History, reports and reminders, including empty, loading, error, success and confirmation states. Record asset licensing and any explicit deferrals.
3. **Mandatory design approval.** The product owner signs off on the references, type and appearance choices, component and state inventory, Persian copy, accessibility target, screens in scope and the acceptance matrix. Record approver, date, version and exclusions in PLANS.md. Broad implementation starts only after this approval.
4. **Implementation and evidence.** Begin with the approved Home and Quick Add designs and the tokens they use. Then apply them to vehicle presentation, History/reports, reminders and Settings/backup with focused behavior regressions. Compare actual app screenshots to the approved references at compact and wider sizes, normal and enlarged text, and each approved appearance. Retain state and motion evidence for affected interactions, with a named build, fixture, viewport and font scale. Resolve blockers, document any accepted deviations and obtain owner acceptance.

### Stop rules

- **P0 release blockers:** verified data loss or corruption, unsafe upgrade or restore, account isolation or other critical security/privacy failure, reproducible crash or ANR, or inability to complete a core journey. Stop the affected release gate. Reproduce with a bounded fixture, fix and rerun the affected case before reopening.
- **P1 UX issues:** usability or accessibility defects with a working path. Record driver impact and a concrete success condition. Fix issues that fail an approved gate or make a documented material improvement. Carry residual issues only with an explicit owner disposition.
- **Optional cosmetic work:** keep in backlog if it passes the agreed design and task criteria. Close design once approved screens, states and acceptance evidence match, with deviations recorded. Reopen only for a concrete defect, a missing acceptance criterion, or an approved product improvement. Rerun a passing test only for a relevant change, material environment change, or specific evidence gap.

## Business validation track

The original plan ran this research in parallel with M2–M4. The product owner deferred recruitment at M2 closure and explicitly deferred the outstanding customer discovery interviews at M3 closeout review on 2026-11-10. Recruitment and interviews remain incomplete. Carry the full sequence, including the dependent synthesis, in the [future product validation backlog](BACKLOG.md#future-product-validation). Scheduling requires a later product decision. Keep research time and notes separate from engineering estimates and test data.

| Original window | Research task | Evidence |
|---|---|---|
| M2 | Recruit 5–10 distinct automotive service businesses in the proposed pilot area. Start with repair and oil-change businesses. | Count contacted, participating, and declining businesses. Track coverage by category. Record a shortfall explicitly if recruiting does not reach five. |
| M3 | Interview participating businesses about how drivers find them, who updates contact or location information, and what consent, verification, and corrections would require. | Use the same core questions. Capture patterns, counterexamples, and reasons for refusal or limited participation. Avoid collecting customer records or sensitive business data. |
| M4 | Summarize willingness to participate, realistic responsibility for upkeep, and evidence needed to keep details current. | Record anonymized findings, refusals, and a recommendation to revisit, narrow, or defer discovery. |

The output is research evidence. Directory and partner dashboard work remain deferred. Review the findings together with the M7 driver readout before proposing any listing experiment. A later experiment needs a defined area, permission or reusable sources, a named listing steward, and a correction process. Map integration has a separate reachability, rights, and cost gate.

## M2 scope revision and closure

The original M2 plan required initial catalog sources, sourced vehicle selection, and parallel validation with 5–10 businesses. No redistributable vehicle dataset has been approved, and no businesses were recruited. On 2026-11-10, the product owner revised M2/MVP acceptance to close on the verified offline Room and manual vehicle setup foundation, the isolated catalog engine and disabled-by-default selection integration, and focused automated interaction checks. Production catalog data and business recruitment are deferred. The real system 150% font check and TalkBack review were explicitly waived with accepted residual risk and remain unperformed. They are not passes. See the signed scope disposition in [PLANS.md](../../PLANS.md#m2-room-and-vehicle-setup).
