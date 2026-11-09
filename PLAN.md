# YourTube — canonical implementation plan

**Status vocabulary:** `TODO` (not begun), `ACTIVE` (in progress), `BLOCKED` (cannot proceed), `VERIFIED` (all listed checks passed and evidence recorded). `VERIFIED` is never inferred from documentation alone. Owner is the **current explicit assignee**, not a permanent role. Only one task is `ACTIVE` by default.

**Initial state:** no Android application has been implemented. Phase 0 is the next milestone. The main agent records owners and evidence after actual work, and may assign independent bounded implementation tasks to its helpers without overlapping file edits. `PROJECT_STATUS.md` is the current snapshot; this file is the task source of truth.

| Task | Milestone and bounded deliverable | Depends on | Owner | Status | Acceptance / verification evidence required |
| --- | --- | --- | --- | --- | --- |
| P0-01 | Verify local Android SDK, Java, Gradle/Android Studio tooling; choose minimum/target SDK and document build strategy. | — | Unassigned | TODO | Versions, environment checks, and selected SDK rationale recorded. |
| P0-02 | Investigate YouTube player, Android WebView, child-directed-client policies and navigation escapes; prototype only after user authorizes implementation. | — | Unassigned | TODO | Decision on acceptable player residual risk, real-device test design, policy obligations in `DECISIONS.md`. |
| P0-03 | Select compatible stable AGP, Kotlin, Compose BOM, Jetpack, Room, and test dependency versions. | P0-01 | Unassigned | TODO | Official compatibility links and pinned-version matrix. |
| P1-01 | Scaffold native Kotlin/Compose Android app, Gradle wrapper, local checks and README build commands. | P0-01, P0-03 | Unassigned | TODO | Clean assemble, unit-test task, and lint tasks run with results. |
| P1-02 | Introduce navigation, app themes, core UI state and domain/repository interfaces. | P1-01 | Unassigned | TODO | UI/navigation unit tests and project compiles. |
| P2-01 | Define Room entities/DAOs for approvals, categories, sort order, playlist snapshots and migrations. | P1-02 | Unassigned | TODO | DAO tests including empty, duplicate, deletion and migration cases. |
| P2-02 | Implement offline Kids home and category navigation from approved-only repository data. | P2-01 | Unassigned | TODO | Empty/populated/changed-data UI tests show approved-only content. |
| P3-01 | Design PIN lifecycle: setup, verification, rate limiting, lockout, reset/recovery policy. | P1-02 | Unassigned | TODO | Threat model, PIN verification/lockout tests, no plaintext storage. |
| P3-02 | Implement hidden Parent Mode entry and route guarding; auto-return on finish, timeout/background. | P3-01, P2-02 | Unassigned | TODO | Back stack, deep links, process recreation and resumed-app negative tests. |
| P4-01 | Integrate parent-only YouTube search/metadata with quotas, paging, errors, and safe API-key configuration. | P3-02, P0-02 | Unassigned | TODO | Fake-network tests and documented real credential setup, no committed secret. |
| P4-02 | Parent CRUD: approve/unapprove/reorder videos and playlists; create/edit/reorder/delete categories. | P4-01, P2-01 | Unassigned | TODO | Repository/UI tests, persistence after relaunch, rollback on failure. |
| P5-01 | Android Share import for approved YouTube URL shapes, preview and explicit parent approval. | P4-02 | Unassigned | TODO | Share intent tests for valid, malformed, unsupported and deep-link abuse. |
| P5-02 | Playlist import snapshots; explicit parent review for refreshes/new remote entries. | P5-01 | Unassigned | TODO | Remote playlist changes do not silently broaden Kids allowlist. |
| P6-01 | Official embedded player integration and allowlist launch checks; handle player error and navigation escape surfaces. | P0-02, P2-02, P3-02 | Unassigned | TODO | Real-device proof, outbound navigation review, player smoke tests. |
| P6-02 | Parent preview and Kids playback state recovery; handling unavailable videos/offline state. | P6-01, P4-01 | Unassigned | TODO | Lifecycle, loading, error, and playback smoke tests. |
| P7-01 | Privacy, security, quota/ToS review; accessibility/UX polish, devices and orientations. | P3-02, P5-02, P6-02 | Unassigned | TODO | Recorded checklist; threat model regressions and manual checks. |
| P7-02 | Release readiness: clean build, tests, no-secret scan, verified acceptance matrix, documentation. | P7-01 | Unassigned | TODO | Actual commands/results, issue list and explicit release decision. |

## Verification commands (defined after P1-01)
Once the Gradle wrapper and tasks exist, use (adjust module paths as scaffolded):

```bash
./gradlew --version
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
./gradlew :app:connectedDebugAndroidTest  # requires configured emulator/device
```

Commands above are **planned, not yet executable** in this starter kit; no Gradle wrapper or `app/` module exists. At a failure, record exact command, exit code, and relevant error; apply a bounded fix and rerun. Do not mark a task `VERIFIED` solely because files were written.

## How to update
- 2026-11-10: Git initialized on `main`; starter kit saved to private GitHub repository `Yavagu/DivineTube`. P0-01 remains TODO: environment inventory has begun, but SDK selection and build strategy are not yet verified.
- At task start: set one row `ACTIVE`, assign its owner, add active task and file scopes to `PROJECT_STATUS.md`.
- At task end: set row `VERIFIED` with evidence in `PROJECT_STATUS.md` or `BLOCKED` with reason, then advance only if dependencies are verified.
- Do not start dependent tasks while an unmet gate is `TODO` or `BLOCKED`.
