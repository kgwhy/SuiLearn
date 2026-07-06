# 真实任务触发示例

用于校准 Agent 在真实任务中的加载路径。示例只说明“应该加载什么、必须做什么、不能做什么、如何验证”，不替代 active change 的 `policy.md` 和 `tasks.md`。

## 示例 1：用户讨论工作流设计

用户请求：

```text
我们是不是应该把工作流写成 Skill，而不是 doc？
```

路由：

- 状态：Explore。
- 角色：Leader。
- 等级：暂不定级，除非用户要求落地修改。
- 加载：`state-machine.md`；若讨论真实任务示例，再加载 `usage-examples.md`。

必须做：

- 只讨论方案、风险和取舍。
- 说明何时需要进入 Spec。

不能做：

- 不直接修改文件。
- 不创建并行 proposal/design 流程。

验证：

- 不需要模块测试。
- 若只是回答问题，最终说明“只读探索，无文件变更”。

## 示例 2：用户要求完善 workflow skill

用户请求：

```text
完善 suilearn-workflow，让它更不容易跳过门禁。
```

路由：

- 状态：Spec/Build，复用或创建一个 workflow change home。
- 角色：Leader。
- 等级：Major，除非明确只是无行为影响的小型措辞修正。
- 加载：`state-machine.md`、`change-levels.md`、`policy-gates.md`、`usage-examples.md`。

必须做：

- 读取 `agents/leader.md`。
- 读取 active change 的 `policy.md` 和 `tasks.md`。
- 声明计划修改文件。
- 将细节放入 references 或 scripts，不让 `SKILL.md` 变长。
- 完成前运行 Skill 检查、OpenSpec 校验和工作流检查器。

不能做：

- 不修改业务代码、契约、产品事实文档或其他 active change。
- 不把完整政策复制进 `SKILL.md`。

验证：

- `scripts/check-skill.ps1`
- `openspec validate <change> --strict`
- `scripts/check-suilearn-workflow.ps1`
- `git diff <base_ref> --stat`

## 示例 3：用户要求实现 Android 功能

用户请求：

```text
把练习页面的错题重做入口加上。
```

路由：

- 状态：Spec 或 Build，取决于是否已有批准任务。
- 角色：Android Agent；Leader 协调。
- 等级：通常 Normal；若涉及契约、存储或跨端行为则升级。
- 加载：`state-machine.md`、`change-levels.md`、`policy-gates.md`；进入 Build 时加载 `subagent-loop.md`。

必须做：

- 确认 active change home。
- 读取 `agents/android.md` 和 active change `policy.md`/`tasks.md`。
- 业务代码编辑前运行或记录 Android 基线测试。
- 按任务要求执行 TDD 或复现步骤。

不能做：

- 不绕过 OpenSpec 直接改业务代码。
- 不让实现 Agent 自证完成。
- 不顺手改产品范围或契约。

验证：

- Android 任务指定测试。
- Test/Review 循环证据。
- Verify 阶段的 diff stat 和文件范围核对。

## 示例 4：用户准备声明完成

用户请求：

```text
这个 change 可以收尾了吗？
```

路由：

- 状态：Verify。
- 角色：Leader。
- 加载：`verification.md`、`policy-gates.md`；Major/cross-role 时加载 `subagent-loop.md` 的 review 规则。

必须做：

- 跑必需验证，或说明不适用原因。
- 跑 `git diff <base_ref> --stat`。
- 检查 tasks 是否全部完成或延期到具名 follow-up change。
- 扫描 `In progress`、`Status: open`、无 Owner 的 `pending` 等陈旧状态。
- 做 reviewer-style 自审。

不能做：

- 没有证据就声明完成。
- 忽略 P0/P1/P2 审查发现。
- 把仍 open 的 archive 说成已完成。

验证：

- 最终报告包含测试结果、文件核对和 Review 闭环。
