# Vehicle Ledger · Concept C

**Design for owner review · 2026-11-16**

Context checked against all four A/B previews, the [screenshot-driven audit](../vehicle-ledger-ux-correction-v1/AUDIT.md), and the supplied Antigravity captures. B's compact scope and ruled transaction list support review. The implemented keyboard-open capture 06 and large-type capture 12 show why Save needs its own IME-aware region. The source routes both current Home actions to a fuel-default draft. C draws distinct entry intents while preserving the existing category and storage contracts.

## Navigation and entry intents

```text
Home → ثبت جدید → bottom sheet → سوخت → paired amount + liters
                              → هزینه → non-fuel form, expense categories
                              → سرویس → same non-fuel form, service categories
```

- **Home:** Active vehicle and recorded odometer occupy one compact row. A checked vehicle sheet opens from the name. Use one scope across Home, «سوابق» and «یادآورها». Changing it refreshes all vehicle-scoped data together. One due reminder precedes the three latest records, with a count only when multiple reminders are due. A due row opens that reminder. Recent rows open their existing records and keep their source chronology. Monthly reporting remains reachable from the ledger/history context. «ثبت جدید» stays near the lower edge of Home above the three persistent destinations. Settings and recovery retain their existing entry points.
- **Entry selector:** Three 48dp-or-larger choices in a dismissible Material bottom sheet, with a close action. Each choice opens its form immediately. «سوخت» opens the fuel draft. «هزینه» opens the non-fuel form with «بیمه»، «کارواش»، «سایر». «سرویس» opens the same form with «تعویض روغن»، «تعمیر»، «قطعات». These are entry shortcuts to the existing seven categories. Preserve the current category key for all reads, reports, backups and writes. Category selection occurs in the form and no category is preselected on a fresh expense or service draft.
- **Back:** Back or Close dismisses a sheet and restores focus to the opener. Once an entry option is selected, dismiss the sheet and open the form. Android Back first dismisses the IME. Back from the form returns to its originating Home or History location and follows the existing draft dismissal behavior. One submit writes one record. After acknowledged success, return to the origin and provide confirmation. On validation or write failure, retain the draft. Pin the vehicle ID for the entire draft and guard simultaneous saves.
- **Empty scope:** Keep current setup, explicit recovery, retry and empty states. Hide the due notice when the scoped query confirms none. When zero to two records are available, show only actual records and readable empty text where appropriate. A pending scope never shows amounts or reminders from the prior vehicle.

## Forms

| Element | Fuel | Expense / Service |
|---|---|---|
| Core fields | Persistent labels «مبلغ کل پرداختی (تومان)» and «حجم سوخت (لیتر)», side by side when space permits. Toman is the full amount paid. Unit price remains outside the saved contract. | Category, amount in toman, editable date. Fuel volume is absent. Service uses the same field geometry, category choices and existing category-dependent amount rule. |
| Calendar and secondary fields | Use editable date row with visible Jalali default from Tehran. Expand «کارکرد و یادداشت» on demand, preserving draft text and showing mileage reference by its own field. | Same date and disclosure rules. |
| Validation | Amount is positive, up to one fractional toman digit and within the existing exact rial range. Volume is positive, up to three decimals, stored in whole milliliters. | Amount is required for insurance, wash and other. The existing oil change, repair and parts categories permit a blank amount. Show «اختیاری» for those categories only. |
| Shared validation | Keep current Jalali parsing/range and stored Gregorian fallback. Mileage is integral and nonnegative, with existing chronology checks. Focus and scroll to the first invalid field, expand its details if needed, and associate the Persian error. Preserve the existing explicit save-without-mileage option for conflicts. Announce storage failure near the action and keep the draft. |

## Keyboard, accessibility and visual language

- Use decimal IME for amount and volume, integral IME for mileage, text IME for editing a slash-delimited date. Fuel starts with amount as the first field; Next moves to liters, then Done hides the IME. Commit occurs through «ثبت سوخت». Keep the same field order when the pair stacks.
- Give the form a scrollable body and a separate sticky Save region that sits above the IME and the applicable navigation inset. No overlay on input bounds. Keep the first error in view; on very short heights, preserve body scrolling and reduce chrome before reducing touch sizes. Screen-reader focus remains predictable across validation and sheets.
- One cool light canvas `#F3F6F8`, white surface `#FFFFFF`, ink `#112D34`, petrol action `#075A63`, amber warning `#FFF0D3`. Use Vazirmatn, 16–20dp outer gutters, 8–12dp control radii, thin section rules and minimum 48 × 48dp touch targets. Amber encodes due urgency with a text label. Persian numerals and grouped toman, mixed vehicle names isolated, RTL logical start/end, and visible focus. Stack paired fields at 150% or when labels need more room. A sticky dock and sheet may grow vertically. Confirm TalkBack, native IME locale and edge-to-edge behavior on Android only after approval.
- Preserve Room v4, snapshot v1, exact integer legacy-rial money, existing Jalali/Tehran rules, linked reading and deletion behavior, reminder rules and offline recovery. No new financial fields, analytics, dependencies or service lifecycle.

## Preview and stop point

The local prototype uses fixed synthetic data for سمند LX, ۱۲۰٬۵۰۰ کیلومتر and ۲۵ آبان ۱۴۰۵. It is a browser interaction specimen with an illustrative Persian IME. It performs no Android writes, and the domain validation above remains authoritative. Each PNG uses a 390 × 844 phone viewport.

[Interactive prototype](prototype.html) · [Home](previews/c-home.png) · [Quick Entry Selector](previews/c-selector.png) · [Fuel with keyboard](previews/c-fuel-keyboard.png) · [General Expense](previews/c-expense.png)

Concept C is presented for explicit owner approval. No Android implementation follows from this package.

### Browser preview notes

Open the local prototype on Home and use «ثبت جدید», or add `?scene=selector`, `?scene=fuel-keyboard`, or `?scene=expense` to open the corresponding sample. Add `text=150` as another query parameter to inspect large type. Record, reminder, and Settings actions are visual navigation references. They stay outside the interactive entry prototype.

Chrome rendered the four PNGs and passed 20 viewport checks across 320–430px, including the 150% text variant, minimum tappable bounds, and the Save position above the sample keyboard. Interaction checks covered all three selector routes, all seven existing category mappings, sheet focus return, vehicle scope, field errors and sample submit. No native Android or driver testing ran.
