# SuiLearn 工作流

SuiLearn 工作流是本仓库唯一的软件开发生命周期。它吸收 OpenSpec 风格的
规格驱动开发状态机、Superpowers 风格的子 Agent/TDD/调试/验证纪律，以及
SuiLearn 的角色和文件边界策略。

项目级状态机：

```text
Explore -> Spec --[Approval Gate]--> Build -> Verify --[Sync Gate]--> Archive
             ^                           |
             +---- spec issue found -----+
```

## 原则

- 一个生命周期：不要运行并行的 proposal、design 或 plan 流程。
- 一个变更目录：新变更统一进入 `openspec/changes/<change-name>/**`。
- 一组当前事实：稳定事实写入产品、架构、技术选型和契约文档。
- 主 Agent 负责协调；聚焦子 Agent 负责实现、测试、审查和修复。
- 证据先于完成声明。
- 完成是原子状态：代码、测试、审查发现、验证记录、归档记录和当前事实同步必须一致，才能声明变更完成。

## 变更等级

使用足以保护当前工作的最小等级。等级决定文档重量、审批严格度和 Build 循环强度。

| 等级 | 适用场景 | 必需产物 | 默认 Build 循环 |
|---|---|---|---|
| Tiny | 单角色、无产品/架构/契约/存储变化，通常不超过 2 个文件 | `tasks.md` 任务说明和 `policy.md` 条目 | L1 |
| Normal | 用户可见行为、多文件实现或有意义的测试工作 | `proposal.md`、`design.md`、`tasks.md`、`policy.md` | L2 |
| Major | 跨角色、共享文件、契约、存储、架构、工作流或高风险变化 | proposal、design、specs、tasks、policy、verification、archive notes | L3 |

Fast Track 只允许用于 `Tiny` 工作。Tiny 任务仍需记录 `base_ref`、允许文件、禁止文件、验证命令和最终 diff/文件范围证据。若实现中发现范围扩大，必须立即升级为 `Normal` 或 `Major` 并回到 `Spec`。

## 状态：Explore

目的：理解问题、约束、风险，以及是否需要形成正式变更。

允许：

- 阅读文档、代码、测试、日志和 CodeGraph 上下文。
- 提出澄清问题。
- 比较方案并识别风险。

禁止：

- 写业务代码。
- 把 `docs/chat.md` 或对话想法当成已确认需求。

退出条件：

- 问题和预期结果已能清楚表述，或因为尚未准备好而停止。

## 状态：Spec

目的：产出完整变更包。

位置：

```text
openspec/changes/<change-name>/
  proposal.md
  design.md
  tasks.md
  specs/
  policy.md
  verification.md
  archive.md
```

各等级最低产物：

- `Tiny`：`tasks.md` 和 `policy.md`。
- `Normal`：`proposal.md`、`design.md`、`tasks.md` 和 `policy.md`。
- `Major`：`proposal.md`、`design.md`、`specs/**`、`tasks.md`、`policy.md`、`verification.md` 和 `archive.md`。

产物用途：

- `proposal.md`：说明做什么、为什么做、范围、非目标和验收标准。
- `design.md`：说明实现方案、数据/API 影响、替代方案和风险。
- `tasks.md`：列出可执行任务、Owner、允许文件、测试和审查重点。
- `policy.md`：记录角色归属、等级、`base_ref`、文件锁、worktree 模式、允许路径和禁止路径。

关联变更：

- 当存在多个 active change 时，Leader 必须在 `tasks.md` 或 `policy.md` 中明确主变更，以及每个关联/子变更。
- 子变更不能被父变更“顺手视为完成”，除非它自己的任务、验证和归档记录已经完成，或明确记录为仍然 active。
- 派发 Reviewer 时，必须说明哪个 change 是主范围，哪些 active changes 不在审查范围内。

可以使用 OpenSpec CLI 创建和检查变更。若 CLI 输出 `changeRoot`、`artifactPaths` 或产物顺序，应使用这些具体值，而不是猜测路径。

退出条件：Approval Gate 通过。

## Approval Gate

进入 `Build` 前，Leader 必须确认：

- 范围、非目标、验收标准和受影响的当前事实文档已经明确。
- 每个任务都有 Owner、允许文件、禁止文件、测试命令和审查重点。
- 已记录 `base_ref`。
- 现有 worktree 变更已被归类为本范围内或既有变更。
- 共享文件使用严格串行执行或 worktree 隔离。
- active lock 不与新任务范围冲突。
- 业务代码任务已记录一个基线状态：
  - `passed`：记录命令和输出。
  - `unavailable`：记录工具、依赖、模拟器、服务或网络不可用原因，并给出替代验证。
  - `skipped`：只允许 Tiny 或纯文档任务，并记录原因。

