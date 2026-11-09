# DivineTube Verification Log

This document records verification commands, outcomes, and remaining checks across DivineTube development phases.

---

## 1. Universal Family Redesign & Room v2 Migration (Version 0.2.0 — 2026-11-10)

### Verification status categories

- **Source implementation:** COMPLETE. Universal family Home, Continue Watching, featured entries, Room v1→v2 additive migration with `watch_history` cascade, opt-in automatic playlist refresh, and terminology updates.
- **Build verification:** PASSED. Assembly, compilation, unit tests, and lint checks run cleanly with exit code 0.
- **Device verification:** PENDING. Blocked because no physical Android device or pre-configured emulator is connected (`adb devices` is empty).
- **External API verification:** PENDING. Blocked because no YouTube Data API key is configured in `local.properties`. Keyless library operations work; real search, metadata lookups, and player embed checks require a restricted key.
- **Release compliance:** PENDING. Google Cloud project configuration, child-directed developer self-designation, privacy policy publication, and store signing are operator actions.

### Executed commands and actual results

All commands executed from `D:\Codes\DivineTube` on Windows 11 with PowerShell and `JAVA_HOME=C:\Program Files\Java\jdk-21`:

| Command | Exit Code | Result | Details |
| --- | :---: | :---: | --- |
| `java -version` | 0 | PASSED | OpenJDK 21.0.8 (Oracle Corporation 21.0.8+12-LTS-250) |
| `gradlew.bat --version` | 0 | PASSED | Gradle 8.13, JVM 21.0.8, OS Windows 11 amd64 |
| `gradlew.bat :app:testDebugUnitTest` | 0 | PASSED | **18 tests executed, 0 failures, 0 errors** (YouTube API: 8, Link Parser: 4, Attempt Policy: 4, PIN Crypto: 2) |
| `gradlew.bat :app:lintDebug` | 0 | PASSED | **0 errors, 15 warnings** (all 15 are newer dependency/Gradle version notices per ADR-006) |
| `gradlew.bat :app:assembleDebug` | 0 | PASSED | Debug APK generated at `app/build/outputs/apk/debug/app-debug.apk` |
| `gradlew.bat :app:assembleDebugAndroidTest` | 0 | PASSED | Test APK generated at `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk` |
| `gradlew.bat :app:connectedDebugAndroidTest` | 1 | BLOCKED | `com.android.builder.testing.api.DeviceException: No connected devices!` |
| `adb devices` | 0 | EMPTY | No physical devices or emulators attached |
| `emulator -list-avds` | 0 | EMPTY | No pre-configured Android Virtual Devices (AVDs) found |
| `python verify_migration.py` | 0 | PASSED | SQLite simulation of v1 schema (`1.json`), v1 data population, `MIGRATION_1_2` execution, schema match against `2.json`, category/entry/playlist snapshot preservation, and `watch_history` cascade deletion |
| `git diff --check` | 0 | PASSED | Clean whitespace check across all tracked files |

### Artifacts and build verification

- **Debug APK:** `app/build/outputs/apk/debug/app-debug.apk`
  - Version: `versionCode=2`, `versionName="0.2.0"`
  - Application ID: `com.yourtube.app`
  - Min SDK: 26 (Android 8.0), Target SDK: 36 (Android 16 preview), Compile SDK: 36.1
  - Launcher: `com.yourtube.app.MainActivity`
  - SHA-256: `631780D5EFB9B61497EB87E9CB3B3FA720EA76C0D021F63A486AF032040FE724`
- **Instrumented test APK:** `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`
- **Room schemas:**
  - `app/schemas/com.yourtube.app.data.local.LibraryDatabase/1.json` (v1)
  - `app/schemas/com.yourtube.app.data.local.LibraryDatabase/2.json` (v2)

### Room v1→v2 migration verification

Verified via schema inspection, instrumented tests in `LibraryMigrationTest.kt`, and standalone SQLite migration validation:
1. **Preserved fields and tables:**
   - `categories` (id, name, position) and unique position index.
   - `approved_entries` (id, title, videoIds, playlistId, categoryId, position).
   - Foreign key constraint: `approved_entries.categoryId` -> `categories.id` (ON DELETE SET NULL).
   - Unique constraints on `position` and `playlistId`.
2. **New fields and tables:**
   - `approved_entries.featured` (INTEGER NOT NULL DEFAULT 0).
   - `approved_entries.refreshMode` (TEXT NOT NULL DEFAULT 'MANUAL').
   - `watch_history` (entryId, videoId, episodeIndex, lastOpenedAt, PRIMARY KEY(entryId), FOREIGN KEY(entryId) REFERENCES approved_entries(id) ON DELETE CASCADE).
   - Index on `watch_history(entryId)`.
3. **Data preservation:**
   - Existing approved videos and playlists retain their original primary keys and position order.
   - Deleting a category sets `categoryId = NULL` and moves approved content to Family Favorites.
   - Deleting an approved entry cascades to delete its `watch_history` record.

### Defects discovered and fixed

1. **Category deletion dialog mismatch:**
   - In `YourTubeApp.kt` line 305, the confirmation dialog stated `"Approved videos remain available under All favorites."` while the family redesign renamed this section to `"Family Favorites"`. Corrected to `"Family Favorites"`.
2. **Migration test identity hash typo:**
   - In `LibraryMigrationTest.kt` line 31, the identity hash string for schema v1 was truncated by one character (`'ae229b9ab917952d98812a4bd7fb9f9'`). Corrected to match `1.json` (`'ae229b9ab917952d98812a4bd7fb9f9f'`).
3. **Extended migration test coverage:**
   - Added `watch_history` persistence and retrieval assertions in `LibraryMigrationTest.kt`.
4. **Extended unit test coverage:**
   - Expanded `YoutubeLinkTest.kt` with edge cases for supported URL shapes (mixed case, shorts with query params, music.youtube.com, parenthesized URLs) and length boundary validation (video IDs != 11, playlist IDs < 10 or > 100).
   - Expanded `AttemptPolicyTest.kt` with wall-clock rollback tolerance tests (confirming that rollbacks under 60 seconds are tolerated while rollbacks beyond 60 seconds trigger maximum 1-hour lockout).

---

## 2. Historical Scaffolding & Initial Version Verification (Version 0.1.0 — 2026-11-10)

*Historical record of initial build verification prior to the universal family redesign:*

| Command | Result |
| --- | --- |
| `java -version` | 21.0.8, passed |
| `gradlew.bat --version` | Gradle 8.13, JVM 21.0.8, Windows 11 |
| `gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest` | Passed, exit 0, 87 tasks; debug app and device-test APKs produced |
| `gradlew.bat :app:connectedDebugAndroidTest` | Blocked, exit 1: `DeviceException: No connected devices!` |
| `adb devices` | Empty device list |
| SDK `aapt dump badging` | Package: `com.yourtube.app`, version 0.1.0, min 26, target 36 |
| `git diff --check` | Passed |
| Secret scan | Clean; no hardcoded keys in tracked files |

Original JVM unit test count: 17 tests (8 YouTube API, 4 link parser, 3 attempt policy, 2 PIN crypto).
Original APK SHA-256: `967E2D0E760E83EF34EEA9855D57728E9D8E37A01133A205D2DD4EB171CC2CCF`.
All original verification records are preserved above for traceability.
