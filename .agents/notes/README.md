# Agent Notes

SuiLearn 的长期决策记录，只保存“为什么这样决定”和“放弃了什么”。当前事实写入 `docs/**` 与 `contracts/**`；这里的 note 不复制当前事实。

## 路径即状态

```text
.agents/notes/{proposed,implemented,rejected}/{feature,bug-fix,simplification,architecture,process,testing}/YYYY-MM-DD-slug.md
```

- `proposed/`：未实施的提案。
- `implemented/`：已落地的决策，随代码事实保持更新。
- `rejected/`：已否决的提案；只在它仍能防止重蹈覆辙时保留。

日期是首次提出日期，用 `YYYY-MM-DD`。

## 何时写

- Major：必须新增或更新至少一条。
- Standard：只要涉及行为取舍、架构判断、配置默认值语义或可复用契约，就必须写。
- Light：可选。

Agent Note 是单语 Markdown，不建 `.zh.md` 或 `.i18n.yaml`。

## 格式

前两行固定：

```markdown
# Agent Note: <动作型标题>

Status: proposed
```

`Status` 必须与目录一致。拒绝时写一句话原因：

```markdown
Status: rejected — <原因>
```

### proposed

```markdown
## Problem
## Proposal
## Alternatives considered
## Acceptance criteria
## Risks
```

### implemented

```markdown
## Problem
## Decision
## Alternatives considered
## Consequences
```

`implemented/` 只写现在时，文件路径、类名、默认值变化时在同一次改动中更新事实，不改写决定本身。

### rejected

保留提案时的 `## Problem`、`## Proposal` 和 `## Alternatives considered`。

## 硬规则

- `## Alternatives considered` 必须有，且只记录真实考虑过的替代方案。
- 决定被完全取代时新开一条 note 并交叉链接，不把旧 note 重写成相反结论。
- 删除 note 前先迁移其中仍有用的 rationale，并修复引用。
- `python3 scripts/check_agent_notes.py` 校验格式；归档前必须通过。
