# Local Android release APK workflow

The direct APK workflow uses provisional release ID `org.amirariana.vehiclelog` and the existing Kotlin namespace `org.unishiasy.vehiclelog`. Confirm the permanent application ID before the first public release. Changing the application ID creates a separate Android installation. [ADR 0003](../architecture/ADR/0003-signed-apk-distribution.md)

The preparation slice permits preflight and manifest checks. Key creation, release APK generation and signing, and phone installation require separate approval.

## Preflight

Use Bash on the Linux development machine with JDK 17, Android SDK Platform 36, Build Tools 36.0.0, and a verified local Gradle Wrapper and dependency cache. Match any local `sdk.dir` to `ANDROID_HOME`. The script uses the Gradle 8.13 wrapper in offline mode. Confirm the wrapper distribution is cached first because the wrapper resolves its own distribution before Gradle processes `--offline`. [Local Android setup](ANDROID_SETUP.md)

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-amazon-corretto
export ANDROID_HOME="$HOME/Android/Sdk"
./scripts/release-apk.sh --check 1 0.1.0
```

`--check` validates the version arguments, JDK, SDK, and required local commands. It does not run Gradle or access a keystore. Run `./scripts/check-android.sh` for debug build, lint, and test packaging checks. Before actual signing, record the exact source revision and check the working tree so that the release inputs can be reproduced.

## Developer-owned key

After approval, create a developer-owned PKCS12 keystore in a private directory outside the repository. The following commands are for a real local terminal under the developer's control. Use a separate keystore if the approved operation is a signing rehearsal. For a new key, choose the path and alias before running the commands.

```bash
(
    umask 077
    key_dir="$HOME/.local/share/vehiclelog-signing"
    install -d -m 700 -- "$key_dir"
    keystore="$key_dir/vehiclelog-release.p12"
    if [[ -e "$keystore" || -L "$keystore" ]]; then
        printf 'Keystore path already exists.\n' >&2
        exit 1
    fi
    "$JAVA_HOME/bin/keytool" -genkeypair -storetype PKCS12 \
        -keystore "$keystore" -alias vehiclelog-release \
        -keyalg RSA -keysize 3072 -validity 10000
)
```

Enter the password at `keytool`'s own prompt. Use a strong, unique password and keep it in the developer's password manager. The workflow uses one PKCS12 password for the store and key. Keep an encrypted backup of the keystore on separate storage and confirm that the backup can be opened and identifies the same certificate. Keep access to the password separately from the keystore. Loss of the key or password can prevent updates over installations signed with that key. Exposure of the key can let someone else sign an APK under the same identity. [Android app signing](https://developer.android.com/studio/publish/app-signing)

Read and record the public SHA-256 certificate fingerprint independently from the intended keystore and alias. Remove colons from the fingerprint when supplying it to the script.

```bash
"$JAVA_HOME/bin/keytool" -list -v -storetype PKCS12 \
    -keystore "$HOME/.local/share/vehiclelog-signing/vehiclelog-release.p12" \
    -alias vehiclelog-release
```

## Build, align, sign, and verify

Choose a positive `versionCode` no higher than 2100000000 and greater than the code on every earlier build in the same release line. Choose a `versionName` of 1–64 ASCII characters from letters, digits, dots, underscores, plus signs, and hyphens, starting with a letter or digit. A prerelease label such as `0.1.0-rc.1` works. The script accepts both values as arguments and passes them as Gradle properties for the release variant only. Keep a release ledger of allocated codes, exact source inputs, fingerprints, and checksums. The script refuses an existing output directory for a code in this checkout. The developer is responsible for tracking codes used elsewhere. [Android versioning](https://developer.android.com/studio/publish/versioning)

Run the script only after approving a release build and signing operation. Set `CERT_SHA256` to the public certificate fingerprint read above.

```bash
./scripts/release-apk.sh 1 0.1.0 \
    "$HOME/.local/share/vehiclelog-signing/vehiclelog-release.p12" \
    vehiclelog-release "$CERT_SHA256"
```

1. Gradle assembles the unsigned release. It receives only the version overrides as release inputs and has no release signing configuration. Finish other Gradle builds in this checkout before running the release script.
2. AAPT2 checks the expected package and version and rejects a debuggable APK. `zipalign -P 16 4` aligns the unsigned APK, including 16 KiB alignment of uncompressed native libraries.
3. `apksigner` signs the aligned APK in a private staging directory. The keystore path, alias, and expected public fingerprint can be visible as process arguments. The password is entered at the signer prompt. The script requires terminal input and output and disables terminal echo during signing as protection for the signer's fallback input path. Build Tools 36.0.0 uses Java's `Console.readPassword` when available and can otherwise read the `stdin` line. The script leaves password input to the signer, so Gradle and Bash never receive the password. `keytool` likewise reads the password at its own prompt when password flags are omitted. [APK signer options](https://developer.android.com/tools/apksigner)
4. The script checks alignment and signature, compares the single signer certificate fingerprint with the expected fingerprint, checks package and version again, and generates and verifies `SHA256SUMS`. It publishes the APK, checksum, and `release.txt` together under the ignored `build/releases/<versionCode>/` directory. The script locks out another invocation in the same checkout, uses a staging directory on the same filesystem, and refuses to overwrite the destination. The unsigned intermediate remains in the normal ignored Gradle build directory. [APK alignment](https://developer.android.com/tools/zipalign)

Use a trusted workstation and a private terminal without session input recording. Terminal no-echo prevents display of what you type. It cannot protect against keystroke capture, a compromised SDK or JDK, or privileged access to the process or its memory while signing. The script avoids password arguments, exported passwords, temporary password files, and prompts handled by the shell. It restores the terminal settings on exit. After a forced kill that prevents cleanup, run `stty sane` in that terminal before reuse. Reproducibility here means pinned inputs, commands, and verified output identity. Record each APK's checksum without assuming byte-for-byte rebuilds.

## M1 two-version Redmi signing rehearsal

After separate authorization to build, sign, and install release artifacts:

1. Produce two verified APKs from the same source revision. Use the same provisional release ID and certificate and an increasing version code, for example code 1 with `0.1.0` and code 2 with `0.1.1` if both codes are unused. Record both manifests, signer SHA-256 values, and checksums.
2. Use `adb devices -l` and read the target's model and OS with `adb -s "$SERIAL" shell getprop ro.product.model` and `adb -s "$SERIAL" shell getprop ro.build.version.release`. Select the Redmi Note 9S on Android 12. Check `adb -s "$SERIAL" shell pm path org.amirariana.vehiclelog` before the first install. If a release package is already present, stop and check its provenance and version before making any change.
3. Install the lower version with `adb -s "$SERIAL" install "$APK_1"`. Verify the installed package and version, record its UID and first-install time from `adb -s "$SERIAL" shell dumpsys package org.amirariana.vehiclelog`, and check launcher and airplane-mode cold startup.
4. Install the higher version with `adb -s "$SERIAL" install -r "$APK_2"`. Require success without removing the package, the expected higher installed version, the same package UID and first-install time, and a successful offline launch with the Persian RTL Home. If either installation fails, record the exact error and leave both debug installations in place. M1 has no ledger records. Record-preservation testing starts when persistence is available.
5. Record the physical phone and Android version, APKs, signer, commands, checks and results in [PLANS.md](../../PLANS.md). Keep the release artifacts and evidence together.


## M4.6 release readiness audit

**2026-11-14 · OPEN.** The M4 gate requires verified signed M3 (Room v3) and M4 (Room v4) artifacts for `org.amirariana.vehiclelog`, the same approved developer test certificate, and an M4 version code greater than the M3 code. Perform the upgrade in place on a populated M3 installation and check the package, version, UID, first-install time, offline cold launch, and preservation of vehicles, entries, linked and standalone readings, and monthly reports. Use a disposable profile or dedicated test device where available. Establish the safety of any proposed use of the existing Redmi release package first. [M4.6 execution record](../../PLANS.md#m46--signed-local-prototype-acceptance)

| Retained artifact | Application ID | Version | APK SHA-256 |
|---|---|---|---|
| `build/releases/1/vehiclelog-1-0.1.0.apk` | `org.amirariana.vehiclelog` | 1 / 0.1.0 | `edded9c9cc0f6142dda0029b07187e24522ef065c32ef5cf9b7e9bf9470935d5` |
| `build/releases/2/vehiclelog-2-0.1.1.apk` | `org.amirariana.vehiclelog` | 2 / 0.1.1 | `1761b660c8bfd33c7c688189e5e87d3c9f953da6c556ca10a4451b3330fc310c` |

Both are M1 release artifacts. Both passed alignment, checksum, and v2/v3 signature verification with one signer and certificate SHA-256 `4da34964d2bd5de0bf98713e02b240fdb9438d18964d5d0c9efad248f1a8748a`. The ignored unsigned intermediate has code 2 and failed APK signature verification as expected. The app and instrumentation debug APKs use different package IDs and certificate SHA-256 `2937210215654d1944357664cd8d2a45963855ebcbbf0e4e780fbf645055a58c`. The retained M4.4 source backup contains copies of the two M1 release APKs.

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-amazon-corretto
export ANDROID_HOME="$HOME/Android/Sdk"
bash -n scripts/release-apk.sh
./scripts/release-apk.sh --check 3 0.2.0
./scripts/release-apk.sh --check 4 0.3.0
sdk="$ANDROID_HOME/build-tools/36.0.0"
for apk in build/releases/{1,2}/*.apk; do
    "$sdk/aapt2" dump badging "$apk"
    "$sdk/apksigner" verify --verbose --print-certs "$apk"
    "$sdk/zipalign" -c -P 16 4 "$apk"
    sha256sum "$apk"
done
for release in build/releases/{1,2}; do
    (cd "$release" && sha256sum -c SHA256SUMS)
done
```

