# YourTube — significant technical decisions

Only record choices that affect architecture, security, user-visible behavior, cost, or migration. Use `Proposed` when further validation/approval is required; `Accepted` means a project default, **not** an assertion of runtime feasibility.

## ADR-001 — Native Android UI (Accepted, 2026-11-09)
- **Choice:** Kotlin, Jetpack Compose, Room, DataStore, Android-native sharing; version choices deferred to P0-03.
- **Reasoning:** Native navigation, lifecycle, and intent handling suit a single Android app and avoid a separate UI runtime.
- **Tradeoff:** Android-specific code; no iOS/web app in initial scope.

## ADR-002 — Cross-platform truth lives in Git files (Accepted, 2026-11-09)
- **Choice:** `AGENTS.md` + shared spec, plan, status, decisions, and one latest handoff are canonical. `GEMINI.md` is a concise Antigravity entry point. Codex and Antigravity are independent clients.
- **Reasoning:** Portable without APIs, brokers, paid services, or synchronization daemons.
- **Tradeoff:** Context must be explicitly maintained and verified against code; private chats do not transfer. Default to sequential ownership.

## ADR-003 — Official YouTube embed with safety feasibility gate (Implementation accepted; release verification pending, 2026-11-10)
- **Choice:** Parent-only Data API discovery plus official embedded playback; use a local approved-item snapshot for kids' launch targets.
- **Reasoning:** Complies with the intended official playback path and avoids prohibited stream extraction.
- **Risks/tradeoffs:** Player UI and external navigation are not a complete allowlist boundary; child-directed API terms and embed requirements may affect feasibility. P0-02 must establish current policy and real-device behavior; strict containment may require changing the product promise.
- **Session resolution:** The user explicitly authorized complete implementation with documented embed limitations. This authorizes a development integration, not a claim of real-device success or child-directed release compliance. No connected device/API key is supplied. Those external verification gates remain open.

## ADR-004 — Credentials and PINs stay out of Git (Accepted, 2026-11-09)
- **Choice:** No hardcoded secrets or plaintext PIN. If an Android-client YouTube API key is used, restrict it through Google Cloud and treat it as extractable; never embed a server secret. PIN verification needs slow salted storage and attempt throttling.
- **Reasoning:** Device apps cannot conceal embedded static credentials; a four-digit PIN alone is weak without rate limiting.
- **Tradeoff:** Cloud Console setup required for search; potential stronger security/auth architecture needs separate review.

## ADR-005 — No auto-refresh expansion of kids' playlist allowlist (Accepted, 2026-11-09)
- **Choice:** Approving a playlist captures reviewed video IDs. Any newly added remote item needs renewed parent approval before reaching Kids Mode.
- **Reasoning:** Dynamic publisher playlists could silently introduce unapproved videos.
- **Tradeoff:** Parents must approve updates; remote deletion/unavailability still needs graceful handling.