## 状态：Build

目的：按风险匹配的循环执行已批准任务。

Build 循环等级：

```text
L1 Tiny:
  Implementer -> Verify

L2 Normal:
  Implementer -> Test -> Review -> Fix when needed

L3 Major:
  Implementer -> Test -> Spec Review -> Code Review -> Fix -> repeat
```

协调者职责：

- 根据变更等级和任务风险选择循环等级。
- 在所选循环需要子 Agent 时，派发仅带任务局部上下文的新子 Agent。
- 提供精确任务文本、相关产物摘录、允许/禁止路径、测试命令和期望返回格式。
- 文件有重叠时保持串行。
- 判断失败应返回 Fix、返回 Spec，还是阻塞并等待用户确认。

实现 Agent 规则：

- 行为变更遵循 RED -> GREEN -> REFACTOR。
- Bug 修复先写失败回归测试，或记录明确复现步骤。
- 重构需要保护性测试或等价验证。
- 使用满足任务的最小变更。
- 返回变更文件、测试、假设和阻塞点。

测试 Agent 规则：

- 在可行时独立运行任务要求的命令。
- 报告原始命令输出、退出码、失败和手工验证替代方案。
- 不接受实现 Agent 的自测作为最终证据。

审查 Agent 规则：

- L2 在单角色且低风险时可以使用合并审查。
- L3 必须分离 Spec Review 和 Code Review。
- Spec Review 先于 Code Review。
- Spec Review 检查 proposal/design/specs/tasks 一致性。
- Code Review 检查角色边界、质量、可维护性、测试充分性、过度实现和回归风险。
- P0/P1 问题必须修复并复审后，任务才能完成。
- Major 变更在实现后、Verify 关闭前必须进行最终审查。Leader 必须记录每个发现及其处置结果。
- 最终审查发现的 P2 必须修复、迁移到带 Owner 和理由的具名 follow-up change，或由用户显式接受；不能从最终报告中消失。

停止条件：

- 同一任务在同一文件上超过三轮修复仍未完成。
- 必需文件超出已批准范围。
- 产品、架构、契约或数据决策存在歧义。
- 测试无法运行且无法定义可信的手工验证。

## 状态：Verify

目的：证明整个变更可以关闭。

Leader 必须：

- 运行或收集最终模块测试/构建输出。
- 运行 `git diff <base_ref> --stat`。
- 对照 `policy.md` 和角色规则检查变更文件。
- 确认 `tasks.md` 中每个任务已完成或明确延期。
- 对大范围变更派发最终审查。
- 当变更较大或跨多个任务时，在 `verification.md` 中记录验证证据。

关闭检查：

- `tasks.md` 中每个任务都为 done，或延期工作链接到具名 follow-up change。
- `verification.md` 包含 `状态：已通过。`，或包含与最终报告一致的明确非发布状态。
- 对于声称完成的变更，`archive.md` 不得停留在 `Status: open`。
- 最终审查发现必须记录处置结果：已修复、延期到具名 change、用户接受或不适用。
- 当前事实文档不得继续把新工作指向 `docs/proposals/**` 等退役流程。
- 状态词扫描应限定在工作流产物和当前事实文档的工作流段落；例如 "pending content" 这类产品状态术语不是工作流未完成。

退出条件：Sync Gate 通过。

## Sync Gate

归档前：

- 稳定产品结论同步到 `docs/product-requirements.md`。
- 稳定架构结论同步到 `docs/architecture.md`。
- 稳定技术基线决策同步到 `docs/tech-selection.md`。
- 稳定契约同步到 `contracts/**`。
- 不受影响的类别在 archive note 中记录 "not affected"。
- 未完成项迁移到具名新 change，或记录为延期并包含 Owner、理由和验证影响。
- archive note 记录是否扫描了当前事实文档中的退役流程引用，以及修复了什么或哪些内容仅作为历史材料保留。

## 状态：Archive

目的：关闭变更并保留历史。

归档记录必须包含：

- 变更名称。
- 最终状态。
- 实现引用：commit、PR、任务卡或 working-tree 引用。
- 验证摘要。
- 已同步的当前事实文档。
- 延期项，如有。
- 最终审查摘要，包括每个 P0/P1/P2 发现及其关闭方式。

