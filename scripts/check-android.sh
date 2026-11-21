#!/usr/bin/env bash
set -euo pipefail

fail() {
    printf '%s\n' "$*" >&2
    exit 1
}

offline=true
device=false
for option in "$@"; do
    case "$option" in
        --online) offline=false ;;
        --device) device=true ;;
        *) fail 'Usage: check-android.sh [--online] [--device]' ;;
    esac
done

: "${JAVA_HOME:?Set JAVA_HOME to a local JDK installation}"
: "${ANDROID_HOME:?Set ANDROID_HOME to the local Android SDK}"
[[ -x "$JAVA_HOME/bin/java" && -x "$JAVA_HOME/bin/javac" ]] || fail 'JAVA_HOME must point to a JDK.'

platform="$ANDROID_HOME/platforms/android-36"
if [[ ! -r "$platform/android.jar" || ! -r "$platform/package.xml" ]] ||
    ! grep -Fq 'path="platforms;android-36"' "$platform/package.xml"; then
    fail 'Install base Android SDK Platform 36 (platforms;android-36).'
fi
[[ -x "$ANDROID_HOME/build-tools/36.0.0/aapt2" ]] || fail 'Install Android SDK Build Tools 36.0.0.'
adb="$ANDROID_HOME/platform-tools/adb"
[[ -x "$adb" ]] || fail 'Install Android SDK Platform Tools.'

cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.."
options=(--no-daemon --console=plain
    "-Dorg.gradle.java.home=$JAVA_HOME"
    -Pandroid.builder.sdkDownload=false
    -Porg.gradle.java.installations.auto-download=false)
tasks=(:app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest)

if "$offline"; then
    options+=(--offline)
fi
if "$device"; then
    device_count="$("$adb" devices | awk '$2 == "device" {count++} END {print count+0}')"
    [[ "$device_count" == 1 ]] || fail 'Connect and authorize exactly one test device.'
    tasks+=(:app:connectedDebugAndroidTest)
fi

exec env ANDROID_SDK_ROOT="$ANDROID_HOME" \
    GRADLE_USER_HOME="${GRADLE_USER_HOME:-$PWD/.gradle/local-user-home}" \
    ./gradlew "${options[@]}" "${tasks[@]}"
