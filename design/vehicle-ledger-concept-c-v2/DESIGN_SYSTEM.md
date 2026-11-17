# Concept C design system

Use the existing bundled Vazirmatn font at `app/src/main/res/font/vazirmatn.ttf`. The prototype uses the matching [asset](assets/Vazirmatn.ttf) and [OFL notice](assets/OFL-Vazirmatn.txt). Source of presentation tokens is this table and [prototype.css](prototype.css). CSS viewport 390 × 844 represents graphic dp geometry at default scale; browser text pixels are reference geometry for scalable sp.

## Semantic colors

| Token | Light | Dark | Role |
|---|---|---|---|
| Canvas | `#F3F6F8` | `#101D22` | Screen and system chrome |
| Surface | `#FFFFFF` | `#192B31` | Bars, sheets, entry group, inputs, report |
| Ink | `#112D34` | `#EAF4F5` | Primary text and unselected navigation |
| Muted ink | `#4D6269` | `#AFC5C9` | Units, help, subtitle |
| Action / on-action | `#075A63` / `#FFFFFF` | `#A6DEDF` / `#10383B` | Primary button, report accent, selected outline |
| Wash | `#DBECEF` | `#254249` | Selected destination, gentle notice |
| Rule / control outline | `#D2E0E3` / `#71858B` | `#344C54` / `#809DA4` | Divider / actionable border |
| Focus | `#125D9A` | `#A8E0F0` | 2dp focus outline |
| Warning / on-warning | `#FFF0D3` / `#694414` | `#382A16` / `#F4D497` | Active due or operational warning |
| Error background / danger / on-danger | `#FAE9E7` / `#A12C30` / `#FFFFFF` | `#40262B` / `#FFB4AB` / `#4B1418` | Error copy and destructive action |
| Success / success ink | `#E4F1E9` / `#23533B` | `#1C352D` / `#A6E1B7` | Confirmed operation |
| Scrim | `#142E35A3` | `#060A0DBD` | CSS RGBA order; convert alpha to Android ARGB when transcribing |

Use existing Material state layers. Preserve label contrast and outline after alpha is applied. Maintain at least 4.5:1 for normal text and 3:1 for large text, visible focus, and control edges. Treat rules as decorative; all states also have text or semantic state. Light/dark/system comes from existing ThemeMode. Match system icon appearance to the effective app background, even when it differs from the OS setting. IME and Android document picker appearance follow the platform.

## Type and formatting

| Role | sp / target line height | Weight |
|---|---|---|
| App bar title | 22 / 30 | 800 |
| Section heading | 18 / 26 | 750 |
| Report and record amount | 30 / 38 | 800 |
| Emphasized numeric input | 18 / 26 | 750 |
| Body / action | 16 / 25 | 400 / 750 |
| Field label / supporting | 14 / 22 | 750 / 400 |
| Metadata / compact status | 13 / 20, 12 / 18 | 400 / 750 |

Use scaled sp and allow growth. Align by baseline where feasible. Use zero tracking, standard Persian characters and ZWNJ, and RTL logical start/end. Isolate mixed vehicle names and all numeric/date runs. Format Persian numbers with `٬` and `٫`; put the unit after the amount and associate it semantically. Wrap exceptionally long exact amounts instead of truncating. Keep LTR editing for mixed credential identifiers/icons unaffected, dates and numeric input where slash and decimal order matters. Preserve entered notes and text direction. Mirror Back and drill-in arrows only where directional.

## Geometry and components

| Component | Implementation specification |
|---|---|
| Screen scaffold | 20dp logical sides at 390dp, 16dp on widths ≤350dp. Use an approximately 4dp spacing grid for 4/8/12/16/20/24dp. Use a centered content column capped near 560dp on wide Android. Android safe drawing insets own system chrome. |
| App bar | 60dp minimum content height, 48dp min trailing/Back target. Keep title and optional vehicle subtitle in one logical column. Toolbar stays visible during ordinary scrolling. |
| Persistent navigation | Three equal width 48dp min destinations. One selected wash and selected semantics. Icon 17–20dp with visible Persian label. Above it, Home and History have 48dp «ثبت جدید», Reminders has 48dp «ثبت یادآور». Root-only docking. |
| Primary and secondary actions | Primary filled, secondary outlined, text action plain. 48dp min touch bounds, 10dp radius, 8–10dp vertical and 14–16dp horizontal padding. Row/button height grows with wrapped text. Enabled → pressed/focused using M3 state layer. Disable repeated commit while busy and label the in-progress operation. |
| Destructive action | Red text link in details/form, red filled confirmation inside sheet. A contextual sentence identifies the affected record. Initial focus on Cancel/close. Never delete from a tap on a transaction list row. |
| Transaction row | 60dp min height. 52dp day column, 10dp gap, thin dividers, category/name and optional contextual data, exact total aligned to logical end. Group same day under one date heading, with occurrence/creation/id order retained. At enlarged type place amount under content. Entire record is one accessible target. |
| Vehicle selector | Compact label + model + downward affordance, whole label is a 48dp trigger. Sheet rows use check and selection semantics. Mileage value sits in adjacent group and can wrap below. |
| Entry/confirmation sheet | Full-width Android modal, 16dp top radii, 20dp logical sides, 48dp close. Type choices 64dp minimum with a 38dp icon tile and one short explanation. The body scrolls before action targets shrink. Back/outside dismissal acts like Close unless an intentional protected cloud modal. |
| Fields and disclosure | Persistent 14sp labels above outlined 48dp fields, min 52dp for paired fuel inputs. 8dp radius, 1dp control outline, 2dp focus. Two columns ratio about 1.25:1 at 390dp if text fits. Stack at 150% or compact widths. Date and optional detail each have 48dp disclosure row with expanded semantics. Error attaches below its input. |
| Sticky form footer | Separate surface with 1dp divider and 8dp inset. Adjust for IME once. Body scrolls independently. When constrained, reduce decorative padding before reducing visible field/action clearance. Ensure focused input is visible and no overlay intersects it. |
| Report | One raised total summary with 4dp top accent and dashed division for counts. Category amount is exact; 4dp bar decorative, starts at RTL start, absent for unpriced-only rows. |
| Notice and empty states | 3dp logical-start edge, 6dp radius, 12dp padding. Warning/error/success mapped semantically. Empty state is title, brief explanation and relevant next action. Loading retains structure with neutral placeholders and one accessible busy announcement. |
| Icons and motion | Use existing Android resource glyphs where available, about 20dp with consistent stroke, and no new dependency. Do not separately repeat spoken labels for decoration. Use normal platform sheet and navigation motion, honor reduced animations. No animated count, chart, or decorative transition required. |

At 150% verify 320–390dp-equivalent widths, long local or mixed names, transaction amount wrapping, modal focus and concise warning layout. Keep 48 × 48dp minimum for every control, including close, selector, sheet and back. Use TalkBack headings, meaningful names, selection/error states and a live region for the outcome once, with predictable focus after navigation. Test native IME Next, Done, Back and insets on a small device. Keep credentials and working paths out of feedback and logs.
