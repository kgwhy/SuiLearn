# 设计

## 分层结构

采用四层结构：

```text
workflow-as-skill
policy-as-ruler
rationale-as-doc
verification-as-code
```

- `AGENTS.md` / ruler：常驻红线、优先级、角色入口、编辑前后门禁。
- `suilearn-workflow` Skill：Agent 执行时的轻量状态路由器。
- `suilearn-workflow/references/**`：按需加载的状态、门禁和执行细节。
- `docs/development-workflow.md`：面向人类和审查的完整制度说明。
- `scripts/check-suilearn-workflow.ps1`：可自动化的约束检查。

## 渐进式加载路径

```text
收到任务
  -> 读取 AGENTS.md 常驻规则
  -> 判定 role / state / change level / 是否编辑文件
  -> 触发 suilearn-workflow Skill
  -> 按状态读取对应 reference
  -> 编辑前读取 Gate A、角色文件和 active change policy/tasks
  -> 完成前读取 Verify/Gate C reference
```

## Skill 结构

`SKILL.md` 只保留：

- 状态机。
- 路由问题。
- 状态到 reference 的映射。
- 不可协商项。

新增或强化 references：

- `state-machine.md`：状态与退出条件。
- `policy-gates.md`：Gate A/B/C/D。
- `change-levels.md`：Tiny / Normal / Major。
- `subagent-loop.md`：L1/L2/L3 Build 循环。
- `verification.md`：完成前证据和关闭检查。
- `usage-examples.md`：真实任务触发示例。
- `forward-testing.md`：workflow skill 前向测试路径。

## 风险

- 规则拆分后可能出现重复或冲突。缓解：本轮只建立结构，后续再逐条归属。
- Agent 可能仍然读取完整 doc。缓解：`AGENTS.md` 和 Skill 都明确“按需加载，doc 作为完整说明”。
- OpenSpec 校验可能要求固定文件布局。缓解：保留 Major change 的完整产物。
