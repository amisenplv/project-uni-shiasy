# Vehicle Ledger · UX correction sprint v1

**Decision checkpoint · 2026-11-16**

## Evidence and diagnosis

Reviewed all nineteen PNGs in the supplied Antigravity directory, the current Compose routes and view models, the prior Antigravity audit, and the local design package including the light and dark proposal. Counts and visibility below are observations of the supplied captures. Source findings describe the current checkout. The files do not establish task timing, user preference or physical accessibility outcomes. Git has no commit baseline, so there is no reliable diff from a previously approved UI.

| Area | Captured evidence | What the working source confirms |
|---|---|---|
| Home | In 03 and dark 03, the vehicle panel, shortcuts, warning panel and recent cards all compete for attention. In 04, two full vehicle-selection cards appear before a second representation of the selected vehicle. Home 13 at 150% still shows the shortcuts, but the labels wrap and recent records fall below the fold. | HomeScreen builds a full selector card for each vehicle and then a separate selected-vehicle panel. Both Home callbacks currently resolve to the same Fuel-default draft. General expense needs its own presentation route and explicit existing category choice. The active vehicle has separate asynchronous mileage and recent-history collection. The immediate selection update clears mileage, while previous recent rows can remain visible until the next observer emits. |
| Entry | 05 and dark 05 show a generic cost/service heading over fuel and a large expanded optional area. In 06, decimal keyboard and field errors obscure the save action; 12 shows the same vertical pressure at 150%. The two fuel labels name an amount in toman and liters, but the amount does not say it is the total paid. | EntryFormScreen places the header and everything including Save in one vertical scroll container. Details default to expanded. Amount and volume both use decimal entry and share a row even when enlarged. Validation already recognizes exact amount and liters independently. There is no stored or required unit price. |
| History | 07 and dark 07 already have day markers and a ruled rail. Two complete records and part of the third are visible. The bottom area of each card repeats Delete. 08 confirms an explicit deletion dialog already exists. | LedgerScreen uses a lazy list, per-vehicle scroll state and date headings. It still renders block cards and inline Delete. A history redesign should keep existing sort and confirmation, free vertical space, and keep destructive actions discoverable in record details. |
| Reminders | 10 and dark 10 show the creation form before reminders. Only part of the first due item is visible. | RemindersScreen inserts notification options and the creation form before load/empty/list states. Due, pending and closed conditions already exist. A list-first arrangement is feasible with the existing rules. |
| Supporting screens | 01/02 show empty setup and manual vehicle creation. 09 and dark 09 show reporting for the active vehicle and an empty month. 11 and dark 11 show appearance and manual backup. | The alternatives keep entry points to setup, existing reporting, Settings and recovery. Those supporting screens are outside visual design for this checkpoint. |

Source references  
[Emulator evidence directory](</home/amir/.gemini/antigravity/brain/24a527bc-3dcd-49df-a31e-6cfe2d48e738/screenshots>) · [Earlier Antigravity audit](</home/amir/.gemini/antigravity/brain/24a527bc-3dcd-49df-a31e-6cfe2d48e738/VEHICLE_LEDGER_DESIGN_SYSTEM_AND_UX_AUDIT.md>) · [Earlier design proposal](../vehicle-ledger-redesign/README.md) · [HomeScreen](../../app/src/main/java/org/unishiasy/vehiclelog/ui/home/HomeScreen.kt) · [HomeViewModel](../../app/src/main/java/org/unishiasy/vehiclelog/ui/home/HomeViewModel.kt) · [LedgerScreen](../../app/src/main/java/org/unishiasy/vehiclelog/ui/ledger/LedgerScreen.kt) · [EntryDraft](../../app/src/main/java/org/unishiasy/vehiclelog/ui/ledger/EntryDraft.kt) · [RemindersScreen](../../app/src/main/java/org/unishiasy/vehiclelog/ui/reminders/RemindersScreen.kt) · [Data design](../../docs/architecture/DATABASE_DESIGN.md)

## Navigation and state proposal

