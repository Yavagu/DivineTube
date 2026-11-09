# YourTube — current state snapshot

**Updated:** 2026-11-10 (Asia/Calcutta)

**Phase:** First Android development build delivered; device/API/release verification blocked.

**Active task:** None running. Next action: connected-device and credentialed playback verification from `docs/VERIFICATION.md`.

**Current platform/owner:** Codex main. Bounded storage/security and YouTube subagents finished; no other platform edits were observed. Shared documentation remains main-agent-owned.

**Repository:** Public at https://github.com/Yavagu/DivineTube, branch main tracking origin/main. Implementation committed as 0fcf1de and pushed on 2026-11-10 at the user's explicit request; publication handoff recorded in the following commit. Source publication does not clear release gates.

## Implemented
- Native Compose/Material 3 Kids library with approved-only launch targets, category thumbnails, offline/empty/error handling.
- Hidden long-press parent PIN entry, setup/confirmation/privacy acknowledgement, Keystore-protected salted PBKDF2 verifier, persistent exponential throttling, background/Back/Done/idle locking.
- Room v1 categories, approvals/order, playlist snapshots; DataStore PIN settings. Category CRUD/order, content assignment/removal/order, explicit snapshot refresh.
- Strict pasted/shared YouTube-link review and explicit approval; share intake always PIN-gated. Manual playlist snapshot entry works without credentials.
- Official Data API search, paging, fetched snapshots, playback status lookup, Android-restricted-key headers, bounded network/error handling.
- Official privacy-enhanced IFrame integration with package identity, controls, saved-ID Previous/Next, timeout/error UI, teardown. This is source implementation, not proof of on-device playback.

## Verified build evidence
- JDK 21.0.8; Gradle 8.13; SDK 36.1; compile 36.1 / target 36 / min 26.
- `gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest` passed (exit 0).
- 17 JVM tests passed. Lint: 0 errors, 14 dependency-version notices. Android test APK compiled.
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk` (no API key configured).
- `aapt dump badging` verified package/launcher/min/target metadata; installation/launch NOT tested.
- `connectedDebugAndroidTest` attempted, failed with `No connected devices!`; no device tests executed. No AVD was configured.

## Genuine blockers / limitations
- No connected Android device/emulator: UI, startup, actual Keystore/Room persistence, lifecycle, and player runtime checks remain open.
- No YouTube Data API key supplied: real search and playback eligibility/embedded playback remain untested. Keyless approvals are supported; keyless playback is intentionally blocked by YouTube's mandatory Made for Kids lookup.
- No child-directed Google registration, privacy-policy publication, legal/retention/branding audit, or real-player navigation-escape acceptance performed. Not release-ready and not a complete safety boundary.
- API-fetched titles remain transient; persisted labels are explicit local parent labels. Approval/bookmark retention still belongs in the release policy review.

## Changed paths / handoff
New `app/`, root Gradle files/wrapper, `docs/PRIVACY.md`, `docs/VERIFICATION.md`; README replaced with build/install/key instructions and original starter README preserved as `docs/AGENT_SETUP.md`. PLAN, PROJECT_STATUS, HANDOFF and DECISIONS updated. Local SDK config is ignored; generated APKs/reports/build caches remain ignored. Existing agent configurations/instructions and PROJECT_SPEC are preserved.

See `docs/VERIFICATION.md` for command history, tests, artifact hash, acceptance matrix, and concrete device smoke sequence. See ADR-006–008 for dependency, playback-policy and security choices.
