#!/usr/bin/env bash
set -euo pipefail
umask 077
cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.."

: "${ANDROID_HOME:?Set ANDROID_HOME to the existing SDK}"
: "${M53_DEVICE:?Set M53_DEVICE to a fresh disposable emulator serial}"
: "${M53_ADB_PORT:?Set M53_ADB_PORT to the isolated ADB server port}"
: "${M53_OUTPUT:?Set M53_OUTPUT to an existing private result directory}"
: "${VEHICLE_PG_SOCKET:?Set VEHICLE_PG_SOCKET to the running synthetic dev fixture}"
export VEHICLE_PG_DATABASE=vehicle_m51_dev
[[ $M53_DEVICE =~ ^emulator-[0-9]+$ ]]
adb=( "$ANDROID_HOME/platform-tools/adb" -P "$M53_ADB_PORT" -s "$M53_DEVICE" )
package=org.amirariana.vehiclelog.debug
runner="$package.test/androidx.test.runner.AndroidJUnitRunner"
cloud=org.unishiasy.vehiclelog.data.cloud.CloudDeviceTest
[[ $("${adb[@]}" shell getprop ro.kernel.qemu | tr -d '\r') == 1 ]]
[[ $("${adb[@]}" shell getprop sys.boot_completed | tr -d '\r') == 1 ]]

issue_invite() { node server/src/admin.ts invite; }
instrument() {
    local label=$1 selector=$2
    shift 2
    "${adb[@]}" shell am instrument -w -r -e localCloudFixture true -e class "$selector" "$@" "$runner" > "$M53_OUTPUT/$label.log"
    grep -E 'OK \(|FAILURES|INSTRUMENTATION_FAILED|Process crashed|shortMsg=' "$M53_OUTPUT/$label.log"
    grep -q 'OK (' "$M53_OUTPUT/$label.log"
}
put_count() { grep -c '^request PUT /v1/backup$' "$M53_OUTPUT/proxy-events" || true; }
"${adb[@]}" install -r app/build/outputs/apk/debug/app-debug.apk
"${adb[@]}" install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
"${adb[@]}" shell pm grant "$package" android.permission.POST_NOTIFICATIONS
"${adb[@]}" reverse tcp:8765 tcp:8765
cleanup() {
    printf '%s\n' normal > "$M53_OUTPUT/proxy-mode"
    "${adb[@]}" reverse --remove tcp:8765 2>/dev/null || true
}
trap cleanup EXIT

: > "$M53_OUTPUT/proxy-events"
printf '%s\n' normal > "$M53_OUTPUT/proxy-mode"
instrument cloud-ui org.unishiasy.vehiclelog.data.cloud.CloudSettingsDeviceTest -e invite "$(issue_invite)"
[[ $(put_count) == 0 ]]
instrument cloud-room "$cloud#localRoundTripUsesRoomAndKeystoreAndRevokedSessionKeepsLedger" -e invite "$(issue_invite)"
[[ $(put_count) == 1 ]]

rm -f -- "$M53_OUTPUT/put-committed"
printf '%s\n' hold-put > "$M53_OUTPUT/proxy-mode"
"${adb[@]}" shell am instrument -w -r -e localCloudFixture true -e class "$cloud#stageUploadUntilHarnessKillsProcess"     -e invite "$(issue_invite)" "$runner" > "$M53_OUTPUT/cloud-force-death.log" &
flight=$!
for ((i=0; i<150; i++)); do
    if [[ -f "$M53_OUTPUT/put-committed" ]]; then break; fi
    if ! kill -0 "$flight" 2>/dev/null; then break; fi
    sleep .15
done
test -f "$M53_OUTPUT/put-committed"
test -n "$("${adb[@]}" shell pidof "$package")"
"${adb[@]}" shell am force-stop "$package"
if "${adb[@]}" shell pidof "$package" >/dev/null; then exit 1; fi
printf '%s\n' normal > "$M53_OUTPUT/proxy-mode"
wait "$flight" || true
printf '%s\n' 'Committed upload observed. App force-stopped before acknowledgement.'
[[ $(put_count) == 2 ]]
instrument cloud-restart "$cloud#readPendingAfterActualProcessDeathWithoutSending"
[[ $(put_count) == 2 ]]
instrument cloud-retry "$cloud#explicitRetryAfterProcessDeathConfirmsOriginalDespiteLaterEdits"
[[ $(put_count) == 3 ]]
instrument cloud-ack-restart "$cloud#confirmedMetadataSurvivesAnotherProcessRestart"
[[ $(put_count) == 3 ]]

printf '%s\n' drop-before-put > "$M53_OUTPUT/proxy-mode"
instrument cloud-drop-before "$cloud#disconnectBeforeServerReceivesUploadIsUnconfirmed" -e invite "$(issue_invite)"
printf '%s\n' drop-after-put > "$M53_OUTPUT/proxy-mode"
instrument cloud-drop-after "$cloud#disconnectAfterServerCommitsUploadIsUnconfirmed" -e invite "$(issue_invite)"
count=$(put_count)

"${adb[@]}" reverse --remove tcp:8765
"${adb[@]}" shell cmd connectivity airplane-mode enable
"${adb[@]}" shell svc wifi disable
"${adb[@]}" shell svc data disable
[[ $("${adb[@]}" shell settings get global airplane_mode_on | tr -d '\r') == 1 ]]
reminder_fixture="m43-restart-m53${RANDOM}"
offline_suite=org.unishiasy.vehiclelog.data.backup.BackupRestoreTest
offline_suite+=,org.unishiasy.vehiclelog.data.backup.BackupDocumentPickerTest
offline_suite+=,org.unishiasy.vehiclelog.reminders.ReminderSchedulingTest
offline_suite+=,org.unishiasy.vehiclelog.reminders.ReminderWorkerTest
instrument offline-regressions "$offline_suite" \
    -e notificationPermissionExpectation granted -e notificationRestartPhase write -e notificationRestartFixture "$reminder_fixture"
"${adb[@]}" shell am force-stop "$package"
instrument offline-reminder-restart "org.unishiasy.vehiclelog.reminders.ReminderWorkerTest#deliveryMarkerSurvivesProcessRestart" \
    -e notificationRestartPhase read -e notificationRestartFixture "$reminder_fixture"
[[ $(put_count) == "$count" ]]
