# Tasks

## 1. Spec And Policy

- Status: done
- Owner: Leader Agent
- Allowed files:
  - `openspec/changes/migrate-postgres-lob-text/**`
- Forbidden files:
  - `services/api/**`
  - `apps/**`
  - `contracts/**`
  - `docs/proposals/**`
- Verification:
  - `git diff <base_ref> --stat`

## 2. Backend Text Mapping And Migration

- Status: done
- Owner: Server Backend Agent
- Allowed files:
  - `services/api/**`
- Forbidden files:
  - `apps/**`
  - `contracts/**`
  - `docs/product-requirements.md`
  - `docs/tech-selection.md`
  - `docs/proposals/**`
- Implementation:
  - Replace persistence entity `String @Lob` mappings with text column mappings.
  - Add a PostgreSQL-only startup migration for legacy `oid` columns.
  - Add tests for mapping and migration behavior.
- Baseline:
  - `passed`: `mvn -f services/api/pom.xml test -q`
- Verification:
  - `mvn -f services/api/pom.xml test -q`

## 3. Verify And Close

- Status: done
- Owner: Leader Agent
- Allowed files:
  - `openspec/changes/migrate-postgres-lob-text/**`
  - `docs/architecture.md`
- Forbidden files:
  - `services/api/**`
  - `apps/**`
  - `contracts/**`
  - `docs/proposals/**`
- Verification:
  - `mvn -f services/api/pom.xml test -q`
  - `powershell -ExecutionPolicy Bypass -File scripts/check-suilearn-workflow.ps1 -BaseRef <base_ref> -ClosingChange migrate-postgres-lob-text`
  - `git diff <base_ref> --stat`

## 4. Current Fact Sync

- Status: done
- Owner: Architect Agent
- Allowed files:
  - `docs/architecture.md`
  - `openspec/changes/migrate-postgres-lob-text/**`
- Forbidden files:
  - `services/api/**`
  - `apps/**`
  - `contracts/**`
  - `docs/proposals/**`
- Result:
  - Backend persistence rules now state that long text and JSON strings use database `text`, with startup migration for legacy PostgreSQL Large Object (`oid`) columns.
