# YourTube — Codex + Antigravity development starter kit

This is a **configuration and context kit only**. It contains **no Android application source, Gradle wrapper, APK, secret, API bridge, or service**. Both platforms use the same repo and the same `PLAN.md`, `PROJECT_STATUS.md`, `DECISIONS.md`, and `HANDOFF.md`. Their private conversations and live agent memories do **not** synchronize.

## 1. Extract and prepare

1. Extract the ZIP. Open the contained **`project/`** folder as the repository/workspace root (or copy its contents to the root of your existing project). Keep the `.codex/`, `.agents/`, and `.gitignore` hidden folders/files; on Windows enable **View > Hidden items** if necessary.
2. Use Git to track actual project progress. For an empty directory you may run `git init` and commit the starter files after review. For an existing repo, **do not overwrite** pre-existing `AGENTS.md`, plans, or work; merge carefully.
3. Install current Codex CLI or IDE extension and Google Antigravity App/CLI, signing in with your **ChatGPT Plus** and **Google AI Pro** accounts respectively. Confirm your subscription and model quotas inside each product; neither subscription guarantees separate API-key access or unlimited usage.
4. For future Android development, install Android Studio, Android SDK, JDK and an emulator or physical device; the first plan tasks must choose and validate versions. YouTube Data API usage later requires your own Google Cloud project/API setup and policy review. No API credential is supplied.

## 2. Start with OpenAI Codex

1. Open `project/` in Codex CLI, Codex IDE extension, or local Codex app. **Trust the project** when asked: Codex deliberately skips project-local `.codex/` configuration for untrusted checkouts.
2. Its `.codex/config.toml` requests **`gpt-6-astra` / `medium`** for the main agent and caps concurrent spawned threads at **2**. The files `.codex/agents/planner.toml` and `grunt.toml` request `gpt-6-luna` with medium/low reasoning respectively.
3. Check available models with the client model selector or CLI `/model`. If Astra is unavailable to your signed-in Plus account/client, override the project default for the session, for example `codex -m gpt-6.1-sol` (choose medium), or update `.codex/config.toml` **after verifying availability**. If Luna is unavailable, replace it in both agent TOMLs and the `[agents]` default with an accessible compatible model; confirm its supported reasoning efforts. Do not set up API billing just to use this kit.
4. Confirm custom agents are recognized by asking: **“Which project custom agents are available? Do you see `planner` and `grunt`? Do not edit files.”** If the client has agent UI, inspect them there. The agents are project-scoped, not the root `AGENTS.md` document itself.
5. Paste the entire `START_PROMPT.txt`. It instructs the root to inspect Git and the shared context, then carry out the next unblocked task. Delegation is selective rather than guaranteed for every task.

CLI example (from the `project/` directory):

```bash
codex -m gpt-6-astra
```

The model argument uses the local signed-in Codex account; it is **not** an API client call. On Windows you can run the equivalent command in PowerShell if Codex CLI is installed.

## 3. Start with Google Antigravity

1. Open **the same `project/` directory** in Antigravity App, CLI, or supported IDE surface. Select the project/workspace and sign in with Google AI Pro. Ensure the agent has appropriate workspace read/write permissions; keep terminal execution sandboxed and review approvals.
2. Antigravity automatically loads both root `AGENTS.md` and `GEMINI.md`. The project custom agents live at:
   - `.agents/agents/orchestrator/agent.md` — selectable primary agent, `model: pro` tier;
   - `.agents/agents/planner/agent.md` — callable planning subagent, `model: flash` tier;
   - `.agents/agents/implementer/agent.md` — callable bounded implementation subagent, `model: flash` tier.
3. In Antigravity CLI open `/agents` to inspect or choose the `orchestrator` primary agent; in the App use the corresponding custom-agent selector if available. Ask **“Which workspace custom agents and rules did you load? Do not edit files.”** Confirm the three agents and root rules. If agents are not shown, confirm you opened the repository root, restart/reload the app after adding definitions, and check the installed version's custom-agent support.
4. Select an available strong primary model in your UI and appropriate thinking level. Here `pro`/`flash` are **Antigravity custom-subagent model tiers**, not specific Gemini release identifiers; manually choose the exact available model in the primary model selector. Do not invent an exact ID in these files.
5. Paste the entire `START_ANTIGRAVITY.txt`. On supported App/CLI versions the primary agent can use native `invoke_subagent` for `planner` or `implementer`. If your IDE/surface cannot invoke them, work in the main agent or manually select an agent and pass a scoped task. Do not assume a seamless multi-agent automation when the UI doesn't provide it.

You do **not** need to invoke `/teamwork` or `/boost`: those are separate platform orchestration features, not required for this two-subagent workflow. No external bridge or billed Gemini API is installed.

## 4. How context is loaded

