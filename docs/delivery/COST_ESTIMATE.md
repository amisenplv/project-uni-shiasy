# Provisional effort and cost estimate

All effort and calendar figures are provisional planning ranges for one developer with Codex assistance. A focused engineering day is about 5–6 hours of implementation, review, debugging, automated checks, physical-device verification, or release work. The developer owns acceptance, signing, and release decisions. The ranges assume small reviewable slices and include a normal amount of regression work and retesting.

| Milestone | Original engineering person-days | Revised engineering person-days | Main uncertainty |
|---|---:|---:|---|
| M2 | 5–7 | 6–9 | Room and KSP approval, first schema and test fixtures, catalog aliases and source rights, persistence across reboot |
| M3 | 8–11 | 10–15 | Rial and toman wording, Jalali boundaries, atomic ledger and odometer edits, populated migration |
| M4 | 6–9 | 8–13 | Reminder behavior and permissions across devices, file provider failures, restore validation and rollback, signed upgrade |
| M5 | 9–14 | 13–20 | Auth and account isolation, backend setup, retries and stale writes, real database tests, second-installation recovery |
| M6 | 6–9 | 9–14 | Privacy and deletion, off-host restore drill, distribution and network reachability, cross-device upgrades |
| M7 | 4–6 | 5–8 | Pilot support, regression fixes and signed updates, evidence gaps and readout |
| M2–M4 to local prototype | 19–27 | 24–37 | Includes physical-device acceptance and signed prototype upgrade |
| M2–M6 to closed pilot APK | 34–50 | 46–71 | Includes the M5 recovery work in the current sequence |
| M2–M7 through pilot readout | 38–56 | 51–79 | Engineering effort includes M7 fixes and readout |

| Focused days per week | Local prototype from M2 start | Closed pilot APK from M2 start | Four-week readout from M2 start |
|---|---:|---:|---:|
| 3 | 8–13 weeks | 16–24 weeks | 20–28 weeks |
| 4 | 6–10 weeks | 12–18 weeks | 16–22 weeks |
| 5 | 5–8 weeks | 10–15 weeks | 14–19 weeks |

Actual weekly availability is unconfirmed. Use four focused days per week as a planning scenario until the developer confirms capacity. The table rounds effort up to whole weeks and starts after M1 closure. The readout adds four weeks to closed pilot readiness and assumes M7 engineering fits in that window. Pre-build and M1 effort are excluded. Approvals, pilot recruitment, host and network qualification, or delays accessing physical devices can extend elapsed time. Track M2–M4 business validation separately as non-technical time. Time spent on that work also reduces the developer's available engineering days.

## M1 evidence behind the re-estimate

- **One-time baseline work.** Pinning and provisioning JDK 17, SDK 36, and the Gradle cache, resolving initial dependencies, creating the Persian shell and first instrumentation harness, addressing the initial MIUI launch blocker, establishing the signing workflow, and the initial test-key backup restore. These activities can recur after a toolchain, machine, key, or device change. New Room and backend infrastructure are separately included in M2 and M5.
- **Recurring milestone work.** Implement and review each slice, reproduce and fix defects, rerun build, lint, and applicable regression checks, verify physical-device behavior, sign and check applicable releases, rehearse upgrades, and record evidence. Include time for device availability, build variant differences, and reruns in the planning ranges.
- **Observed timings.** The initial combined build was recorded at 6m 50s. Later successful runs included a 50-second offline host check and a 52-second connected test command. Earlier download and instrumentation attempts failed or stalled and needed diagnosis. These are command durations. The original M1 range was 3–5 focused days, and no complete time log exists for setup, coding, debugging, review, signing, and device work.
- **Forecast method.** Reuse the M1 foundation and estimate new scope from each milestone's acceptance tests. The low ends assume available test devices and straightforward implementation. The high ends allow ordinary rework in the areas named above. Review the ranges after M2 with actual focused time and separate time spent waiting on approvals or external access. [Roadmap](ROADMAP.md) and [M1 evidence](../../PLANS.md#m1-first-signed-apk)

## Cash costs

M1–M4 use a development machine and Android device. Hosted runtime costs begin in M5. Use US$50–150 per month as a provisional internal allowance for an eligible host, staging, domain, monitoring, and encrypted off-host backups. Obtain quotes in the developer's payable currency before provisioning. SMS, map services, and app-store expenses are outside the pilot budget.
