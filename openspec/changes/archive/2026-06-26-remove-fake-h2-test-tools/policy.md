# Policy

## Change

- Name: `remove-fake-h2-test-tools`
- Level: Major
- base_ref: `cc8b0c1c5172088229e37948fa2989f868f5a831`
- Workflow: Explore -> Spec -> Build -> Verify -> Archive
- Worktree mode: serial

## Roles

- Leader Agent coordinates the change record.
- Architect Agent owns technology baseline and architecture fact updates.
- Server Backend Agent owns backend runtime, provider, datasource, and test updates.

## Allowed Paths

- `openspec/changes/remove-fake-h2-test-tools/**`
- `docs/architecture.md`
- `docs/tech-selection.md`
- `services/api/**`

## Forbidden Paths

- `apps/android/**`
- `apps/web/**`
- `contracts/**`
- `docs/product-requirements.md`
- `docs/proposals/**`
- `docs/superpowers/**`

## File Locks

- `openspec/changes/remove-fake-h2-test-tools/**`
- `docs/architecture.md`
- `docs/tech-selection.md`
- `services/api/**`

## Baseline

Module baseline command passed before business-code edits:

```powershell
mvn -f services/api/pom.xml test -q
```

The backend output contained Spring Boot startup logs and Mockito/ByteBuddy dynamic agent warnings, but no failures.