Both preflights passed. Codes 3 and 4 are unused in the local release directories and exceed both installed and retained release codes. They are candidates only. Confirm the full release ledger before allocation. The recovered M3 source compiles, and signing it with the independently verified `vehiclelog-m1-test` identity would create a new baseline with M3 code. The developer must confirm that route and perform signing in a private terminal. The audit did not access a keystore.

```bash
adb="$ANDROID_HOME/platform-tools/adb"
serial=4c1ad625
pkg=org.amirariana.vehiclelog
"$adb" devices -l
for prop in ro.product.model ro.build.version.release ro.build.version.sdk; do
    "$adb" -s "$serial" shell getprop "$prop"
done
for package in "$pkg" "$pkg.debug"; do
    "$adb" -s "$serial" shell pm path --user 0 "$package"
    "$adb" -s "$serial" shell dumpsys package "$package" |
        grep -m5 -E 'versionCode=|versionName=|firstInstallTime=|lastUpdateTime=|userId='
done
apk_path=$("$adb" -s "$serial" shell pm path --user 0 "$pkg" | tr -d '\r' | sed -n 's/^package://p')
audit_dir=$(mktemp -d /tmp/m46-installed.XXXXXX)
"$adb" -s "$serial" pull "$apk_path" "$audit_dir/base.apk"
sha256sum "$audit_dir/base.apk"
cmp -s "$audit_dir/base.apk" build/releases/2/vehiclelog-2-0.1.1.apk
```

The Redmi returned Android 12/API 31. The installed release was version code 2, name `0.1.1`, UID 10472, with first-install time `2026-11-09 12:15:33` in device local time. The installed APK checksum and byte comparison matched the local version-2 M1 artifact. The installed debug app was code 1, name `0.1.0`, UID 10470. Release signing, installation, and the signed and offline acceptance steps remain pending.

### M4.6 reconstructed baseline preflight

The product owner approved building a reconstructed M3 test baseline from tree `189f4788853494afbad51d8a5c80f1d7e4dd7903`. It has no historical signed-release provenance. The archive SHA-256 matched the audit. An archive member comparison against `git archive 189f4788853494afbad51d8a5c80f1d7e4dd7903` matched all 98 paths, types, modes, and contents. A raw comparison of the tar containers differed. The historical and current Gradle files both declare `org.amirariana.vehiclelog`. The preflight commands above passed for 3 and 4, and neither code appears in the available release ledger, artifact inventory, retained M4.4 backup, or installed packages. The developer must confirm any allocations elsewhere. Both retained M1 APK checksums and Room schema exports v1–v4 still match the pre-M4.5 checksums.

In addition to the device queries above, the following read-only checks were run on the Redmi:

```bash
"$adb" -s "$serial" shell pm list users
for package in "$pkg" "$pkg.debug"; do
    "$adb" -s "$serial" shell dumpsys package "$package" | grep -E 'User (0|10):'
    apk_path=$("$adb" -s "$serial" shell pm path --user 0 "$package" | tr -d '\r' | sed -n 's/^package://p')
    probe_dir=$(mktemp -d /tmp/m46-device.XXXXXX)
    "$adb" -s "$serial" pull "$apk_path" "$probe_dir/base.apk"
    sha256sum "$probe_dir/base.apk"
    "$sdk/apksigner" verify --print-certs "$probe_dir/base.apk" | grep 'certificate SHA-256 digest'
done
"$adb" -s "$serial" shell run-as "$pkg" sh -c 'test\ -d\ databases'
"$adb" -s "$serial" shell run-as "$pkg.debug" ls -1 databases
"$ANDROID_HOME/emulator/emulator" -accel-check
```

User 0 has both apps. User 10 has neither app. The release APK is the retained code 2 with the documented test certificate. The debug code 1 APK has SHA-256 `b3d80f92f6a3da165bb7e66e326b7654ad412215ff34e96813f3c08a3e0e6432`, the debug certificate above, and a private `vehicle-ledger.db` plus WAL/SHM files. Android denied `run-as` for the nondebuggable release app, leaving its private data state unknown. The emulator acceleration check reported status 8 and missing `/dev/kvm`.

**Preflight BLOCKED.** The existing public certificate is verified. The private keystore alias still needs verification in the developer's private terminal using the `keytool -list -v` procedure above with `vehiclelog-m1-test`. Compare its SHA-256 fingerprint to the documented test certificate and record only the public match. The connected Redmi has shared package binaries across its user profiles, so installing either candidate for user 10 would replace user 0's release binary. Leave both existing installations intact.

Use a dedicated physical Android device with the release package absent across all Android users. After checking the full version ledger and private key, use separate M3 and M4 build copies and the documented signer in the developer's private terminal. Record the M3 tree and archive hash, capture the exact M4 build inputs, and preserve both signed outputs in separate version directories. On the dedicated device, seed synthetic representative M3 records, record package version, UID, and first-install time, then use an in-place M4 update and check data, reports, and offline launch. After the dedicated device is on M4, use a verified empty test profile on that device or a second dedicated device for restore. Keep the upgraded fixture for populated-restore rejection. This allows the empty restore to exercise the signed M4 binary without changing the upgraded fixture or the original Redmi.

Signing and release acceptance stopped at preflight. No new APK, signed migration, signed offline launch, notification acceptance, or signed SAF export/restore result exists.

### M4.6 emulator-first follow-up

The developer may perform the following read-only verification in a private local terminal. Enter the absolute path of the existing test PKCS12 file and enter its password only at the JDK prompt. Confirm that the alias is a `PrivateKeyEntry` and reports the expected public SHA-256. Record the match and verify that codes 3 and 4 are unallocated in the complete release ledger for `org.amirariana.vehiclelog`, including APKs built or shared outside this checkout.

```bash
read -r -p 'Absolute path to the existing test keystore: ' m46_test_keystore
LC_ALL=C /usr/lib/jvm/java-17-amazon-corretto/bin/keytool \
    -list -v -storetype PKCS12 -keystore "$m46_test_keystore" \
    -alias vehiclelog-m1-test
```

Expected certificate SHA-256 with separators removed: `4da34964d2bd5de0bf98713e02b240fdb9438d18964d5d0c9efad248f1a8748a`. The M1 record documents a successful historical private-key check. A current check and allocation confirmation for M4.6 are pending. Continue using the developer-controlled signer workflow after both confirmations. Use a fresh extraction of the verified archive for M3, and a separate captured copy of the M4 build inputs. Verify the signed outputs independently before installing either APK.