## ADR-006 — Build baseline and local-only architecture (Accepted, 2026-11-10)
- **Choice:** JDK 21 for local builds, Java bytecode 17, Gradle 8.13, AGP 8.13.2, Kotlin/Compose compiler 2.3.10, KSP 2.3.4; compile SDK 36.1, target 36, minimum 26. Compose BOM 2025.12.01, Core 1.17.0, Activity 1.12.1, Lifecycle 2.10.0, Room 2.8.4, DataStore 1.2.0, coroutines 1.10.2. These are pinned stable versions, not a claim to be the newest. Lint's 14 remaining warnings are newer-version notices; no baseline hides them.
- **Evidence:** Gradle wrapper, Android app/test APK assembly, 17 JVM tests, and lint pass on the installed SDK/JDK. Official version references: [AGP](https://developer.android.com/build/releases/about-agp), [Kotlin compatibility](https://kotlinlang.org/docs/gradle-configure-project.html), [Compose BOM](https://developer.android.com/develop/ui/compose/bom), [Room](https://developer.android.com/jetpack/androidx/releases/room), [DataStore](https://developer.android.com/jetpack/androidx/releases/datastore), [KSP releases](https://github.com/google/ksp/releases).
- **Structure:** One activity, AndroidViewModel + StateFlow, explicit mode/playback state rather than a persistent navigation back stack. This intentionally avoids restoring parent authorization via saved navigation state. No extra networking/image/player library: bounded HTTPS Data API transport, platform image decoding, official IFrame API in WebView. Production AndroidX/Kotlin libraries use their Apache-2.0 licenses; no new backend or commercially licensed player.
- **Tradeoff:** Older supported baseline avoids an unrelated AGP 9 migration. Tests compile for a device, but are not a substitute for running them. Direct graph setup in Application is sufficient for v1.

## ADR-007 — Playback policy conflicts with the keyless requirement (Accepted safe default; user requirement blocked, 2026-11-10)
- **Finding:** YouTube's [developer policies](https://developers.google.com/youtube/terms/developer-policies) require a Made for Kids lookup for each embedded video. The [official guide](https://developers.google.com/youtube/data/guides/made_for_kids_status) uses Data API video status. The IFrame itself needs no key, but omitting that lookup would not satisfy the stated policy requirement.
- **Choice:** Pasted/shared-link approval and manual playlist snapshots work without a key. Search, fetched snapshots, preview, and playback need a restricted Data API key. Playback looks up status anew and fails closed on missing MFK/embeddable status or network/API errors. A known MFK false value is permitted; this is not a parental suitability classifier. No scraping or metadata endpoint workaround.
- **Identity:** Local HTML uses the installed package ID as HTTPS base URL / origin per [embedded-player identity requirements](https://developers.google.com/youtube/terms/required-minimum-functionality), loading the privacy-enhanced official iframe. Standard controls/branding remain intact; autoplay off. Fullscreen playback was updated from disabled to user-requested tap-to-fullscreen in landscape per ADR-011. Native Previous/Next select saved video IDs rather than a live remote playlist.
- **Unresolved release obligations:** Developer child-directed notification/designation, published privacy/contact information, legal review, real API-key restriction checks, physical-device WebView/escape testing, and API-data retention review. Search titles remain transient; saved labels are explicitly parent labels, and approval IDs/snapshots are local bookmarks. Do not treat that distinction as a completed retention-policy audit. Privacy-enhanced mode does not eliminate tracking or account-level obligations.

## ADR-008 — Parent security, recovery, and backup (Accepted, 2026-11-10)
- **Choice:** 6–12 digit PIN; random salt and 210,000-iteration PBKDF2-HMAC-SHA256; AES-GCM Keystore protection of the verifier in DataStore. Persistent attempt reservation before hashing; fifth failure starts 30-second exponential backoff, capped at one hour. Elapsed realtime within boot resists clock edits; reboot preserves at least the current penalty; backwards wall time fails closed.
- **Session:** No persisted authorization. Epoch invalidation and cancellation prevent late PIN/network work reopening a locked session. Pause, Back/Done, process death, and a two-minute idle deadline lock the app; rotation also locks. Shared links always return to the PIN gate. FLAG_SECURE protects screenshots/recents.
- **Recovery:** No unauthenticated PIN reset. Clearing Android app data resets both PIN and library. Cloud backup and device transfer exclusions prevent moving an unusable Keystore-bound verifier and limit casual restore bypass. Rooted devices and OS data-clearing authority are outside the threat model.
- **Verification:** Pure verifier/backoff tests pass. Keystore, DataStore persistence, background/timeout navigation, and Room persistence need device execution; compiled instrumentation tests cover core scenarios but do not establish runtime proof.

## ADR-009 — One universal family Home and compatibility-preserving terminology (Accepted, 2026-11-10)
- **Choice:** DivineTube has one shared Viewer Mode Home for every family member and one hidden PIN-protected Admin Mode. There are no viewer profiles or separate kids/parents homes. Visible child-specific terminology is replaced, but the existing application ID, database name, security preference name, Keystore alias, and internal class/package names remain unchanged to preserve installed data and signing continuity.
- **Home model:** Ordered category sections, one featured approved entry, and Continue Watching based on locally saved last-opened episode. All launch targets are still re-read from the approved Room stream.
- **Accessibility:** Favor large labels/cards/touch targets, direct Home/Previous/Next/episode actions, predictable Back behavior, and minimal depth. Device TalkBack, large-font, orientation, and screen-size checks remain release gates.

## ADR-010 — Playlist refresh trust and truthful recovery (Accepted, 2026-11-10)
- **Choice:** Manual reviewed playlist refresh remains the default. An administrator can opt a specific playlist into `AUTOMATIC` after a warning; trusted playlists refresh opportunistically when Admin Mode opens and therefore do not require background scheduling or new permissions. This uses official Data API quota and never changes non-trusted snapshots silently.
- **Persistence:** Additive Room migration v1→v2 adds `featured`, `refreshMode`, and a cascade-linked `watch_history` row without changing existing category/entry IDs, snapshots, positions, database identity, PIN storage, or application ID.
- **Progress limitation:** Persist last-opened entry/video/episode and timestamp only. Although the official IFrame API exposes current time, reliable cross-WebView position restoration has not been device-validated; DivineTube does not show or claim percentage progress.
- **Policy review:** Official documentation checked on 2026-11-10 continues to require API client identity, Made for Kids status handling, unobscured standard player controls, and child-directed compliance. Automatic playback remains disabled; the app does not scrape, download, suppress ads, or claim player containment.

## ADR-011 — Tap-to-fullscreen landscape playback architecture (Accepted, 2026-11-10)
- **Choice:** Update embedded YouTube player to support YouTube-style tap-to-fullscreen playback using the official IFrame Player API parameters (`fs=1`), explicit iframe fullscreen permissions (`allow="encrypted-media; picture-in-picture; fullscreen" allowfullscreen`), and native `WebChromeClient` custom view lifecycle (`onShowCustomView` and `onHideCustomView`).
- **Activity & window host:** Managed by `DefaultFullscreenHost` owned by `MainActivity` and made available to Compose via `LocalFullscreenHost`. When entering fullscreen, attaches the Chromium-provided custom view to `window.decorView` with `MATCH_PARENT` layout and black background, requests `ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE`, and hides system bars with transient swipe behavior (`WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE`).
- **Configuration & parent security:** `MainActivity` declares `android:configChanges="orientation|screenSize|screenLayout|smallestScreenSize"` to prevent Activity recreation and playback interruption during fullscreen rotation. To preserve the security boundary defined in ADR-008, `onConfigurationChanged()` explicitly checks whether Admin Mode or the PIN gate is active (`model.state.value.parent || model.state.value.gate`) and invalidates parent authorization (`model.lock()`), while normal Viewer Mode playback continues without locking. `onPause()` retains immediate lock-on-background and cleans up fullscreen.
- **Navigation & lifecycle:** Single Compose `BackHandler` updated to prioritize exiting fullscreen before closing playback or locking. Cleanup is idempotent, avoids recursive callbacks to Chromium, matches requesting WebView sources to avoid cross-player leaks, and releases fullscreen on episode changes, retries, player errors, or Activity destruction.
- **Tradeoffs:** Fullscreen overlay sits at the native Window decor level above Compose; real-device verification of YouTube player controls and gesture navigation remains a physical device release gate. Does not resolve the underlying unconfirmed black-player device issue (PLAY-01).
