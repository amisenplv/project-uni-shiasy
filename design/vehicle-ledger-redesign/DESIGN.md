# Vehicle Ledger · Design system and screen behavior

**Proposed light and dark appearances · 2026-11-16.** The 390 × 844 browser viewport is the visual reference. CSS pixels map approximately to dp for geometry and to sp for type at default scale. Android insets and fonts take precedence over the decorative browser status area. [Prototype](prototype.html) · [PNG index](README.md#visual-references)

## Visual direction

The vehicle readout is the focal point. A white identity panel carries the vehicle name and model year. A navy inset isolates the latest recorded mileage and its unit. Cobalt identifies commitment actions, with amber reserved for due service and operational warnings. Exact figures in History and Report share a consistent alignment. History uses a day column and a short ruled rail to make the date structure visible. The report combines its selected period, total and counts into a single receipt-like summary. Vazirmatn serves both body and display roles.

```text
HOME (read from right to left)
[دفتر خودرو                                 تنظیمات]
[ نام و مدل خودرو                       ویرایش / انتخاب ]
[ آخرین کارکرد                           ۸۴٬۶۲۰ کیلومتر ]
[ ثبت سوخت                              ثبت هزینه ]
[ نوبت سرویس                         عنوان، موعد و وضعیت ]
[ ثبت‌های اخیر                         همه سوابق هزینه‌ها ]
[ دسته و تاریخ                                مبلغ ]
[ گزارش ماهانه / یادآورها                         ]
```

## Light tokens

| Semantic role | Hex | Usage |
|---|---|---|
| Paper / raised surface | `#F2F5F7` / `#FFFFFF` | App canvas / inputs and raised panels |
| Ink / muted | `#132136` / `#505E73` | Primary content and secondary metadata |
| Rule / outline | `#D5DEEA` / `#758298` | Decorative dividers and significant control boundaries |
| Odometer / divider / on-odometer metadata | `#172B54` / `#687B9B` / `#D5E0F4` | Home readout, surface rule and metadata |
| Action / pressed / focus | `#254FC9` / `#1A3FA9` / `#2149B7` | Primary action with white text and light-surface focus indicator |
| Quiet wash / blue ink | `#E7EDF9` / `#254695` | Date markers, icons, selected navigation, report bars |
| Warning surface / text | `#FFF1D7` / `#694510` | Due service and recovery warnings |
| Danger surface / text and fill | `#F9E9E5` / `#9B2C2A` | Blocking and inline errors and destructive confirmation |
| Success surface / text | `#E9F3EC` / `#23533B` | Confirmed action notices |

Measured contrast ratios include ink/paper 14.77:1, muted/paper 6.01:1, outline/paper 3.55:1, outline/input 3.89:1, white/odometer 13.91:1, odometer metadata 10.46:1, white/action 6.91:1, warning 7.65:1, error 6.39:1, white/destructive 7.53:1, focus/paper 7.10:1, quiet ink/wash 7.48:1, and success 7.80:1. Icon, text and semantics identify selected, error and due states. Recheck actual Compose colors when applying state layers. [Android accessibility guidance](https://developer.android.com/guide/topics/ui/accessibility/apps)

## Dark tokens

The navy canvas, raised blue-gray surfaces, and brighter odometer inset preserve the depth and reading order of the light proposal. Primary actions use a lighter cobalt with dark text to stay legible after dusk. Warning, error and success have distinct foreground/surface pairs. [Dark Home](previews/dark/home.png) · [Dark overview](previews/dark/overview.png)

| Semantic role | Dark values |
|---|---|
| Canvas / raised surface / review mat | `#101727` / `#19243A` / `#0C1220` |
| Ink / muted | `#EFF4FF` / `#B6C3DC` |
| Rule / outline | `#34435F` / `#7E91B2` |
| Odometer / divider / text / muted | `#1D3461` / `#637DA8` / `#F6F8FF` / `#C5D5FC` |
| Primary / pressed / text / focus | `#9AB4FF` / `#B4C7FF` / `#132650` / `#B7CBFF` |
| Quiet wash / blue ink / selection underline | `#243556` / `#B1C6FF` / `#9AB4FF` |
| Warning fill / text | `#392B17` / `#F3CE8D` |
| Error fill / text and destructive fill / destructive text | `#3F252D` / `#FFB4AB` / `#4B1418` |
| Success fill / text | `#1C352D` / `#A6E1B7` |
| Modal scrim | `#050914B8` |

Dark contrast includes ink on canvas 16.23:1, muted on raised surface 8.73:1, control outline on raised surface 4.86:1, odometer text 11.53:1, primary action text 7.28:1, focus on raised surface 9.60:1, warning 9.15:1, error 8.17:1, destructive button text 8.76:1 and success 8.82:1.

### Appearance control

Under «تنظیمات» → «ظاهر», the «حالت نمایش» picker offers «تنظیم گوشی», «روشن» and «تیره». The default follows the device appearance. Explicit choices apply immediately and persist across prototype reloads when browser storage is available. Theme changes update colors in place while preserving the current screen, focus and drafts. Desktop review controls mirror the same preference. Confirm system-bar icons, native selects, dark IME and Files appearance on Android. Adopt durable local theme preference only if the owner approves that behavior for the app.

## Typography and locale

| Role | Size / line height at 100% | Weight | Usage |
|---|---|---:|---|
| Display | 36 / 42 sp | 825 or nearest supported | Mileage and monthly total |
| Screen title | 24 / 32 sp | 800 | Toolbar title, empty-state hierarchy uses Section below |
| Section / leading value input | 20 / 29 sp | 750 or 700 input | Section headings and emphasized amount |
| Body / action | 16 / 26 sp, 16 / 23 action | 400 / 750 | Body, action and prominent row values |
| Label / metadata | 14 / 22 sp | 750 / 400 | Field labels, secondary units and summary details |
| Compact | 12 / 17 sp | 750 | Closed reminder states, secondary compact labels |

Use the bundled full variable Vazirmatn reference file v33.003 for design comparison. It carries OFL 1.1. For Android, after asset approval, pin the binary and notice, check Android font configuration on minSdk and actual Persian shaping at 100% and 150%, and use static weight assets if variable behavior is inadequate on a supported device. Use an Android Persian-capable system fallback until asset approval, and re-baseline any metric differences with the owner. Zero tracking and text transforms in Persian. Apply scalable sp, no fixed-height text boxes, no hard baseline crop. [Vazirmatn upstream](https://github.com/rastikerdar/vazirmatn) · [Bundled license](assets/OFL-Vazirmatn.txt)

- UI voice is concise formal Persian. «ثبت سوخت» creates, «ذخیره تغییرات» updates, «انصراف» leaves a dialog or draft, «لغو یادآور» cancels an active reminder. Keep Persian ی and ک, natural ZWNJ, and Persian punctuation. Use consistent «سوابق هزینه‌ها»، «گزارش ماهانه»، «کارکرد»، «پشتیبان‌گیری دستی».
- Use Persian digits, thousands separator `٬`, decimal `٫`, and amount before unit. Visually and semantically isolate each amount/date and keep its unit associated. Align record amounts at logical end. Use tabular figures where available. Preserve exact money string and all significant decimal digits even when wrapping on a narrow device. Do not squeeze type to fit.
- Display in toman and use existing integer legacy-rial parsing and formatting. Allow one fractional digit in toman when the stored value requires it. Fuel liters use up to three decimal places from whole milliliters. Odometer is whole kilometers. Use source validation and keep the live edit cursor stable. Isolate mixed-script vehicle names with content direction and preserve original stored names and notes. Slash dates follow year/month/day inside isolated numeric runs. Gregorian fallback is labeled and retained.
- Use Tehran civil dates and existing Jalali helpers. New expense date is captured at draft creation. Month choices come from existing state. Date field uses the text IME so slash remains reachable. If a Jalali calendar is ever approved separately, validate Saturday-first weeks and Friday styling with RTL arrows.

## Layout and reusable components

Spacing scale in dp is 4, 8, 12, 16, 20, 24, 32, 40. At 390dp, 20dp logical margins yield a 350dp working area. Use 16dp at <=340dp. Toolbar min height 64dp with 48dp Back and trailing action. Section rhythm 24dp, row padding 8dp vertically, dividers 1dp. One content column, centered and capped around 560dp on larger widths in the Android design. Preserve logical RTL order. The desktop prototype is a 390px handset beside design review controls.

| Component | Geometry and behavior | Ownership / reuse |
|---|---|---|
| Screen scaffold, toolbar | Safe area, min 64dp header, RTL Back, optional subject, scrollable body. Semantics expose one page heading. | Shared UI shell. Route owner supplies title and callbacks. |
| Primary, secondary, quiet, danger | 48dp min height and width, 8dp radius. 16sp action label. Outline 1dp on secondary. Destructive filled only at final confirmation. Disabled content remains legible. | Shared wrappers over existing Compose Material primitives and behavior. |
| Icon button / row icon | 20dp glyph, about 1.7dp weight. 48dp icon-button target. Noninteractive icon backing 40dp square, 8dp corners. Mirror only directional glyphs. Decorative icons hidden from accessibility. | Shared icon system. Use one matching set from available Compose vectors; prototype vectors are visual references. |
| Vehicle readout / report summary | 12dp shell radius, 12dp vehicle padding, 8dp navy readout radius, no shadow. Vehicle selector is a 48dp trigger. Report uses a 4dp cobalt edge and a dashed subtotal rule. | Shared surface tokens. Home and Report own their data. |
| Data row | 60dp min height (64dp expense History). Weighted labels, end amounts and thin dividers. History uses a 40dp day column with a localized heading and a single activation target per record. Home shows up to two read-only recent rows. Year and Latin names use bidi isolation. | Shared row alignment. History, picker and Settings add their own semantics. |
| Notebook switch | Two 48dp minimum cells, selected 3dp underline and short translated labels. Fixed below bar, selection exposed; content state remains owned by each sibling. Allow cells to grow with text. | Notebook host. No duplicate navigation state. |
| Field, select, disclosure | 48dp min control height, 8dp radius, visible 14sp label, persistent unit. Focus stroke 2dp. Error text below and associated. Optional details reveal actual draft fields and last-mileage reference, with error auto-reveal. | State from product-specific ViewModel. Shared themed inputs. Reuse existing validation. |
| Sticky submit | 1dp divider, 8dp top and 4dp bottom padding plus relevant inset. One primary action stretches full width; place after scrollable body in focus order. | Shared form scaffold. Busy, text and result host remain feature-owned. |
| Sheet | 12dp top radius, 20dp inset, safe-area and scrolling if needed. Use semantic dialog, accessible title and predictable dismissal. Destructive initial focus on «انصراف» or closing action. | Shared modal shell. Product state and conditions remain in screen/ViewModel. |
| Notice and status | 3dp start edge and tinted surface, 12dp inset. Warning and error hold concise title, sentence and recovery. Status lives near the affected content. | Shared renderer. Outcome text maps from typed product result, no raw exceptions. |
| Report category row | Label and exact amount share first line when space allows; proportional bar 4dp underneath. Bar progress begins at RTL start. No bar for unpriced-only category, whole month with zero money, or denominator missing. | Report owns ranked presentation and exact data. Use source category keys and outputs. |
| Loading placeholders | Reserve final readout/list geometry. Use steady quiet wash with one accessible loading announcement. | Shared visual. Screen owns loading/failure/empty treatment. |

### Interaction at 150% and with an IME

Use logical start/end, RTL focus order by semantic groups, 48 × 48dp controls, retained panel/readout and labels, and vertical stacking of paired actions and dense History columns at large text as needed. Compare a real platform 150% setting on 360dp and a second physical device. At 200%, let controls reflow and grow. Put oversize fluid numerals on their own line. Labels and metadata can wrap. Avoid ellipsizing financial and recovery content. Break a very long preserved token only at presentation and announce the exact whole value. Avoid standard menus with clipped labels.

With IME open, resize the viewport using Compose insets once, keep the footer above it, and let the body scroll independently. If insets leave too little room for a useful input area, allow the footer to scroll into reach instead of overlaying fields. Use decimal IME for amount/volume, integer IME for mileage/year, text IME with slash for dates. Establish Next/Done traversal across the currently visible fields and stop at optional groups unless opened. Field submission identifies the first invalid field, opens its disclosure, scrolls it into view and focuses it. Do not erase other fields. System Back first dismisses IME. Give sheets logical reading order, trapped focus and focus restoration. Repeat stage-specific loading and outcome once through a polite live region. Do not use transient toasts as the sole confirmation for blocked writes or recovery. Motion is a 150ms color transition, optional 180ms opacity on destination change, and system default sheet movement. Respect reduced motion, use immediate state changes when requested, and avoid progress or list animations that alter measured layouts.

## Screen specifications

Shared loading and failure pattern: render reserved structural skeleton where a read is pending. Render a durable state-specific error with retry on read failure. Hold accepted values and draft until an actual outcome. While saving, use the existing pending guards and make the primary label action-specific. When data are genuinely absent, show the relevant empty state and available next action.

| Screen / visual | Layout, primary and secondary actions, navigation | State and accessibility details |
|---|---|---|
| A. [First launch](previews/empty.png) | App toolbar and Settings, one short introduction, «افزودن خودرو» first, separate «بازیابی از فایل». | After confirmed empty load. On DB open/read failure show retry in place of empty content. Never imply empty after failed read. Focus heading after initial state settles; restore link remains reachable after 150% wrap. |
| B. [Add](previews/vehicle-add.png) / [Edit vehicle](previews/vehicle-edit.png) | Name first, optional year, calendar choice only when relevant; Save footer. Back returns to caller. | Show explicit calendar requirement for nonblank valid year. Preserve existing catalog links/labels on edit. No inferred calendar and no plate. If missing car, explain and return to picker/Home. Numeric LTR-in-isolation, Persian labels. Long names wrap. |
| C. [Home, one vehicle](previews/home.png) | Toolbar and Settings, white identity shell with navy mileage inset, paired entry shortcuts, due service with title/date/status, up to two recent records, and report/reminder links. | Mileage loading and missing reading are distinct. Use «هنوز کارکردی ثبت نشده است.» on actual absence. If a read failure is collapsed by existing VM, clarify state at the presentation boundary before claiming empty. Due strip shows the first due item and total if multiple, then opens reminder list; failed reminder load can offer retry without blocking logging. |
| D. [Home, multiple](previews/home-multi.png) / [picker sheet](previews/switch.png) | Same vehicle readout. Name becomes a named chooser. Sheet rows select, edit separately, and permit add. No duplicate active vehicle block. | Actual selected state and title announced, single change resets vehicle-scoped display and report month. Pending new mileage never shows former vehicle’s value. Short and long names wrap. Respect one active scope, see owner gate. |
| E. [Fuel](previews/fuel.png) | Scoped full-screen form. Pair amount and liters at default scale when the labels fit. Keep the date visible. Stack at 150% and compact widths. Optional mileage/note disclosure. Save at bottom. | Required amount/volume and exact validation. Today is editable. Keep volume/mileage precision and optional relationships. Open disclosure on linked data in Edit or on error. Back returns to the caller, including empty History. |
| F. [Other expense/service](previews/expense.png) | Category selector first, amount conditioned on category, date, optional disclosure and Save. | Existing six general categories. No implied choice. Show «اختیاری» only for oil/repair/parts. Blank allowed means no amount. Preserve date/note/mileage and input that still applies when the category changes. Allow vertical form growth. |
| G. [Validation and IME](previews/validation.png) | Error under field with red stroke, contextual localized copy, footer above IME. | Example invalid nonpositive amount. Handle missing required amount, invalid liter/date, invalid mileage, constraint/missing record, storage and conflict. On conflict show «کارکرد با ثبت‌های قبل یا بعد از این تاریخ سازگار نیست.» and offer correction or «ثبت بدون کارکرد». During edit explicitly state whether choosing omission removes a linked reading. Keep original draft until commit. The screenshot keypad is illustrative. |
| H. [History](previews/history.png) | Scoped segmented sibling view, month sections with a Jalali day rail in existing date/creation/ID order. Day is announced with month, and the row shows category, a helpful detail and amount. Tap opens Edit. | Date groups are display only. Preserve unknown or out-of-range fallback rows and visible local retry. Show unpriced state in place of value. Empty History offers both entry paths. Loading an edit must keep list and scroll position. |
| I. [Entry edit](previews/edit.png) / [Delete](previews/delete.png) | Reuse entry form with prefilled values and explicit linked reading control. «حذف این ثبت» opens a bottom confirmation with «انصراف» default focus and destructive «حذف ثبت». | Show original Gregorian date if needed. Linked-reading Keep/Set/Remove semantics unchanged, and warn before deleting linked mileage. Keep edit visible on failure. For readable legacy records that cannot open an edit, preserve the existing access to deletion via a read-only result view backed by the existing route and operation. No swipe or undo behavior is assumed. |
| J. [Monthly report](previews/report.png) | Notebook sibling. Month picker and exact total share a white summary with a cobalt top edge. Counts sit beneath a ruled break, followed by category amounts and restrained bars. | Month choices from available source StateFlow, no arbitrary date-range UI. Show empty month with plain explanation, all-unpriced month with count and «بدون مبلغ», preserve unknown labels and unassigned date exception group. Numeric total is the accessible source, decorative bars hidden. |
| K. [Reminders](previews/reminders.png) / [New](previews/reminder-add.png) | Due first, then upcoming, then past with explicit completed/canceled labels. Sticky add opens a form with title and date or mileage. Tap the whole active row to reach complete/cancel. Show remaining distance only for upcoming mileage reminders with a valid last reading, using existing scoped values and a nonnegative display difference. | Preserve data ordering within groups, current rules for thresholds and statuses, and existing schedule cleanup. No automatic service entry, recurring reminder generation, or restore of canceled status. Show meaningful missing-mileage hint, mutation feedback, and notification controls through Settings. |
| L. [Settings](previews/settings.png) / [Notifications](previews/notifications.png) | App-wide appearance chooser, manual backup and date-notification preferences. Return to caller. | Notification states off, on, and blocked by Android. Ask OS permission only through existing opt-in; show «تنظیمات اعلان گوشی» if blocked and refresh on resume. Hide experimental cloud in offline UI. Visible labels signal global scope. |
| M. [Backup menu](previews/backup.png) / [Export prep](previews/export.png) / [Export success](previews/export-success.png) | Explain snapshot scope and readable file, then «انتخاب محل ذخیره». Use existing Android create-document flow. | Export includes all vehicles at capture time. During write, «در حال ذخیره فایل پشتیبان…». Report bad provider, size/data limits, storage failure and cancellation appropriately. Keep export settings available after result. |
| N. [Restore prep](previews/restore.png) / [Blocked](previews/restore-blocked.png) / [Invalid](previews/restore-error.png) / [Success](previews/restore-success.png) | Explicit empty-ledger warning before Android open-document picker; restore reads/validates/imports through existing path. With success, return to Home or Back to origin. | Busy message «در حال بررسی فایل و بازیابی…». For valid restore with incomplete notification reset say «اطلاعات بازیابی شد. بررسی اعلان‌ها کامل نشد.»; do not retry import. Other failure and storage states keep existing rows and offer applicable retry, selecting another file, or Back. Do not prompt users to erase data. Test full copy with 150% scale and keyboard/Back semantics. |

### Additional outcome copy

| Context | Message and next action |
|---|---|
| Null result from Android picker | Stay in prep. If feedback helps, «انتخاب فایل لغو شد.» No severity treatment. |
| Unsupported v1 input | «قالب فایل پشتیبانی نمی‌شود.» → «انتخاب فایل دیگر». |
| Backup/restore exceeds source limits | «اندازهٔ فایل یا تعداد ثبت‌ها از حد مجاز بیشتر است.» → select another file for restore; acknowledge the export restriction. |
| Body read unavailable | «فایل باز نشد. فایل دیگری انتخاب کنید.» → file picker. |
| Output write unavailable | «فایل ذخیره نشد. محل دیگری انتخاب کنید.» → save picker. |
| Storage/DB failure | «دسترسی به اطلاعات دفتر خودرو انجام نشد.» → «تلاش دوباره» when safe. |
| No report date match | «در این ماه هزینه یا سرویسی ثبت نشده است.» → month picker available. |
| Legacy unassigned date | «ثبت‌های بدون ماه شمسی» and «این ثبت‌ها در جمع ماهانه محاسبه نشده‌اند.» Preserve the stored date in an isolated span. |
| Unavailable guarded future route | «بازیابی آنلاین در این نسخه در دسترس نیست.» → «بازیابی از فایل». Only for a guarded route, never a showcase row. |
