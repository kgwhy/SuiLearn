# Design

## Current Problem

The backend currently stores many long strings and JSON strings with `@Lob`. On PostgreSQL, Hibernate/PgJDBC can persist these as Large Object references (`oid`). Reading those values requires an active transaction, and list endpoints can fail when entity-to-model mapping touches the field outside a transaction.

## Target Shape

Persistent long text and JSON-as-string values should use normal PostgreSQL `text` columns. This avoids Large Object stream handling, removes autocommit sensitivity, and keeps the schema simple for backup, inspection, and future migration tooling.

## Entity Mapping

Replace each `@Lob` on `String` fields with:

```java
@Column(columnDefinition = "text")
```

This preserves Java model behavior while changing DDL intent for fresh schemas.

## Existing PostgreSQL Data

Because existing databases may already have `oid` columns, entity annotation changes alone are not enough. Add a small backend startup migration component that:

1. Runs only when the datasource is PostgreSQL.
2. Inspects `information_schema.columns` for the known table/column pairs.
3. Converts columns whose `udt_name` is `oid`:

```sql
ALTER TABLE <table>
ALTER COLUMN <column> TYPE text
USING CASE
  WHEN <column> IS NULL THEN NULL
  ELSE convert_from(lo_get(<column>), 'UTF8')
END
```

Columns already stored as `text` are skipped, making the migration idempotent.

## Risk And Rollback

- If a legacy large object cannot be read, startup migration fails fast instead of silently corrupting text.
- Large objects are not unlinked in this change, so rollback can still inspect old large object storage if needed.
- The immediate read-only transaction annotations can remain. They are harmless and still protect any future transactional read mapping.

## Tests

- Entity architecture test: no persistence entity imports or uses `jakarta.persistence.Lob`.
- Migration unit test: H2/no PostgreSQL metadata is skipped; PostgreSQL `oid` metadata produces expected conversion SQL through a mocked `JdbcTemplate`.
- Existing backend test suite remains the integration regression.
