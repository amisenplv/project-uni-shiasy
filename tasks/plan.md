# Driver interface review

This review covers the active-vehicle header on Home, optional mileage entry in the ledger editor, and estimates in the trip calculator. The current Compose working tree supplies the behavioral evidence. The [redesign specification](../docs/superpowers/specs/2026-11-23-vehicle-ledger-ui-redesign-design.md) supplies the intended Persian hierarchy, RTL layout and 48 dp touch targets. A retained [150% Reminders capture](../build/vehicle-ledger-wheel-picker/reminder-fab-dark-150.png) shows an earlier three-destination shell. It illustrates how much space Persian controls need at large text sizes. Fresh captures are needed to judge the current layout.

| Priority | Finding | Driver impact |
| --- | --- | --- |
| 1 | The [ledger editor](../app/src/main/java/org/unishiasy/vehiclelog/ui/ledger/LedgerScreen.kt) implements the mileage and notes header as a clickable row with a text triangle. It exposes no explicit disclosure state. | A TalkBack user must explore past the header to discover whether fields are open. Confirm the spoken output on a phone. |
| 2 | The [Home header](../app/src/main/java/org/unishiasy/vehiclelog/ui/home/HomeScreen.kt) ellipsizes the vehicle and its summary on separate single lines. The vehicle name precedes an unreserved switch indicator in the same row. | Similar long vehicle names may lose their distinguishing suffix and leave too little space for the indicator. Reproduce at narrow width before scheduling a layout change. |
| 3 | The [trip calculator](../app/src/main/java/org/unishiasy/vehiclelog/ui/tools/TripCalculatorScreen.kt) renders numeric results on first open. Missing or invalid inputs resolve to zero, including an absent price when fuel is already known. Presets and prefilled consumption can also display Latin digits. | A driver can read an incomplete estimate as a free trip or encounter different digit styles between fields and results. |

Reuse the existing Compose screens, theme, resources and offline calculations. Prioritize logging and vehicle identification before the optional calculator. The tasks can be estimated independently. Coordinate edits to existing uncommitted tests and the shared string resources when implementing.

The [task checklist](todo.md) contains the order, file scope and acceptance checks. The [execution record](../PLANS.md) and [device test strategy](../docs/delivery/TEST_STRATEGY.md) govern subsequent implementation evidence. This review used source and retained imagery. Current-build rendering and TalkBack behavior still need device verification.

## Latest local changes

Against local HEAD `b72f0fb`, the reminder FAB and the new-reminder form submit button both say «ثبت یادآور». The FAB focuses the form and the submit button saves it. Prioritize distinguishing these actions before tasks 1–4. See [RemindersScreen.kt](../app/src/main/java/org/unishiasy/vehiclelog/ui/reminders/RemindersScreen.kt) and [strings.xml](../app/src/main/res/values/strings.xml).

The date picker now uses three snapping Jalali wheels. The updated tests already exercise leap Esfand, confirmation and cancellation. Physical TalkBack traversal across the wheels remains a verification task. The pending numeric formatting change also introduces `PersianNumericTransformation`. Evaluate it for calculator fields in task 4 and verify decimal editing and cursor mapping.
