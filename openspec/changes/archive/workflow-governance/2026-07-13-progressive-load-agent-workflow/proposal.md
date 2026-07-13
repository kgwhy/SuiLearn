# 渐进式加载 Agent 工作流

## 背景

当前 SuiLearn 工作流把大量规则集中在 `AGENTS.md` 和
`docs/development-workflow.md` 中，Agent 在执行时容易一次性加载完整流程。
这与 Skill 的渐进式加载原则冲突：高频入口应保持轻量，细节应在具体状态、
角色或任务触发时再读取。

## 目标

- 将 SuiLearn 工作流调整为“ruler 入口 + workflow skill 路由 + reference 细节 + doc 说明”的结构。
- 让 `suilearn-workflow` 成为状态路由器，明确按状态和门禁加载哪些 reference。
- 保留 `docs/development-workflow.md` 作为人类可读的完整事实说明，但不再要求每次全量读取。
- 保留 `AGENTS.md` 的不可违反红线和最小入口规则。

## 非目标

- 不细分每一条规则最终属于 ruler 还是 Skill；本变更先落结构。
- 不改变业务代码、产品范围、架构决策或跨端契约。
- 不创建 `docs/proposals/**` 或 `docs/superpowers/**` 下的新流程产物。

## 验收标准

- `AGENTS.md` 明确常驻规则只保留红线、优先级、最小路由和门禁入口。
- `suilearn-workflow/SKILL.md` 保持轻量，只负责状态判断和 reference 导航。
- `suilearn-workflow/references/**` 包含按状态、门禁、等级、文档分层和子 Agent 循环拆分的细节。
- `docs/development-workflow.md` 明确自己是人类事实说明和完整政策文档，不是每次任务的强制全量加载入口。
- 工作流检查和 OpenSpec 校验通过，或记录不可运行原因。
