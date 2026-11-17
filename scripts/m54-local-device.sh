#!/usr/bin/env bash
set -euo pipefail
umask 077
cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.."

: "${ANDROID_HOME:?Use the existing local Android SDK}"
: "${VEHICLE_PG_SOCKET:?Use the running disposable synthetic PostgreSQL socket}"
: "${M54_BACKEND_PORT:?Use the existing loopback-only test backend}"
export VEHICLE_PG_DATABASE=vehicle_m51_dev
export ANDROID_ADB_SERVER_PORT=5095
fixture=$(mktemp -d "$PWD/build/m54-android.XXXXXX")
echo "Evidence: $fixture"
export ANDROID_AVD_HOME="$fixture/avd" ANDROID_EMULATOR_HOME="$fixture/emulator" ANDROID_USER_HOME="$fixture/emulator"
mkdir -p "$ANDROID_AVD_HOME" "$ANDROID_USER_HOME"
base_adb=( "$ANDROID_HOME/platform-tools/adb" -P "$ANDROID_ADB_SERVER_PORT" )
emulator="$ANDROID_HOME/emulator/emulator"
package=org.amirariana.vehiclelog.debug
runner="$package.test/androidx.test.runner.AndroidJUnitRunner"
class=org.unishiasy.vehiclelog.data.cloud.CloudRecoveryDeviceTest
settings=org.unishiasy.vehiclelog.data.cloud.CloudRecoverySettingsDeviceTest
account="m54_replacement_${fixture##*.}"
account=${account,,}
proxy_pid=
emu_pid=
adb_owned=
cleanup() {
    if [[ -n $emu_pid ]]; then kill "$emu_pid" 2>/dev/null || true; wait "$emu_pid" 2>/dev/null || true; fi
    if [[ -n $proxy_pid ]]; then kill "$proxy_pid" 2>/dev/null || true; wait "$proxy_pid" 2>/dev/null || true; fi
    if [[ -n $adb_owned ]]; then "${base_adb[@]}" kill-server || true; fi
}
trap cleanup EXIT

# Fail before touching an existing daemon, emulator or loopback service.
python3 - <<'PY'
import socket
for port in (5095, 5594, 5595, 5596, 5597, 8765):
    with socket.socket() as probe:
        probe.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        probe.bind(("127.0.0.1", port))
        probe.listen(1)
PY
"${base_adb[@]}" start-server
adb_owned=yes
printf '%s\n' normal > "$fixture/proxy-mode"
python3 scripts/m53-loopback-fixture.py "$fixture" "$M54_BACKEND_PORT" > "$fixture/proxy.log" 2>&1 &
proxy_pid=$!

