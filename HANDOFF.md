# YourTube — cross-platform handoff

This file records the **latest meaningful task boundary**, not a transcript. The receiving platform must read `AGENTS.md`, `PROJECT_SPEC.md`, the relevant `PLAN.md` rows, `PROJECT_STATUS.md`, and `DECISIONS.md`, then verify `git status`, diff, branch, files, and tests before trusting any claim below. If the branch/worktree is unexpectedly dirty or another platform owns the active task, stop and clarify ownership. Never treat this document as a lock or automatic memory sync.

## Latest handoff: BOOTSTRAP
- **Recorded:** 2026-11-09
- **Source → destination:** Starter-kit generator → first Codex **or** Antigravity session
- **Task/objective:** Initialize repository workflow; no Android application work started.
- **Work completed:** Shared requirements, roadmap, role definitions, platform instructions, and startup prompts prepared.
- **Files modified:** Project workflow files only (`AGENTS.md`, `GEMINI.md`, `PROJECT_SPEC.md`, `PLAN.md`, `PROJECT_STATUS.md`, `HANDOFF.md`, `DECISIONS.md`, `.codex/`, `.agents/`, startup prompts, README, `.gitignore`).
- **Decisions:** See `DECISIONS.md` (particularly player safety and shared authority).
- **Tests executed/outcomes:** Android builds/tests **not run**; no app code. See README for starter ZIP validation, which does not prove local agent loading.
- **Git branch/HEAD/status:** Unknown after extraction; new working directory may not be a Git repository. Receiving agent must inspect and record it.
- **Uncommitted changes:** Unknown after extraction; no assumption of a clean checkout.
- **Outstanding problems:** Local tools/versions and YouTube embedding policy/child-directed applicability must be checked.
- **Next action:** Take `P0-01` in `PLAN.md`; review `P0-02` before committing to any playback design.

## Replace this section at the next meaningful handoff
Use the same fields in order; replace the **Latest handoff** section rather than appending endless session history. Keep unusually important architectural reasoning in `DECISIONS.md` instead. **Never record tokens, API keys, PINs, personal data, or raw secret-bearing logs.**

1. `Recorded`: ISO date/time with timezone.
2. `Source → destination`: Codex → Antigravity, Antigravity → Codex, or Same platform/new session.
3. `Task/objective`: PLAN task ID and desired outcome.
4. `Work completed`: implemented behavior only; mark unverified explicitly.
5. `Files modified`: concise relevant file paths.
6. `Decisions`: reference `DECISIONS.md` IDs.
7. `Tests executed/outcomes`: **exact command**, run environment, pass/fail/skip, and reason; distinguish not run.
8. `Git branch/HEAD/status`: branch name, abbreviated HEAD, staged/unstaged/untracked summary, relevant worktree; if unavailable say so.
9. `Uncommitted changes`: owned paths and any pre-existing/unrelated edits; never silently discard them.
10. `Outstanding problems`: blockers, failing tests, ambiguous ownership, open risks.
11. `Next action`: concrete next step, ownership, files to inspect, and verification to perform.

## Concurrent work rules
- Default: close/pause one editor's task and record this handoff before the other edits the same checkout.
- Advanced: separate Git branches/worktrees and non-overlapping file ownership; reconcile changes via review, never blind overwrite.
- A receiving agent cannot rely on private conversation context, model memory, or Markdown to synchronize live state. Actual Git and filesystem checks are mandatory.
