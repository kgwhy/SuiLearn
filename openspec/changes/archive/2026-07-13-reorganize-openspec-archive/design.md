## Context

OpenSpec stores active changes directly below `openspec/changes/` and the
current archive is a flat collection of date-prefixed directories. The project
needs a durable capability-oriented history without changing active-change
resolution or duplicating archived files. The installed generic archive skill
currently targets the flat archive path, so the project needs a local,
validated archive entry point.

## Goals / Non-Goals

**Goals:**

- Store every archived change beneath exactly one technical capability domain.
- Keep the date-prefixed change directory unchanged inside its domain.
- Provide a repository-local archive command that validates the domain and
  prevents overwriting an existing destination.
- Maintain an auditable archive index with the primary domain and optional
  related-domain tags.
- Migrate every existing archive directory without losing files.

**Non-Goals:**

- Change the layout or semantics of active changes.
- Duplicate one archived change into several domain directories.
- Rename archive change directories or rewrite their historical artifacts.
- Synchronize delta specs as part of this structural migration.

## Decisions

### Use one fixed primary domain per archived change

The initial domain set is `workflow-governance`, `platform-runtime`,
`learning-rag`, and `developer-tooling`. Each archive operation requires one
of these values. Related domains are index metadata only. This is preferred to
multi-home storage because there is a single canonical filesystem location for
each historical change.

### Keep date-prefixed change directories as leaf nodes

The resulting layout is
`openspec/changes/archive/<domain>/YYYY-MM-DD-<change-name>/`. Retaining the
leaf name preserves chronological context, existing references, and rollback
clarity while making capability history browsable.

### Provide a project-local PowerShell archive command

`scripts/archive-openspec-change.ps1` will take a change name, primary domain,
and optional related domains. It will verify that the source is an active
direct child of `openspec/changes`, validate the domain, reject an existing
target, move the directory, and update the index. This is preferred to
modifying a global skill because it is versioned with the repository and can
be tested here.

### Treat the archive index as a navigation record

`openspec/changes/archive/README.md` will list primary domain, date, change
name, and related-domain tags. The archived change directory remains the
authoritative historical artifact; the index must not copy proposal, task, or
spec content.

## Risks / Trade-offs

- [Tools assume a flat archive] → Validate `openspec list --json` after a
  nested archive and document the project-local command as the required entry
  point.
- [Incorrect domain assignment] → Require an explicit primary-domain argument
  and record it in the index; do not infer it from a name.
- [Partial migration] → Precompute every source and destination, reject any
  existing target, and verify source absence plus destination presence after
  each move.
- [Index and filesystem diverge] → The command derives its entry from the
  completed move and tests index output against archive directories.

## Migration Plan

1. Add the archive domain registry, local command, index, and workflow
   guidance.
2. Verify the command on a temporary fixture outside the real archive.
3. Move existing archive directories into one of the four domains and generate
   the index from that mapping.
4. Run OpenSpec active-change discovery and a repository scan to verify that
   no archive directory remains at the old flat level.
5. Roll back before commit by moving directories back to their recorded flat
   paths and restoring the prior index if a validation step fails.

## Open Questions

None. The approved policy is a single primary domain with optional index-only
related-domain tags.