With the verified keystore path in `m46_test_keystore`, run the release command from each corresponding source copy, using the project Gradle cache described in the M3 compile record. The signature and alignment checks apply to each copy's output.

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-amazon-corretto
export ANDROID_HOME="$HOME/Android/Sdk"
m46_cert=4da34964d2bd5de0bf98713e02b240fdb9438d18964d5d0c9efad248f1a8748a
# In the verified M3 source copy:
./scripts/release-apk.sh 3 0.2.0 "$m46_test_keystore" vehiclelog-m1-test "$m46_cert"
# In the captured M4 source copy:
./scripts/release-apk.sh 4 0.3.0 "$m46_test_keystore" vehiclelog-m1-test "$m46_cert"
```

For each version, from its source copy, check the manifest, certificate, alignment, and generated checksum. Record the printed package and version, signature verification results, and APK SHA-256 in the release ledger.

```bash
m46_sdk="$ANDROID_HOME/build-tools/36.0.0"
# Run with 3 in the M3 copy and 4 in the M4 copy.
m46_code=3
for m46_apk in "build/releases/$m46_code/"*.apk; do
    "$m46_sdk/aapt2" dump badging "$m46_apk" | grep '^package:'
    "$m46_sdk/apksigner" verify --verbose --print-certs "$m46_apk"
    "$m46_sdk/zipalign" -c -P 16 4 "$m46_apk"
done
(cd "build/releases/$m46_code" && sha256sum -c SHA256SUMS)
```

On a confirmed clean emulator, target its explicit serial for each ADB command. Install M3, seed synthetic vehicles, entries, and readings, and record the version, UID, first-install time, and reports. Install M4 with `adb -s emulator-5580 install -r "$m46_apk"` over that fixture and check the records and reports before adding M4 data. Exercise the offline launch, reminders, and actual document-picker export. Attempt restore on the upgraded populated fixture, then restore the same retained file into a second clean M4 fixture. Verify only data present at export returns. Keep virtual-device evidence supplemental to the signed physical-device acceptance gate.

The host lists `Medium_Phone_API_37.0` and has the API 37 x86_64 Play Store image. The acceleration check exited 8 with no `/dev/kvm`. The disposable AVD copied configuration only, with its own AVD and emulator homes. The first launch under `/tmp` failed the userdata disk-space check. A separate workspace-local AVD under `.gradle` passed the disk check. It started with `-accel off -no-window -gpu swiftshader -no-audio -no-snapshot -no-metrics -port 5580` and a separate ADB server on port 5076. ADB listed `emulator-5580` offline during the probe. Neither the 55-second attempt nor the 165-second repeat yielded a completed boot in the probes. No APK was installed and no emulator app check was executed. The original AVD and Redmi installations were untouched.

### M4.6 approved test builds

On 2026-11-14, the developer confirmed the existing test key's alias, `PrivateKeyEntry` type, and documented certificate. The developer also confirmed no off-ledger builds assigned codes 3 or 4 and approved their allocation. Both release-script toolchain preflights passed. The M3 build copy was extracted using `git archive 189f4788853494afbad51d8a5c80f1d7e4dd7903 | tar -xp -C "$m46_stage/m3"` and matched the 98 files and file modes in the retained archive. The current M4 copy captures the Gradle inputs, app sources, Room schema exports, and release script. This M3 source is for a reconstructed test baseline with no historical signed release.

The two input checksum manifests are retained under `build/m46/inputs.vA8Ful/`. The SHA-256 of `m3-inputs.sha256` is `9fffcfff796348237144567fbbd9634401c7c50de20f9529af13a80e21217d24`. The SHA-256 of `m4-inputs.sha256` is `0d9a6fd89c180b23435967bbcff474637e11eb7100c3c2122166048f0bc09968`. These identify the build inputs.

Run the two release commands in a private local Bash terminal. Enter the password only at each signer prompt. The existing keystore remains outside the workspace. The release script refuses to overwrite an existing version directory.

```bash
(
    set -e
    m46_project='/home/amir/Documents/ChatGPT/Project UNI Shiasy'
    m46_stage="$m46_project/build/m46/inputs.vA8Ful"
    m46_key="$HOME/.local/share/vehiclelog-signing/vehiclelog-m1-test.p12"
    m46_cert=4da34964d2bd5de0bf98713e02b240fdb9438d18964d5d0c9efad248f1a8748a
    export JAVA_HOME=/usr/lib/jvm/java-17-amazon-corretto
    export ANDROID_HOME="$HOME/Android/Sdk"
    export GRADLE_USER_HOME="$m46_project/.gradle/local-user-home"
    (cd "$m46_stage/m3" && sha256sum -c ../m3-inputs.sha256 &&
        ./scripts/release-apk.sh 3 0.2.0 "$m46_key" vehiclelog-m1-test "$m46_cert")
    (cd "$m46_stage/m4" && sha256sum -c ../m4-inputs.sha256 &&
        ./scripts/release-apk.sh 4 0.3.0 "$m46_key" vehiclelog-m1-test "$m46_cert")
)
```

Expected outputs are `build/m46/inputs.vA8Ful/m3/build/releases/3/vehiclelog-3-0.2.0.apk` and `build/m46/inputs.vA8Ful/m4/build/releases/4/vehiclelog-4-0.3.0.apk`, each with `release.txt` and `SHA256SUMS`. Independent signed artifact verification passed on 2026-11-15. See the checks below.

For independent verification, run `bash build/m46/inputs.vA8Ful/verify-signed.sh` from the project. It checks each release's manifest, 16 KiB alignment, signature and certificate, APK checksum, captured source checksums, and the historical artifact and schema checksums.

Unsigned readiness was checked from each prepared copy with the same JDK, SDK and Gradle cache as above, using the approved code and name in:

```bash
./gradlew --offline --no-daemon --console=plain --max-workers=4 \
    "-Dorg.gradle.java.home=$JAVA_HOME" -Pandroid.builder.sdkDownload=false \
    -Porg.gradle.java.installations.auto-download=false \
    "-PvehiclelogReleaseVersionCode=$version" "-PvehiclelogReleaseVersionName=$name" :app:assembleRelease
```

The first sandboxed Gradle attempt could not initialize its local lock service. The offline readiness builds were retried outside the sandbox. Gradle did not receive the keystore path or any signing credentials during these checks.

Both readiness builds succeeded. M3 took 4m 30s and M4 took 4m 46s, each with 50 tasks executed. AAPT2 confirmed the approved application ID and versions. The APKs produced by these readiness builds are unsigned. Their SHA-256 values are `2d05e7b7ff77b46cc9468a8839386fa6b9290329876d8f6e52f6ac15eaacc37c` (M3) and `bd634b677efd7f3da1c8df670b6bb1bdbed19bddb6972e4db14085c7430adb7c` (M4). The subsequent private signing step produces separate APKs with their own hashes. Both immutable M1 APKs and historical Room schemas v1–v4 still match their recorded checksums.

### M4.6 independent signer and emulator checks

On 2026-11-15, `bash build/m46/inputs.vA8Ful/verify-signed.sh` passed independently. Both artifacts are nondebuggable, use `org.amirariana.vehiclelog`, passed `zipalign -c -P 16 4`, and verified with one signer under APK signature schemes v2 and v3. The certificate SHA-256 is `4da34964d2bd5de0bf98713e02b240fdb9438d18964d5d0c9efad248f1a8748a`. Both source manifests, both retained M1 artifacts, and historical Room schemas v1–v4 passed SHA-256 verification. The signed APKs were used as supplied by the developer.

| Artifact | Version | SHA-256 |
|---|---|---|
| `build/m46/inputs.vA8Ful/m3/build/releases/3/vehiclelog-3-0.2.0.apk` | 3 / 0.2.0, reconstructed M3 test baseline | `c07ff482a24df3dc72a17ee341e5247676a131396b7dcf05aa1f251c9d20db62` |
| `build/m46/inputs.vA8Ful/m4/build/releases/4/vehiclelog-4-0.3.0.apk` | 4 / 0.3.0, signed M4 test prototype | `c95be26d57ce0b0f3450944eed8ef22bdfbddbf737a7eb69557d1150c85c2f22` |

The sole local image is the API 37 x86_64 Google Play image. The previous software probes timed out with TCG CPU feature warnings and no verified Android boot. The current `emulator -accel-check` reported missing KVM inside the sandbox, while the host returned status 0 and usable KVM version 12. The previous software-boot stall was not diagnosed further. The fresh `M46_Signed_Upgrade` AVD used separate AVD and emulator homes under `build/m46/isolated-acceptance.Bt4Agl/`, a dedicated ADB server on port 5076, and serial `emulator-5580`. Only the existing AVD configuration was read. Its userdata and snapshots were excluded.

```bash
emulator @M46_Signed_Upgrade -accel on -no-window -gpu swiftshader -no-audio \
    -no-snapshot -no-metrics -no-boot-anim -camera-front none -camera-back none \
    -cores 2 -show-kernel -port 5580
