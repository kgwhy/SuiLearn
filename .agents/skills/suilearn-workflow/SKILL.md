---
name: suilearn-workflow
description: 当在 SuiLearn 中处理工作流路由、OpenSpec 变更、实现任务、验证、归档、角色/文件策略，或判断应加载哪些项目流程规则时使用。
---

# SuiLearn 工作流

本 Skill 是轻量路由器。完整政策在 `docs/development-workflow.md`；只加载当前状态所需 reference。

## 状态机

```text
Explore -> Spec --[Approval Gate]--> Build -> Verify --[Sync Gate]--> Archive
```

## 先路由

行动前回答：

- 当前状态？
- Owner 角色？
- 变更等级：Light、Standard 还是 Major？
- 是否编辑文件？
- 是否有 active `openspec/changes/<change-name>`？

## 按需加载

| 需要 | 读取 |
| --- | --- |
| 状态和退出条件 | `references/state-machine.md` |
| 变更等级 | `references/change-levels.md` |
| 编辑/完成门禁 | `references/policy-gates.md` |
| Build 循环 | `references/subagent-loop.md` |
| 完成和关闭 | `references/verification.md` |
| 归档目录 | `references/archive-organization.md` |
| 真实触发示例 | `references/usage-examples.md` |
| 修改本 Skill 的行为测试 | `references/forward-testing.md` |

编辑文件前必须读取：

- `agents/<role>.md`
- active change 的 `tasks.md`；Standard/Major 还要读取 `policy.md`

## 路由规则

- 探索请求留在 Explore，不写业务代码。
- Light 变更可以只创建 `tasks.md`，但必须写 Owner、允许文件、验证命令和完成定义。
- Standard 变更进入 `openspec/changes/<change-name>/**`，最低产物为 `tasks.md + policy.md`；新功能再补 proposal。
- 已批准且任务可独立验证/提交的 Standard 可使用 L2 Auto。
- Major 变更使用完整产物。
- 业务代码实现必须来自已批准任务。
- 实现 Agent 不能自证完成；独立 Test 或独立 Review 至少有一项。
- 完成声明使用统一 `STATUS / Changed files / Tests / Summary / Assumptions / Blockers` 格式。

## 不可协商项

- 同一用户问题链路只使用一个 active change home。
- 不在 `docs/proposals/**` 下创建新文件。
- 不绕过角色归属、文件边界或批准门禁。
- 业务代码变更需要测试或明确复现步骤。
- 证据先于完成声明。
- 通用 `.codex/skills/openspec-*` 只是参考；与本文冲突时以本文为准。
