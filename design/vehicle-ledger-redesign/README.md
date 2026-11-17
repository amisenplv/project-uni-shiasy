# Vehicle Ledger redesign proposal

**Awaiting product-owner review · 2026-11-16**

[Open the light prototype](prototype.html?theme=light#home) · [Open the dark prototype](prototype.html?theme=dark#home) · [Light overview](previews/overview.png) · [Dark overview](previews/dark/overview.png)

Vehicle-first Home gives fuel and other expenses direct actions, with a prominent odometer readout, a due service with its date, and two recent records. The notebook combines a Jalali day rail in History with a monthly spending summary. A returning driver can reach manual file recovery before creating the first vehicle.

## Deliverables

- [Product diagnosis, reference study, information architecture and user-flow diagrams](UX.md)
- [Design system, Persian copy and behavior for every A–N screen](DESIGN.md)
- [Antigravity implementation handoff and open owner decisions](ANTIGRAVITY.md)
- [Prototype HTML](prototype.html), [styles](prototype.css), [interaction fixtures](prototype.js), [Vazirmatn font](assets/Vazirmatn.ttf) and [font license](assets/OFL-Vazirmatn.txt)
- [Browser verification and release-integrity check](VERIFICATION.md)

The prototype opens directly from disk. Appearance defaults to the system setting. Choose «حالت نمایش» in Settings or in the desktop review controls. At desktop widths, choose a scenario in the review index or follow controls in the handset. The base phone frame is 390 × 844. At mobile widths the handset uses the available screen. To open an individual scenario, append its route hash to the prototype URL. To review browser-only text enlargement, open [History at 150%](prototype.html?capture=1&text=150#history) or another route with those options. The JSON picker and keyboard drawings are examples. Form validation is lightweight and illustrative. Saving, deleting, reminders, permission changes and recovery demonstrate view states with synthetic fixtures. The preview accesses no production data, captures no real document, and performs no domain transactions.

## Visual references

| Journey | PNG screens |
|---|---|
| Start and manage vehicles | [First launch](previews/empty.png) · [Add vehicle](previews/vehicle-add.png) · [Edit vehicle](previews/vehicle-edit.png) |
| Home and switching | [Home](previews/home.png) · [Multiple vehicles](previews/home-multi.png) · [Vehicle picker](previews/switch.png) |
| Capture | [Fuel](previews/fuel.png) · [Expense and service](previews/expense.png) · [Validation and IME](previews/validation.png) |
| Review and correct | [History](previews/history.png) · [Edit](previews/edit.png) · [Delete confirmation](previews/delete.png) · [Loading](previews/loading.png) |
| Monthly spending | [Monthly report](previews/report.png) |
| Reminders | [Due, upcoming and past](previews/reminders.png) · [Create reminder](previews/reminder-add.png) · [Date notifications](previews/notifications.png) |
| Settings and export | [Settings](previews/settings.png) · [Manual backup](previews/backup.png) · [Export prep](previews/export.png) · [Export success](previews/export-success.png) |
| Restore | [Restore prep](previews/restore.png) · [Populated-ledger rejection](previews/restore-blocked.png) · [Invalid file](previews/restore-error.png) · [Success](previews/restore-success.png) |
| 360px at 150% text | [Home](previews/home-text150.png) · [Validation and IME](previews/validation-text150.png) · [History](previews/history-text150.png) · [Delete confirmation](previews/delete-text150.png) |

All deliverables live in this directory. This proposal does not implement Android, change the database or API, enable cloud or catalog features, alter a release artifact, or record milestone acceptance. The owner review is the stopping point.


## Dark preview index

Dark PNGs mirror the light filenames under `previews/dark/`. The package includes 25 normal-size states and four at 150%.

| Screen group | Dark PNGs |
|---|---|
| Home and setup | [Home](previews/dark/home.png) · [Multi-vehicle selector](previews/dark/switch.png) · [First launch](previews/dark/empty.png) |
| Capture | [Fuel](previews/dark/fuel.png) · [Expense](previews/dark/expense.png) · [Validation and IME](previews/dark/validation.png) |
| Review | [History](previews/dark/history.png) · [Delete confirmation](previews/dark/delete.png) · [Monthly report](previews/dark/report.png) · [Reminders](previews/dark/reminders.png) |
| Settings and recovery | [Appearance setting](previews/dark/settings.png) · [Manual backup](previews/dark/backup.png) · [Restore error](previews/dark/restore-error.png) · [Restore success](previews/dark/restore-success.png) |
| 150% text | [Home](previews/dark/home-text150.png) · [History](previews/dark/history-text150.png) · [Validation](previews/dark/validation-text150.png) · [Delete](previews/dark/delete-text150.png) |
