# Archive

Status: blocked by local PostgreSQL availability

## Change

`remove-fake-h2-test-tools`

## Implementation Reference

Working tree changes from base ref `cc8b0c1c5172088229e37948fa2989f868f5a831`.

## Summary

- Removed production `FakeAiProvider` and `FakeEmbeddingProvider`.
- Removed H2 from backend Maven dependencies and default datasource configuration.
- Switched backend defaults to PostgreSQL and OpenAI-compatible provider mode.
- Updated tests to use deterministic in-test providers instead of production fake beans.
- Updated current architecture and tech baseline docs to remove fake/H2 as defaults.

## Verification

- `mvn -f services/api/pom.xml test-compile -q`: passed.
- `mvn -f services/api/pom.xml -Dtest=AiProviderStatusServiceTest test -q`: passed.
- `mvn -f services/api/pom.xml -DskipTests package -q`: passed.
- `mvn -f services/api/pom.xml test -q`: blocked by missing PostgreSQL at `localhost:5432`.

## Deferred Items

- Run full backend integration tests after PostgreSQL is available via `SUILEARN_TEST_DB_URL` or local port 5432.

## Review

No code-review blocker identified in the edited files. Release readiness is blocked on PostgreSQL-backed full test verification.