adb -P 5076 -s emulator-5580 shell getprop sys.boot_completed
adb -P 5076 -s emulator-5580 shell getprop ro.build.version.sdk
```

Kernel logs in `build/m46/isolated-acceptance.Bt4Agl/boot.log` show `sys.boot_completed=1` at about 83 seconds. ADB returned `1` and `37` after initially reporting offline through guest startup. The release and debug IDs were absent before testing. No commands targeted the existing Redmi or existing AVD.

The signed upgrade commands used the dedicated ADB server throughout:

```bash
adb -P 5076 -s emulator-5580 install build/m46/inputs.vA8Ful/m3/build/releases/3/vehiclelog-3-0.2.0.apk
adb -P 5076 -s emulator-5580 shell dumpsys package org.amirariana.vehiclelog
adb -P 5076 -s emulator-5580 shell am force-stop org.amirariana.vehiclelog
adb -P 5076 -s emulator-5580 shell am start -W \
    -n org.amirariana.vehiclelog/org.unishiasy.vehiclelog.MainActivity
adb -P 5076 -s emulator-5580 install -r build/m46/inputs.vA8Ful/m4/build/releases/4/vehiclelog-4-0.3.0.apk
adb -P 5076 -s emulator-5580 shell dumpsys package org.amirariana.vehiclelog
adb -P 5076 -s emulator-5580 shell cmd connectivity airplane-mode enable
adb -P 5076 -s emulator-5580 shell svc wifi disable
adb -P 5076 -s emulator-5580 shell svc data disable
adb -P 5076 -s emulator-5580 shell dumpsys connectivity
adb -P 5076 -s emulator-5580 shell dumpsys telephony.registry
```

Both installs returned `Success`. The app ID number stayed `10235` and first-install time stayed `2026-11-15 04:02:02` as the version moved from 3 to 4. Signed M3 and M4 both cold-launched after force-stop. M4 cold-launched twice with airplane mode on, Wi-Fi off, no active default network, and telephony radio and data connection state 0. The global `mobile_data` preference still read 1. The runtime connection was down.

The synthetic M3 UI fixture contained vehicle `M46-Ledger`, a fuel entry for 1,234.5 toman with 12.345 liters, note `M46Fuel`, and a linked 1,500 km reading, plus a wash entry for 200.1 toman. ADB-driven input and Android UI Automator captured the same history before and after upgrade (excluding the transient save banner) and identical Aban 1405 monthly report text with two entries and a 1,434.6 toman total. This exercises the shipping Room migration by opening the populated ledger after an in-place signed upgrade. The release UI offers no standalone reading creation and exposes no record UUIDs. Standalone readings were absent from this fixture, and pre-upgrade UUIDs could not be compared directly.

In signed M4 on API 37, the Android notification permission prompt was denied. The emulator showed the blocked state and the date reminder still appeared due in the reminder screen and Home. No matching active notification was observed during denial. Enabling the app's notifications through Android settings granted `POST_NOTIFICATIONS`; `dumpsys notification` showed the matching `date-reminder:<UUID>` record. Completing the date reminder showed the completed status and removed the active notification.

The actual SAF Create Document picker exported `vehicle-ledger-backup.json` to Downloads and the app displayed success. The 1,129-byte plaintext JSON SHA-256 is `75b71e8e1173e481c9c3f78a16c27b6ea3360de061ad8300daafccc7662d9d86`. The snapshot contains one vehicle, two entries (12,345 and 2,001 rials), one 12,345 mL fuel value, one 1,500 km reading linked by entry UUID, and one active date reminder. The envelope and rows carry business fields only. The later 300.2 toman wash with note `M46Later` was created after export. The populated-ledger SAF restore displayed the empty-ledger-required error, and History still showed the later record and earlier records afterward. `aapt2 dump xmltree` verified packaged `allowBackup=false`, `fullBackupContent=false`, and database exclusions in both data-extraction sections. No actual Android device-transfer test was performed.

The 1,600 km reminder stayed pending at a recorded 1,500 km and was then canceled through the signed UI. On the same disposable emulator, `pm create-user M46-Empty-Restore`, `am start-user -w 10`, `cmd package install-existing --user 10 org.amirariana.vehiclelog`, and `am switch-user 10` created a separate empty app installation while preserving the upgraded user 0 fixture. After switching users, MediaStore `content insert` and `content write --user 10` staged the unchanged source file in the new user's Downloads. A readback passed `cmp`. This file transfer did not bypass the app picker. The app's SAF Open Document picker selected the file, and restore reported success.

A fresh export through the picker in user 10 matched all original parsed JSON fields except `exportedAt`, including all exported IDs and the link and reminder state. `M46Later` was absent. After `am force-stop --user 10` and a cold relaunch, Home displayed the restored due reminder. Accepting the Android permission prompt produced one active notification for the same reminder UUID in user 10, even though it had already been delivered under user 0. A further force-stop and cold launch preserved in-app due status and no active notification was observed in the five-second post-launch probe. This verifies fresh delivery in the restored profile and gives bounded restart evidence. A preexisting stale marker in an otherwise empty profile was not seeded or inspected in this signed exercise.

UI and picker actions were driven with `adb -P 5076 -s emulator-5580 shell input` and inspected with `uiautomator dump`. Python assertions compared the history, report, and JSON exports. No manual physical-device checks or Gradle tests ran in this pass. No SAF provider write fault was induced. A zero-byte document seen during test setup came from a shell transfer attempt while the recipient user was in the background. The app's actual export reported success and produced the verified nonempty file. The evidence logs, layout dumps, and snapshots are under `build/m46/isolated-acceptance.Bt4Agl/`. The disposable emulator was stopped with `adb -P 5076 -s emulator-5580 emu kill` and both user fixtures were retained on disk.

**M4 remains OPEN.** The physical gate still requires an isolated physical device for signed populated upgrade, offline cold launch, notification behavior and SAF recovery. Preserve the existing Redmi installations. Use a fixture with a standalone reading and recorded pre-upgrade UUIDs for full signed data-preservation evidence. The applicable physical release acceptance remains separate from these emulator results.

### M4.6 automated gap follow-up

On 2026-11-15, a new KVM-backed API 37 x86_64 Google Play AVD, `M46_Automated_Gaps`, booted under `build/m46/automated-gaps.pV4CWc/` (ADB server 5077, serial `emulator-5582`). It used the same isolated launch flags as the previous run, with a different name, port 5582, and fresh userdata. The earlier signed upgrade fixture and existing user AVD were retained. No commands targeted the Redmi. Preflight and final `bash build/m46/inputs.vA8Ful/verify-signed.sh` verified both signed APKs and the historical APK and schema checksums.

**Migration fixture — debug Room evidence.** The sole installed system image reports `ro.build.type=user` and `ro.debuggable=0`. On this disposable AVD, `adb root` refused root and `run-as org.amirariana.vehiclelog id` refused the nondebuggable release app. M3 has no standalone-reading creation or UUID export in its release UI. The deterministic fixture was therefore opened by the cached M4 debug APK, SHA-256 `b3d80f92f6a3da165bb7e66e326b7654ad412215ff34e96813f3c08a3e0e6432`. This exercise adds field-by-field Room evidence to the prior signed upgrade. A signed upgrade of the seeded fixture remains outstanding.

```bash
p=build/m46/automated-gaps.pV4CWc
adb() { "$HOME/Android/Sdk/platform-tools/adb" -P 5077 -s emulator-5582 "$@"; }
debug=org.amirariana.vehiclelog.debug
python3 "$p/v3-fixture.py" build \
    build/m46/inputs.vA8Ful/m3/app/schemas/org.unishiasy.vehiclelog.data.local.VehicleDatabase/3.json
