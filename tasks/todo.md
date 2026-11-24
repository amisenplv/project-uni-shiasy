# Driver interface tasks

Paths below identify existing files by name. Production screen links appear in the [review](plan.md). Resource work uses app/src/main/res/values/strings.xml. Estimates are focused engineering time.

### First priority · Distinguish starting a reminder from saving it · half a day

RemindersScreen.kt, strings.xml and ReminderEditAndRecurringRobolectricTest.kt. Independent.

- [ ] Label the floating action «یادآور جدید» and keep «ثبت یادآور» for submitting the form. Verify the floating action focuses the title without creating a record and submission creates exactly one reminder.
- [ ] From the form, use TalkBack on a physical phone to choose a Jalali day, month and year. Confirm the announced selection matches the saved date and Back preserves the draft. Record phone, OS and build alongside the existing emulator evidence.

### 1. Make optional mileage discoverable with TalkBack · half a day

LedgerScreen.kt, strings.xml and LedgerEntryUiTest.kt. Independent.

- [ ] Expose the full 48 dp disclosure row as one action with a Persian name and expanded or collapsed state. Make the triangle decorative.
- [ ] From a fuel entry, expand with TalkBack, enter mileage, collapse and reopen. Verify the value persists and hidden fields leave accessibility traversal.
- [ ] Exercise the existing ledger editor instrumentation suite and record spoken feedback on a physical phone.

### 2. Keep the selected vehicle identifiable · half to one day

HomeScreen.kt and HomeScreenTest.kt. Independent.

- [ ] At 320 dp and 150% font, capture Home with two long Persian vehicle names that differ near the end and a six-digit odometer. Inspect the switch indicator, unit and edit action. Close with evidence if all are usable.
- [ ] If reproduced, reserve space for the switch indicator and let the identifying text or summary wrap. Keep the switch and edit actions separately reachable at 48 dp.
- [ ] Switch to the second vehicle, verify the identifying text and odometer, then open its editor. Repeat at default text size and capture both layouts.

**Logging checkpoint**

- [ ] Run the Home and ledger instrumented classes on a test emulator. Record the APK and screenshots for the narrow layout. Verify the selected vehicle receives the mileage entry.

### 3. Distinguish an incomplete estimate from a calculated cost · half to one day

TripCalculatorScreen.kt, strings.xml and TripCalculatorScreenTest.kt. Independent.

- [ ] Opening or resetting shows a Persian prompt for the missing inputs. Valid distance and consumption show liters even while price is missing. An invalid price keeps cost unavailable and identifies the field to correct.
- [ ] Enter ۱۰۰ km, consumption ۸ liters per ۱۰۰ km, and ۳٬۰۰۰ toman per liter. Verify ۸ liters and ۲۴٬۰۰۰ toman. Clear price and verify the fuel result remains.
- [ ] Cover empty, partial, invalid and valid states with the focused calculator test below.

### 4. Display consistent Persian numerals during trip entry · half a day

TripCalculatorScreen.kt and TripCalculatorScreenTest.kt. Schedule after task 3 because both edit the same fields and tests.

- [ ] Display prefilled consumption, both price presets and typed Latin or Persian digits in Persian. Preserve decimal input, cursor position and deletion in the middle of a value.
- [ ] Enter ۷٫۵ as consumption, apply each preset, rotate and compare the visible fields and estimate. Confirm the keyboard leaves all three fields reachable at 150% text.
- [ ] Run the focused calculator suite and verify editing with a Persian keyboard on a phone.

**Verification checkpoint**

- [ ] Run `./gradlew :app:testDebugUnitTest --tests '*TripCalculatorScreenTest'`, then `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug`. Run `./gradlew :app:connectedDebugAndroidTest` on the designated test emulator.
- [ ] Record commands, outcomes, emulator details, physical phone and OS, APK identity and outstanding issues in PLANS.md. Capture the affected screens in light and dark mode. Record physical TalkBack and keyboard results separately.
