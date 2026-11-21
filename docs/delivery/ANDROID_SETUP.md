# Local Android build and checks

## Prerequisites

- Use a local JDK 25 installation for the Gradle daemon. The wrapper uses the daemon JVM criteria in `gradle/gradle-daemon-jvm.properties`. Java and Kotlin still target JVM 17.
- Use the local Android SDK with base Android Platform 36, Build Tools 36.0.0, and Platform Tools. If Android Studio writes an ignored `local.properties` file, set its `sdk.dir` to the same SDK as `ANDROID_HOME`.
- Run the project with its Gradle Wrapper. The project pins Gradle 9.6.0 and AGP 9.5.2, uses built-in Kotlin, and pins the Compose compiler plugin to 2.3.10. Use a Gradle cache containing the wrapper distribution and pinned project dependencies. The check script defaults to the ignored `.gradle/local-user-home` cache, or honors an explicit `GRADLE_USER_HOME`. [Android build JDK guidance](https://developer.android.com/build/jdks)

On the development machine, set the paths for the current terminal session.

```bash
export JAVA_HOME=/path/to/jdk-25
export ANDROID_HOME="$HOME/Android/Sdk"
bash scripts/check-android.sh
```

The script validates the JDK and SDK, then runs the debug build, lint, JVM test task, and instrumented APK build. It uses Gradle offline mode by default. The wrapper needs its distribution in the cache before Gradle offline mode takes effect. Verify an uncached distribution against the pinned wrapper SHA-256 and Gradle's release checksum before using it. With approval to resolve missing project dependencies from the configured repositories, use `bash scripts/check-android.sh --online`. The script disables automatic JDK and SDK provisioning. [Gradle release checksums](https://gradle.org/release-checksums/)

Debug builds use Android’s standard debug keystore by default. If an existing `debug.keystore` is present at the repository root, the build uses it for continuity with earlier debug APKs.

## Physical-device check

Connect and authorize only the intended Android phone over USB, then run `bash scripts/check-android.sh --device`. The connected task installs the debug and instrumented APKs. If the installed debug APK uses a different certificate, resolve signing compatibility before replacing or uninstalling it. Record the phone model, Android version, and test result in [PLANS.md](../../PLANS.md).

Check the real Home screen with the system set to English. Verify offline cold launch, rotation, and enlarged text separately from the instrumented Persian RTL assertion.

## Inspect the debug backup policy

After a successful build, inspect the merged debug manifest in Android Studio and verify the packaged manifest and rules with the local Build Tools.

```bash
apk=app/build/outputs/apk/debug/app-debug.apk
aapt2="$ANDROID_HOME/build-tools/36.0.0/aapt2"
"$aapt2" dump xmltree --file AndroidManifest.xml "$apk"
"$aapt2" dump xmltree --file res/xml/data_extraction_rules.xml "$apk"
```

Check that `allowBackup` and legacy `fullBackupContent` are false, `dataExtractionRules` points to the packaged rules, and cloud backup and device transfer each exclude the app storage domains. Actual backup and transfer testing with a populated ledger belongs to M4. [Android backup rules](https://developer.android.com/identity/data/autobackup)