adb shell -T "run-as $debug sh -c 'cat > databases/vehicle-ledger.db'" < "$p/fixture-v3.db"
adb exec-out "run-as $debug cat databases/vehicle-ledger.db" > "$p/device-before-v3.db"
cmp "$p/fixture-v3.db" "$p/device-before-v3.db"
adb shell am start -W -n "$debug/org.unishiasy.vehiclelog.MainActivity"
adb shell am force-stop "$debug"
adb exec-out "run-as $debug cat databases/vehicle-ledger.db" > "$p/device-after-v4.db"
adb exec-out "run-as $debug cat databases/vehicle-ledger.db-wal" > "$p/device-after-v4.db-wal"
python3 "$p/v3-fixture.py" compare "$p/device-before-v3.db" "$p/device-after-v4.db"
```

The pre/post inventories contain one vehicle, fuel and insurance entries at `9,223,372,036,854,775,807` and `2,001` legacy rials, and linked and standalone readings at 1,500 and 1,600 km. The known UUIDs, relationship fields, both event dates, creation timestamps, amounts, fuel volume, notes, and all other columns matched exactly. SQLite integrity and foreign-key checks passed. The user version changed from 3 to 4 and the Room identity changed. The app showed the fixture vehicle and 1,600 km current mileage. The database copies, WAL if present, and inventories are retained in the evidence directory.

**Stale delivery markers — debug end-to-end and instrumentation.** In a fresh Android user 10, `run-as org.amirariana.vehiclelog.debug --user 10` seeded valid one-byte delivery markers for the incoming reminder UUID `805717d1-4130-4942-9bad-076f3c360770` and an unrelated UUID ending in `000099` in `no_backup/reminder-delivery/`. The fixture also seeded notification opt-in and granted notification permission in that user. The database file was absent before the first launch. An unchanged byte copy of the test snapshot was staged in Downloads via MediaStore. The debug app restored it through the real SAF picker. The incoming reminder had a fixed 2020 due date so it was already due.

After the app reported restore success, the unrelated marker was gone. Android posted one notification tagged with the incoming UUID and a one-byte marker existed again for that UUID. A subsequent `am force-stop --user 10` and `am start --user 10 -W` showed the due reminder on Home, preserved the new marker, and produced no active notification for it in the eight-second post-launch probe. This exercised disk-backed marker reconciliation and a process restart in the debug variant. Separate debug instrumentation checked ordering at the scheduler boundary:

```bash
adb shell pm grant org.amirariana.vehiclelog.debug android.permission.POST_NOTIFICATIONS
adb shell am instrument -w -r -e class \
    org.unishiasy.vehiclelog.data.backup.BackupRestoreTest#successfulRestoreRemovesStaleActiveNotificationBeforeScheduling \
    org.amirariana.vehiclelog.debug.test/androidx.test.runner.AndroidJUnitRunner
