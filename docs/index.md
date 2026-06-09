# SuiLearn 文档索引

## 核心文档

- `docs/chat.md`：产品灵感讨论材料，历史讨论记录，不作为实现依据。当前规格以 `docs/product-requirements.md` 为准。
- `docs/product-requirements.md`：当前已确认产品规格，作为产品真相源。
- `docs/architecture.md`：当前系统架构、模块边界和演进约束。
- `docs/tech-selection.md`：技术选型与架构决策文档。
- `docs/development-workflow.md`：Leader 驱动的多 Agent 开发流程。

## AI First 文档入口

SuiLearn 文档按“当前规格 + 变更提案 + Git 历史”组织。本节只做入口索引，具体规则见对应文档：

- 当前规格：`docs/product-requirements.md`、`docs/architecture.md`、`docs/tech-selection.md`。
- 变更提案规则：`docs/proposals/README.md`。Proposal 按变更意图组织，同一变更同时影响 PRD、架构、技术基线或契约时，使用同一个 Proposal 的不同影响分区。
- 变更提案模板：`docs/proposals/_template.md`。
- 多 Agent 执行流程：`docs/development-workflow.md`。

## Agent 规则

- `AGENTS.md`：全局协作规则。
- `agents/leader.md`：Leader Agent，多 Agent 调度与质量门禁。
- `agents/product.md`：产品 Agent。
- `agents/architect.md`：架构 Agent。
- `agents/content.md`：内容 Agent。
- `agents/android.md`：Android Agent。
- `agents/server-backend.md`：Server Backend Agent。
- `agents/web-frontend.md`：Web Frontend Agent。
- `agents/test.md`：测试 Agent。
- `agents/reviewer.md`：审查 Agent。
