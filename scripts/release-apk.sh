#!/usr/bin/env bash
set +x
set -euo pipefail
umask 077
export LC_ALL=C

fail() {
    printf '%s\n' "$*" >&2
    exit 1
}

usage() {
    cat <<'EOF'
Usage:
  release-apk.sh --check VERSION_CODE VERSION_NAME
  release-apk.sh VERSION_CODE VERSION_NAME KEYSTORE_PATH KEY_ALIAS CERT_SHA256

--check validates version inputs and the local toolchain without building or signing.
Signing requires an interactive terminal and a developer-owned PKCS12 keystore
outside the repository. CERT_SHA256 is the expected certificate fingerprint,
written as 64 hexadecimal digits without separators. It is public metadata.
EOF
}

case "${1:-}" in
    --help) usage; exit 0 ;;
    --check) check_only=true; shift ;;
    *) check_only=false ;;
esac
if "$check_only"; then
    [[ $# == 2 ]] || fail "$(usage)"
else
    [[ $# == 5 ]] || fail "$(usage)"
fi

version_code=$1
version_name=$2
if [[ ! $version_code =~ ^[1-9][0-9]{0,9}$ ]] || (( version_code > 2100000000 )); then
    fail 'VERSION_CODE must be an integer from 1 to 2100000000 without leading zeros.'
fi
[[ $version_name =~ ^[A-Za-z0-9][A-Za-z0-9._+-]{0,63}$ ]] ||
    fail 'VERSION_NAME must be 1–64 ASCII letters, digits, or . _ + -, starting with a letter or digit.'

: "${JAVA_HOME:?Set JAVA_HOME to a local JDK 17 or JDK 21 installation}"
: "${ANDROID_HOME:?Set ANDROID_HOME to the local Android SDK}"
[[ $JAVA_HOME == /* && $ANDROID_HOME == /* ]] || fail 'Use absolute paths for JAVA_HOME and ANDROID_HOME.'
[[ -x "$JAVA_HOME/bin/java" && -x "$JAVA_HOME/bin/javac" && -x "$JAVA_HOME/bin/keytool" ]] ||
    fail 'JAVA_HOME must point to a complete JDK.'
case "$("$JAVA_HOME/bin/javac" -version 2>&1)" in
    'javac 17'|'javac 17.'*|'javac 21'|'javac 21.'*) ;;
    *) fail 'The release workflow requires JDK 17 or JDK 21.' ;;
esac

platform="$ANDROID_HOME/platforms/android-36"
if [[ ! -r "$platform/android.jar" || ! -r "$platform/package.xml" ]] ||
    ! grep -Fq 'path="platforms;android-36"' "$platform/package.xml"; then
    fail 'Install base Android SDK Platform 36 (platforms;android-36).'
fi
build_tools="$ANDROID_HOME/build-tools/36.0.0"
for tool in aapt2 apksigner zipalign; do
    [[ -x "$build_tools/$tool" ]] || fail "Android SDK Build Tools 36.0.0 is missing $tool."
done
for tool in flock realpath sha256sum stat stty; do
    command -v "$tool" >/dev/null || fail "Required local command is missing: $tool."
done

project_root=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd -P)
[[ -x "$project_root/gradlew" ]] || fail 'The Gradle Wrapper is missing or is not executable.'
if "$check_only"; then
    printf 'Release preflight passed for versionCode=%s versionName=%s.\n' "$version_code" "$version_name"
    exit 0
fi

key_alias=$4
expected_cert=${5,,}
[[ -n "$key_alias" && "$key_alias" != -* ]] || fail 'Supply a keystore alias.'
[[ $expected_cert =~ ^[a-f0-9]{64}$ ]] || fail 'Supply a certificate SHA-256 as 64 hexadecimal digits.'
keystore=$(realpath -e -- "$3") || fail 'The keystore path must exist.'
case "$keystore" in
    "$project_root"|"$project_root"/*) fail 'Keep the keystore outside the repository.' ;;
esac
[[ -f "$keystore" && -r "$keystore" && -O "$keystore" && $(stat -c '%a' -- "$keystore") =~ ^[46]00$ ]] ||
    fail 'Use a readable keystore file you own with permissions 600 or 400.'
[[ -t 0 && -t 1 && -t 2 ]] || fail 'Signing requires a private interactive terminal. Avoid redirection or pipes.'

cd -- "$project_root"
export PATH="$JAVA_HOME/bin:$PATH"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export GRADLE_USER_HOME="${GRADLE_USER_HOME:-$project_root/.gradle/local-user-home}"
export JAVA_HOME ANDROID_HOME

application_id=org.mewlog.vehiclelog
output_root="$project_root/build/releases"
destination="$output_root/$version_code"
mkdir -p -- "$output_root"
exec 9>>"$output_root/.release.lock"
flock -n 9 || fail 'Another release workflow is using this checkout.'
[[ ! -e "$destination" && ! -L "$destination" ]] || fail "Release versionCode $version_code already has an output path."

./gradlew --offline --no-daemon --console=plain "-Dorg.gradle.java.home=$JAVA_HOME" \
    -Pandroid.builder.sdkDownload=false -Porg.gradle.java.installations.auto-download=false \
    "-PvehiclelogReleaseVersionCode=$version_code" "-PvehiclelogReleaseVersionName=$version_name" \
    :app:assembleRelease

verify_manifest() {
    local badging
    badging=$("$build_tools/aapt2" dump badging "$1") || fail 'Could not read APK metadata.'
    grep -Fq "package: name='$application_id' versionCode='$version_code' versionName='$version_name'" <<< "$badging" ||
        fail 'APK application ID or version differs from the requested release.'
    if grep -qx 'application-debuggable' <<< "$badging"; then
        fail 'Refusing a debuggable APK.'
    fi
}

unsigned_apk="$project_root/app/build/outputs/apk/release/app-release-unsigned.apk"
[[ -f "$unsigned_apk" ]] || fail 'Expected unsigned release APK is missing.'
verify_manifest "$unsigned_apk"
stage=$(mktemp -d "$output_root/.staging.XXXXXXXX")
trap 'rm -rf -- "$stage"' EXIT
trap 'exit 130' INT
trap 'exit 143' TERM
trap 'exit 129' HUP

apk_name="vehiclelog-$version_code-$version_name.apk"
aligned_apk="$stage/aligned.apk"
signed_apk="$stage/$apk_name"
"$build_tools/zipalign" -P 16 4 "$unsigned_apk" "$aligned_apk"
(
    ulimit -c 0
    terminal_state=$(stty -g)
    trap 'stty "$terminal_state"' EXIT
    trap 'exit 130' INT
    trap 'exit 143' TERM
    trap 'exit 129' HUP
    # Keep terminal echo off even if Java falls back to reading standard input.
    stty -echo -echonl
    "$build_tools/apksigner" sign --ks "$keystore" --ks-type PKCS12 --ks-key-alias "$key_alias" \
        --ks-pass stdin --v4-signing-enabled false --out "$signed_apk" "$aligned_apk"
)
rm -- "$aligned_apk"

"$build_tools/zipalign" -c -P 16 4 "$signed_apk"
verification=$("$build_tools/apksigner" verify --verbose --print-certs "$signed_apk")
certificate=$(awk '/^Signer #[0-9]+ certificate SHA-256 digest:/ {print $NF}' <<< "$verification")
[[ ${certificate,,} == "$expected_cert" ]] || fail 'The APK must have one signer matching the expected certificate.'
verify_manifest "$signed_apk"

(
    cd -- "$stage"
    sha256sum -- "$apk_name" > SHA256SUMS
    sha256sum --check --strict SHA256SUMS
    printf 'applicationId=%s\nversionCode=%s\nversionName=%s\ncertificateSha256=%s\n' \
        "$application_id" "$version_code" "$version_name" "$expected_cert" > release.txt
)
mv -nT -- "$stage" "$destination"
[[ ! -e "$stage" ]] || fail "An output path already exists for release versionCode $version_code."
printf 'Verified release files in %s\n' "$destination"
cat -- "$destination/release.txt" "$destination/SHA256SUMS"
