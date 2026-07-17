# Policy: local runtime configuration templates

## Change information

- Change: `align-local-runtime-config-templates`
- Status: Verify complete; ready for archive
- Level: Major
- Owner: Leader Agent
- Base ref: `d2926b7533765c88290d8ac729e820cb75224b49`
- Worktree/lock: current workspace; no active `.agents/locks` entry.

## Scope

Allowed implementation files:

- `services/api/config/local.properties.example`
- `.env.example`
- `scripts/check-suilearn-workflow.ps1`
- `openspec/changes/align-local-runtime-config-templates/**`

Forbidden files:

- `compose.yml`
- `services/api/src/**`
- `apps/**`
- `contracts/**`
- `docs/**`
- other `openspec/changes/**` and `openspec/changes/archive/**`

The active change's own `verification.md` and `archive.md` are permitted
Major-change closeout artifacts.

## Required semantics

- The canonical retry setting is `suilearn.adapter.max-retries=0`; the
  deprecated `suilearn.ai.max-retries` must not appear in the local template.
- The local template must configure a host-run API to reach root Compose
  RabbitMQ and MinIO using the same documented local-only credentials as
  `.env.example` and Compose fallbacks.
- Environment variables override template values; production credentials are
  never added to example files.
- All durable-pipeline template settings must preserve the existing application
  defaults unless explicitly overridden for local Compose connectivity.

## Acceptance matrix

| Surface | Default/override semantics | Validation |
| --- | --- | --- |
| `local.properties.example` | Host API uses localhost service endpoints and documented local-only credentials; environment variables can override sensitive values. | Property-key and retry-key scan; Spring configuration smoke test. |
| `.env.example` | Copying the file produces the same RabbitMQ and MinIO credentials as Compose fallbacks. | `docker compose config` renders the expected values. |
| Retry | Only `suilearn.adapter.max-retries=0` is documented; legacy key is absent. | Residual scan plus adapter configuration test. |

## Verification plan

- Run the affected backend configuration tests.
- Render `docker compose config`.
- Scan templates for deprecated retry keys and mismatched local credential
  literals.
- Run `git diff --check` and the workflow checker against the base ref.
- Reproduce the archived-delta checker failure before repair, then verify the
  normal checker and its negative self-test after repair.
