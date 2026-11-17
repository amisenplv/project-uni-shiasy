# Risk register

| Risk | When | Mitigation and decision gate |
|---|---|---|
| Android tooling or dependency downloads are unavailable | M1 | Verify pinned JDK, SDK, wrapper, and dependency access on the actual build host. Record setup time separately. |
| APK install, signature, or update fails | M1 and M6 | Confirm permanent ID and developer-controlled key. Test direct install and a signed upgrade over records. Verify current distribution rules before sharing. |
| Records disappear after schema change or interrupted save | M2–M6 | Export schemas, test populated migrations and transactional writes, and retain the database on failure. |
| Monthly totals are wrong because of currency labels, date conversion, or duplicate writes | M3 | Verify receipts, add locale and date boundary fixtures, test repeated saves, and reconcile on device. |
| Drivers stop because recording is slow or the dashboard has little value | M3 and M7 | Observe the entry journey, measure real repeat saves, and improve the driver loop before expanding scope. |
| Businesses decline participation or details go stale | M2–M4 | Track refusals and upkeep ownership in the research track. Require permission or reusable sources and a steward before considering a directory. |
| Users expect automatic backup or live synchronization | M4–M6 | State each recovery boundary in the app. Show snapshot time and later unsaved changes. Test stale writes and restoring into an empty ledger. |
| Hosting is ineligible or unreachable on pilot networks | M5–M6 | Verify operator eligibility, payable costs, and access on target networks before release. Keep the core local. |
| Private data leaks or a restore re-exposes a deleted account | M5–M6 | Enforce ownership, redact logs, protect tokens, and test deletion reconciliation after an off-host restore. |
| Solo availability or recruitment delays the calendar | M1 and M7 | Re-estimate after M1, use small slices, track research separately, and report cohort denominators and missing data. |

The solo developer maintains the register. Link observed failures and resolved decisions in [PLANS.md](../../PLANS.md). All risks are open until evidence is recorded.
