# Policy

## Change

- Name: `standardize-spec-doc-language`
- Level: Tiny
- base_ref: `cc8b0c1c5172088229e37948fa2989f868f5a831`
- Workflow: Explore -> Spec -> Build -> Verify -> Archive
- Worktree mode: serial

## Roles

- Leader Agent owns workflow documentation and coordination rules.

## Allowed Paths

- `AGENTS.md`
- `docs/development-workflow.md`
- `openspec/changes/standardize-spec-doc-language/**`

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

模块测试不适用：本变更只修改工作流与 Spec 文档语言规则，不修改业务代码。
