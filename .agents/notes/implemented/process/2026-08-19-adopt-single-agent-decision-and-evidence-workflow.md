# Agent Note: 采用单人决策记录与证据工作流
Status: implemented

## Problem

SuiLearn 是单人项目。历史决策只存在于 OpenSpec 归档和 Git 历史中，新会话或上下文压缩后难以恢复“为什么这样决定、放弃了什么”；当前事实文档混入“已批准 Build 目标”，验证命令也没有按 diff 自动收敛，用户可见 UI 变更缺少统一证据要求。

## Decision

工作流新增以下机制：

- `.agents/notes/` 保存 proposed/implemented/rejected 三类长期决策记录，Major 必写，Standard 有取舍时必写；格式由 `scripts/check_agent_notes.py` 校验。
- `scripts/change_scope.py` 输出 committed/staged/unstaged/untracked 变更范围，验证前必须先看 scope。
- 按变更路径选择最小验证，规则在 workflow skill 的 `references/verification-selection.md`。
- 单人自审默认 `review_mode: single-agent`，检查清单在 `.agents/skills/suilearn-review/SKILL.md`。
- 用户可见 UI 变更必须附真实运行证据，规则在 workflow skill 的 `references/ui-evidence.md`。
- `docs/architecture.md` 和 `docs/tech-selection.md` 只写当前事实，目标状态留在 OpenSpec change。

## Alternatives considered

- **继续只依赖 OpenSpec archive**：archive 是历史目录，不便于按生命周期检索，也不强制记录 Alternatives；否决。
- **在 docs/ 下增加 decisions/ 目录**：会把决策理由和当前事实混在同一个文档层，增加 Sync Gate 复杂度；否决。
- **完全照搬 deepseek-harness 的 Agent Notes 归档和 hash 封存**：对单人项目过重；否决。

## Consequences

- Standard/Major 每次多写一个决策文件，但长期减少考古成本。
- 当前事实文档需要一次性迁移目标章节，之后计划只存在于 OpenSpec change。
- 完成门禁和归档前检查增加两个 Python 脚本，验证时间略有增加。
