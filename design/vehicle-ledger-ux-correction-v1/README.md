# Vehicle Ledger independent UX correction v1

**Decision package · 2026-11-16**

- [Screenshot inventory and independent audit](AUDIT.md)
- [UX navigation proposal and decision matrix](CONCEPTS.md)
- [Interactive Home comparison](prototype.html?scene=home) · [Interactive Fuel comparison](prototype.html?scene=fuel)
- [Home comparison PNG](previews/compare-home.png) · [Fuel comparison PNG](previews/compare-fuel.png)

| Proposal | Home | Fuel | Supporting states |
|---|---|---|---|
| A · Quick log | [PNG](previews/a-home.png) | [PNG](previews/a-fuel.png) | [Vehicle picker](previews/a-switch.png) · [Validation and keyboard at 150%](previews/a-keyboard-text150.png) |
| B · Vehicle journal | [PNG](previews/b-home.png) | [PNG · volume step](previews/b-fuel.png) | [Amount step](previews/b-fuel-first.png) · [Vehicle list](previews/b-switch.png) · [Validation and keyboard at 150%](previews/b-keyboard-text150.png) |

Open the HTML locally. At desktop widths, both interactive concepts appear side by side. The individual PNGs use a matched 390 × 844 viewport and the same synthetic vehicle, date, paid amount and liters. B's comparison frame shows the second step with the first value retained. Supporting keyboard shots represent an illustrative numeric IME; the prototype never writes to Android data. Buttons for out-of-scope destinations acknowledge the preview boundary. The release app, dark theme and remaining screens receive no new visual specifications in this checkpoint.

## Prototype verification

Headless Chrome rendered eleven phone images including the four primary screens, with two additional boards assembled from the corresponding PNGs. Thirty-two layout checks covered both concepts at 320, 360, 390 and 430 CSS px, including 150% browser text and a short 640px viewport. Checks covered RTL, horizontal overflow, visible control heights, dialog bounds, and Save position above the illustrative keyboard. Browser interactions checked vehicle selection and scope, dialog Escape and focus return, field errors and focus, and B's value retention through Back and Continue. The prototype generated no HTTP requests or script errors. Sample contrast ratios ranged from 5.92:1 to 13.36:1 for the measured text pairs and 3.87:1 for the field outline on white. Native Android keyboard, TalkBack, real device sizing, and driver task times are outside this evidence.
