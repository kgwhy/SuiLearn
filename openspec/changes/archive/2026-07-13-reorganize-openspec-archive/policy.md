# Archive reorganization policy

## Basic information

- Change: `reorganize-openspec-archive`
- Level: Major
- State: Build, approved by the user after design review
- Owner: Leader Agent
- base_ref: `09972deabe46d7160e9f2f885b2007a27d412d88`
- Execution mode: serial; no durable lock is required because `.agents/locks`
  does not exist and no concurrent task has claimed the target paths.

## Allowed files

- `openspec/changes/reorganize-openspec-archive/**`
- `openspec/changes/archive/**`
- `openspec/specs/capability-domain-archive/spec.md`
- `scripts/archive-openspec-change.ps1`
- `scripts/test-archive-openspec-change.ps1`
- `.agents/skills/suilearn-workflow/SKILL.md`
- `.agents/skills/suilearn-workflow/references/archive-organization.md`

## Forbidden files

- `openspec/changes/build-resilient-knowledge-pipeline/**`
- all other `openspec/specs/**` files
- all other active change directories
- `apps/**`
- `services/**`
- `contracts/**`
- `docs/product-requirements.md`
- `docs/architecture.md`
- `docs/tech-selection.md`
- `docs/proposals/**`
- `docs/superpowers/**`

## Acceptance matrix

| Case | Expected result |
| --- | --- |
| Valid primary domain | The command moves a complete direct-child change under that domain and appends an index entry. |
| Invalid primary domain | The command fails before moving or indexing anything. |
| Existing target | The command fails without replacing or moving a source. |
| Related domains omitted | The index records no related-domain tag; there is no implicit default primary domain. |
| Nested archive | `openspec list --json` does not report it as active. |
| Historical migration | Every former flat archive leaf exists exactly once below a valid domain, with no remaining flat leaf. |

## Verification plan

- This is a workflow and filesystem-organization change; application module
  baseline tests are not applicable because no application code is modified.
- The PowerShell test is the behavioral baseline for archive validation and
  movement. It uses a disposable fixture and cleans it in `finally`.
- Run OpenSpec strict validation, workflow skill validation, `openspec list
  --json`, an archive index/residue scan, and the base-ref diff stat before
  declaring completion.

## Review focus

- No operation infers a domain or copies an archived change to multiple paths.
- Archive index entries describe the filesystem rather than becoming a second
  source of historical truth.
- Existing uncommitted user changes remain outside this change's scope.
