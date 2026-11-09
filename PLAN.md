# DivineTube — canonical implementation plan

**Status vocabulary:** `TODO` (not begun), `ACTIVE` (in progress), `BLOCKED` (cannot proceed), `VERIFIED` (all listed checks passed and evidence recorded). `VERIFIED` is never inferred from documentation alone. Owner is the **current explicit assignee**, not a permanent role. Only one task is `ACTIVE` by default.

**Session authorization (2026-11-10):** User explicitly requested continuous first-version implementation, including official playback with documented limitations. Local implementation proceeds in parallel with verification; real-device and policy/account gates remain release gates and are never treated as passed merely to permit coding. This supersedes the initial sequential scaffolding gate for this session. No claim of complete content isolation.

## Universal family redesign (2026-11-10)

| Task | Deliverable | Status | Evidence required |
| --- | --- | --- | --- |
| FAM-01 | Rename visible product/modes to DivineTube, Viewer Mode, Admin Mode, Approved content, and Home while preserving the PIN boundary. | VERIFIED | Build; navigation tests compile; Viewer cannot reach admin UI. |
| FAM-02 | Shared accessible Home with featured, Continue Watching, ordered category sections, large cards, responsive layout. | VERIFIED | Compose build & lint passed; device large-font/screen-size runtime check pending hardware. |
| FAM-03 | Room v1→v2 data-preserving migration for featured, playlist refresh policy, and last-episode history. | VERIFIED | Migration/repository tests compile; SQLite migration simulation passed; connected device execution pending. |
| FAM-04 | Playlist episode navigation/recovery and manual-default / opt-in automatic refresh. | VERIFIED | Focused persistence/API tests passed; credentialed runtime remains required. |
| FAM-05 | Documentation, clean build/test/lint, secret scan, commit and safe push. | VERIFIED | 18 unit tests passed; lint 0 errors; debug & test APKs assembled; SQLite migration verified; docs/VERIFICATION.md updated; remote push verified. |

Universal family redesign (version 0.2.0) source, build verification, and repository documentation are VERIFIED. Device-only acceptance and credentialed YouTube Data API runtime checks remain pending an attached device/AVD and operator-supplied restricted API key.

| Task | Milestone and bounded deliverable | Depends on | Owner | Status | Acceptance / verification evidence required |
| --- | --- | --- | --- | --- | --- |
| P0-01 | Verify local Android SDK, Java, Gradle/Android Studio tooling; choose minimum/target SDK and document build strategy. | — | Codex | VERIFIED | Versions, environment checks, and selected SDK rationale recorded. |
| P0-02 | Investigate YouTube player, Android WebView, child-directed-client policies and navigation escapes; prototype only after user authorizes implementation. | — | Codex | BLOCKED | Decision on acceptable player residual risk, real-device test design, policy obligations in `DECISIONS.md`. |
| P0-03 | Select compatible stable AGP, Kotlin, Compose BOM, Jetpack, Room, and test dependency versions. | P0-01 | Codex | VERIFIED | Official compatibility links and pinned-version matrix. |
| P1-01 | Scaffold native Kotlin/Compose Android app, Gradle wrapper, local checks and README build commands. | P0-01, P0-03 | Codex | VERIFIED | Clean assemble, unit-test task, and lint tasks run with results. |
| P1-02 | Introduce navigation, app themes, core UI state and domain/repository interfaces. | P1-01 | Codex | BLOCKED | UI/navigation unit tests and project compiles. |
| P2-01 | Define Room entities/DAOs for approvals, categories, sort order, playlist snapshots and migrations. | P1-02 | Codex | BLOCKED | DAO tests including empty, duplicate, deletion and migration cases. |
| P2-02 | Implement offline Kids home and category navigation from approved-only repository data. | P2-01 | Codex | BLOCKED | Empty/populated/changed-data UI tests show approved-only content. |
| P3-01 | Design PIN lifecycle: setup, verification, rate limiting, lockout, reset/recovery policy. | P1-02 | Codex | VERIFIED | Threat model, PIN verification/lockout tests, no plaintext storage. |
| P3-02 | Implement hidden Parent Mode entry and route guarding; auto-return on finish, timeout/background. | P3-01, P2-02 | Codex | BLOCKED | Back stack, deep links, process recreation and resumed-app negative tests. |
| P4-01 | Integrate parent-only YouTube search/metadata with quotas, paging, errors, and safe API-key configuration. | P3-02, P0-02 | Codex | VERIFIED | Fake-network tests and documented real credential setup, no committed secret. |
| P4-02 | Parent CRUD: approve/unapprove/reorder videos and playlists; create/edit/reorder/delete categories. | P4-01, P2-01 | Codex | BLOCKED | Repository/UI tests, persistence after relaunch, rollback on failure. |
| P5-01 | Android Share import for approved YouTube URL shapes, preview and explicit parent approval. | P4-02 | Codex | BLOCKED | Share intent tests for valid, malformed, unsupported and deep-link abuse. |
| P5-02 | Playlist import snapshots; explicit parent review for refreshes/new remote entries. | P5-01 | Codex | BLOCKED | Remote playlist changes do not silently broaden Kids allowlist. |
| P6-01 | Official embedded player integration and allowlist launch checks; handle player error and navigation escape surfaces. | P0-02, P2-02, P3-02 | Codex | BLOCKED | Real-device proof, outbound navigation review, player smoke tests. |
| P6-02 | Parent preview and Kids playback state recovery; handling unavailable videos/offline state. | P6-01, P4-01 | Codex | BLOCKED | Lifecycle, loading, error, and playback smoke tests. |
| P7-01 | Privacy, security, quota/ToS review; accessibility/UX polish, devices and orientations. | P3-02, P5-02, P6-02 | Codex | BLOCKED | Recorded checklist; threat model regressions and manual checks. |
| P7-02 | Release readiness: clean build, tests, no-secret scan, verified acceptance matrix, documentation. | P7-01 | Codex | BLOCKED | Actual commands/results, issue list and explicit release decision. |

