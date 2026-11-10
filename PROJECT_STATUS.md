# DivineTube — current state snapshot

**Updated:** 2026-11-10 (Asia/Calcutta)

**Phase:** Tap-to-fullscreen landscape playback implemented and verified via unit tests and build tasks; physical device runtime verification pending operator setup.

**Active task:** PLAY-02 VERIFIED for source, build, unit and androidTest compilation; device runtime pending. PLAY-01 remains pending device confirmation.

**Current platform/owner:** Antigravity orchestrator. Shared documentation remains main-agent-owned.

## Fullscreen playback implementation (2026-11-10)
- Implemented YouTube-style tap-to-fullscreen playback: enabled official iframe parameter `fs=1`, added `allow="encrypted-media; picture-in-picture; fullscreen" allowfullscreen`, and implemented `WebChromeClient` lifecycle callbacks (`onShowCustomView` and `onHideCustomView`).
- Created Activity-owned `DefaultFullscreenHost` attaching the Chromium custom view to `window.decorView` with `MATCH_PARENT` layout, black background, `SCREEN_ORIENTATION_SENSOR_LANDSCAPE`, and immersive transient system bars via `WindowInsetsControllerCompat`.
- Preserved orientation and session continuity via `android:configChanges="orientation|screenSize|screenLayout|smallestScreenSize"` in `AndroidManifest.xml` and `onConfigurationChanged()` in `MainActivity`. Rotating during PIN gate or Admin Mode explicitly invalidates parent authorization (`model.lock()`), while Viewer Mode playback continues.
- Prioritized Back navigation in Compose `BackHandler` (`YourTubeApp.kt`): Back while fullscreen exits fullscreen first to inline playback without closing playback or restarting video; subsequent Back returns Home.
- Idempotent cleanup and source matching in `FullscreenHost` prevent recursion, handle player disposal, episode switching, retries, and Activity destruction without leaks.
- `gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest` passed (exit 0) on JDK 21. 29 JVM unit tests passed (11 new tests in `FullscreenHostTest`).
- `adb devices -l`: no devices attached. Real playback on a physical Android device remains NOT TESTED.
- Debug APK SHA-256: `C61A995BF8FE59883529566A1DC862B1CE1BD513023A9ED3077D257E64836CEF`.

**Repository:** Branch `feat/tap-to-fullscreen` branched from `main` (commit `0a28b4f`).

## Implemented
- Tap-to-fullscreen landscape playback: expands to Android display upon user tap on YouTube fullscreen button, requests landscape, hides system bars, restores portrait/previous orientation and system bars on exit (via Back button, gesture, or YouTube control).
- Universal family Home: single shared Viewer Mode Home for all family members, large cards, featured entry banner, Continue Watching (based on locally stored last-opened episode), and ordered category sections.
- Hidden Admin Mode: PIN-gated admin console with approved content management, manual and opt-in automatic playlist updates, category ordering, and YouTube intake.
- Room v1→v2 migration: additive migration preserving existing categories, approved videos/playlists, ordering, and positions while introducing `featured`, `refreshMode`, and foreign-key cascade `watch_history`.
- Safe playlist refresh policy: manual by default, opt-in automatic refresh opportunistically on Admin Mode open without background services or extra permissions.
- Last-opened episode memory: persists entry/video/episode ID and timestamp to enable resuming series from Home without claiming unverified percentage-based playback time.
- Terminology alignment: DivineTube, Viewer Mode, Admin Mode, Approved Content, Home, preserving all existing package/application/DB IDs and Android Keystore aliases.

## Verified build evidence
- JDK 21.0.8; Gradle 8.13; SDK 36.1; compile 36.1 / target 36 / min 26.
- `gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest` passed (exit 0).
- 29 JVM unit tests passed (8 YouTube API, 4 link parser, 4 attempt policy, 2 PIN crypto, 11 fullscreen host & navigation priority).
- Lint: 0 errors, 15 dependency/Gradle version notices only (per ADR-006). Android test APK compiled.
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk` (versionCode 2, versionName 0.2.0, SHA-256 `C61A995BF8FE59883529566A1DC862B1CE1BD513023A9ED3077D257E64836CEF`).
- Android test APK: `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk` (SHA-256 `45C1602D30A3458F9FA734811C4E5A9E81995E7FE2371D607E07982E4D57AFAB`).
- `git diff --check` passed cleanly; secret/credential pattern scan passed.
- `adb devices -l` empty: real device playback and connected instrumented tests remain NOT TESTED.

## Genuine blockers / limitations
- Real-device playback verification: PLAY-01 (black player issue) remains pending confirmation on a physical Android device; fullscreen support does not fix or claim to fix underlying player initialization issues.
- No connected Android device/emulator: UI, startup, actual Keystore/Room persistence on device, lifecycle, and player runtime checks remain open.
- No YouTube Data API key supplied: real search and playback eligibility/embedded playback remain untested against live endpoints. Keyless approvals and local management are supported; keyless playback is intentionally blocked by YouTube's mandatory Made for Kids lookup.
- No child-directed Google registration, privacy-policy publication, legal/retention/branding audit, or real-player navigation-escape acceptance performed. Not release-ready and not a complete safety boundary.

## Changed paths in this session
- `app/src/main/java/com/yourtube/app/ui/FullscreenHost.kt`: New Activity-owned fullscreen controller interface and implementation.
- `app/src/main/AndroidManifest.xml`: Added `configChanges` for orientation and screen size handling.
- `app/src/main/java/com/yourtube/app/MainActivity.kt`: Integrated `DefaultFullscreenHost`, provided `LocalFullscreenHost`, handled `onConfigurationChanged` security check, and added lifecycle cleanup in `onPause` and `onDestroy`.
- `app/src/main/java/com/yourtube/app/ui/PlayerScreen.kt`: Enabled `fs=1`, added `allow="... fullscreen" allowfullscreen`, connected `webChromeClient` custom view callbacks, and added `onRelease` fullscreen cleanup.
- `app/src/main/java/com/yourtube/app/ui/YourTubeApp.kt`: Prioritized exiting fullscreen in Compose `BackHandler`.
- `app/src/test/java/com/yourtube/app/ui/FullscreenHostTest.kt`: Added 11 focused JVM unit tests for fullscreen state transitions, duplicate entry, idempotent cleanup, callback notification, orientation restoration, source matching, Back navigation priority, and rotation security.
- `app/src/androidTest/java/com/yourtube/app/ui/FullscreenIntegrationTest.kt`: Added Compose / Activity instrumentation tests for fullscreen view attachment, orientation changes, and cleanup.
- `DECISIONS.md`: Corrected ADR-007 fullscreen note and added ADR-011 for fullscreen architecture.
- `PLAN.md`: Added PLAY-02 and documented status.
- `PROJECT_STATUS.md`: Updated active state, artifacts, and test counts.
- `HANDOFF.md`: Recorded full implementation details and verification evidence.
- `docs/VERIFICATION.md`: Recorded verification commands, results, APK hashes, and testing status.
