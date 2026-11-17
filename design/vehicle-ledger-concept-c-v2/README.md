# Vehicle Ledger · complete Concept C design

**2026-11-16 · design package for implementation handoff**

[Open the interactive preview index](prototype.html?scene=index) · [Open Concept C Home](prototype.html) · [View the screen overview](previews/overview.png)

Use [ANTIGRAVITY_IMPLEMENTATION.md](ANTIGRAVITY_IMPLEMENTATION.md) as the starting point. It names the exact reference screens and requirements for each implementation slice. The [UX specification](UX_SPEC.md) defines routes, Back, screen states, validation and Persian copy. The [design system](DESIGN_SYSTEM.md) contains both appearances, exact tokens and component measurements. The [gated cloud specification](GATED_CLOUD.md) covers the existing synthetic debug interface without changing when it is exposed.

## Visual directory

The base views are 390 × 844 PNGs. The prototype assets and both scripts work offline in the same directory. Add `theme=dark` or `text=150` to a preview URL to check the alternate appearance or enlarged text. The viewport reference is browser CSS geometry. All accounts, financial records, vehicles and recovery results are synthetic.

| Group | Key previews |
|---|---|
| First launch and vehicles | [First launch](previews/c-first.png) · [Home](previews/c-home.png) · [Vehicle sheet](previews/c-vehicle-switch.png) · [Management](previews/c-vehicles.png) · [Add](previews/c-vehicle-add.png) · [Edit](previews/c-vehicle-edit.png) · [Gated catalog](previews/c-catalog.png) |
| Entry | [Selector](previews/c-selector.png) · [Fuel with IME](previews/c-fuel-keyboard.png) · [Fuel validation](previews/c-fuel-error.png) · [Expense](previews/c-expense.png) · [Service](previews/c-service.png) |
| Review | [History](previews/c-history.png) · [Details](previews/c-record.png) · [Edit](previews/c-record-edit.png) · [Delete confirmation](previews/c-delete.png) · [Report](previews/c-report.png) · [Month picker](previews/c-month.png) |
| Reminders | [List](previews/c-reminders.png) · [Date form](previews/c-reminder-create.png) · [Mileage form](previews/c-reminder-mileage.png) · [Detail](previews/c-reminder-detail.png) · [Cancel confirmation](previews/c-reminder-cancel.png) |
| Settings and manual recovery | [Settings](previews/c-settings.png) · [Appearance](previews/c-appearance.png) · [Notifications](previews/c-notifications.png) · [Blocked notifications](previews/c-notification-blocked.png) · [Backup menu](previews/c-backup.png) · [Export](previews/c-export.png) · [Restore](previews/c-restore.png) · [Populated ledger](previews/c-restore-blocked.png) · [Invalid file](previews/c-restore-invalid.png) · [Success](previews/c-restore-success.png) · [Success with notice](previews/c-restore-warning.png) |
| Gated synthetic cloud | [Sign-in](previews/c-cloud-auth.png) · [Bound](previews/c-cloud-bound.png) · [Restore review](previews/c-cloud-restore.png) · [Protected code](previews/c-cloud-recovery.png) |
| Appearance and large text | [Dark Home](previews/c-home-dark.png) · [Dark Fuel](previews/c-fuel-keyboard-dark.png) · [Dark report](previews/c-report-dark.png) · [Dark Settings](previews/c-settings-dark.png) · [150% Home](previews/c-home-150.png) · [150% Fuel](previews/c-fuel-keyboard-150.png) · [150% History](previews/c-history-150.png) |

All individual filenames appear in [PREVIEW_INDEX.md](PREVIEW_INDEX.md). Alternate errors that share a visual layout use the exact copy and recovery actions in the [UX specification](UX_SPEC.md#error-and-result-copy). Native Android keyboard, permissions and Files appearance remain platform-owned. Prototype submit/restore actions are demonstrations and do not write application data.

The package is ready for an implementation assignment. All current workspace writes were scoped to this design directory. The earlier Concept C package remains a separate reference. Android builds, APK installs, physical devices, commits and release operations were outside this work.

[Browser verification record](VERIFICATION.md)
