# 验证

## 状态

状态：已通过。

## 计划

- 运行 OpenSpec 严格校验。
- 运行 SuiLearn 工作流检查器。
- 运行 diff stat。
- 人工核对 changed files 是否均在 `policy.md` 允许范围内。
- 人工核对 `AGENTS.md`、Skill 和 reference 是否体现渐进式加载结构。

## 结果

### `openspec validate progressive-load-agent-workflow --strict`

首次运行失败，原因是 requirement 正文首句缺少 `MUST/SHALL`，已修复。

修复后结果：退出码 0。

```text
Change 'progressive-load-agent-workflow' is valid
```

### `python C:\Users\youku\.codex\skills\.system\skill-creator\scripts\quick_validate.py D:\SuiLearn\.agents\skills\suilearn-workflow`

结果：退出码 1。

```text
Traceback (most recent call last):
  File "C:\Users\youku\.codex\skills\.system\skill-creator\scripts\quick_validate.py", line 10, in <module>
    import yaml
ModuleNotFoundError: No module named 'yaml'
```

说明：校验脚本依赖缺失，未能执行。已用本地 PowerShell 结构检查替代。

### Skill frontmatter and references check

结果：退出码 0。

```text
Skill frontmatter and references check passed.
```

### `powershell -ExecutionPolicy Bypass -File scripts/check-suilearn-workflow.ps1 -BaseRef f220d65b475e3dc9f97d2bf31a0d94e186caa2c4`

结果：退出码 0。

```text
SuiLearn Workflow policy check passed.
```

说明：输出包含 `C:\Users\youku/.config/git/ignore: Permission denied` 和 LF/CRLF warning，不影响检查结果。

### `git diff --check`

结果：退出码 0。

```text
warning: in the working copy of '.agents/skills/suilearn-workflow/SKILL.md', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of '.agents/skills/suilearn-workflow/agents/openai.yaml', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of '.agents/skills/suilearn-workflow/references/policy-gates.md', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of '.agents/skills/suilearn-workflow/references/state-machine.md', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of '.agents/skills/suilearn-workflow/references/subagent-loop.md', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'AGENTS.md', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'docs/development-workflow.md', LF will be replaced by CRLF the next time Git touches it
```

### `git diff f220d65b475e3dc9f97d2bf31a0d94e186caa2c4 --stat`

结果：退出码 0。

```text
 .agents/skills/suilearn-workflow/SKILL.md          | 78 ++++++++++------------
 .../skills/suilearn-workflow/agents/openai.yaml    |  4 +-
 .../suilearn-workflow/references/policy-gates.md   | 41 ++++++++++--
 .../suilearn-workflow/references/state-machine.md  | 45 +++++++------
 .../suilearn-workflow/references/subagent-loop.md  | 17 +++++
 AGENTS.md                                          | 14 ++--
 docs/development-workflow.md                       |  7 ++
 7 files changed, 132 insertions(+), 74 deletions(-)
```

说明：未跟踪的新文件不会出现在 `git diff --stat` 中；已通过 `git status --short` 核对新增 references 和本 change 目录。

## 追加验证：Skill 中文化

### `openspec validate progressive-load-agent-workflow --strict`

结果：退出码 0。

```text
Change 'progressive-load-agent-workflow' is valid
```

### Skill frontmatter and references check

结果：退出码 0。

```text
Skill frontmatter and references check passed.
```

### `powershell -ExecutionPolicy Bypass -File scripts/check-suilearn-workflow.ps1 -BaseRef f220d65b475e3dc9f97d2bf31a0d94e186caa2c4`

结果：退出码 0。

```text
SuiLearn Workflow policy check passed.
```

### `rg -n "Use this|Read these|Before|Completion|Progressive Loading|State Machine|Policy Gates|Subagent|Verification And Closure|Required Evidence|Return Format|Non-Negotiables|Route First|Load By Need|Routing Rules" .agents/skills/suilearn-workflow`

初次扫描命中 `references/policy-gates.md:1:# Policy Gates`，已改为中文标题。

复跑结果：退出码 1，无匹配，表示上述英文说明短语已清理完成。

## 追加验证：Skill 质量标准 reference

结论：该 reference 后续已撤回。原因是它属于通用 Skill 作者方法论，不属于 `suilearn-workflow` 的工作流职责。当前 `suilearn-workflow` 不再包含 `references/skill-quality.md`。

### `openspec validate progressive-load-agent-workflow --strict`

结果：退出码 0。

```text
Change 'progressive-load-agent-workflow' is valid
```

### Skill frontmatter and references check

结果：退出码 0。

```text
Skill frontmatter and references check passed.
```