| File | Codex | Antigravity |
| --- | --- | --- |
| `AGENTS.md` | Automatically discovered repo instructions | Automatically loaded workspace rule |
| `GEMINI.md` | Not a Codex auto-instruction file; read only if needed | Automatically loaded workspace rule |
| `PROJECT_SPEC.md`, `PLAN.md`, `PROJECT_STATUS.md`, `HANDOFF.md`, `DECISIONS.md` | Explicitly read at start as relevant | Explicitly read at start as relevant |
| `.codex/config.toml`, `.codex/agents/*.toml` | Codex config and custom agents (trusted project) | Not native Antigravity config |
| `.agents/agents/*/agent.md` | Not native Codex agent config | Native custom agents for supported App/CLI |

Keep `AGENTS.md` short and canonical; `GEMINI.md` adds only Antigravity-specific guidance. The two start prompts both resume **the same** `PLAN.md`; neither regenerates an independent roadmap.

## 5. Switch safely between platforms

1. Finish or pause the active task, run applicable tests, inspect `git status` and changes, then update `PLAN.md`, `PROJECT_STATUS.md`, significant decisions, and the latest section of `HANDOFF.md`.
2. Prefer committing a reviewed, verified boundary (a commit is recommended, **not automatic**). If changes remain uncommitted, list those paths and reasons in `HANDOFF.md`.
3. **Stop the editing session** in the first platform. Open the exact same working tree in the other, read the shared context, verify branch/diff/tests, then claim the next task.
4. Do not let two platforms edit the same files simultaneously. If intentional parallel work is needed, use separate Git worktrees/branches and explicit owners; review and integrate deliberately. Markdown ownership alone is not a lock.

## 6. Verification and troubleshooting

Starter-kit inspection can be done with any ZIP reader plus TOML/YAML validation. A typical standard-library TOML check (Python 3.11+) is:

```bash
python -c "import pathlib,tomllib; [tomllib.loads(p.read_text()) for p in pathlib.Path('.codex').rglob('*.toml')]; print('TOML valid')"
```

For native recognition you **must test on your own installed clients**: the generator cannot authenticate your accounts or execute Codex/Antigravity on your device. If Codex ignores configs, check project trust, installed version, and model access. If Antigravity doesn't list an agent, check `.agents/agents/<name>/agent.md`, YAML frontmatter, workspace root, and reload. If only one of its surfaces supports invocation, use the documented manual fallback. Check available permissions before running shell commands.

For Android implementation **after P1-01**, expected checks include:

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
./gradlew :app:connectedDebugAndroidTest  # only with device/emulator
```

On Windows use `gradlew.bat`. **These commands cannot run yet** because this kit intentionally does not include application code or a Gradle wrapper. When tasks begin, adjust exact module paths and commands to the actual scaffold and record real results in `PROJECT_STATUS.md`.

### Safety gate before video playback

YourTube is intended to display parent-approved YouTube videos, but the **official embedded player may offer links or suggestions that your app cannot fully suppress**. It is not an OS-level kiosk and does not guarantee that a child cannot reach unapproved content or leave the app. The first phase explicitly reviews this with actual device testing and the current child-directed-client API policies. Do not skip that gate or describe the app as a fully sealed content allowlist.

### No additional API charges by default

This kit uses the signed-in Codex and Antigravity apps/CLI, not separately billed OpenAI/Gemini API integrations. Future use of the YouTube **Data API** is a distinct Google Cloud service with quota/policy conditions; it is not Gemini API access. Google/YouTube may still require setup and compliance steps independently of your AI subscriptions.

## 7. Reference documentation (checked 2026-11-09)

- [OpenAI — Codex project configuration and trust](https://developers.openai.com/codex/config-basic)
- [OpenAI — Codex subagents and custom TOML schema](https://developers.openai.com/codex/multi-agent)
- [OpenAI — Codex model selection](https://learn.chatgpt.com/docs/models)
- [Google Antigravity — rules and auto-loaded files](https://antigravity.google/docs/rules/)
- [Google Antigravity — custom subagents, paths, and YAML schema](https://antigravity.google/docs/subagents)
- [Google Antigravity — model tiers and subscription access](https://antigravity.google/docs/models)
- [YouTube — API terms and child-directed clients](https://developers.google.com/youtube/terms/api-services-terms-of-service)
- [YouTube — IFrame Player reference](https://developers.google.com/youtube/iframe_api_reference)

## 8. Compatibility and limits

- Local Codex releases support `.codex/config.toml` plus `.codex/agents/*.toml`; model availability and usage depend on the signed-in plan, client version, and rollout. Root `AGENTS.md` is discovered automatically; other markdown project context is not.
- Antigravity App/CLI supports `.agents/agents/<name>/agent.md` custom agent definitions with YAML frontmatter. Native selection/delegation behavior can differ by Antigravity surface/version, and model identifiers shown to users may differ from the supported `pro`/`flash` custom-agent tiers. Primary model/thinking level and permissions require manual selection.
- Neither platform confirms live agent memory from the other. No tests of real signed-in agent loading, Android compilation, YouTube credentials, physical-device player behavior, or child-directed compliance were possible when generating this ZIP.
