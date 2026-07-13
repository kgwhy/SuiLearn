## 1. Change controls

- [x] 1.1 Owner: Leader Agent. Created Major proposal, design, policy, capability spec, verification, and archive records. Allowed files: `openspec/changes/reorganize-openspec-archive/**`. Forbidden files: `apps/**`, `services/**`, `contracts/**`, and current-fact documents. Test command: `openspec validate reorganize-openspec-archive --strict` (passed). Review focus: the change is independent of product work.

## 2. Archive command

- [x] 2.1 Owner: Leader Agent. Added the disposable-fixture test and project-local archive command. Allowed files: `scripts/test-archive-openspec-change.ps1`, `scripts/archive-openspec-change.ps1`. Forbidden files: application code and contracts. Test command: `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/test-archive-openspec-change.ps1` (passed). Review focus: explicit domain validation, collision handling, reserved-directory rejection, index consistency, and cleanup.

## 3. Workflow guidance

- [x] 3.1 Owner: Leader Agent. Added the single-primary-domain archive rule and local-command guidance. Allowed files: `.agents/skills/suilearn-workflow/SKILL.md`, `.agents/skills/suilearn-workflow/references/archive-organization.md`. Forbidden files: `docs/proposals/**`, `docs/superpowers/**`, application code, and contracts. Test command: `powershell -ExecutionPolicy Bypass -File .agents/skills/suilearn-workflow/scripts/check-skill.ps1` (passed). Review focus: no second workflow and active changes remain direct children.

## 4. Historical migration

- [x] 4.1 Owner: Leader Agent. Moved each of 24 flat archive leaves into one approved domain and created the navigation index. Allowed files: `openspec/changes/archive/**`. Forbidden files: `openspec/changes/build-resilient-knowledge-pipeline/**`, application code, contracts, and current-fact documents. Test command: archive scan (passed: 0 flat leaves, 24 nested leaves, 0 missing links). Review focus: each historical change has one canonical location.

## 5. Sync, verify, and review

- [x] 5.1 Owner: Leader Agent. Synced `capability-domain-archive` to its sole allowed main-spec path; ran final command, skill, OpenSpec, active-discovery, residue/index, and diff-stat checks; completed independent review closure. Allowed files: `openspec/specs/capability-domain-archive/spec.md`, `openspec/changes/reorganize-openspec-archive/verification.md`. Forbidden files: other main specs, application code, contracts, and unrelated active changes. Test commands: all passed. Review focus: no flat residue, no active nested archive, and all changed files remain in policy.
