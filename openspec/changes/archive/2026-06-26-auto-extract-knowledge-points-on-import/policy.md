# Policy

## Change

- Name: `auto-extract-knowledge-points-on-import`
- Level: Tiny
- base_ref: `cc8b0c1c5172088229e37948fa2989f868f5a831`
- Workflow: Explore -> Spec -> Build -> Verify
- Worktree mode: serial

## Roles

- Leader Agent coordinates this Fast Track record.
- Web Frontend Agent owns the implementation under `apps/web/src/App.tsx`.
- No Android, backend, contract, product, architecture, or tech-selection changes are authorized.

## Allowed Paths

- `openspec/changes/auto-extract-knowledge-points-on-import/**`
- `apps/web/src/App.tsx`

## Forbidden Paths

- `services/**`
- `apps/android/**`
- `contracts/**`
- `docs/product-requirements.md`
- `docs/architecture.md`
- `docs/tech-selection.md`
- `docs/proposals/**`
- `docs/superpowers/**`

## File Locks

- `openspec/changes/auto-extract-knowledge-points-on-import/**`
- `apps/web/src/App.tsx`

## Baseline

Before the web code edit, the web build passed:

```text
npm --prefix apps/web run build
exit 0
```

The output included TypeScript build and Vite production build success with no failures.

