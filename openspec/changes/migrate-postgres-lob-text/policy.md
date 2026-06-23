# Policy

## Change

- Name: `migrate-postgres-lob-text`
- Level: Normal
- base_ref: `cc8b0c1c5172088229e37948fa2989f868f5a831`
- Workflow: Explore -> Spec -> Build -> Verify -> Archive
- Worktree mode: serial

## Roles

- Leader Agent coordinates OpenSpec artifacts and final verification.
- Server Backend Agent owns implementation under `services/api/**`.
- Architect Agent owns Sync Gate update to `docs/architecture.md`.
- No Android, Web, Product, or Contract changes are authorized.

## Allowed Paths

- `openspec/changes/migrate-postgres-lob-text/**`
- `services/api/**` for Task 2 only
- `docs/architecture.md` for Sync Gate only

## Forbidden Paths

- `apps/android/**`
- `apps/web/**`
- `contracts/**`
- `docs/product-requirements.md`
- `docs/tech-selection.md`
- `docs/proposals/**`
- `docs/superpowers/**`

## File Locks

- `openspec/changes/migrate-postgres-lob-text/**`
- `services/api/src/main/java/com/suilearn/api/persistence/entity/**`
- `services/api/src/main/java/com/suilearn/api/persistence/**`
- `services/api/src/test/java/com/suilearn/api/**`
- `docs/architecture.md`

## Baseline

Before business-code edits, backend baseline passed:

```text
mvn -f services/api/pom.xml test -q
exit 0
```

The output contained Spring Boot test startup logs and Mockito/ByteBuddy dynamic agent warnings, but no failures.
