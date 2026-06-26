# Archive

Status: passed

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
- `mvn -f services/api/pom.xml test -q`: passed, exit 0 (2026-06-26) against PostgreSQL 16.14. Full suite: 53 tests, 0 failures, 0 errors. See `verification.md`.

## Deferred Items

- None. The previously deferred PostgreSQL-backed full integration test now runs and passes; the H2-removal blocker is resolved.

## Review

No code-review blocker identified in the edited files. Release readiness is confirmed: production fake providers and H2 are removed, backend defaults to PostgreSQL + OpenAI-compatible provider, and the full PostgreSQL-backed test suite is green.

## Final Review Summary

- P0: none.
- P1: none.
- P2: none.