| Area and primary goal | Primary and secondary actions | Navigation and Back | Empty, load, error and keyboard states |
|---|---|---|---|
| Home and switching. Identify the active vehicle and start the intended task. | A puts fuel/expense first and uses a modal vehicle picker. B puts recent activity first, uses one persistent record action, and opens a dedicated vehicle list. Both expose existing History, Reminders, reporting and Settings. | A returns directly to the same scroll position on Home. B retains the active vehicle across its three top-level destinations. In A dismissing the picker restores its trigger. In B Back from the vehicle list leaves the current selection intact; tapping a row makes a new selection and returns. Keep Add/Edit in the chooser flow. | Vehicle load, genuine empty ledger, and read failure are distinct. No-vehicle state offers Add and Restore. Preserve previously visible content during same-vehicle retry when safe. On scope change, gate mileage, recent entries, reminders and reports by the new ID. Keep entry available without optional reads. No keyboard needed to switch. |
| Fuel and expense. Record an event quickly and accurately. | A opens a one-screen fuel form. B opens a type chooser, then collects the receipt total and liters in two steps. Both use the existing expense categories in the general-expense path; preserve the amount-optional categories. Date, mileage and notes stay reachable. | A Back first dismisses keyboard, then leaves the draft through the existing guard or dismissal behavior. B Back from liters first dismisses keyboard, then returns to amount with the value retained. In both, the draft pins the vehicle ID and a single save commits the whole record. No partial records. | Blank new entry contains empty required inputs and one clearly visible default date. Distinguish field errors from save/storage errors. Keep values and focus/announce the first invalid field. Keep the primary action above the IME while the body scrolls. A groups values at normal scale and stacks them when needed. B keeps one editable required input per step. |
| History. Find and inspect an existing record. | Primary action is opening a row. Secondary actions are edit and delete from the record, with a clear confirmation and linked-mileage consequences. Keep reporting reachable from the existing ledger context. | A enters History from Home. B gives it a persistent destination. Back from an opened record returns to the same scoped list and position. | Show no-records, pending and retry separately. Use dense rows at default size and allow growth at 150%. The list needs no keyboard. Editing follows the entry behavior. Detail design awaits selection. |
| Reminders. See what is due and act on it. | Due and upcoming entries lead. Open an existing reminder to complete/cancel. Create via a separate action. Keep opt-in date notifications and OS controls reachable. | A routes from its due line or list shortcut. B gives Reminders a persistent destination. Back from a creation form returns to the scoped list. | Distinguish no reminders, no due items, missing mileage, failed loads and failed writes. Keep status visible without color alone. The keyboard belongs to the creation form. Detail design awaits selection. |

### Fuel amount and precision

Both concepts label the editable amount **«مبلغ کل پرداختی (تومان)»** and the separate volume **«حجم سوخت (لیتر)»**. The examples use **۲۴۰٬۰۰۰ تومان** and **۴۰ لیتر**. A unit price would be a derived or separately entered value with rounding and edit-reconciliation questions. This proposal leaves it outside both save paths. Preserve positive amount, at most one fractional toman digit as supported by the current parser, positive liters with up to three decimals, exact integer legacy rials and whole milliliters. Keep the editable Jalali date, Tehran default, optional linked odometer, and history, reminder and snapshot behavior unchanged. Amount and liters take decimal IME, year/date keeps slash accessible, and mileage takes numeric IME. Known linked reading conflicts retain the existing explicit save-without-mileage option and existing edit/delete semantics.

## Visual concept plan

Both concepts use the same sample set and palette so interaction differences are easy to compare. Cool stock `#F3F6F8`, white `#FFFFFF`, deep teal `#11363B`, action teal `#075A63`, ink `#112D34`, and amber warning fill `#FFF0D3`. Vazirmatn for Persian UI and isolated numbers. Use 48px minimum action height, 16–20px gutters, 8–12px radius and no gradients. The dark tray in A is a task boundary. The date rules in B carry chronology.

