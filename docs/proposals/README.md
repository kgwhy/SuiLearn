# SuiLearn 变更提案目录

`docs/proposals/` 用于记录尚未并入当前规格的产品、架构或技术变更。它不是历史版本库，也不是完整 PRD 副本目录。

本文件是 Proposal 规则的单点真相。`AGENTS.md`、`docs/development-workflow.md` 和 `docs/index.md` 只引用本文件，不重复维护完整状态或门禁说明。

## 定位

- 当前事实写入 `docs/product-requirements.md`、`docs/architecture.md` 和 `docs/tech-selection.md`。
- 未来变更写入 `docs/proposals/*.md`。
- 历史追溯依赖 Git commit 和 `git diff`。
- Proposal 实现完成后，稳定结论必须合并回对应当前规格文档。

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

- 改当前规格的哪一部分。
- 是否修改产品范围、架构边界或技术选型。
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
