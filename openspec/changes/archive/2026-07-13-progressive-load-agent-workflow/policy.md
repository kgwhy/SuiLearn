# 策略

## 等级

Major。

原因：本变更修改项目工作流、Agent 常驻规则和 workflow skill 的加载结构，
影响后续所有角色的执行方式。

## 角色与 Owner

- Leader Agent：`AGENTS.md`、`docs/development-workflow.md`、`agents/*.md`（本轮用户明确要求的角色定义补充）、`.agents/skills/suilearn-workflow/**`、本 change 产物。

## base_ref

`f220d65b475e3dc9f97d2bf31a0d94e186caa2c4`

## 文件锁

串行执行，不创建持久锁。已检查 `.agents/locks` 不存在。

## 允许修改文件

- `AGENTS.md`
- `docs/development-workflow.md`
- `agents/*.md`（仅本轮用户明确要求的角色身份定位补充）
- `.agents/skills/suilearn-workflow/**`
- `openspec/changes/progressive-load-agent-workflow/**`

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

- `openspec validate progressive-load-agent-workflow --strict`
- `powershell -ExecutionPolicy Bypass -File scripts/check-suilearn-workflow.ps1 -BaseRef f220d65b475e3dc9f97d2bf31a0d94e186caa2c4`
- `git diff f220d65b475e3dc9f97d2bf31a0d94e186caa2c4 --stat`

## 测试说明

本变更只修改工作流、Agent 角色定义、Skill 和 OpenSpec 产物，不涉及业务代码、构建产物或运行服务。
模块测试不适用；以 OpenSpec 校验、工作流检查和 reviewer-style 自审作为验证。
