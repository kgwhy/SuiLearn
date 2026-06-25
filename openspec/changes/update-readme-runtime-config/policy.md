# Policy

## Change

- Name: `update-readme-runtime-config`
- Level: Tiny
- base_ref: `3b8aababf1e49294a32a41eb8ed1780632364ad5`
- Workflow: Explore -> Spec -> Build -> Verify -> Archive
- Worktree mode: serial

## Roles

- Leader Agent owns this documentation-only coordination update because the user explicitly requested README changes.

## Allowed Paths

- `README.md`
- `openspec/changes/update-readme-runtime-config/**`

## Forbidden Paths

- `apps/**`
- `services/**`
- `contracts/**`
- `docs/product-requirements.md`
- `docs/architecture.md`
- `docs/tech-selection.md`
- `docs/proposals/**`
- `docs/superpowers/**`

## Baseline

模块测试不适用：本变更只更新 README 配置说明，不修改业务代码、构建配置或契约。