```

Result: `OK (1 test)`, 0.782 s. It verified the seeded marker and stale active notification were removed before scheduling. No signed app private marker was seeded.

**SAF write failure — unmodified signed M4.** The disposable emulator installed the approved signed M4 APK and a separate debug-signed test-only `DocumentsProvider` APK (source and binary under the evidence directory's `failing-provider/`). That provider created the destination document, retained the first 16 bytes supplied through a proxy file descriptor, and returned `EIO`. Through the actual DocumentsUI Create Document picker, the signed app showed `خواندن یا ذخیرهٔ فایل انجام نشد. فایل یا محل دیگری انتخاب کنید.` and no success message. Test output checked `adb logcat -d -s M46Fault` for the injected error, and `adb exec-out run-as org.unishiasy.m46fault cat files/vehicle-ledger-backup.json` retrieved a 16-byte invalid JSON document. The provider left it behind. This is an observed provider-specific limitation. UI Automator produced transient setup timeouts and the guest showed a system-process responsiveness dialog. After selecting Wait, the signed app's error result was visible and recorded. No production code was changed.

**Disposition.** M4 remains OPEN. Field-by-field signed migration needs a disposable Android image with supported fixture access, or a separately authorized test harness that can seed the nondebuggable package. Physical acceptance still requires the signed populated upgrade, offline cold launch, notification paths, and SAF export and recovery on an isolated physical device. The emulator and instrumentation results above preserve prior signed evidence and do not satisfy the physical gate.

### M4.6 signed fixture feasibility recheck

`bash build/m46/inputs.vA8Ful/verify-signed.sh` passed again for both approved APKs, their source manifests, retained M1 APKs, and Room schemas. `find "$HOME/Android/Sdk/system-images" -name source.properties` returned only the API 37 Google Play x86_64 image. The prior disposable-emulator probes remain applicable: `adb -P 5077 -s emulator-5582 root` refused root on the production image, and `adb -P 5077 -s emulator-5582 shell run-as org.amirariana.vehiclelog id` refused the nondebuggable package. Neither probe was repeated on the retained fixture.

Inspection of the matching M3 source (`git grep --no-index` for `createStandaloneReading`, `UUID.randomUUID`, and the SAF document contracts) found standalone creation only in the repository and generated IDs in the supported vehicle and entry workflows. No M3 picker supports fixture import or pre-upgrade identifier export. `aapt2 dump xmltree --file AndroidManifest.xml` on the signed M3 APK confirmed `allowBackup=false` and `fullBackupContent=false`.

Stopped before a new install or upgrade. The previous signed run verifies the vehicle, two displayed entries, linked reading, monthly total, and persistence after cold restart. Exact pre/post UUIDs, standalone readings, and raw field equality remain unverified on signed APKs. The debug fixture remains separate evidence. A disposable image that supports explicit test-fixture access would allow the same v3 database to be inventoried before and after the signed upgrade. M4 remains OPEN, including the physical gate.

### M4.6 signed database integrity acceptance

On 2026-11-15, the approved AOSP package `system-images;android-35;default;x86_64` revision 2 (extension 13) was installed from Google's `x86_64-35_r02.zip`. The 782,404,023-byte archive passed catalog SHA-1 `2d857d170c0d1b827149565da34b3383e5306f7f`. No other package was installed. Emulator 37.2.13 passed KVM acceleration checks. The fresh `M46_AOSP35_Integrity` AVD used 2 CPUs, a 6 GiB userdata partition, separate AVD and emulator homes, console 5584, and ADB server 5085. The guest reported boot completion 1, API 35, userdebug, and debuggable 1. `adb -P 5085 -s emulator-5584 root` reported `restarting adbd as root`; `shell id -u` returned 0 and `shell id` reported `uid=0(root)`.

Exact executable commands, raw output, and inventories are retained under `build/m46/aosp.K9wXV1/` in `start-emulator.sh`, `signed-migration.sh`, `database.py`, `boot.log`, and `signed-migration.log`. The two APK paths and hashes in the independent signer table above were reverified before and after this run with `bash build/m46/inputs.vA8Ful/verify-signed.sh`.

Signed M3 created the empty Room v3 database. After force-stop, the script copied the main file and any nonempty WAL, used SQLite's backup API to stage a standalone copy, and inserted only the synthetic business rows with foreign keys enabled. It retained the v3 metadata and identity. The staged file was written into the original database inode while M3 was stopped, after removing only the copied fixture's old sidecars on the disposable guest. Owner, group, mode and SELinux context matched before and after staging. Signed M3 reopened the seeded database and its pre-upgrade readback matched the fixture before upgrade.

`adb -P 5085 -s emulator-5584 install -r` upgraded `org.amirariana.vehiclelog` from 3 / 0.2.0 to 4 / 0.3.0. Android app ID 10148 and first-install time `2026-11-15 06:14:20` were unchanged. The pre/post inventories matched byte for byte, SHA-256 `8d4f92c4d10a64eb17bf11eb43f59f5b811f2afde96fee7f5de7184dfe32bb60`. Every vehicle, entry and reading field matched, including the five known UUIDs, NULLs, dates and timestamps, exact amounts of 9,223,372,036,854,775,807 and 2,001 rials, linked 1,500 km and standalone 1,600 km. SQLite integrity and foreign-key checks passed on both copies. The post-upgrade database had user version 4, the exported Room v4 identity, an empty reminders table, and both expected reminder validation triggers.

After force-stop, M4 cold-launched with no active default network and retained the seeded vehicle and the exact two-entry Farvardin 1405 total of 922,337,203,685,477,780.8 toman. All captured report labels matched M3. The AVD was stopped with its fixture retained. No physical device or existing user AVD was targeted. The signed database gate passed and M4 remains OPEN for physical-device acceptance.

### M4 development handoff

**Development complete. Physical release acceptance DEFERRED / NOT VERIFIED.** The product owner approved starting M5 development while the physical release gate stays open. The signed APK verification and isolated emulator evidence above retain their original results. Public release readiness is unestablished.

The outstanding gate is a populated signed upgrade, offline cold launch, reminder lifecycle and notifications, and SAF export and restore on an isolated physical device. Record permission denial and grant where supported, confirm the exported point in time on a separate empty installation, and verify populated-ledger rejection. Preserve the existing Redmi installations. [Deferred gate and completion criteria](BACKLOG.md#deferred-m4-physical-release-acceptance)

### M4.6 physical acceptance preflight

Re-ran `bash build/m46/inputs.vA8Ful/verify-signed.sh` with Build Tools 36 and Corretto 17. M3 code 3 / 0.2.0 and M4 code 4 / 0.3.0 verified as `org.amirariana.vehiclelog`, each with one signer, valid v2/v3 signatures, and the recorded APK and certificate SHA-256 values. The retained artifact and Room schema checks passed. `adb -s 4c1ad625 get-state` returned device not found, and `adb devices -l` returned no devices. Historical M1 release and separate debug identities remain context until reverified on the connected phone.

After reconnect, verify the serial, Android build, current user, both package IDs and app UIDs, and all users where release is installed. Pull only the release base APK and compare it to the retained M1 APK and test certificate. Android shares a package binary across its installed users. Proceed with the proposed user-0 preparation only if release installs in other profiles will not be affected.

The proposed destructive step, pending explicit authorization for this command, is:

```bash
"$HOME/Android/Sdk/platform-tools/adb" -s 4c1ad625 shell pm clear --user 0 org.amirariana.vehiclelog
```

This clears the release app's user-0 private ledger, preferences, and cache, while retaining the package installation and leaving the debug app and independent SAF documents alone. Confirm success before installing the retained M3 APK in place on user 0. Seed a fresh M3 ledger and record its observable fields before installing M4 in place without a data clear. Use a verified empty and authorized profile or separate target for restore, retaining the upgraded ledger. Keep any profile actions within the owner-approved test environment. The ADB preflight stopped before any state-changing action, and physical acceptance remains open.

**Live reconnect preflight.** The owner conditionally approved the exact one-time clear above. Read-only checks:

```bash
adb="$HOME/Android/Sdk/platform-tools/adb"
"$adb" devices -l
"$adb" -s 4c1ad625 get-state
"$adb" -s 4c1ad625 shell 'getprop ro.product.model; getprop ro.build.version.release; getprop ro.build.version.sdk; am get-current-user; pm list users'
"$adb" -s 4c1ad625 shell dumpsys package org.amirariana.vehiclelog
"$adb" -s 4c1ad625 shell dumpsys package org.amirariana.vehiclelog.debug
"$adb" -s 4c1ad625 shell pm path --user 0 org.amirariana.vehiclelog
```

Verified Redmi Note 9S (curtana), Android 12/API 31, current user 0. Both packages were installed only for user 0. User 10 Security Space reported `installed=false` and `ceDataInode=0` for both. Release was code 2 / 0.1.1, UID 10472. Debug was code 1 / 0.1.0, UID 10470, and was inspected only through package metadata. Pulled the release `base.apk` to `/tmp/m46-redmi.8ueaRj/base.apk`. `cmp` against the retained M1 code-2 APK passed. SHA-256 was `1761b660c8bfd33c7c688189e5e87d3c9f953da6c556ca10a4451b3330fc310c`. `apksigner verify --print-certs` returned the expected matching M3/M4 certificate SHA-256 `4da34964d2bd5de0bf98713e02b240fdb9438d18964d5d0c9efad248f1a8748a`. The retained signed verification script passed again.

`adb -s 4c1ad625 shell input keyevent KEYCODE_WAKEUP` did not leave the device ready for UI testing. A subsequent `dumpsys window policy` showed secure keyguard, screen off and sleeping. Paused before clearing or installing. The approved clear remains unused. User 10 was inspected for package state only. It has not been accepted as the restore target. No physical acceptance checks passed in this preflight.

**Unlocked-device follow-up.** ADB reported serial `4c1ad625` attached over USB. Live checks showed `mWakefulness=Awake`, keyguard `showing=false`, and user 0 active. Both package identities and user states matched the reconnect preflight. A new `adb pull` of the user-0 release base APK matched the previously verified M1 release byte for byte. The M3 and M4 SHA-256 values matched the retained signed artifact record.

The exact approved `"$HOME/Android/Sdk/platform-tools/adb" -s 4c1ad625 shell pm clear --user 0 org.amirariana.vehiclelog` command returned exit 255. Android rejected it with `SecurityException` because the shell lacked `android.permission.CLEAR_APP_USER_DATA` for this package. The install command did not run. Follow-up `dumpsys package` comparisons showed unchanged version, UID and per-user data inode for release (code 2, UID 10472, owner inode 826348) and debug (code 1, UID 10470, owner inode 815676). Neither package was installed for user 10. Evidence retained under `build/m46/redmi-physical.QrxFkv/`. No alternate destructive command or Settings action was attempted. Physical acceptance remains open.

**Owner clear and signed M3 start.** A second exact ADB clear received the same permission denial. Opened only the release App Info with `adb -s 4c1ad625 shell am start --user 0 -W -a android.settings.APPLICATION_DETAILS_SETTINGS -d package:org.amirariana.vehiclelog`. The owner reported clearing all release app data. The Android event log recorded `stop org.amirariana.vehiclelog due to clear data` for user 0. Package metadata still matched the debug baseline, including its UID and private-data inode. The retained M3 was installed with `adb -s 4c1ad625 install -r --no-streaming --user 0 build/m46/inputs.vA8Ful/m3/build/releases/3/vehiclelog-3-0.2.0.apk` and returned `Success`. `adb -s 4c1ad625 shell am start --user 0 -W -n org.amirariana.vehiclelog/org.unishiasy.vehiclelog.MainActivity` cold-launched it successfully. Package metadata verified code 3 / 0.2.0 and the empty-ledger screenshot is `build/m46/redmi-physical.QrxFkv/m3-start.png`. MIUI denied `adb -s 4c1ad625 shell input tap 500 752` with `INJECT_EVENTS`. Stopped before adding data or attempting the M4 update. The physical gate remains open pending the populated upgrade and other acceptance checks.

### M4 Redmi fresh-install smoke test

The owner stopped the populated M3 fixture exercise and requested installation and testing of the latest build. The latest retained signed offline APK is M4 `0.3.0`, code 4. The later M5.4 Android APK uses the debug package for local cloud testing. Live preflight found the release package absent from both user 0 and user 10. The debug package was present on user 0 and remained untouched.

```bash
adb="$HOME/Android/Sdk/platform-tools/adb"
apk=build/m46/inputs.vA8Ful/m4/build/releases/4/vehiclelog-4-0.3.0.apk
"$adb" -s 4c1ad625 install --no-streaming --user 0 "$apk"
"$adb" -s 4c1ad625 shell am start --user 0 -W -n org.amirariana.vehiclelog/org.unishiasy.vehiclelog.MainActivity
"$adb" -s 4c1ad625 shell am force-stop --user 0 org.amirariana.vehiclelog
"$adb" -s 4c1ad625 shell am start --user 0 -W -n org.amirariana.vehiclelog/org.unishiasy.vehiclelog.MainActivity
```

Install returned `Success`. The pulled installed base APK matched the verified retained APK byte for byte. Release was code 4 / 0.3.0, UID 10474, installed only for user 0. The debug version, UID and per-user data inodes matched before and after. Both launches returned `Status: ok` and `LaunchState: COLD` (517 ms and 354 ms reported total launch times). The foreground activity was the release app in both runs. The relaunch screenshot showed Persian RTL Home with the empty vehicle list, Add Vehicle, and Settings. Evidence is under `build/m46/redmi-m4-smoke.a9bu4u/`.

Fresh install and launch smoke test passed on Redmi Note 9S, Android 12/API 31. The populated physical M3→M4 upgrade, offline launch, reminders, notifications and SAF recovery were outside this smoke run. The physical acceptance gate remains open.

## Android offline MVP 0.3.1 release candidate

**Scope audit: PASS for the unsigned artifact.** The product owner accepted the Slice 1 UI hardening and allowed the dormant M5 implementation to remain in the APK. The release build compiles with `LOCAL_CLOUD_ENABLED=false`. Both repository initialization and access to the cloud Settings UI are gated. The DEX retains the loopback endpoint literal. AAPT2 verified the absence of `INTERNET`, a release network security config, a cleartext override, and any cloud component in the merged manifest. The permissions match signed M4 exactly, including the existing WorkManager permissions for local reminder delivery. The release package is nondebuggable and retains `allowBackup=false`, `fullBackupContent=false`, and the cloud-backup and device-transfer exclusions. The codec and all four Room schemas match the retained M4 sources. The shared M5 backup repository and Settings wiring are also included in the audit.

**Source provenance.** The workspace has an unborn Git HEAD. Captured 118 build inputs, including Gradle files and wrapper, Android sources and tests, Room exports and the release script, in `build/offline-rc5.U4tGuX/source/`. The SHA-256 of the sorted per-file manifest `build-inputs.sha256` is `ebd7330805ad26150e0faa5d1a438f2675c39563d352304fb6746907e72352ce`. Relative to the retained signed M4 source, the app differences are the four accepted UI files and the Home and Ledger tests, plus M5's flag, debug manifest and debug network resource, cloud implementation and tests, and shared `AppContainer`, `MainActivity`, `BackupRepository` and `BackupSettingsScreen` changes. The build dependency declarations match M4. No production source change was made for this candidate.

**Unsigned artifact.** `build/offline-rc5.U4tGuX/source/app/build/outputs/apk/release/app-release-unsigned.apk` is `org.amirariana.vehiclelog`, code 5, name `0.3.1`, min SDK 26 and target SDK 36. SHA-256 `15ee5d69e4f25be3b357dcf5ca56ba4a35ba4b3874b102cdd21d39437e58d3b5`. ZIP alignment passed. The expected signing certificate remains `4da34964d2bd5de0bf98713e02b240fdb9438d18964d5d0c9efad248f1a8748a`. The historical M3 and M4 signatures, APK hashes and alignment, the two M1 APK hashes, and all Room schema hashes passed the retained verifier.

With the project root as the initial working directory, the release audit used:

```bash
rc="$PWD/build/offline-rc5.U4tGuX"
export JAVA_HOME=/usr/lib/jvm/java-17-amazon-corretto ANDROID_HOME="$HOME/Android/Sdk"
export GRADLE_USER_HOME="$PWD/.gradle/local-user-home"
scripts/release-apk.sh --check 5 0.3.1
bash build/m46/inputs.vA8Ful/verify-signed.sh
cd "$rc/source"
./gradlew --offline --no-daemon --console=plain --max-workers=4 \
    -Pandroid.builder.sdkDownload=false -Porg.gradle.java.installations.auto-download=false \
    -PvehiclelogReleaseVersionCode=5 -PvehiclelogReleaseVersionName=0.3.1 :app:assembleRelease
