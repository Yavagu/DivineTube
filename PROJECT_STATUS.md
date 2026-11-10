# DivineTube — current state snapshot

**Updated:** 2026-11-10 (Asia/Calcutta)

**Phase:** Playback Resume & Continue Watching Popup implemented and verified via unit tests and build tasks; physical device runtime verification pending operator setup.

**Active task:** PLAY-03 VERIFIED for source, build, unit and androidTest compilation; device runtime pending. Fullscreen (PLAY-02) complete and preserved. PLAY-01 remains pending device confirmation.

**Current platform/owner:** Antigravity orchestrator. Shared documentation remains main-agent-owned.

## Playback Resume & Continue Watching Popup (2026-11-10)
- Implemented Room v2→v3 additive migration introducing `playback_progress` table (`entryId`, `videoId`, `positionSeconds`, `durationSeconds`, `updatedAt`, `completed`) with compound primary key `(entryId, videoId)` and foreign key cascade on `approved_entries(id)`. Existing `watch_history` is preserved without erasing saved timestamps.
- Extended JavaScript-to-Kotlin bridge in `PlayerScreen.kt` via `PlayerEvents.onProgress`: tracks YouTube's `getCurrentTime()` and `getDuration()` every 5 seconds while playing, and immediately on pause or completion (`onStateChange`). Videos ending or reaching the final 10 seconds are marked completed and reset to 0:00.
- Implemented playlist-aware resume: reopening a partially watched video belonging to an approved playlist opens the original approved playlist snapshot with full navigation and episode list intact at that episode's index, starting at that episode's saved timestamp. Switching episodes restores each episode's own saved progress or 0:00.
- Implemented Home-screen resume popup: shows a Material 3 dialog on app launch for the most recently unfinished approved video watched >= 30 seconds with >= 30 seconds remaining. Offers "Resume", "Start Over" (clearing saved progress to start at 0:00 while keeping playlist context), and "Not Now" (dismissing the prompt for the launch session). Suppressed during Admin Mode, PIN entry, or active playback.
- JDK 21 build: `gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest` passed (exit 0). 36 JVM unit tests passed (7 new tests in `PlaybackResumeTest`).
- Debug APK SHA-256: `763917A0C243C3AF15792A111233F81421A37A7A386FFEC5E725BF26ACD31160`.

**Repository:** Branch `main` on https://github.com/Yavagu/DivineTube.

## Implemented
- Playback resume & progress tracking: granular per `(entryId, videoId)` progress tracking with YouTube `start` parameter cueing and 5-second interval reporting.
- Home Continue Watching popup: launch dialog for unfinished approved content.
- Tap-to-fullscreen landscape playback: expands to Android display upon user tap on YouTube fullscreen button, requests landscape, hides system bars, restores portrait/previous orientation and system bars on exit (via Back button, gesture, or YouTube control).
- Universal family Home: single shared Viewer Mode Home for all family members, large cards, featured entry banner, Continue Watching, and ordered category sections.
- Hidden Admin Mode: PIN-gated admin console with approved content management, manual and opt-in automatic playlist updates, category ordering, and YouTube intake.
- Room database migrations v1→v2 and v2→v3.

## Verified build evidence
- JDK 21.0.8; Gradle 8.13; SDK 36.1; compile 36.1 / target 36 / min 26.
- `gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest` passed (exit 0).
- 36 JVM unit tests passed (8 YouTube API, 4 link parser, 4 attempt policy, 2 PIN crypto, 11 fullscreen host, 7 playback resume).
- Lint: 0 errors, 15 dependency/Gradle version notices only (per ADR-006). Android test APK compiled.
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk` (versionCode 2, versionName 0.2.0, SHA-256 `763917A0C243C3AF15792A111233F81421A37A7A386FFEC5E725BF26ACD31160`).
- Android test APK: `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk` (SHA-256 `98A0F158DE7ED1E11F0934E998D0B177709334AF118EF014F81C55659BB47647`).
- Schema v3 exported at `app/schemas/com.yourtube.app.data.local.LibraryDatabase/3.json`.
- `git diff --check` passed cleanly; secret/credential pattern scan passed.
- `adb devices -l` empty: real device playback and connected instrumented tests remain NOT TESTED.

## Genuine blockers / limitations
- Real-device playback verification: PLAY-01 (black player issue) remains pending confirmation on a physical Android device.
- No connected Android device/emulator: UI, startup, actual Keystore/Room persistence on device, lifecycle, and player runtime checks remain open.
- No YouTube Data API key supplied: real search and playback eligibility/embedded playback remain untested against live endpoints. Keyless approvals and local management are supported; keyless playback is intentionally blocked by YouTube's mandatory Made for Kids lookup.
- No child-directed Google registration, privacy-policy publication, legal/retention/branding audit, or real-player navigation-escape acceptance performed. Not release-ready and not a complete safety boundary.

## Changed paths in this session
- `app/src/main/java/com/yourtube/app/data/local/LibraryDatabase.kt`: Added `PlaybackProgress` entity, DAO queries, version 3, and `MIGRATION_2_3`.
- `app/src/main/java/com/yourtube/app/YourTubeApplication.kt`: Registered `MIGRATION_2_3`.
- `app/src/main/java/com/yourtube/app/data/local/LibraryRepository.kt`: Added `savePlaybackProgress` and `clearPlaybackProgress`.
- `app/src/main/java/com/yourtube/app/AppViewModel.kt`: Added `Playback.startSeconds`, `ResumeCandidate`, `resumeCandidate` flow, `saveProgress`, `clearProgress`, `savedPosition`, and `resumePlayback`.
- `app/src/main/java/com/yourtube/app/ui/PlayerScreen.kt`: Extended `PlayerEvents` with `onProgress`, wired JS 5-second interval and state-change reporting, passed `startSeconds` to iframe URL, and saved progress on progress events.
- `app/src/main/java/com/yourtube/app/ui/YourTubeApp.kt`: Added Home resume popup dialog and `formatProgressTime` formatting helper.
- `app/schemas/com.yourtube.app.data.local.LibraryDatabase/3.json`: Exported Room v3 schema.
- `app/src/test/java/com/yourtube/app/ui/PlaybackResumeTest.kt`: Added 7 unit tests for time formatting, candidate eligibility, playlist resume context preservation, start-over, completion, and episode switching.
- `app/src/androidTest/java/com/yourtube/app/data/local/LibraryMigrationTest.kt`: Added `MIGRATION_2_3` validation.
- `DECISIONS.md`, `PLAN.md`, `PROJECT_STATUS.md`, `HANDOFF.md`, `docs/VERIFICATION.md`: Recorded design decisions, task progress, and verification results.
