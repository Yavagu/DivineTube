# Verification — 2026-11-10, Asia/Calcutta

## Actual commands and results

Executed from `D:\Codes\DivineTube` with PowerShell and `JAVA_HOME=C:\Program Files\Java\jdk-21`:

| Command | Result |
| --- | --- |
| `java -version` using JDK 21 absolute path | 21.0.8, passed |
| `gradlew.bat --version` | Gradle 8.13, JVM 21.0.8, Windows 11 |
| `gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest` | Final run **passed**, exit 0, 87 tasks; debug app and device-test APKs produced |
| `gradlew.bat :app:connectedDebugAndroidTest` | **Blocked/failed**, exit 1: `DeviceException: No connected devices!`; no instrumented test executed |
| `adb devices` | Empty device list; no configured AVD was found |
| SDK `aapt dump badging app/build/outputs/apk/debug/app-debug.apk` | Verified installable-package metadata: `com.yourtube.app`, version 0.1.0, min 26, target 36, launcher MainActivity; does not prove launch |
| `git diff --check` | Passed; Git emitted only Windows line-ending notices |
| SDK `apksigner verify app/build/outputs/apk/debug/app-debug.apk` | Passed, exit 0; debug signature validates |
| Credential-pattern `rg -l` scan (source/docs; excluding Git, generated builds, Gradle caches and local.properties) | No matches, exit 1 as expected for no matches; not a comprehensive secret audit |

JVM results: **17 tests, 0 failures, 0 errors**: YouTube API 8, link parser 4, attempt policy 3, PIN crypto 2. Tests cover hostile/ambiguous URLs, ID validation, API identity headers, pagination, missing status/key, cancellation/error sanitization, unavailable playlist items, PIN hash matching, persistent-backoff policy and clock/reboot handling. These use fake network responses; none contacted a credentialed YouTube endpoint.

Android lint: **0 errors, 14 warnings**. All remaining warnings are newer dependency/Gradle versions, deliberately left visible for the pinned compatible baseline. No lint baseline or error suppression was introduced. Initial Gradle DSL errors were fixed (Properties import and compilerOptions); an intermediate lockout test run saw old main bytecode while agent edits completed, and the settled rerun passed. Lint initially rejected the unescaped Windows drive colon in ignored local.properties; that and the Android 12+ data-extraction configuration were corrected and lint rerun.

Compiled but **not executed** device tests: 5 Room repository tests (delete/unassign, duplicate/order, refresh snapshot, invalid input, close/reopen); 1 isolated DataStore/Keystore PIN test; 2 Compose navigation/share-gate tests. No migrations exist before initial schema v1; its JSON is exported in `app/schemas` and destructive migration fallback is absent.

Artifacts:

- `app/build/outputs/apk/debug/app-debug.apk` — debug-signed, no configured API key.
- SHA-256: `967E2D0E760E83EF34EEA9855D57728E9D8E37A01133A205D2DD4EB171CC2CCF`.
- `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk` — device-test harness.
- `app/build/reports/tests/testDebugUnitTest/index.html`, `app/build/reports/lint-results-debug.html` — actual reports, generated/ignored by Git.
- Gradle distribution checksum pinned in wrapper properties from the official distribution endpoint; wrapper `--version` rerun successfully after pinning.

## Acceptance evidence and missing checks

| Area | Implemented | Remaining verification |
| --- | --- | --- |
| Launch and Kids home | Compose approved-only cards, category thumbnails, empty/offline display | Actual install/launch, rendering, accessibility, orientations |
| PIN | 6–12 digits, salted slow verifier, Keystore encryption, reserved persistent attempts | Keystore/device execution, wrong/correct PIN, process death during hashing |
| Parent boundaries | Hidden long-press entry; pause, Back/Done, 2-minute idle lock; no saved auth | Device background, recents, timeout during network/PIN work, rotation |
| Approval/share | Strict link parser, authenticated share intake, explicit review/approval | Official YouTube app Share flow, hostile intents on a device |
| Persistence | Room v1 categories/order, saved videos and playlist snapshots, transactions | Execute disk reopen/device tests and manual force-stop/relaunch |
| Parent CRUD | Category CRUD/order, entry remove/order/assignment, snapshot refresh on approval | UI taps and database effects on device; duplicates/rollback |
| Search/API | Official v3, Android key headers, paging, quota/network errors | Restricted real key, quota/private/deleted fixtures, actual MFK responses |
| Playback | Official privacy-enhanced IFrame, package Referer/origin, controls, error/timeout UI, teardown | Playback/device WebView/error153, audio stop on pause, landscape, YouTube recommendation/link escape audit |
| Playlists | IDs snapshotted; app selects saved videos with Previous/Next | Upstream playlist adds/removes do not alter saved launch IDs; explicit refresh comparison |
| Release/privacy | Local disclosure, no analytics/backend, minimal permissions, no backup/transfer | Child-directed self-designation, published privacy/contact, legal/retention/branding review |

## Device smoke sequence (not yet run)

Use a dedicated test device/install; tests exercise local app data.

1. Install APK; launch with no key or network. Confirm empty Kids home and no search/admin controls.
2. Long-press title. Set PIN/consent; cancel and retry mismatched confirmation; lock and verify wrong/correct PIN. Fail five times, relaunch/reboot, check throttle. Try clock changes. Restore normal time afterwards.
3. Create/rename/reorder/delete categories; approve a pasted video and a manual playlist snapshot. Force-stop/relaunch; confirm contents and order. Delete category; confirm retained approvals under All favorites.
4. Share an official YouTube link while locked and while unlocked; both must show PIN and never auto-approve. Reject non-YouTube URLs and malformed/ambiguous IDs.
5. Unlock, start a request, immediately Home/Back/rotate; confirm no late response unlocks Parent Mode. Leave parent idle for more than 120 seconds. Check recents does not show parent details.
6. Configure a restricted Data API key and rebuild. Test video/playlist search, pagination, exhausted quota, unavailable/missing-status videos, and multi-page playlist review.
7. After completing child-directed setup, preview/play known embeddable videos. Check normal controls, errors, networking failures, teardown/audio, player viewport, and every recommendation/link escape. Do not claim containment from app navigation checks.
8. Modify a test playlist upstream; saved snapshot must remain unchanged. Fetch a refresh; verify no update until explicit approval. Confirm updated IDs/order after relaunch.
9. Run `connectedDebugAndroidTest`; record all results and fix failures before changing plan rows to VERIFIED. Check TalkBack, large font, small phone/tablet and orientation manually.

**Release decision:** Not release-ready. Code/build deliverables exist, but launch, real playback, device security/persistence and policy/account obligations remain unverified. Keyless playback is blocked by the required status lookup, not silently substituted with unofficial playback.
