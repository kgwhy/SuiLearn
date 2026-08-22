# 单人决策记录与证据工作流设计

## Context

SuiLearn 已有 Explore -> Spec -> Build -> Verify -> Archive 状态机、OpenSpec change、角色文件和工作流 checker。本次改动不替换状态机，只补三块：长期决策记忆、按 diff 的最小验证、单人自审与 UI 证据。

## Goals / Non-Goals

**Goals:**

- 决策理由可跨会话检索，且格式可机器校验。
- 验证范围由实际 diff 决定，完成门禁先看 scope 再跑命令。
- 单人开发以独立执行和延迟自审代替独立 Agent。
- 当前事实文档不再承载计划状态。

**Non-Goals:**

- 不引入双语文档机制。
- 不引入多 PR 栈管理。
- 不修改业务代码或 API 契约。
- 不引入 deepseek-harness 的 hash 封存归档。

## Decisions

### 决策记录放在 `.agents/notes/`，路径编码生命周期

`proposed/implemented/rejected/{class}/YYYY-MM-DD-slug.md`。类沿用 feature、bug-fix、simplification、architecture、process、testing。

**Alternative**: 放 `docs/decisions/`。否决：与当前事实文档同一层，增加 Sync Gate 复杂度。

**Alternative**: 只保留 OpenSpec archive。否决：archive 不易按生命周期检索，且不强制记录 Alternatives。

### 当前事实文档只写当前事实

计划状态只存在于 active change；归档时只有验证通过的事实才写入当前事实文档。

**Alternative**: 保留“已批准 Build 目标”标注。否决：状态会腐烂，且读者无法一次读出现状。

### 单人验证默认规则

Test 用干净 shell 独立执行并保留原始输出；Review 用新会话/延迟自审或用户确认，记录 `review_mode: single-agent`。

**Alternative**: 继续要求独立 Agent。否决：单人项目不总是能派发子 Agent，且该要求会迫使假独立。

## Risks / Trade-offs

- 每个 Standard/Major 多写一个 note，短期成本上升；换取长期决策可检索。
- `docs/architecture.md` 与 `docs/tech-selection.md` 的一次性迁移有描述回归风险；迁移时逐段对照代码和 Compose 配置。
- 脚本新增会扩大 workflow checker 面；新增 Python unittest 覆盖核心分支。