boot_wait() {
    for ((i=0; i<150; i++)); do
        if [[ $("${adb[@]}" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r') == 1 ]]; then return; fi
        kill -0 "$emu_pid"
        sleep 2
    done
    echo 'Disposable emulator did not boot' >&2
    return 1
}
boot() {
    local name=$1 port=$2
    local avd="$ANDROID_AVD_HOME/$name.avd"
    mkdir "$avd"
    cat > "$ANDROID_AVD_HOME/$name.ini" <<CONFIG
avd.ini.encoding=UTF-8
path=$avd
target=android-35
CONFIG
    cat > "$avd/config.ini" <<CONFIG
AvdId=$name
PlayStore.enabled=false
abi.type=x86_64
disk.dataPartition.size=6G
hw.cpu.arch=x86_64
hw.cpu.ncore=2
hw.ramSize=2048
hw.lcd.width=945
hw.lcd.height=2100
hw.lcd.density=420
image.sysdir.1=system-images/android-35/default/x86_64/
tag.id=default
target=android-35
CONFIG
    adb=( "${base_adb[@]}" -s "emulator-$port" )
    "$emulator" -avd "$name" -port "$port" -no-window -no-audio -no-snapshot -no-boot-anim         -gpu swiftshader_indirect -accel on > "$fixture/$name-emulator.log" 2>&1 &
    emu_pid=$!
    boot_wait
    [[ $("${adb[@]}" shell getprop ro.kernel.qemu | tr -d '\r') == 1 ]]
    "${adb[@]}" shell getprop ro.build.fingerprint
    "${adb[@]}" shell wm size
    "${adb[@]}" shell wm density
    "${adb[@]}" install app/build/outputs/apk/debug/app-debug.apk
    "${adb[@]}" install app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
    "${adb[@]}" shell pm grant "$package" android.permission.POST_NOTIFICATIONS
    "${adb[@]}" reverse tcp:8765 tcp:8765
    "${adb[@]}" shell input keyevent 82
}
instrument() {
    local label=$1 selector=$2
    shift 2
    "${adb[@]}" shell am instrument -w -r -e localCloudFixture true -e account "$account" -e class "$selector" "$@" "$runner" > "$fixture/$label.log"
    grep -E 'OK \(|FAILURES|INSTRUMENTATION_FAILED|Process crashed|shortMsg=' "$fixture/$label.log"
    grep -q 'OK (' "$fixture/$label.log"
}
invite() { node server/src/admin.ts invite; }
count_transfers() { grep -Ec '^request (GET|PUT) /v1/backup$' "$fixture/proxy-events" || true; }

boot M54_Source 5594
instrument source "$class#uploadOnInstallationAThenAddLaterRecord" -e invite "$(invite)"
"${adb[@]}" emu kill
wait "$emu_pid" || true
emu_pid=

boot M54_Replacement 5596
printf '%s\n' hold-get > "$fixture/proxy-mode"
"${adb[@]}" shell am instrument -w -r -e localCloudFixture true -e account "$account" \
    -e class "$class#startDownloadOnEmptyBAndWaitForHarnessTermination" "$runner" > "$fixture/force-death.log" &
flight=$!
for ((i=0; i<150; i++)); do
    [[ ! -f $fixture/get-ready ]] || break
    kill -0 "$flight"
    sleep .15
done
test -f "$fixture/get-ready"
"${adb[@]}" shell am force-stop "$package"
if "${adb[@]}" shell pidof "$package" >/dev/null; then exit 1; fi
printf '%s\n' normal > "$fixture/proxy-mode"
wait "$flight" || true
count=$(count_transfers)
instrument settings "$settings"
[[ $(count_transfers) == "$((count + 1))" ]]
"${adb[@]}" shell am force-stop "$package"
count=$(count_transfers)
instrument restart "$class#afterRestartRestoredLedgerIsExactAndNoRetryRuns"
[[ $(count_transfers) == "$count" ]]

# This second interruption uses a separate on-disk Room fixture in B.
"${adb[@]}" shell am instrument -w -r -e localCloudFixture true -e account "$account" \
    -e class "$class#holdAfterCommitBeforeRecordingCloudReceipt" "$runner" > "$fixture/force-death-after-commit.log" &
flight=$!
for ((i=0; i<150; i++)); do
    if "${adb[@]}" shell run-as "$package" test -f files/m54-commit-ready; then break; fi
    kill -0 "$flight"
    sleep .15
done
"${adb[@]}" shell run-as "$package" test -f files/m54-commit-ready
"${adb[@]}" shell am force-stop "$package"
if "${adb[@]}" shell pidof "$package" >/dev/null; then exit 1; fi
wait "$flight" || true
count=$(count_transfers)
instrument restart-without-receipt "$class#committedRowsSurviveWithoutCloudReceiptAndBlockRepeatRestore"
[[ $(count_transfers) == "$count" ]]

for fault in truncate-get oversize-get corrupt-get; do
    printf '%s\n' "$fault" > "$fixture/proxy-mode"
    case "$fault" in
        truncate-get) issue=DownloadInterrupted ;;
        oversize-get) issue=DownloadTooLarge ;;
        corrupt-get) issue=RestoreInvalid ;;
    esac
    instrument "$fault" "$class#downloadFaultLeavesRoomAndNotificationMarkersUntouched" -e expectedIssue "$issue"
done
printf '%s\n' normal > "$fixture/proxy-mode"
instrument rollback-and-auth "$class#finalAuthorizationAndSqliteFailuresDoNotChangeExistingDataOrMarkers"
instrument code-recovery "$class#recoveryCodeRedemptionRotatesTheCodeAndRevokesAnEarlierSession" -e invite "$(invite)"

count=$(count_transfers)
"${adb[@]}" reverse --remove tcp:8765
"${adb[@]}" shell cmd connectivity airplane-mode enable
"${adb[@]}" shell svc wifi disable
"${adb[@]}" shell svc data disable
[[ $("${adb[@]}" shell settings get global airplane_mode_on | tr -d '\r') == 1 ]]
# Exercise DocumentsUI before the offline OS reboot, which can leave an AOSP System UI ANR over the picker.
instrument offline-saf org.unishiasy.vehiclelog.data.backup.BackupDocumentPickerTest,org.unishiasy.vehiclelog.data.backup.BackupRestoreTest
"${adb[@]}" reboot
"${adb[@]}" wait-for-device
boot_wait
"${adb[@]}" shell input keyevent 82
[[ $("${adb[@]}" shell settings get global airplane_mode_on | tr -d '\r') == 1 ]]
instrument offline-reboot "$class#afterRestartRestoredLedgerIsExactAndNoRetryRuns"
instrument offline-logging "$class#logAnEntryOfflineInRecoveredLedger"
[[ $(count_transfers) == "$count" ]]
echo 'Local M5.4 device checks passed.'
