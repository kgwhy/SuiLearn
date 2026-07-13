## ADDED Requirements

### Requirement: Archived changes have one primary technical domain
The project SHALL store each archived OpenSpec change at
`openspec/changes/archive/<primary-domain>/YYYY-MM-DD-<change-name>/`. The
primary domain MUST be exactly one of `workflow-governance`,
`platform-runtime`, `learning-rag`, or `developer-tooling`.

#### Scenario: Archive a completed change into its primary domain
- **WHEN** an operator archives `example-change` with primary domain
  `platform-runtime` on 2026-07-13
- **THEN** the canonical archive directory is
  `openspec/changes/archive/platform-runtime/2026-07-13-example-change/`

#### Scenario: Reject an unknown primary domain
- **WHEN** an operator supplies a primary domain outside the configured domain
  set
- **THEN** the archive operation SHALL fail without moving the change

### Requirement: Archived changes have a single canonical location
The project MUST NOT duplicate an archived change directory across technical
domains. An archived change MAY have related-domain tags in the archive index,
but those tags MUST NOT create another copy of its artifacts.

#### Scenario: Record a cross-domain relationship
- **WHEN** an archived change's primary domain is `platform-runtime` and it
  also relates to workflow governance
- **THEN** the archive index records `workflow-governance` as a related-domain
  tag and the filesystem contains only the `platform-runtime` copy

### Requirement: Archive navigation is auditable
The project SHALL maintain `openspec/changes/archive/README.md` as a
navigation index. For every archived change, the index MUST record its primary
domain, date-prefixed change directory name, and optional related-domain tags.
The index MUST NOT replace or duplicate the change's historical artifacts.

#### Scenario: Find a change from the archive index
- **WHEN** an operator reads the archive index
- **THEN** the operator can locate each indexed change under the declared
  primary-domain directory

### Requirement: Project-local archive operations preserve active discovery
The repository-local archive command SHALL move only an active direct child of
`openspec/changes`, reject an existing target, and preserve the entire change
directory. Nested archive directories MUST NOT appear as active changes in
`openspec list --json`.

#### Scenario: Reject an archive destination collision
- **WHEN** the requested domain and date-prefixed target directory already
  exist
- **THEN** the archive command SHALL fail without moving the source directory

#### Scenario: Verify nested archives are not active
- **WHEN** a change has been moved under an archive domain
- **THEN** `openspec list --json` does not list that change as active