apk=app/build/outputs/apk/release/app-release-unsigned.apk
tools="$ANDROID_HOME/build-tools/36.0.0"
"$tools/aapt2" dump badging "$apk"
"$tools/aapt2" dump permissions "$apk"
"$tools/aapt2" dump xmltree --file AndroidManifest.xml "$apk"
"$tools/zipalign" -c -P 16 4 "$apk"
sha256sum "$apk"
sha256sum --quiet -c "$rc/build-inputs.sha256"
```

The initial sandboxed build could not start Gradle's local lock service because no usable wildcard IP was available. The host offline build succeeded in 2m 4s with 52 tasks executed. The preparation used the installed Gradle and SDK tools. The standalone Android CLI was unavailable.

**Version and signing gate.** The product owner confirmed that code 5 was never assigned to another Vehicle Ledger release APK, including outside the workspace, and approved it for the offline MVP candidate `0.3.1`. No existing code 5 output was present at preflight. The retained unsigned APK and all 118 captured inputs passed checksum verification. Its 16 KiB ZIP alignment, release identity, manifest and permissions passed reinspection. The historical M1, M3 and M4 APK and Room v1–v4 checksum checks also passed. Physical acceptance remains **DEFERRED / NOT VERIFIED**.

The normal `scripts/release-apk.sh` invokes Gradle before signing. For this retained candidate, the following command uses the same developer-owned PKCS12 keystore, alias, certificate and SDK signer on the already-aligned APK. It preserves the reviewed APK and source bytes and writes to a separate immutable release directory. Run the signing step in a private local Bash terminal and enter the password only at the signer prompt.

```bash
rc='/home/amir/Documents/ChatGPT/Project UNI Shiasy/build/offline-rc5.U4tGuX'
bash "$rc/sign-retained-rc5.sh" --check
bash "$rc/sign-retained-rc5.sh"
bash "$rc/sign-retained-rc5.sh" --verify
```

Preflight passed and is recorded in `build/offline-rc5.U4tGuX/rc5-signing-preflight.log`. The helper uses `apksigner sign --ks <developer-owned-path> --ks-type PKCS12 --ks-key-alias vehiclelog-m1-test --ks-pass stdin --v4-signing-enabled false --out <staged-signed-apk> <retained-unsigned-apk>` with terminal echo disabled. It checks the v2 and v3 signatures, single signer certificate, alignment, release manifest, M4 permission list, and original APK entry bytes before moving the signed output to `build/releases/5/vehiclelog-5-0.3.1.apk` without overwriting an existing version. The `--verify` step repeats the checks against the retained inputs and checks the signed SHA-256 sidecar and the historical artifacts.

**Signed RC5 verification passed.** The developer signed privately and reported the signed checksum. The independent check ran `bash build/offline-rc5.U4tGuX/sign-retained-rc5.sh --verify` from the project root and recomputed the same SHA-256. Full output is in `build/offline-rc5.U4tGuX/rc5-signed-verification.log`.

| Field | Verified value |
|---|---|
| Artifact | `build/releases/5/vehiclelog-5-0.3.1.apk` |
| Signed SHA-256 | `7c13b93e04826b49c3c75671e20949bfa30a6ec940c1f01825d0845ff94cc07f` |
| Size | 8,920,405 bytes |
| Package and version | `org.amirariana.vehiclelog`, version code 5, version name `0.3.1` |
| SDK range metadata | min SDK 26, target SDK 36 |
| Signature | APK Signature Schemes v2 and v3 verified, one signer |
| Signer certificate SHA-256 | `4da34964d2bd5de0bf98713e02b240fdb9438d18964d5d0c9efad248f1a8748a` |
| Alignment | `zipalign -c -P 16 4` passed |

All 138 original APK ZIP entries matched the retained unsigned payload byte for byte. AAPT2 confirmed the same permissions as signed M4 and the same manifest as the audited unsigned candidate. There is no `INTERNET` permission, release network security config, cleartext override or registered cloud component. The build retains disabled M5 code. The retained unsigned SHA-256, all 118 captured inputs, the M1, M3 and M4 APK checksums and Room schema v1–v4 checksums passed again.

The initial Android instrumentation run recorded 41 passes, one Home mileage assertion failure and one expected skip. The assertion raced the separate Room mileage flow, and the unchanged retry passed. Home can briefly show the empty-mileage message while the flow loads. The evidence shows no persistent mileage defect. The signed RC5 was not installed or run during artifact verification. It is available for controlled testing on an authorized isolated target. Physical acceptance remains **DEFERRED / NOT VERIFIED**, and public release readiness is unestablished.

**Development closeout.** The [six-criterion Android Offline MVP review](../../PLANS.md#android-offline-mvp-development-closeout) passed. Reverification with `bash build/offline-rc5.U4tGuX/sign-retained-rc5.sh --verify` repeated the artifact and provenance checks. The [focused populated M4→RC5 physical upgrade and offline cold launch](#populated-redmi-rc5-upgrade-pass) subsequently passed. The [M4 physical gate](BACKLOG.md#deferred-m4-physical-release-acceptance) remains separately deferred and open. Public release readiness remains unestablished.

### Focused Redmi M4-to-RC5 upgrade

**BLOCKED · Missing populated baseline.** Redmi Note 9S (`curtana`), serial `4c1ad625`, returned ADB state `device`, Android 12/API 31 and current user 0. Release `org.amirariana.vehiclelog` was `0.3.0` code 4, UID 10474, data inode 846827, and first-install time `2026-11-16 13:07:08` in device local time. The separate debug package was `0.1.0` code 1, UID 10470 and data inode 815676. For both packages, user 0 reported `installed=true` and user 10 Security Space reported `installed=false` with data inode 0.

The RC5 APK matched SHA-256 `7c13b93e04826b49c3c75671e20949bfa30a6ec940c1f01825d0845ff94cc07f`, v2/v3 signatures, certificate SHA-256 `4da34964d2bd5de0bf98713e02b240fdb9438d18964d5d0c9efad248f1a8748a`, 16 KiB alignment, package and version. The pulled M4 base APK matched the retained signed code 4 APK byte for byte. Its SHA-256 was `c95be26d57ce0b0f3450944eed8ef22bdfbddbf737a7eb69557d1150c85c2f22`, and its verified signer matched RC5.

```bash
adb=( "$HOME/Android/Sdk/platform-tools/adb" -s 4c1ad625 )
tools="$HOME/Android/Sdk/build-tools/36.0.0"
export JAVA_HOME=/usr/lib/jvm/java-17-amazon-corretto
evidence=build/redmi-rc5.IRmqhJ
apk=build/releases/5/vehiclelog-5-0.3.1.apk
sha256sum "$apk"
"$tools/apksigner" verify --verbose --print-certs "$apk"
"$tools/zipalign" -c -P 16 4 "$apk"
"$tools/aapt2" dump badging "$apk"
"${adb[@]}" get-state
"${adb[@]}" shell 'getprop ro.product.model; getprop ro.product.device; getprop ro.build.version.release; getprop ro.build.version.sdk; am get-current-user; pm list users'
"${adb[@]}" shell dumpsys package org.amirariana.vehiclelog
"${adb[@]}" shell dumpsys package org.amirariana.vehiclelog.debug
remote=$("${adb[@]}" shell pm path --user 0 org.amirariana.vehiclelog | sed -n 's/^package://p' | tr -d '\r')
"${adb[@]}" pull "$remote" "$evidence/installed-m4-base.apk"
cmp "$evidence/installed-m4-base.apk" build/m46/inputs.vA8Ful/m4/build/releases/4/vehiclelog-4-0.3.0.apk
"$tools/apksigner" verify --print-certs "$evidence/installed-m4-base.apk"
"${adb[@]}" shell am start --user 0 -W -n org.amirariana.vehiclelog/org.unishiasy.vehiclelog.MainActivity
"${adb[@]}" exec-out screencap -p > "$evidence/m4-home.png"
```

The M4 launch returned `Status: ok` and `LaunchState: HOT` and resumed the release app. The screenshot under the evidence directory showed Persian RTL Home with «هنوز خودرویی ثبت نکرده‌اید.» and the Add Vehicle action. No vehicle, expense, mileage or dashboard baseline was available. No installation, force-stop, data clear, removal, network change or debug app interaction was performed. The focused upgrade and offline persistence check remain open. The separate M4 physical gate also remains **DEFERRED / NOT VERIFIED** and open.

#### Populated Redmi baseline follow-up

**BLOCKED · Fuel date differs from the expected fixture.** Repeated `adb -s 4c1ad625 get-state`, the device, user, package and base APK checks above. Redmi Note 9S `curtana` returned API 31 and active user 0. Release remained code 4 / `0.3.0`, UID 10474, first-install time `2026-11-16 13:07:08`, and user-0 data inode 846827. Debug remained code 1 / `0.1.0`, UID 10470 and user-0 data inode 815676. Neither package was installed for user 10. The pulled release APK again matched retained M4 byte for byte. Its certificate and RC5's certificate matched the approved fingerprint, and the RC5 digest and package metadata matched the release record.

The same `am start --user 0 -W` command returned `Status: ok` and `LaunchState: WARM`. The explicit-serial `shell input tap 535 1122` attempt returned exit 255 and `INJECT_EVENTS`. The owner opened History manually. `adb -s 4c1ad625 exec-out screencap -p` captured the stable before screens in `build/redmi-rc5-upgrade.ZOWcjD/m4-home.png` and `m4-history.png`. The same directory retains the package, launch and connectivity command output.

| Field | Expected | Observed on signed M4 |
|---|---|---|
| Vehicle and model year | پژو ۲۰۶ تست, 1400 Solar Hijri | Matched |
| Current mileage | 15,000 km | Matched |
| Fuel date | `1405/08/20` | `1405/08/25` (25 Aban 1405) |
| Fuel values | 150,000 toman, 30 liters, linked 15,000 km | Matched |
| Wash | `1405/08/21`, 50,000 toman | Matched |
| Aban dashboard total | 200,000 toman | Unverified because the check stopped at the date mismatch |

Read-only connectivity inspection found airplane mode disabled, Wi-Fi and mobile data preferences 0, and no active default network. The offline RC5 cold launch was not run. No install, data clear, uninstall, app data write, debug app interaction or connectivity change was performed. The focused upgrade and the separate M4 physical release gate remain open.

#### Populated Redmi RC5 upgrade pass

The product owner accepted the observed `1405/08/25` fuel date for the fixture. Rechecked that the Redmi was on M4 code 4 with release installed for user 0 only, checked the RC5 SHA-256, and captured the M4 Aban dashboard before installation. The document trail is retained under `build/redmi-rc5-upgrade.ZOWcjD/`.

```bash
adb=( "$HOME/Android/Sdk/platform-tools/adb" -s 4c1ad625 )
app=org.amirariana.vehiclelog
apk="$PWD/build/releases/5/vehiclelog-5-0.3.1.apk"
evidence=build/redmi-rc5-upgrade.ZOWcjD
"${adb[@]}" exec-out screencap -p > "$evidence/m4-aban.png"
"${adb[@]}" install -r "$apk"
"${adb[@]}" shell dumpsys package "$app"
"${adb[@]}" shell dumpsys package "$app.debug"
remote=$("${adb[@]}" shell pm path --user 0 "$app" | sed -n 's/^package://p' | tr -d '\r')
"${adb[@]}" pull "$remote" "$evidence/rc5-installed.apk"
cmp "$evidence/rc5-installed.apk" "$apk"
sha256sum "$evidence/rc5-installed.apk"
"${adb[@]}" shell am force-stop --user 0 "$app"
"${adb[@]}" shell pidof "$app"
"${adb[@]}" shell am start --user 0 -W -n "$app/org.unishiasy.vehiclelog.MainActivity"
```

Install returned `Success`. The pulled APK matched the signed RC5 bytes. Release advanced to code 5 / `0.3.1`, retained UID 10474, first-install time `2026-11-16 13:07:08` and user-0 data inode 846827. The debug before/after metadata files matched byte for byte. Neither package was installed for user 10. The release process was absent after force-stop. Launch returned `Status: ok`, `LaunchState: COLD`, and `TotalTime: 354` ms. Android reported no active default network before launch, after launch, and on the report screen. Wi-Fi and mobile data settings were already 0 and airplane mode was off. No connectivity settings changed.

| Observable value | Signed M4 baseline | Signed RC5 after offline cold launch |
|---|---|---|
| Vehicle | پژو ۲۰۶ تست, model year 1400 Solar Hijri | Matched |
| Current mileage | 15,000 km | Matched |
| Fuel | 25 Aban 1405, 150,000 toman, 30 liters, linked 15,000 km | Matched |
| Wash | 21 Aban 1405, 50,000 toman | Matched |
| Aban 1405 report | 200,000 toman, two entries, zero unpriced. Fuel 150,000 and wash 50,000 toman. | Matched |

The owner navigated History and the dashboard because MIUI blocked shell touch injection. Persian RTL Home, History and the report were captured using `adb -s 4c1ad625 exec-out screencap -p` in the evidence directory as `rc5-offline-home.png`, `rc5-offline-history.png` and `rc5-offline-aban.png`. No hang or crash was observed in the exercised flow. This check covers UI-visible fixture data. It did not inspect internal database identifiers or exercise M3→M4 on physical hardware, reminder and notification behavior or SAF recovery. The broader [M4 physical release gate](BACKLOG.md#deferred-m4-physical-release-acceptance) remains **DEFERRED / NOT VERIFIED** and open.
