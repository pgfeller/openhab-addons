# Task Consolidation

Merge fragmented Spec Kit tasks into fewer, complete implementation tasks while
preserving requirements, tests and progress.

The agent reads your spec, plan, tasks and relevant code, then merges work that
belongs together. For example, a service change, its facade, callers and tests
can become one task. Keeping them separate requires concrete evidence.

## Install

Requires **Spec Kit 1.2.1+**, an initialized project and an agent integration.
Clone this repository, then run from your project's root:

```bash
specify extension add --dev /absolute/path/to/spec-kit-extension-task-consolidation
```

In `.specify/extensions.yml`, place its `after_tasks` hook before commit or publish
hooks. Keep it enabled, with `optional: false` and no condition. Check the order
again after reinstalling; Spec Kit 1.2.1 uses YAML order rather than `priority`.

## Use

Run `/speckit.tasks` as usual. Consolidation runs automatically after task generation.

To consolidate an existing task list, run in your coding agent:

```text
/speckit.task-consolidation.consolidate
```

In Codex skills mode, use `$speckit-task-consolidation-consolidate`.
The feature must already have `spec.md`, `plan.md` and `tasks.md`.

## Results

- **`tasks.md`** — merged tasks with requirements, tests, dependencies and progress preserved.
- **`consolidation-review.md`** — merge reasons, code evidence and any ID mappings.
- **Console report** — `TASK CONSOLIDATION STARTED`, followed by a mandatory
  `TASK CONSOLIDATION RESULT` showing before/after counts, merges, retained boundaries,
  evidence, saved files, checks and limitations. No-merge and failed runs report too.

Only the two Markdown files change. The extension does not implement tasks or run
application tests. It has no target task count; if nothing merges, it must explain
why the strongest candidate stayed separate. See the [command](commands/consolidate.md)
for the full review rules and report template.

## Update or remove

After updating your clone, reinstall from your project's root:

```bash
specify extension add --dev /absolute/path/to/spec-kit-extension-task-consolidation --force
```

To uninstall:

```bash
specify extension remove task-consolidation
```

Migrating from the old preset? Remove it with `specify preset remove task-consolidation`
and remove its task-generation wrapper before installing. Keep one consolidation hook.

## Compatibility

Spec Kit 1.2.1 is the tested installation baseline; newer versions are allowed without
an upper bound. The current consolidation instructions still need an independent
execution trial. PowerShell and Python execution remain untested.
