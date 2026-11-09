# YourTube — Antigravity entry point

Antigravity loads this file **and** root `AGENTS.md` as workspace instructions. Follow `AGENTS.md` as the shared authority; do not treat this file as a competing specification. These files are auto-loaded by supported Antigravity installations; `PROJECT_SPEC.md`, `PLAN.md`, `PROJECT_STATUS.md`, `HANDOFF.md`, and `DECISIONS.md` are **explicit reads**, not necessarily injected automatically.

At session start, read `PROJECT_SPEC.md`, the current task in `PLAN.md`, `PROJECT_STATUS.md`, the latest `HANDOFF.md`, and relevant `DECISIONS.md`. Check `git status`, branch, diff, files, and actual tests. If resuming from Codex, reconcile its recorded edits and outstanding work with the checkout before changing anything.

Use the custom `orchestrator` as primary agent when the Antigravity interface supports primary-agent selection. For suitable bounded subtasks, invoke `planner` for focused analysis and `implementer` for owned code/test changes via native subagent tooling; do **not** assume automatic delegation in every surface/version. Limit concurrent editors to two with separate file ownership; prefer one active editor. If invocation is unavailable, do the work in the primary agent or manually select an agent and copy a bounded task—do not imply parallel execution occurred.

The orchestrator owns integration, security review, final verification, and all shared plan/status/decision/handoff edits. On a meaningful pause, record exact test results, modified files, Git state and next action in `HANDOFF.md` so Codex can resume. Preserve pre-existing changes and ask if ownership is unclear. Never claim the YouTube embed guarantees a perfect content boundary.
