# DivineTube — current state snapshot

**Updated:** 2026-11-10 (Asia/Calcutta)

**Phase:** Universal family redesign implemented (version 0.2.0); device/API/release verification blocked.

**Active task:** FAM-05: Documentation, build/test verification, secret scan, commit and push.

**Current platform/owner:** Antigravity orchestrator. Shared documentation remains main-agent-owned.

**Repository:** Public at https://github.com/Yavagu/DivineTube, branch main tracking origin/main. Prior release 0.1.0 committed as 0fcf1de and 6e85e78; universal family redesign (version 0.2.0) ready to push.

## Implemented
- Universal family Home: single shared Viewer Mode Home for all family members, large cards, featured entry banner, Continue Watching (based on locally stored last-opened episode), and ordered category sections.
- Hidden Admin Mode: PIN-gated admin console with approved content management, manual and opt-in automatic playlist updates, category ordering, and YouTube intake.
- Room v1→v2 migration: additive migration preserving existing categories, approved videos/playlists, ordering, and positions while introducing `featured`, `refreshMode`, and foreign-key cascade `watch_history`.
- Safe playlist refresh policy: manual by default, opt-in automatic refresh opportunistically on Admin Mode open without background services or extra permissions.
- Last-opened episode memory: persists entry/video/episode ID and timestamp to enable resuming series from Home without claiming unverified percentage-based playback time.
- Terminology alignment: DivineTube, Viewer Mode, Admin Mode, Approved Content, Home, preserving all existing package/application/DB IDs and Android Keystore aliases.

## Verified build evidence
- JDK 21.0.8; Gradle 8.13; SDK 36.1; compile 36.1 / target 36 / min 26.
- `gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest` passed (exit 0).
- 17 JVM tests passed. Lint: 0 errors, dependency version notices only. Android test APK compiled.
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk` (versionCode 2, versionName 0.2.0).
- Schema v2 exported at `app/schemas/com.yourtube.app.data.local.LibraryDatabase/2.json`.
- `git diff --check` passed cleanly; secret/credential pattern scan passed.
- `connectedDebugAndroidTest` remains blocked pending connected device/AVD.

## Genuine blockers / limitations
- No connected Android device/emulator: UI, startup, actual Keystore/Room persistence, lifecycle, and player runtime checks remain open.
- No YouTube Data API key supplied: real search and playback eligibility/embedded playback remain untested. Keyless approvals are supported; keyless playback is intentionally blocked by YouTube's mandatory Made for Kids lookup.
- No child-directed Google registration, privacy-policy publication, legal/retention/branding audit, or real-player navigation-escape acceptance performed. Not release-ready and not a complete safety boundary.

## Changed paths / handoff
Updated `PROJECT_SPEC.md`, `PLAN.md`, `README.md`, `DECISIONS.md`, `docs/PRIVACY.md`, `app/build.gradle.kts`, `AndroidManifest.xml`, `AppViewModel.kt`, `YourTubeApplication.kt`, `LibraryDatabase.kt`, `LibraryRepository.kt`, `PlayerScreen.kt`, `YourTubeApp.kt`, `NavigationTest.kt`, `LibraryRepositoryTest.kt`, and added `2.json` schema and `LibraryMigrationTest.kt`.