### `powershell -ExecutionPolicy Bypass -File scripts/check-suilearn-workflow.ps1 -BaseRef f220d65b475e3dc9f97d2bf31a0d94e186caa2c4`

结果：退出码 0。

```text
SuiLearn Workflow policy check passed.
```

### 前向测试说明

前向测试要求已改由 `references/forward-testing.md` 承载，范围限定为验证 `suilearn-workflow` 本身是否改变 Agent 行为。本轮未派发 sub-agent 做真实任务前向测试，因为当前工具要求用户显式授权委派；因此本轮验证限于结构、OpenSpec、工作流检查和人工核对，前向测试作为后续可执行项。

## 追加验证：示例、前向测试和 skill-local 脚本

### `.agents\skills\suilearn-workflow\scripts\check-skill.ps1`

结果：退出码 0。

```text
SuiLearn workflow skill check passed.
```

### `openspec validate progressive-load-agent-workflow --strict`

结果：退出码 0。

```text
Change 'progressive-load-agent-workflow' is valid
```

### `powershell -ExecutionPolicy Bypass -File scripts/check-suilearn-workflow.ps1 -BaseRef f220d65b475e3dc9f97d2bf31a0d94e186caa2c4`

结果：退出码 0。

```text
SuiLearn Workflow policy check passed.
```

### `git diff --check`

结果：退出码 0。

说明：输出仅包含 LF/CRLF warning。

### `git status --short`

结果：退出码 0。

说明：确认新增文件包括 `references/usage-examples.md`、`references/forward-testing.md` 和 `scripts/check-skill.ps1`。`git diff --stat` 不显示未跟踪新文件，这是 Git 正常行为。

### 前向测试说明

已新增 `references/forward-testing.md`，包含测试矩阵、推荐 prompt、通过标准和记录格式。当前仍未执行真实 sub-agent 前向测试，原因是当前 sub-agent 工具要求用户显式授权委派；本轮以脚本检查、OpenSpec 校验、工作流检查器和人工核对作为替代验证，并在 archive 延期项中保留真实任务前向测试。

## 追加验证：移除非 workflow reference

### 残留引用扫描

结果：退出码 0。

说明：`.agents/skills/suilearn-workflow/**` 中不再引用 `skill-quality.md` 或 `progressive-loading.md`；扫描仅在本 change 历史记录中命中撤回说明。

### `.agents\skills\suilearn-workflow\scripts\check-skill.ps1`

结果：退出码 0。

```text
SuiLearn workflow skill check passed.
```

## 追加验证：AGENTS.md 瘦身

### 行数核对

结果：退出码 0。

```text
Lines      : 68
Words      : 204
```

### `openspec validate progressive-load-agent-workflow --strict`

结果：退出码 0。

```text
Change 'progressive-load-agent-workflow' is valid
```

### `powershell -ExecutionPolicy Bypass -File scripts/check-suilearn-workflow.ps1 -BaseRef f220d65b475e3dc9f97d2bf31a0d94e186caa2c4`

结果：退出码 0。

```text
SuiLearn Workflow policy check passed.
```

### `git diff --check`

结果：退出码 0。

说明：输出仅包含 LF/CRLF warning。

### 范围核对

`AGENTS.md` 已压缩为常驻 ruler，只保留唯一工作流、优先级、加载入口、编辑/完成门禁、不可违反项和角色索引。细节继续由 `.agents/skills/suilearn-workflow/references/**` 和 `docs/development-workflow.md` 承载。

## 追加验证：Agent 身份定位

### `openspec validate progressive-load-agent-workflow --strict`

结果：退出码 0。

```text
Change 'progressive-load-agent-workflow' is valid
```

### `powershell -ExecutionPolicy Bypass -File scripts\check-suilearn-workflow.ps1 -BaseRef f220d65b475e3dc9f97d2bf31a0d94e186caa2c4`

结果：退出码 0。

```text
SuiLearn Workflow policy check passed.
```

说明：输出包含 `C:\Users\youku/.config/git/ignore: Permission denied` 和 LF/CRLF warning，不影响检查结果。

### `git diff --check`

结果：退出码 0。

说明：输出仅包含 LF/CRLF warning。

### `rg -n "^## 身份定位|你是一名" agents`

结果：退出码 0。

说明：9 个 `agents/*.md` 均已在文件顶部补充 `## 身份定位`，并使用“你是一名...”句式明确角色身份。

### 范围核对

本轮仅扩展到用户明确要求的 `agents/*.md` 角色定义，并同步更新本 change 的 policy/tasks/verification/archive。未修改业务代码、产品正式需求、架构正式文档或契约；模块测试不适用。
