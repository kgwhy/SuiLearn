## Why

The current date-flat archive makes it difficult to discover the history of a
technical capability, especially workflow governance and runtime changes that
evolve over multiple independent changes. The project needs a stable,
auditable capability-domain archive structure before more history accumulates.

## What Changes

- Group archived OpenSpec changes below a single technical capability domain
  while retaining the existing date-prefixed change directory name.
- Define four initial domains: `workflow-governance`, `platform-runtime`,
  `learning-rag`, and `developer-tooling`.
- Add an archive index that records each change's primary domain and optional
  related-domain tags without duplicating its files.
- Update the project archive workflow so future archives select exactly one
  primary domain and validate the nested target path.
- Migrate existing archived changes into the applicable domain directories.

**BREAKING**: Consumers that assume archives are immediate children of
`openspec/changes/archive/` must traverse one additional domain directory.

## Capabilities

### New Capabilities

- `capability-domain-archive`: Organize OpenSpec archive history by one
  primary technical capability domain with an auditable index.

### Modified Capabilities

None.

## Impact

- Affects `openspec/changes/archive/**` and the project OpenSpec archive
  guidance.
- Does not change application code, API contracts, or product behavior.
- Requires validating the installed OpenSpec CLI's active-change discovery
  against nested archive directories.
