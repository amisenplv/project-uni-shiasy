# Design preview verification

**2026-11-16 · Browser prototype**

Chrome rendered 47 base views, 15 dark references and 10 at 150% browser text. The overview is assembled from 12 base renders. A visual inspection covered each major flow, including the original Concept C views, large-type Fuel, the updated vehicle form, and the gated review and protected code dialogs.

- 60 layout assertions at 320, 360, 390 and 430 CSS px covered RTL, bounds, scroll-body horizontal overflow, app control targets and Save placement. The 320 and 360 checks used 150% browser text. The 390 checks used dark appearance.
- Interaction checks covered modal dismissal/focus, three Home records, Fuel validation and sample save, Expense from History, scoped record editing and linked-mileage removal, delete cancellation, month switching and the synthetic Aban total of ۴٬۲۷۸٬۰۰۰ تومان, theme choice, first-launch restore and populated-ledger rejection, disclosure of vehicle calendar only with a year, and gated restore/protected-code states. Browser fixtures used no HTTP requests and emitted no JavaScript page errors.
- Calculated contrast for eleven representative foreground/background pairs in each theme. Text pairs met 4.5:1 and control edges/focus met 3:1. The minimum sampled normal text pair was 5.92:1 and the light control outline was 3.87:1 on white. These calculations are for the design tokens.
- Native IME, TalkBack, native Android rendering differences, Android DocumentsUI transactions, app timing, Android builds and device acceptance remain outside this preview evidence. The handoff specifies the corresponding future implementation tests.
