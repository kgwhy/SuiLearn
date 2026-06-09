# SuiLearn 变更提案目录

`docs/proposals/` 用于记录尚未并入当前规格的产品、架构或技术变更。它不是历史版本库，也不是完整 PRD 副本目录。

本文件是 Proposal 规则的单点真相。`AGENTS.md`、`docs/development-workflow.md` 和 `docs/index.md` 只引用本文件，不重复维护完整状态或门禁说明。

## 定位

- 当前事实写入 `docs/product-requirements.md`、`docs/architecture.md` 和 `docs/tech-selection.md`。
- 未来变更写入 `docs/proposals/*.md`。
- 历史追溯依赖 Git commit 和 `git diff`。
- Proposal 实现完成后，稳定结论必须合并回对应当前规格文档。

## 组织原则

Proposal 按“变更意图”组织，不按文档类型组织。

同一个变更如果同时影响产品规格、架构、技术基线、契约或实现任务，应使用同一个 Proposal，并在 Proposal 内分区说明：

- 产品影响：是否修改 `docs/product-requirements.md` 的需求、验收标准、阶段范围或 Spec Key。
- 架构影响：是否修改 `docs/architecture.md` 的代码结构、模块边界、数据流或跨端关系。
- 技术基线影响：是否修改 `docs/tech-selection.md` 的技术栈、版本、依赖、禁止项或升级规则。
- 契约影响：是否修改 `contracts/**` 的 OpenAPI、JSON schema 或跨端模型语义。
- 实现影响：涉及哪些 Agent、文件范围、测试和审查重点。

只有在以下情况下才拆成多个 Proposal：

- 两个变更可以独立批准、独立实现、独立上线和独立回滚。
- 一个 Proposal 只是产品范围探索，另一个是产品批准后的技术实现方案，且二者尚不能同时进入实现门禁。

不得因为“同时改 PRD 和架构文档”而拆成两个 Proposal；应在同一 Proposal 中分别写清产品影响和架构影响。

## 状态

Proposal 必须使用以下状态之一：

- `Draft`：草案，可讨论、可修改，不能作为实现依据。
- `Approved`：已确认，可进入实现。
- `Implemented`：已完成并合并回当前规格。
- `Archived`：废弃、合并到其他 Proposal，或仅保留历史参考。

## 命名

按主题命名，不按版本复制完整 PRD：

```text
docs/proposals/ai-tutor.md
docs/proposals/course-flow-redesign.md
docs/proposals/rag-knowledge-base.md
```

版本号只作为字段，例如 `Target: v0.3`，不作为目录层级的默认依据。

## 实现门禁

只有 `Approved` Proposal 可以作为代码实现依据。进入实现前必须明确：

- 改当前规格的哪一部分，包括产品规格、架构、技术基线和契约影响。
- 是否修改产品范围、架构边界、技术选型、版本基线或跨端契约。
- 影响哪些角色和文件范围。
- 验收标准。
- 实现完成后如何合并回当前规格。

## 关闭规则

实现完成时，Leader 或负责 Agent 必须确认：

- 已把稳定结论合并回当前规格文档。
- Proposal 状态已更新为 `Implemented`，或明确留下未完成项。
- 已记录实现引用，例如 commit、PR、issue、任务卡或验证记录。
- 如果 Proposal 被废弃或替代，状态改为 `Archived` 并写明原因。

## Spec Key

默认不建立全量需求 ID。只有跨模块、高风险、需要测试或审查稳定引用的能力，才使用轻量 `Spec Key`，例如：

```text
SPEC-AI-TUTOR
SPEC-RAG-TRACEABILITY
SPEC-HOME-PACK-HEADER
```

普通需求不编号。
