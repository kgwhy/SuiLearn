# Migrate PostgreSQL LOB Text Storage

## Why

Production PostgreSQL requests can fail with `Unable to access lob stream` when Hibernate maps `String @Lob` fields to PostgreSQL Large Object columns. The immediate transactional read workaround keeps current endpoints alive, but it leaves the schema dependent on driver-managed large objects and autocommit-sensitive reads.

## What

- Replace service API `String @Lob` entity mappings with ordinary text columns.
- Add a PostgreSQL startup migration that converts legacy `oid` large-object columns to `text` using `lo_get`.
- Keep H2 tests and local development no-op for the PostgreSQL-only migration.
- Add backend tests for entity mappings and migration SQL behavior.

## Scope

- In scope: `services/api/**` persistence entities, backend migration code, and backend tests.
- In scope: this OpenSpec change directory.
- Out of scope: API contract changes, Android/Web changes, new dependencies, and external migration tooling such as Flyway.

## Acceptance Criteria

- No JPA entity in `services/api/src/main/java/com/suilearn/api/persistence/entity` uses `@Lob`.
- PostgreSQL `oid` columns created by old mappings are converted to `text` before normal API use.
- Re-running the migration is safe when columns are already `text`.
- `mvn -f services/api/pom.xml test -q` passes.
- SuiLearn workflow policy check passes for this change.
