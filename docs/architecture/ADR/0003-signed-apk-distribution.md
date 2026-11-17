# ADR 0003 · Direct signed APK for the pilot

**Status:** Approved technical baseline. The release application ID `org.amirariana.vehiclelog` is provisional. The developer must explicitly confirm the permanent ID before the first public release. Signing setup and pilot channel eligibility are still to be confirmed.

**Decision:** Build a signed APK in M1 and verify an in-place update on the developer's physical Android phone. Qualify the actual download and install channel in M6. Use a separate `.debug` application ID for development.

The provisional release ID produces the debug ID `org.amirariana.vehiclelog.debug`. The Kotlin namespace and source package remain `org.unishiasy.vehiclelog`. Android treats the new debug ID and the earlier `org.unishiasy.vehiclelog.debug` ID as separate installations.

The developer controls the release key and encrypted backup outside the repository. Keep the application ID and signing certificate stable across releases, increment `versionCode`, and record the source revision, certificate fingerprint, and APK checksum. Verify each release with `apksigner` and exercise a signed update over existing records.

Pin the build inputs and document the release commands. Direct distribution requires checking the install path and current regional developer verification rules on the pilot devices. [Android app signing](https://developer.android.com/studio/publish/app-signing), [APK signature verification](https://developer.android.com/tools/apksigner), [Android developer verification](https://developer.android.com/developer-verification)
