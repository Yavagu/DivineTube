# YourTube for Android

Native Kotlin / Jetpack Compose app for a parent-curated YouTube library. Room stores approvals, categories, ordering, and playlist snapshots; DataStore and Android Keystore protect the parent PIN state. No account, analytics, backend, stream extraction, or download service.

**Development build, not release-certified or a content-safety boundary.** YouTube controls its player, recommendations, advertising, and some navigation. This app only controls which saved items it launches. See [limitations and verification](docs/VERIFICATION.md) and [privacy](docs/PRIVACY.md).

## Build and install

Prerequisites: JDK 21 (JDK 17 also supported by the selected Android plugin), Android SDK platform **36.1**, build tools **35.0.0**, internet for first dependency resolution. Minimum device: Android 8.0 / API 26. Target: API 36.

Create an ignored `local.properties` in the repository root:

```properties
sdk.dir=C\:/Users/User/AppData/Local/Android/Sdk
# Optional for library editing; required for search and playback status checks:
youtube.apiKey=YOUR_RESTRICTED_YOUTUBE_DATA_API_KEY
```

PowerShell, from this directory:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-21'
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
# Connect an authorized Android device with USB debugging enabled:
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r app/build/outputs/apk/debug/app-debug.apk
.\gradlew.bat :app:connectedDebugAndroidTest
```

On macOS/Linux, configure `sdk.dir` for your machine and use `sh ./gradlew` with the same tasks. Open the repository root in Android Studio to run on a device or create an emulator. Debug APK output: `app/build/outputs/apk/debug/app-debug.apk`.

## First use

1. Open YourTube. **Long-press the YourTube heading** to enter the parent PIN gate. On first use, read the privacy/player notice, accept the terms, and create a 6–12 digit PIN.
2. In **Add**, paste a supported YouTube link, optionally give it a title, and choose **Review link**. Preview where API setup permits, choose a category, then explicitly approve.
3. Search finds videos or playlists through the official Data API. Search and pagination use quota only when requested.
4. A playlist approval is a saved snapshot, capped at 500 videos. With an API key, fetch its contents; without a key, paste a playlist link and one reviewed video URL per line into the snapshot field. Existing snapshots change only after renewed explicit approval. No automatic publisher updates.
5. **Categories** supports create/rename/up/down/delete. **Library** supports remove, move, assignment to a category, and fetching a playlist refresh for review in Add. Deleting a category keeps its approvals under All favorites.
6. Choose **Done & lock**. Kids see only approved launch targets. A saved playlist plays its saved videos one at a time using Previous/Next; it never launches the publisher's live playlist.
7. From the official YouTube Android app, use **Share → YourTube**. Import requires the parent PIN and explicit approval; sharing never adds a video by itself.

Leaving the app, Back from Parent Mode, or two minutes of inactivity locks Parent Mode. Rotation also locks deliberately. The PIN is not recoverable: Android Settings → Apps → YourTube → Storage → Clear data resets it **and permanently deletes the local library**. Device uninstall has the same effect. This is not protection against a rooted device or a person controlling Android app-data settings.

## YouTube API setup

1. Create your own Google Cloud project and enable **YouTube Data API v3**.
2. Create an API key. Restrict it to **YouTube Data API v3** and the Android application ID **`com.yourtube.app`** plus the SHA-1 of the signing certificate.
3. Run `.\gradlew.bat :app:signingReport` locally to find the debug SHA-1. Configure production signing separately before any release; never publish a debug key as production identity.
4. Put the key only in ignored `local.properties` as `youtube.apiKey=...`, then rebuild and reinstall. Do not commit this file or paste keys into issue reports. The client sends `X-Android-Package` and `X-Android-Cert` to the Data API.
5. A client key is extractable from its APK. Restrict it, monitor quota, rotate if exposed, and do not distribute a developer's unrestricted key. No server credentials are used.
6. Complete YouTube's child-directed-client designation/notification requirements and privacy review before child deployment. A checkbox in this app does not perform Google account registration or satisfy legal obligations.

**Keyless playback is intentionally unavailable:** current YouTube policies require a Made for Kids status lookup before embedding. Pasted/shared-link approvals and local organization work without a key, but preview/playback fail closed if the lookup is unavailable or the video cannot be embedded. A returned `madeForKids=false` is a known status, not an automatic prohibition; suitability remains the parent's decision. Real API restrictions and player behavior still need a credentialed device smoke test. See [official status lookup guidance](https://developers.google.com/youtube/data/guides/made_for_kids_status) and [developer policies](https://developers.google.com/youtube/terms/developer-policies).

## Source and workflow

- `app/src/main/java/com/yourtube/app/ui`: Compose library, parent editor, privacy notice, and embedded player.
- `AppViewModel.kt`, `MainActivity.kt`: state, session boundary, lifecycle, and guarded share intake.
- `data/local`: Room schema/repository; `data/youtube`: strict link parser and official Data API client.
- `security`: Keystore-encrypted salted PBKDF2 verifier and persistent monotonic-clock lockout.
- `app/src/test`: JVM tests; `app/src/androidTest`: Room/PIN/navigation device tests; `app/schemas`: initial Room schema.
- `PLAN.md`, `PROJECT_STATUS.md`, `HANDOFF.md`, `DECISIONS.md`: shared project truth.

Original Codex/Antigravity setup notes are preserved in [docs/AGENT_SETUP.md](docs/AGENT_SETUP.md). Both platforms continue to use the existing instruction/configuration files; no broker or paid AI API was added.
