---
name: suilearn-workflow
description: 当在 SuiLearn 中处理工作流路由、OpenSpec 变更、实现任务、验证、归档、角色/文件策略，或判断应加载哪些项目流程规则时使用。
---

# SuiLearn 工作流

将本 Skill 作为 SuiLearn 的轻量工作流路由器。只加载当前状态、角色和门禁所需的
reference。不要把 `docs/development-workflow.md` 当作每个任务都必须全量读取的
入口；它是完整的人类可审查政策源。

## 状态机

```text
Explore -> Spec --[Approval Gate]--> Build -> Verify --[Sync Gate]--> Archive
             ^                           |
             +---- spec issue found -----+
```

## 先路由

行动前先回答：

- 当前处于哪个状态：Explore、Spec、Build、Verify 还是 Archive？
- 哪个角色拥有相关文件？
- 变更等级是 Tiny、Normal 还是 Major？
- 是否会编辑文件？
- 是否已有 active `openspec/changes/<change-name>` 变更目录？

## 按需加载

仅在条件适用时读取这些 reference：

| 需要 | 读取 |
| --- | --- |
| 判断状态或退出条件 | `references/state-machine.md` |
| 查看真实任务触发示例 | `references/usage-examples.md` |
| 设计或执行前向测试 | `references/forward-testing.md` |
| 判定 Tiny/Normal/Major | `references/change-levels.md` |
| 准备编辑或完成声明 | `references/policy-gates.md` |
| 执行已批准的 Build 任务 | `references/subagent-loop.md` |
| 声明完成或关闭变更 | `references/verification.md` |

确定性检查优先运行 `scripts/check-skill.ps1`，再按项目要求运行 OpenSpec 和工作流检查器。

若要编辑文件，还必须读取：

- `agents/<role>.md`
- active change 的 `policy.md`
- active change 的 `tasks.md`

## 路由规则

- 探索性请求停留在 Explore，不写业务代码。
- 工作流、产品、架构、契约或行为变化进入 Spec，并使用一个
  `openspec/changes/<change-name>/**` 变更目录。
- 已批准任务进入 Build。主 Agent 负责协调；循环等级需要时，由聚焦 Agent
  分别实现、测试、审查和修复。
- 声明完成前先进入 Verify。证据先于成功声明。
- 稳定事实已同步或标记为不受影响后，完成的变更进入 Archive。

## 不可协商项

- 同一用户问题链路只使用一个 active change home。
- 不在 `docs/proposals/**` 下创建新文件。
- 不创建 Superpowers design 或 plan 文档。
- 不绕过角色归属或 active change policy。
- 使用足以保护工作的最小有效变更等级；范围扩大时立即向上重分类。
- 业务代码变更需要 TDD 或明确复现步骤。
- 实现 Agent 不能自证完成。
- 证据先于完成声明。
