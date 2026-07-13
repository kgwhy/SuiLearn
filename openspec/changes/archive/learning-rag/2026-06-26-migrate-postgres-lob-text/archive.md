# Archive

Status: passed

## Change

`migrate-postgres-lob-text`

## Implementation Reference

Working tree changes from base ref `cc8b0c1c5172088229e37948fa2989f868f5a831`.

## Summary

- Replaced backend JPA `String @Lob` mappings with ordinary `text` columns.
- Added PostgreSQL startup migration for legacy Large Object (`oid`) columns.
- Kept read-only transaction boundaries around persistence facade reads.
- Synchronized the stable backend persistence rule to `docs/architecture.md`.

## Verification

- `mvn -f services/api/pom.xml test -q`: passed.
- `rg -n -F '@Lob' services/api/src/main/java/com/suilearn/api/persistence/entity`: no matches.
- `powershell -ExecutionPolicy Bypass -File scripts/check-suilearn-workflow.ps1 -BaseRef cc8b0c1c5172088229e37948fa2989f868f5a831 -ClosingChange migrate-postgres-lob-text`: passed.

## Deferred Items

Deferred items: none

none

## Review

No P0/P1/P2 findings remain open.
