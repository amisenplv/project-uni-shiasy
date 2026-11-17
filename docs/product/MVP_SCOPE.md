# MVP scope

## M4 local prototype

- Signed Android APK, Persian RTL, one active vehicle in the UI, and manual vehicle setup. Searchable sourced-catalog selection remains part of the original target, but activation is deferred until a redistributable dataset is approved, its provenance is recorded, and the activated build is verified on a physical device. The disabled-by-default integration and synthetic test fixture do not provide production catalog coverage.
- Quick fuel, oil, repair, insurance, wash, parts, and miscellaneous logging. Include correction and deletion, self-reported maintenance history, current recorded mileage, and monthly Jalali spending.
- User-configured date and mileage reminders. Date notifications are opt-in and best effort. Due reminders remain visible inside the app.
- Manual file backup and validated restore into an empty ledger. The driver must export and retain the file. Manual export is not automatic backup and covers records present when the file is created.

M4 development is complete. Signed migration and emulator acceptance passed. The product owner deferred the isolated physical-device release checks and approved proceeding to M5 development. Physical release acceptance remains open and public release readiness is unestablished. [Deferred physical release gate](../delivery/BACKLOG.md#deferred-m4-physical-release-acceptance)

## M6 market-ready closed pilot

Add optional invite-only cloud recovery, a tested distribution and signed update path, local and cloud privacy controls, support, and voluntary aggregate pilot measurement. Cloud recovery restores the last successfully uploaded snapshot. It does not provide real-time multi-device synchronization or merge. Logging remains available offline.

## Production target · Cafe Bazaar

On 2026-11-16 the product owner established Cafe Bazaar as the long-term public release target for Iranian drivers. The completed offline MVP and focused signed RC5 Redmi upgrade are the foundation. [Product Visual Design & UX Polish](../delivery/ROADMAP.md#product-visual-design--ux-polish) follows that foundation and requires approval before broad implementation. The production product retains offline vehicle and expense management, reporting, reminders, manual recovery and optional explicit cloud recovery. Public release requires approved, operationally verified cloud recovery and privacy controls, physical acceptance, signed update continuity, Persian support material and verified applicable store requirements. This direction leaves milestone acceptance, cloud and server pauses, and existing deferrals in force. [Production checklist](../delivery/BAZAAR_RELEASE_READINESS.md)

## Deferred

The original research plan scheduled recruitment, interviews, and synthesis alongside M2–M4. The product owner deferred recruitment at M2 closure and explicitly deferred business customer discovery interviews at M3 closeout review on 2026-11-10. Recruitment and interviews remain incomplete. The [future product validation backlog](../delivery/BACKLOG.md#future-product-validation) retains the original requirements and the dependent synthesis. Public directory, maps, and partner dashboards require a separate decision after the driver readout. Defer multi-vehicle UI, shared access, fuel-economy analytics, SMS, gamification, payment and billing, marketplace transactions, AI diagnosis, predictive maintenance, trip tracking, weather alerts, resale certification, and fleet management. Revisit features when evidence establishes a recurring job, data and operational requirements, and a release owner.

See [delivery roadmap](../delivery/ROADMAP.md) and [pilot decision rules](SUCCESS_METRICS.md).

## Scope decisions and residual risk

On 2026-11-10, the product owner approved closing M2 with manual vehicle setup and offline persistence as the accepted vehicle-setup scope. The unperformed real system 150% font test and TalkBack review are **Waived — Accepted Residual Risk**, not passes. The product owner deferred business recruitment. Revisit font and accessibility checks before materially changing the form, enabling a production catalog, or broadening distribution.

The catalog and business validation goals remain deferred. M2 closure does not claim catalog sourcing, business recruitment, or the waived visual/accessibility checks are complete.

On 2026-11-11, M3 closed with accepted residual risks. The product owner deferred the manual airplane-mode walkthrough, human Quick Add timing, and verification of the median-under-15-seconds and at-least-80% first-attempt completion targets. All four remain **DEFERRED / NOT VERIFIED**. [M3 closure evidence and residual risks](../../PLANS.md#m3-acceptance-and-closure-disposition) · [Future product validation backlog](../delivery/BACKLOG.md#future-product-validation)
