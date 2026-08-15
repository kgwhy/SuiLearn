# SuiLearn Agent 规则

## 常驻红线

SuiLearn 使用一个原生工作流：

```text
Explore -> Spec -> Build -> Verify -> Archive
```

完整政策见 `docs/development-workflow.md`。执行时先用 `.agents/skills/suilearn-workflow` 路由，只加载当前状态和角色所需内容。

## 会话恢复

新会话或上下文被压缩后，先重读本文件和 `.agents/skills/suilearn-workflow/SKILL.md` 完成状态路由，再继续执行。

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
4. 声明计划修改文件并核对允许范围。
5. 业务代码编辑前运行基线测试，或记录不可用原因。

每批编辑前声明：

```text
📝 本次修改: <file list>
```

## 完成门禁

声明完成前：

1. 运行必需验证，或说明不适用原因。
2. 运行 `git diff <base_ref> --stat`。
3. 核对文件都在允许范围。
4. 核对任务完成或延期到具名 follow-up。
5. 做 reviewer-style 自审。

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
- 实现 Agent 不能自证完成；独立 Test 或独立 Review 至少一项。
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
