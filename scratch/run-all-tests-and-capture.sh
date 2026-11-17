#!/usr/bin/env bash
set -euo pipefail
umask 077
cd "/home/amir/Documents/ChatGPT/Project UNI Shiasy"

export ANDROID_HOME="/home/amir/Android/Sdk"
export ANDROID_ADB_SERVER_PORT=5097

fixture=$(mktemp -d "$PWD/build/ui-test-fixture.XXXXXX")
echo "Fixture directory: $fixture"
export ANDROID_AVD_HOME="$fixture/avd" ANDROID_EMULATOR_HOME="$fixture/emulator" ANDROID_USER_HOME="$fixture/emulator"
mkdir -p "$ANDROID_AVD_HOME" "$ANDROID_USER_HOME"

base_adb=( "$ANDROID_HOME/platform-tools/adb" -P "$ANDROID_ADB_SERVER_PORT" )
emulator="$ANDROID_HOME/emulator/emulator"
package=org.amirariana.vehiclelog.debug
runner="$package.test/androidx.test.runner.AndroidJUnitRunner"
port=5584
emu_pid=
adb_owned=

unset http_proxy https_proxy HTTP_PROXY HTTPS_PROXY all_proxy ALL_PROXY

cleanup() {
    echo "Cleaning up..."
    if [[ -n $emu_pid ]]; then kill "$emu_pid" 2>/dev/null || true; wait "$emu_pid" 2>/dev/null || true; fi
    if [[ -n $adb_owned ]]; then "${base_adb[@]}" kill-server 2>/dev/null || true; fi
    rm -rf "$fixture"
}
trap cleanup EXIT

"${base_adb[@]}" start-server
adb_owned=yes

name="test_ui_$port"
avd="$ANDROID_AVD_HOME/$name.avd"
mkdir -p "$avd"
cat > "$ANDROID_AVD_HOME/$name.ini" <<CONFIG
avd.ini.encoding=UTF-8
path=$avd
target=android-35
CONFIG
cat > "$avd/config.ini" <<CONFIG
AvdId=$name
PlayStore.enabled=false
abi.type=x86_64
disk.dataPartition.size=4G
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
env -u http_proxy -u https_proxy -u HTTP_PROXY -u HTTPS_PROXY -u all_proxy -u ALL_PROXY \
    "$emulator" -avd "$name" -port "$port" -no-window -no-audio -no-snapshot -no-boot-anim \
    -gpu swiftshader -accel on > "$fixture/$name-emulator.log" 2>&1 &
emu_pid=$!

echo "Waiting for emulator to boot..."
for ((i=0; i<150; i++)); do
    if [[ $("${adb[@]}" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r') == 1 ]]; then
        echo "Boot completed"
        break
    fi
    if ! kill -0 "$emu_pid" 2>/dev/null; then
        echo "Emulator process died! Log:"
        cat "$fixture/$name-emulator.log"
        exit 1
    fi
    sleep 2
done

"${adb[@]}" install -r app/build/outputs/apk/debug/app-debug.apk
"${adb[@]}" install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
"${adb[@]}" shell pm grant "$package" android.permission.POST_NOTIFICATIONS 2>/dev/null || true
"${adb[@]}" shell input keyevent 82

echo "Running VehicleSetupTest..."
"${adb[@]}" shell am instrument -w -r -e class org.unishiasy.vehiclelog.ui.vehicle.VehicleSetupTest "$runner" | tee "$fixture/VehicleSetupTest.log"

echo "Running HomeScreenTest..."
"${adb[@]}" shell am instrument -w -r -e class org.unishiasy.vehiclelog.ui.home.HomeScreenTest "$runner" | tee "$fixture/HomeScreenTest.log"

echo "Running LedgerEntryUiTest..."
"${adb[@]}" shell am instrument -w -r -e class org.unishiasy.vehiclelog.ui.ledger.LedgerEntryUiTest "$runner" | tee "$fixture/LedgerEntryUiTest.log"

echo "Running VisualAuditCaptureTest..."
"${adb[@]}" shell am instrument -w -r -e class org.unishiasy.vehiclelog.ui.audit.VisualAuditCaptureTest "$runner" | tee "$fixture/VisualAuditCaptureTest.log"

mkdir -p /home/amir/.gemini/antigravity/brain/24a527bc-3dcd-49df-a31e-6cfe2d48e738/screenshots
echo "Pulling captured screenshots..."
"${adb[@]}" pull /sdcard/Android/data/org.amirariana.vehiclelog.debug/files/visual_audit/. /home/amir/.gemini/antigravity/brain/24a527bc-3dcd-49df-a31e-6cfe2d48e738/screenshots/ || true

echo "Summary of test results:"
grep -E 'OK \(|FAILURES|INSTRUMENTATION_FAILED' "$fixture"/*.log

