# YourTube — project specification

## 1. Purpose and users
YourTube is a **native Android application** for a parent to curate YouTube videos and playlists, and for a child to browse that curated home screen. It has two UI modes: Kids Mode (default, no discovery/admin UI) and hidden PIN-protected Parent Mode. A parent may approve items by searching YouTube or using Android Share from the YouTube app. Local data holds approvals and preferences; video playback uses an official YouTube embed, subject to safety and policy feasibility checks.

Primary users: one parent managing one Android device, and children watching on it. A single local profile, online playback, and an English-first UI are initial assumptions, not irrevocable decisions.

## 2. Mandatory functional requirements

| ID | Requirement | Observable acceptance |
| --- | --- | --- |
| F01 | Kids Mode is the launch/default screen; shows only saved parent-approved videos, playlists, and categories. | With an empty database, no discoverable catalog appears; with seeded approvals, only those appear. |
| F02 | Child-facing UI has no search, arbitrary URL input, edit controls, account login, or Parent Mode shortcut. | UI and navigation tests cannot reach admin routes without PIN. |
| F03 | Parent Mode entry is intentionally non-obvious and PIN-gated, with PIN setup/reset policy, lockout/backoff, and background timeout. | Correct/incorrect PIN, repeated attempts, recreation, and return from background are tested. |
| F04 | Parent Mode can search YouTube for videos/playlists, preview content, and add/remove/reorder approved items. | Search errors and empty results handled; saved changes survive process recreation. |
| F05 | Accept shared YouTube video/playlist links from the official Android share sheet; parse and validate IDs and reject unsupported URLs. | Valid and hostile/malformed samples tested; shared item is not auto-approved without parent confirmation. |
| F06 | Parent can create, rename, reorder, and delete local custom categories, and organize approved entries in them. | Category operations and persistence tested; deletion behavior is explicit. |
| F07 | Finish/exit Parent Mode immediately locks back to Kids Mode; leaving the app or timing out does not leave admin controls accessible. | Back-stack, app-resume, and deep-link tests enforce the mode boundary. |
| F08 | Play approved content through the official supported YouTube IFrame/embed integration; never scrape/download/redistribute streams. | Proof-of-concept on actual device shows permitted playback and documents controllable versus uncontrollable UI. |
| F09 | Only locally approved entries appear as Kids Mode launch targets. Approval of a playlist is a **snapshot** of its approved item IDs until a parent reviews refreshes. | New upstream playlist additions are not automatically whitelisted. |
| F10 | Parent sees useful offline, unavailable-video, removed-video, and API-quota error states. | Screens remain usable without unexpected catalog exposure. |

## 3. Mandatory nonfunctional requirements

| ID | Requirement | Acceptance |
| --- | --- | --- |
| N01 | Native Kotlin + Jetpack Compose; Android Jetpack patterns, predictable navigation/state. | Build, static checks, and key Compose navigation tests pass. |
| N02 | Privacy-first: no analytics, ads SDK, user account, cloud sync, or child profile data by default. | Dependency/network/manifest review documents what data goes to YouTube. |
| N03 | Local persistence and responsive UI; network/data operations off main thread; loading and failures handled. | Repository unit tests and interaction smoke tests pass. |
| N04 | Approved content and settings survive app restart; database migrations are tested when schema changes. | Persistence and migration tests pass. |
| N05 | Least-privilege Android permissions and safe URL/intent/WebView handling. | Security checklist and negative tests pass. |
| N06 | Accessible controls, readable contrast, large tap targets, orientation/state resilience. | Manual accessibility and device sanity checks recorded. |
| N07 | Respect YouTube API policy, branding, embedded-player rules, quota, and child-directed client conditions. | Policy review and feasibility decision documented before release planning. |

