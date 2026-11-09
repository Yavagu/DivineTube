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

## ADR-003 — Official YouTube embed with safety feasibility gate (Proposed, 2026-11-09)
- **Choice:** Parent-only Data API discovery plus official embedded playback; use a local approved-item snapshot for kids' launch targets.
- **Reasoning:** Complies with the intended official playback path and avoids prohibited stream extraction.
- **Risks/tradeoffs:** Player UI and external navigation are not a complete allowlist boundary; child-directed API terms and embed requirements may affect feasibility. P0-02 must establish current policy and real-device behavior; strict containment may require changing the product promise.

## ADR-004 — Credentials and PINs stay out of Git (Accepted, 2026-11-09)
- **Choice:** No hardcoded secrets or plaintext PIN. If an Android-client YouTube API key is used, restrict it through Google Cloud and treat it as extractable; never embed a server secret. PIN verification needs slow salted storage and attempt throttling.
- **Reasoning:** Device apps cannot conceal embedded static credentials; a four-digit PIN alone is weak without rate limiting.
- **Tradeoff:** Cloud Console setup required for search; potential stronger security/auth architecture needs separate review.

## ADR-005 — No auto-refresh expansion of kids' playlist allowlist (Accepted, 2026-11-09)
- **Choice:** Approving a playlist captures reviewed video IDs. Any newly added remote item needs renewed parent approval before reaching Kids Mode.
- **Reasoning:** Dynamic publisher playlists could silently introduce unapproved videos.
- **Tradeoff:** Parents must approve updates; remote deletion/unavailability still needs graceful handling.
