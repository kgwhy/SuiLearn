# SuiLearn Agent 规则

## 常驻红线

SuiLearn 使用一个原生工作流：

```text
Explore -> Spec -> Build -> Verify -> Archive
```

完整政策见 `docs/development-workflow.md`。执行时先用 `.agents/skills/suilearn-workflow` 路由，只加载当前状态和角色所需内容。

## 会话恢复

新会话或上下文被压缩后，先重读本文件和 `.agents/skills/suilearn-workflow/SKILL.md` 完成状态路由；涉及设计取舍时再读 `.agents/notes/README.md` 定位长期决策，然后继续执行。

## 优先级

1. 用户显式指令。
2. 本文件与角色文件。
3. `docs/development-workflow.md` 和 workflow skill。
4. active `openspec/changes/<change-name>` 产物。
5. 工具或技能默认规则。

## 编辑门禁

编辑文件前：

1. 判断状态、Owner、变更等级和 active change home。
2. 记录 `base_ref`。
3. 读取 `agents/<role>.md` 和 active change 的 `tasks.md`；Standard/Major 再读 `policy.md`。
4. Major 或 Standard 有取舍时，定位或创建本次改动的 `.agents/notes/` 决策记录。
5. 声明计划修改文件并核对允许范围。
6. 业务代码编辑前运行基线测试，或记录不可用原因。

每批编辑前声明：

```text
📝 本次修改: <file list>
```

## 完成门禁

声明完成前：

1. 运行 `python3 scripts/change_scope.py --base <base_ref>`，按 workflow skill 的 `verification-selection.md` 选择最小验证并执行。
2. 运行 `python3 scripts/check_agent_notes.py`。
3. 运行 `git diff <base_ref> --stat`。
4. 核对文件都在允许范围。
5. 核对任务完成或延期到具名 follow-up。
6. 按 `.agents/skills/suilearn-review/SKILL.md` 做单人自审。

统一返回格式：

```text
STATUS: DONE / DONE_WITH_CONCERNS / NEEDS_CONTEXT / BLOCKED
Changed files:
Tests:
Summary:
Assumptions:
Blockers:
```

## 不可违反

- 业务代码实现必须来自已批准任务。
- 同一用户问题链路只使用一个 active change home。
- 不绕过角色归属、文件边界或批准门禁。
- 单人项目默认独立验证：Test 用干净 shell 独立执行并保留原始输出；Review 用新会话/延迟自审或用户确认，并记录 `review_mode: single-agent`。
- 用户可见 UI 变更必须附真实运行证据。
- 当前事实文档只写已落地事实；未落地 Build 目标只存在于 active change。
- 不在 `docs/proposals/**` 创建新文件。
- 通用 `.codex/skills/openspec-*` 只是参考；与 SuiLearn 工作流冲突时，以本项目和 `docs/development-workflow.md` 为准。

## 角色索引

- Leader：`agents/leader.md`
- Product：`agents/product.md`
- Architect：`agents/architect.md`
- Content：`agents/content.md`
- Android：`agents/android.md`
- Server Backend：`agents/server-backend.md`
- Web Frontend：`agents/web-frontend.md`
- Test：`agents/test.md`
- Reviewer：`agents/reviewer.md`

未指定角色时先推断主要角色并说明理由；跨角色工作由 Leader 协调。
