# DivineTube — current state snapshot

**Updated:** 2026-11-10 (Asia/Calcutta)

**Phase:** Dark Theme, YouTube-Style Home Redesign, Video Title Resolution, and Graceful Playlist Import implemented and verified via unit tests, build tasks, and lint; physical device runtime verification pending operator setup.

**Active task:** REDESIGN-01 & REDESIGN-02 VERIFIED for source, build, unit and androidTest compilation, and lint; device runtime pending. Fullscreen (PLAY-02) and Playback Resume (PLAY-03) complete and preserved. PLAY-01 remains pending device confirmation.

**Current platform/owner:** Antigravity orchestrator. Shared documentation remains main-agent-owned.

## Video Title Resolution & Graceful Playlist Import (2026-11-10)
- Actual Video Title Resolution: Resolved video cards on Home and Category tabs to display real YouTube video titles instead of synthetic episode strings (e.g. avoiding "Mahabharat — Episode 1"). Playlist series context is preserved truthful as subtitle metadata (e.g. "Mahabharat • Episode 1 of 50"). Real titles are displayed on Player screen and episode picker buttons.
- Title Persistence & Fetching: Persistent `VideoTitleStore` (`SharedPreferences("video_titles")` + `ConcurrentHashMap`) without requiring database migration. Populated from YouTube Data API playlist imports, search, link preparation, and non-intrusive background oEmbed fetcher (`YoutubeTitleFetcher`).
- Graceful Private Video Handling: `YoutubeApi.playlistVideos()` automatically skips private, deleted, or unavailable videos (`continue`) instead of aborting the import. Throws an error only if zero playable videos remain in the playlist.

## Dark Theme & YouTube-Style Home Redesign (2026-11-10)
- Visual Design: Near-black background (`#0F0F0F`), surface (`#181818`), container surfaces (`#222222`/`#282828`), white primary text, muted-gray secondary text (`#AAAAAA`), and YouTube-style red (`#FF0033`) active tabs, badges, and accents. Material 3 dark color scheme applied throughout Viewer Mode, Admin Mode, Player Screen, dialogs, and system bars. Edge-to-edge layout with dark status bar and navigation bar.
- Fixed Top Header: Header with custom DivineTube branding (red play badge + bold white text). Hidden long-press opens PIN-protected Admin Mode. No fake notification, search, or subscription buttons.
- Horizontally Swipeable Tab Bar: Dynamic tabs `Home | Playlists | Category 1 | Category 2 | Category 3 | +N More`. Displays first 3 custom categories; if more exist, shows `+N More` which expands the tab row in-place. Thin red underline (`#FF0033`) for selected tab with automatic scroll into view via `ScrollableTabRow`.
- Home Feed: Vertically scrollable 16:9 full-width video card feed containing approved standalone videos and playlist episodes. Order randomized once per fresh app launch and kept stable across recomposition, tab switching, and returning from playback. No duplicate `(entryId, videoId)` items. Compact typography and truthful metadata (playlist name, episode index, saved progress, duration badge, thumbnail progress bar).
- Playlists & Category Feeds: Dedicated Playlists tab displaying approved playlists with episode counts, recovering last-opened episode and saved progress. Custom category tabs displaying approved content assigned to each category, preserving playlist context when opening episodes. Clean empty states.
- Feature Preservation: Preserved tap-to-fullscreen landscape playback, resume watching timestamps, continue watching launch popup, playlist navigation (Previous/Next/Episodes), hidden long-press Admin Mode with PBKDF2 PIN security, YouTube iframe player integration with Made for Kids restrictions.
- JDK 21 build: `gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest` passed (exit 0). 44 JVM unit tests passed (7 in `HomeFeedTest`, 9 in `YoutubeApiTest`).
- Debug APK SHA-256: `13C0C9BB899F863F991DC966AF022A7C19041B9934063F88C705F7AF6320A00C`.

**Repository:** Branch `main` on https://github.com/Yavagu/DivineTube.

## Implemented
- Actual YouTube video title display across Home feed, category tabs, and Player screen with playlist series subtitle context.
- Graceful playlist import bypassing private, deleted, and unavailable videos.
- Standalone persistent `VideoTitleStore` with oEmbed fallback and API snapshot population.
- YouTube-style dark theme UI: near-black background (`#0F0F0F`), `#181818` surfaces, white text, `#AAAAAA` secondary text, and `#FF0033` accents across entire app.
- Horizontally swipeable tab bar with dynamic categories, `+N More` expansion, and red tab underline indicator.
- Randomized video feed on Home with 16:9 thumbnails, truthful metadata, and duration/progress badges.
- Dedicated Playlists tab and category-filtered video feeds preserving playlist context.
- Playback resume & progress tracking: granular per `(entryId, videoId)` progress tracking with YouTube `start` parameter cueing and 5-second interval reporting.
- Home Continue Watching popup: launch dialog for unfinished approved content.
- Tap-to-fullscreen landscape playback: expands to Android display upon user tap on YouTube fullscreen button, requests landscape, hides system bars, restores portrait/previous orientation and system bars on exit.
- Hidden Admin Mode: PIN-gated admin console with approved content management, manual and opt-in automatic playlist updates, category ordering, and YouTube intake.
- Room database migrations v1→v2 and v2→v3.

## Verified build evidence
- JDK 21.0.8; Gradle 8.13; SDK 36.1; compile 36.1 / target 36 / min 26.
- `gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest` passed (exit 0).
- 44 JVM unit tests passed (9 YouTube API, 4 link parser, 4 attempt policy, 2 PIN crypto, 11 fullscreen host, 7 playback resume, 7 home feed).
- Lint: 0 errors, 17 dependency/deprecation notices only. Android test APK compiled.
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk` (versionCode 2, versionName 0.2.0, SHA-256 `13C0C9BB899F863F991DC966AF022A7C19041B9934063F88C705F7AF6320A00C`).
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
- `app/src/main/java/com/yourtube/app/data/local/VideoTitleStore.kt`: SharedPreferences + memory cached persistent store for video titles.
- `app/src/main/java/com/yourtube/app/data/youtube/YoutubeTitleFetcher.kt`: Lightweight oEmbed metadata fetcher for keyless background title retrieval.
- `app/src/main/java/com/yourtube/app/YourTubeApplication.kt`: Initialized `videoTitles` store.
- `app/src/main/java/com/yourtube/app/AppViewModel.kt`: Populated `videoTitles` during playlist imports and search; scheduled background title fetching; updated `homeFeed` to resolve actual video titles and truthful playlist subtitles.
- `app/src/main/java/com/yourtube/app/data/youtube/YoutubeApi.kt`: Updated `playlistVideos()` to skip private and deleted items without failing the playlist import.
- `app/src/main/java/com/yourtube/app/ui/YourTubeApp.kt`: Resolved video titles in category feeds.
- `app/src/main/java/com/yourtube/app/ui/PlayerScreen.kt`: Displayed actual video titles as headline and in episode picker buttons.
- `app/src/test/java/com/yourtube/app/ui/HomeFeedTest.kt`: Added unit test `feedDisplaysActualVideoTitleRatherThanPlaylistEpisodeName()`.
- `app/src/test/java/com/yourtube/app/data/youtube/YoutubeApiTest.kt`: Added unit test `playlistIgnoresPrivateAndUnavailableVideos()`.
- `DECISIONS.md`, `PLAN.md`, `PROJECT_STATUS.md`, `HANDOFF.md`: Updated architecture decisions, canonical plan, project status snapshot, and handoff.