```text
A · QUICK LOG                       B · VEHICLE JOURNAL
[ vehicle / switch     settings ]   [ journal          settings ]
[ current mileage             ]    [ scoped vehicle / switch   ]
[  fuel        | expense      ]    [ recent dated entries       ]
[ due item                    ]    [ due reminder               ]
[ history / reminders / report]    [ Home | history | reminders ]
                                   [ new record                 ]

Fuel · A                           Fuel · B
[ vehicle / back ]                  [ vehicle / back ]
[ amount | liters ]                 [ 1 amount  →  2 liters ]
[ date / optional details ]         [ active input ]
[ save ]                            [ continue / save ]
```

## Decision tradeoffs

| Decision | A · Quick log | B · Vehicle journal |
|---|---|---|
| Home priority | Direct fuel and expense actions remain visible without scrolling at ordinary scale. A due line and quiet destination rows follow. | The first viewport shows recent dated activity. A New Record button and bottom destinations support regular review and cross-destination movement. |
| Vehicle switching | Tap the vehicle name, then choose in a bottom sheet. The background preserves task context. Long vehicle lists need a scrollable sheet. | Tap the scoped vehicle, then choose on a full destination. More room for long lists and names, with an additional full-screen transition. |
| Home → first fuel field | One tap to open the fuel form. | Two taps via New Record and Fuel. |
| Fuel input | Amount and liters visible together at ordinary scale. Scanning and correction are fast, but both fields and their error copy compete for space with the keyboard. | Amount then liters. Continue adds a tap. Back preserves the draft. A compact summary keeps the first amount visible at the second step. One editable value per step keeps more vertical space for error and IME. |
| Important open question | Can drivers quickly find the optional date and mileage controls without extra instruction? | Does the extra entry effort pay off for drivers who use the app as a journal, especially with a short screen or enlarged font? |

Review A and B as complete Home/Fuel pairs or indicate which interaction you want revised. No direction is selected. History and Reminders remain at navigation and state level for this checkpoint.

### Accessibility, readability and feasibility comparison

| Criterion | A · Quick log | B · Vehicle journal |
|---|---|---|
| Status clarity | Selected vehicle, last odometer and both next actions share one dark surface. Home omits a repeated latest-record list. | The vehicle and last odometer share a compact scope row. Three sample records explain recent activity. Mileage is less prominent and requires a deliberate glance. |
| Information density | Low reading load for frequent logging. Reviewing a record takes a visit to History. | More record context on Home. Persistent navigation and a full-width New Record action reserve vertical space that could otherwise show another row. |
| Navigation predictability | One Home hub with direct links. A short vehicle list is natural as a modal and can be canceled without a route change. | Persistent Home, History and Reminders give stable locations. A larger scope-selection destination gives vehicle names and actions more room. Retaining top-level scroll and focus across tabs needs explicit design and testing. |
| Persian and visual personality | A compact, dark vehicle-and-action surface prioritizes the next task. The known amount and liter labels can wrap on narrow screens; stack those fields when needed. | White space, dated rules and aligned numbers make the vehicle log recognizable. At 150%, record amounts stack below labels and the visible record count drops. |
| Keyboard and 150% | Both numbers are editable on one screen. At large text with the keyboard open, one value or the date may require an internal scroll. The action stays above the IME. | A second action and extra navigation are required. One active number per step gives longer labels and errors more room. Step Back restores the amount and leaves the liters draft intact. |
| Compose feasibility | Reuse the existing draft and write operations, with a separate form scaffold and modal vehicle picker. | Uses the same draft and validation contracts, with additional presentation state for step progression, vehicle list and top-level navigation. Verify Back, scope switches and recreation. |
| Measurement | Compare time from Home to valid save using amount and liters with both default and 150% text. | Use the same inputs and measure the full flow, including the record type chooser and Continue. Earlier estimates are route counts only. |

For general expense, keep the supported categories and their existing required/optional amount policies. A enters that draft directly. B selects it in the type chooser and opens a category-first form. Expense, detailed History, detailed Reminders and the rest of the app remain at scope and navigation level for this checkpoint.