使用 OpenSpec 目录布局时，已完成变更移动到 `openspec/changes/archive/`。如果 OpenSpec CLI 提供 archive 命令，优先使用它。

## 当前事实文档

```text
docs/chat.md                  # 灵感材料，默认只读
docs/product-requirements.md  # 产品真相源
docs/architecture.md          # 架构和模块边界
docs/tech-selection.md        # 技术决策和约束
contracts/**                  # 跨端契约真相源
```

已退役：

```text
docs/proposals/**             # 仅保留历史迁移参考
docs/superpowers/specs/**     # 不是项目事实源
docs/superpowers/plans/**     # 不是项目事实源
```

## 文件归属

| 范围 | 默认 Owner |
| --- | --- |
| `docs/product-requirements.md` | Product Agent |
| `docs/architecture*.md` | Architect Agent |
| `docs/tech-selection.md` | Architect Agent |
| `contracts/**` | Architect Agent |
| `apps/android/**` | Android Agent |
| `services/api/**` | Server Backend Agent |
| `apps/web/**` | Web Frontend Agent |
| 内容源文件和内容指南 | Content Agent |
| 测试代码和测试报告 | Test Agent，或任务所属实现 Agent |
| `AGENTS.md`、`docs/development-workflow.md`、`docs/index.md` | Leader Agent |
| `openspec/changes/**` | Leader 协调；Owner 取决于具体变更范围 |

实现 Agent 只能修改自己拥有的范围，除非 `policy.md` 或 Leader 明确授权更多范围。

## 锁与 Worktree 规则

- 同一文件同一时间只能有一个 Owner。
- 共享文件需要严格串行执行或 worktree 隔离。
- 契约变更先于消费端适配。
- 只有契约稳定后，消费端适配才能并行开始。
- 持久锁可存放在 `.agents/locks/<task-id>.json`。

最小锁记录：

```json
{
  "task_id": "short-task-name",
  "owner": "Agent name",
  "base_ref": "commit sha",
  "mode": "serial | worktree",
  "status": "active | released",
  "locked_paths": ["path/**"],
  "created_at": "YYYY-MM-DD"
}
```

## 测试命令

| 范围 | Windows / PowerShell | Unix shell |
|---|---|---|
| Android 单元测试 | `.\gradlew.bat :app:testDebugUnitTest --no-daemon` | `./gradlew :app:testDebugUnitTest --no-daemon` |
| Android 构建 | `.\gradlew.bat :app:assembleDebug --no-daemon` | `./gradlew :app:assembleDebug --no-daemon` |
| 后端测试 | `mvn -f services/api/pom.xml test -q` | `mvn -f services/api/pom.xml test -q` |
| Web 构建 | `npm --prefix apps/web run build` | `npm --prefix apps/web run build` |
| 工作流检查 | `powershell -ExecutionPolicy Bypass -File scripts/check-suilearn-workflow.ps1` | 可用时使用 PowerShell Core |

如果工具不可用，报告准确原因，并提供手工验证清单。

## 子 Agent 派发模板

每个子 Agent prompt 必须包含：

- 角色。
- 任务名称和精确任务文本。
- 相关产物摘录。
- 允许文件和禁止文件。
- `base_ref` 和审查 diff 命令。
- 必需测试。
- 返回格式。
- 是否允许写文件。

返回格式：

```text
STATUS: DONE / DONE_WITH_CONCERNS / NEEDS_CONTEXT / BLOCKED
Changed files:
Tests:
Summary:
Assumptions:
Blockers:
```

## 审查严重级别

- P0：阻塞运行、数据破坏、安全问题或严重范围违规。
- P1：核心行为错误、重大架构违规或重要测试缺失。
- P2：可维护性、边界情况或中等回归风险。
- P3：风格、命名或轻微文档问题。

P0/P1 阻塞完成。

## 工作流检查器

关闭工作流变更和 CI 中应尽量运行检查器：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/check-suilearn-workflow.ps1 -BaseRef <base_ref>
```

检查器会拒绝退役文档流程下的变更，并标记常见工作流漂移，例如修改受保护实现/事实文档路径却没有 active OpenSpec change 的 `tasks.md` 和 `policy.md`。

关闭指定 change 时，传入 `-ClosingChange <change-name>`，额外检查该 change 的产物是否仍停留在 open 或 in-progress 状态：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/check-suilearn-workflow.ps1 -BaseRef <base_ref> -ClosingChange <change-name>
```
