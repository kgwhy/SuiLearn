# 策略

## 等级

Major。

原因：本变更修改项目工作流事实文档和 Agent 门禁规则，影响后续所有角色的执行方式。虽然代码量小，但属于工作流变更。

## 角色与 Owner

- Leader Agent：`docs/development-workflow.md`、`AGENTS.md`、本 change 产物。

## base_ref

`5ec2fafe308afc71032ffa8284ff9c4c4abea3ad`

## 文件锁

串行执行，不创建持久锁。

## 允许修改文件

- `docs/development-workflow.md`
- `AGENTS.md`
- `openspec/changes/tighten-small-config-workflow/**`

## 禁止修改文件

- `apps/**`
- `services/**`
- `contracts/**`
- `docs/product-requirements.md`
- `docs/architecture.md`
- `docs/tech-selection.md`
- `docs/proposals/**`
- `docs/superpowers/**`
- 其他 active change 目录

## 验证

- `openspec validate tighten-small-config-workflow --strict`
- `powershell -ExecutionPolicy Bypass -File scripts/check-suilearn-workflow.ps1 -BaseRef 5ec2fafe308afc71032ffa8284ff9c4c4abea3ad`
- `git diff 5ec2fafe308afc71032ffa8284ff9c4c4abea3ad --stat`

## 测试说明

本变更只修改工作流文档和 OpenSpec 产物，不涉及业务代码、构建产物或运行服务。模块测试不适用；以 OpenSpec 校验、工作流检查和文档自审作为验证。