## Verification commands (defined after P1-01)
Once the Gradle wrapper and tasks exist, use (adjust module paths as scaffolded):

```bash
./gradlew --version
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
./gradlew :app:connectedDebugAndroidTest  # requires configured emulator/device
```

Commands are executable. In version 0.2.0 verification on 2026-11-10:
`assembleDebug`, `testDebugUnitTest` (18 tests), `lintDebug` (0 errors, 15 version notices), and `assembleDebugAndroidTest` passed with exit code 0.
`connectedDebugAndroidTest` exited with code 1 (`DeviceException: No connected devices!`) because no device or emulator is connected. See `docs/VERIFICATION.md`.
At a failure, record exact command, exit code, and relevant error; apply a bounded fix and rerun. Do not mark a task `VERIFIED` solely because files were written.

## How to update
- 2026-11-10 implementation boundary: P0-01/P0-03/P1-01 verified by actual toolchain/build/lint; P3-01 by design + verifier/lockout unit tests (device Keystore remains P3-02); P4-01 by fake-transport tests + key setup documentation. Universal family redesign tasks FAM-01 through FAM-05 are VERIFIED for source, build, unit test, and documentation scope. All other rows are implemented or reviewed but BLOCKED on their listed device/UI/policy acceptance evidence. BLOCKED does not mean no source exists. Next: provide an Android device/emulator and restricted API key, execute the smoke matrix in docs/VERIFICATION.md, complete child-directed obligations, and only then reassess release.
- 2026-11-10: Git initialized on `main`; starter kit saved to private GitHub repository `Yavagu/DivineTube`. P0-01 remains TODO: environment inventory has begun, but SDK selection and build strategy are not yet verified.
- At task start: set one row `ACTIVE`, assign its owner, add active task and file scopes to `PROJECT_STATUS.md`.
- At task end: set row `VERIFIED` with evidence in `PROJECT_STATUS.md` or `BLOCKED` with reason, then advance only if dependencies are verified.
- Release remains blocked by unmet runtime/policy gates. The explicit session authorization above permitted implementation while these checks were unavailable; it does not mark the checks passed.

## Public source publication — VERIFIED (2026-11-10)
- User explicitly requested public GitHub upload. Implementation committed as 0fcf1de and pushed to origin/main; GitHub API confirmed PUBLIC visibility for Yavagu/DivineTube.
- Credential-pattern scan passed across 54 publishable files and existing commit history; sensitive/generated filename scan passed. Local SDK settings, keys, APKs, and build outputs remain ignored.
- Publication does not certify an app release. Existing device/API/policy blockers remain unchanged. No build or runtime tests rerun for this publishing-only task.
