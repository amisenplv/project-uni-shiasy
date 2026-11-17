# ADR 0001 · Single-module Android with Room

**Status:** Approved technical baseline. Implementation awaits M1 approval.

**Decision:** Build one Kotlin and Jetpack Compose Android module. Use screen-level ViewModels with `StateFlow`, hand-wired concrete repositories, and Room for the local ledger.

The direct APK target and solo capacity favor one build pipeline and a small dependency graph. Native Android provides platform storage, notifications, and install testing. Room gives the ledger transactional writes, explicit schema evolution, and device-level migration tests. Add modules or dependencies when a specific need justifies them.

Start with JDK 17, AGP 8.13.2, Gradle Wrapper 8.13, minimum API 26, and compile and target API 36. Pin compatible Kotlin, Compose, SDK build tools, and later KSP and Room at their milestone gates. Verify the tuple on the actual build host. Keep business logic independent of Compose and persistence details where it improves testability. [AGP compatibility](https://developer.android.com/build/releases/agp-8-13-0-release-notes), [Android architecture guidance](https://developer.android.com/topic/architecture/recommendations)
