# YourTube — shared agent instructions

These instructions apply to both Codex and Google Antigravity. `PROJECT_SPEC.md` defines *what* to build; `PLAN.md` defines *what to do next*. Do not implement a feature contrary to either without resolving the discrepancy.

## Mission and engineering boundaries
- Build a native, privacy-conscious Android app that curates parent-approved YouTube videos/playlists for children. **Do not claim the embedded YouTube player is a complete content-safety boundary.**
- Kotlin, Jetpack Compose, Android Jetpack architecture; prefer unidirectional state, small testable components, Room for approved content, DataStore for settings, and platform-native sharing.
- Keep UI, domain/policy, persistence, and YouTube integration separate. No unrelated frameworks, backend, data collection, or third-party player without approval.
- Favor simple maintainable code and small reviewable changes. Verify current dependency versions, compatibility, licenses, and official policies before adding libraries.

## Security and quality
- Default-deny access to parent-only features. PIN checking must be rate-limited and resistant to casual bypass; never store plaintext PINs, secrets, or tokens in Git. Minimize Android permissions, WebView navigation, logging, tracking, and network calls.
- YouTube playback and API use must respect official terms, especially child-directed-client requirements and embed limits. Gate uncertain safety/compliance assumptions before implementing playback.
- Do not bypass approvals, controls, protected workflows, or edit production systems without permission. Ask before destructive, irreversible, externally impactful, or requirements-changing steps; preserve user edits.
- Test changed behavior (unit/instrumented/UI where relevant), review edge cases and security-sensitive paths, and report command, outcome, and unrun checks honestly. Fix bounded failures, rerun affected checks, then escalate blockers.

## Session start and delegation
1. Read `PROJECT_SPEC.md`, then the relevant sections of `PLAN.md`, `PROJECT_STATUS.md`, `HANDOFF.md`, and `DECISIONS.md`; do not reread unrelated files.
2. Inspect actual Git status/diff and relevant code. A handoff is a claim to verify, not proof. Select the next unblocked task from `PLAN.md`.
3. Main agent owns architecture, integration, verification, and shared documentation. Delegate only bounded work with **files owned**, acceptance criteria, and required tests; use at most two concurrent editing subagents and no overlapping edits. The main agent alone updates `PLAN.md`, `PROJECT_STATUS.md`, `HANDOFF.md`, and `DECISIONS.md` unless explicitly reassigned.
4. By default, only one platform edits this checkout at a time. Separate branches/worktrees plus assigned ownership and review are required for deliberate cross-platform concurrency. Markdown ownership is not a lock; if ownership is uncertain, stop and ask.

## Finish and handoff
- Change task to `VERIFIED` only after its acceptance checks genuinely pass; otherwise use `TODO`, `ACTIVE`, or `BLOCKED` and record missing evidence.
- At meaningful task boundaries update `PLAN.md`, the concise snapshot `PROJECT_STATUS.md`, and `HANDOFF.md`. Add to `DECISIONS.md` only for significant tradeoffs. Include actual tests, modified paths, Git state, blockers, and the next action.
- Shared files preserve explicit context, **not** private chats or live agent memory. No external broker, MCP bridge, paid API, or sync daemon is part of this kit.
