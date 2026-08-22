# 采用单人决策记录与证据工作流

## Why

SuiLearn 是单人项目。当前问题：

- 历史决策只存在于 OpenSpec 归档和 Git 历史，新会话或上下文压缩后难以恢复“为什么这样决定、放弃了什么”。
- `docs/architecture.md` 和 `docs/tech-selection.md` 混入“已批准 Build 目标”，当前事实文档不再是纯当前事实。
- 验证命令没有按 diff 范围收敛，单人开发容易跑全量或漏跑。
- 用户可见 UI 变更没有统一证据要求。
- 单人环境仍把独立 Test/Review 写成“降级”，而不是默认路径。

## What Changes

- 新增 `.agents/notes/` 单语长期决策记录，以及 `scripts/check_agent_notes.py` 格式校验。
- 新增 `scripts/change_scope.py` 输出 committed/staged/unstaged/untracked 变更范围。
- workflow skill 新增 `verification-selection.md` 和 `ui-evidence.md` reference。
- 新增 `.agents/skills/suilearn-review/` 单人自审清单。
- `AGENTS.md`、`docs/development-workflow.md` 将单人验证改为默认规则，并加入 Agent Note、变更范围、UI 证据门禁。
- 当前事实文档移除“已批准 Build 目标”标注；已实现目标改写为当前事实。

## Capabilities

### New

- `single-agent-workflow`: 单人项目的决策记录、最小验证选择、自审和 UI 证据规则。

### Modified

- `efficient-batch-workflow`: 最终 Verify 增加 change-scope 和 Agent Notes 校验。

## Impact

- 只修改 workflow 治理文件：`AGENTS.md`、`docs/development-workflow.md`、`docs/architecture.md`、`docs/tech-selection.md`、`docs/product-requirements.md`、`.agents/**`、`scripts/**`、`tests/**`。
- 不修改 `apps/**`、`services/**`、`contracts/**` 业务代码和契约。