## 4. Preferred technology and structure
- Language/UI: Kotlin, Jetpack Compose, Material 3, Android Gradle Plugin + Gradle wrapper (versions selected and pinned when scaffolding).
- Architecture: single-activity, Navigation Compose, ViewModels + StateFlow, coroutines, repository interfaces, dependency injection only if complexity warrants it.
- Persistence: Room entities/DAOs for categories, approved videos, approved playlists, playlist-video snapshots and sort order; DataStore for non-secret preferences. Android Keystore-backed protection as appropriate for PIN verifier/secret handling; use a slow salted verifier and lockout policy, not reversible PIN storage.
- Networking: approved official YouTube Data API v3 for parent-only search/metadata, with pagination, quota awareness, failure handling, and Android-restricted key configuration if a client key is used. Never assume a bundled API key is secret; a backend or different auth approach requires separate approval.
- Playback: official YouTube IFrame Player embedded via a carefully constrained Android WebView if the current official guidance and device tests support it. Block arbitrary WebView navigation and apply documented player parameters; do not suppress or alter YouTube attribution.
- Android integration: `ACTION_SEND`/`ACTION_VIEW` if appropriate, typed destinations, URL/ID validation, deep-link guard.
- Proposed source boundaries after implementation begins: `ui/kids`, `ui/parent`, `ui/player`, `domain`, `data/local`, `data/youtube`, `security`, `navigation`, with tests mirroring modules/packages. Folder structure is a proposal, not scaffolded code.

## 5. Essential safety and platform limitation
**The app is not a hardened kiosk or guaranteed allowlist.** YouTube's player can expose links, recommendations, overlays, or external surfaces that the app cannot fully control. Do not claim to prevent *all* unapproved viewing, intercept YouTube pages, disable required player controls, or override platform policies. Before calling this child-safe, perform task P0-02 and get a product decision on acceptable residual risk. If strict containment is mandatory, pause and discuss a platform-supported alternative.

The app must never use unsanctioned stream extractors, background media download, ad suppression, or hidden network monitoring. Parent-only discovery does not exempt the app from child-directed YouTube and local privacy requirements. Children can still leave the application using OS controls unless separately configured using legitimate Android device-owner/parental-control features (outside v1).

## 6. Optional enhancements — not approved for automatic implementation
- Multiple local child profiles; schedules/time limits; offline **metadata** caching; curated thumbnails/themes; search filtering; import/export of the local approval list; app lock on device switching; parent audit history.
- Any backend, real cloud sync, YouTube login, accounts, telemetry, platform-wide kiosk restrictions, streaming proxies, or subscriptions require an explicit requirements change and a separate privacy/security review.

## 7. Exclusions and assumptions
- Not YouTube Kids or an official Google/YouTube product; no guarantee of content suitability or moderation beyond the parent's choices.
- No app code exists in this starter kit. No API key, developer account, keystore, or paid cloud infrastructure is included.
- Supported minimum/target Android SDK and build/dependency versions must be chosen in P0-03 from current compatibility docs; don't invent version numbers here.
- No network connection is needed to browse saved metadata, but **video playback and fresh search require the network**.
- Local categories/playlist order are not remote changes to a user's YouTube account.

## 8. Release acceptance gates
1. A real-device playback feasibility check documents embed behavior, link escape routes, and policy compliance; unresolved containment requirement is explicitly accepted or redesign occurs.
2. F01–F10 and N01–N07 have evidence in tests/reviews; critical security and navigation cases have negative tests.
3. Clean build, lint, applicable unit/instrumented/UI tests, dependency/security review, and manual smoke tests are recorded with dates and results.
4. No credentials in source, release artifacts, or shared logs; privacy disclosure and legal/policy review completed as needed.

## 9. Official constraints to revisit before implementation
- [YouTube API Terms, child-directed clients](https://developers.google.com/youtube/terms/api-services-terms-of-service)
- [YouTube API developer policies](https://developers.google.com/youtube/terms/developer-policies)
- [YouTube IFrame Player reference](https://developers.google.com/youtube/iframe_api_reference)
- [YouTube Data API overview and quota](https://developers.google.com/youtube/data/getting-started)
- [Android DataStore vs Room](https://developer.android.com/topic/libraries/architecture/datastore)
