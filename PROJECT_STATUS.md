# DivineTube — current state snapshot

**Updated:** 2026-11-10 (Asia/Calcutta)

**Phase:** Universal family redesign & Room v2 migration verified (version 0.2.0); device and credentialed API testing pending operator setup.

**Active task:** PLAY-01 BLOCKED on device confirmation: player patch built; actual playback and new WebView instrumentation test remain unverified.

**Current platform/owner:** Codex, following user-reported playback failure. Shared documentation remains main-agent-owned.

## Latest playback patch (2026-11-10)
- Added WebChromeClient, explicit WebView layout sizing, accessible JavaScript callback class, callback registration before API script loading, visible loading text, and iframe HTTP/network errors. Exact on-device cause remains unconfirmed.
- `gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest` passed (exit 0), using JDK 21; 18 JVM tests passed. `git diff --check` passed.
- `adb devices -l`: no devices. New `PlayerEventsTest` compiles but has not run; real playback, retry, and episode switching require device verification.
- Updated debug APK SHA-256: `6E1BE233B45ADAD77F51BB54B95E57355E6F76EE294A3AE3C7BD43C8E0AF6372` (supersedes artifact hash below).
- Modified: `PlayerScreen.kt`, new `PlayerEventsTest.kt`, `PLAN.md`, `PROJECT_STATUS.md`, `HANDOFF.md`. Included in the user-requested hotfix commit on `main` for GitHub publication.
- Next: update installed app with rebuilt debug APK without clearing its data, then verify playback on the affected device.

**Repository:** Public at https://github.com/Yavagu/DivineTube, branch main tracking origin/main. Prior release 0.1.0 committed as 0fcf1de and 6e85e78; universal family redesign commit 64b2f30 followed by verification and defect fixes ready to push.

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
- 18 JVM unit tests passed (8 YouTube API, 4 link parser, 4 attempt policy, 2 PIN crypto).
- Lint: 0 errors, 15 dependency/Gradle version notices only (per ADR-006). Android test APK compiled.
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk` (versionCode 2, versionName 0.2.0, SHA-256 `631780D5EFB9B61497EB87E9CB3B3FA720EA76C0D021F63A486AF032040FE724`).
- Android test APK: `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`.
- Schema v2 exported at `app/schemas/com.yourtube.app.data.local.LibraryDatabase/2.json`.
- Room migration verified via standalone SQLite simulation and `LibraryMigrationTest.kt` schema assertions.
- `git diff --check` passed cleanly; secret/credential pattern scan passed.
- `connectedDebugAndroidTest` exited with code 1 (`DeviceException: No connected devices!`) pending connected device/AVD.

## Genuine blockers / limitations
- No connected Android device/emulator: UI, startup, actual Keystore/Room persistence on device, lifecycle, and player runtime checks remain open.
- No YouTube Data API key supplied: real search and playback eligibility/embedded playback remain untested against live endpoints. Keyless approvals and local management are supported; keyless playback is intentionally blocked by YouTube's mandatory Made for Kids lookup.
- No child-directed Google registration, privacy-policy publication, legal/retention/branding audit, or real-player navigation-escape acceptance performed. Not release-ready and not a complete safety boundary.

## Changed paths in this session
- `app/src/main/java/com/yourtube/app/ui/YourTubeApp.kt`: Fixed category deletion dialog message to refer to "Family Favorites" instead of "All favorites".
- `app/src/androidTest/java/com/yourtube/app/data/local/LibraryMigrationTest.kt`: Fixed v1 schema identity hash string and added watch history persistence checks.
- `app/src/test/java/com/yourtube/app/data/youtube/YoutubeLinkTest.kt`: Added edge-case tests for URL shapes (mixed case, query parameters, music.youtube.com, parenthesized URLs) and length boundaries.
- `app/src/test/java/com/yourtube/app/security/AttemptPolicyTest.kt`: Added wall-clock rollback tolerance tests.
- `docs/VERIFICATION.md`: Restructured to clearly separate version 0.2.0 verification from version 0.1.0 records, documenting all commands, exit codes, artifacts, and test counts.
- `PLAN.md`: Reconciled FAM-05 to VERIFIED with exact build and test evidence.
- `PROJECT_STATUS.md`: Updated active task, test counts, APK SHA-256, and verification status.
- `HANDOFF.md`: Recorded full Antigravity handover and verification results.
