# Policy

## Owner

Leader Agent.

## Base Ref

`95de95c1b12a6a72243416f1b1e344ee2f9013fb`

## Mode

Strict serial execution. No parallel subagents were needed for the bootstrap
document rewrite.

## Allowed Files

- `AGENTS.md`
- `docs/development-workflow.md`
- `docs/index.md`
- `docs/proposals/README.md`
- `agents/*.md`
- `openspec/config.yaml`
- `openspec/changes/adopt-suilearn-workflow/**`
- `.agents/skills/suilearn-workflow/**`
- `scripts/check-suilearn-workflow.ps1`

## Forbidden Files

- `apps/android/**`
- `services/api/**`
- `apps/web/**`
- `contracts/**`
- `docs/product-requirements.md`
- `docs/architecture.md`
- `docs/tech-selection.md`

## Tests

Workflow-only change. Business module tests are not applicable. Run:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/check-suilearn-workflow.ps1 -BaseRef 95de95c1b12a6a72243416f1b1e344ee2f9013fb
```
