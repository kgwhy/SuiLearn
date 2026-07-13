# SuiLearn 工作流

SuiLearn 工作流是本仓库唯一的软件开发生命周期。它吸收 OpenSpec 风格的
规格驱动开发状态机、Superpowers 风格的子 Agent/TDD/调试/验证纪律，以及
SuiLearn 的角色和文件边界策略。

本文是完整的人类可审查政策说明和稳定事实源，不是每次 Agent 任务的强制
全量加载入口。Agent 执行时应先读取 `AGENTS.md` 的常驻红线，再使用
`.agents/skills/suilearn-workflow` 作为轻量路由器，按当前状态、角色、门禁
和变更等级读取对应 `references/**`。只有在审查、修改工作流政策、解决规则
冲突或 reference 不足时，才需要回到本文读取完整背景。

项目级状态机：

```text
Explore -> Spec --[Approval Gate]--> Build -> Verify --[Sync Gate]--> Archive
             ^                           |
             +---- spec issue found -----+
```

## 原则

- 渐进加载：常驻规则回答“什么不能违反”，Skill 回答“当前该加载什么”，本文回答“完整制度为什么这样设计”。
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

语言规则：

- `openspec/changes/**` 下的 Spec 产物统一使用中文编写。
- 适用文件包括 `proposal.md`、`design.md`、`tasks.md`、`policy.md`、`verification.md`、`archive.md` 和 `specs/**`。
- 代码标识、命令、路径、API 字段、错误码和必须保留语义的英文术语可以保留原文。

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
- 同一用户问题链路只能有一个 active change home。后续发现的 502、CORS、端口、默认值、环境变量、数据迁移等同根因修复，必须追加到主 change 的 `tasks.md` / `verification.md`，不能散落到无关 change。若另一个 change 只是需要运行环境证据，应引用主 change，而不是复制配置验证细节。

可以使用 OpenSpec CLI 创建和检查变更。若 CLI 输出 `changeRoot`、`artifactPaths` 或产物顺序，应使用这些具体值，而不是猜测路径。

退出条件：Approval Gate 通过。

配置 / 启动 / 集成类变更附加要求：

适用于端口、CORS、Docker/Compose、反向代理、环境变量、CI wrapper、数据库连接、第三方服务地址和本地/容器启动方式等。即使代码量很小，也必须在进入 `Build` 前写清：

- 验收矩阵：列出每个必须支持的运行组合、入口 URL/端口、预期结果和是否允许手动配置。
- 默认值语义：明确用户要的是“默认无需切换”还是“可通过变量切换”；若需求是无需手动切换，验收标准不得只写“可配置”。
- 覆盖口：列出哪些环境变量或配置只用于非默认端口、特殊网络或生产部署。
- 残留扫描项：列出需要检查的一组旧默认值、旧端口、旧 service 名或旧文案，例如 `5173`、`api:8080`、旧 project name。
- 运行态验证计划：至少覆盖一个真实边界，例如浏览器等价 `curl`、CORS preflight、容器环境变量 inspect、服务健康检查或实际 CLI/API 调用。静态解析命令不能单独作为完成证据。

## Approval Gate

进入 `Build` 前，Leader 必须确认：

- 范围、非目标、验收标准和受影响的当前事实文档已经明确。
- 每个任务都有 Owner、允许文件、禁止文件、测试命令和审查重点。
- 已记录 `base_ref`。
- 现有 worktree 变更已被归类为本范围内或既有变更。
- 共享文件使用严格串行执行或 worktree 隔离。
- active lock 不与新任务范围冲突。
- 对配置 / 启动 / 集成类变更，已完成附加要求中的验收矩阵、默认值语义、覆盖口、残留扫描项和运行态验证计划。
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
  Batch[Implementer + task-local tests] -> Test -> Spec Review -> Code Review -> Fix -> repeat
```

### 风险自适应批次

Build 的默认审查单位是风险一致的批次，不是每个细任务。Leader 按依赖顺序、共享文件和风险域组批；批次内由 Implementer 串行完成任务，每个行为变更仍执行 RED -> GREEN -> REFACTOR 和任务局部测试。任务在批次审查前只能标记为“实现完成、待批次审查”，不得声明最终完成。

批次末只组织一轮独立 Test -> Spec Review -> Code Review。审查修复优先运行失败测试和受影响模块回归；批次关闭时运行批次验收命令，最终 Verify 仍运行 change 规定的全量测试。共享配置、公共接口、测试基础设施或跨模块依赖发生变化时，Leader 必须扩大回归范围。

即时审查触发条件：契约或兼容性变化、数据迁移、权限或安全边界、并发/事务/幂等语义、跨模块公共接口，以及无法解释的测试失败。任一条件出现时立即暂停批次并组织相应 Test、Spec Review 或 Code Review，不等批次末统一处理。

上下文和证据按“足够复现、避免重复”原则传递：子 Agent 默认只接收任务卡、相关规格摘录、允许/禁止路径、受影响符号或文件、当前 diff 和验证命令。成功验证只回传命令、退出码、通过/失败计数和必要摘要；失败、间歇性问题或审计要求才附首个根因、关键原始日志片段或日志位置。

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
- 成功时报告命令、退出码、测试计数和必要摘要，不重复传递完整日志。
- 失败时报告首个根因、关键原始输出、失败用例、复现命令和手工验证替代方案。
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
- 第一次返工暴露语义偏差时停止补丁式实现，回到 `Spec` 更新 proposal/design/tasks/specs 中的验收标准。语义偏差包括用户指出“不是这个效果”、默认路径仍需手动配置、或运行态验证证明当前实现只满足“可配置”但不满足“默认可用”。

## 状态：Verify

目的：证明整个变更可以关闭。

Leader 必须：

- 运行或收集最终模块测试/构建输出。
- 运行 `git diff <base_ref> --stat`。
- 对照 `policy.md` 和角色规则检查变更文件。
- 确认 `tasks.md` 中每个任务已完成或明确延期。
- 对大范围变更派发最终审查。
- 当变更较大或跨多个任务时，在 `verification.md` 中记录验证证据。
- 对配置 / 启动 / 集成类变更，执行验收矩阵中的运行态验证，并记录原始输出或不可运行原因。必须同时包含旧默认值/旧端口残留扫描结果。

关闭检查：

- `tasks.md` 中每个任务都为 done，或延期工作链接到具名 follow-up change。
- `verification.md` 包含 `状态：已通过。`，或包含与最终报告一致的明确非发布状态。
- 对于声称完成的变更，`archive.md` 不得停留在 `Status: open`。
- 最终审查发现必须记录处置结果：已修复、延期到具名 change、用户接受或不适用。
- 当前事实文档不得继续把新工作指向 `docs/proposals/**` 等退役流程。
- 状态词扫描应限定在工作流产物和当前事实文档的工作流段落；例如 "pending content" 这类产品状态术语不是工作流未完成。
- 配置 / 启动 / 集成类变更不得只用构建、静态解析或单一 happy path 声明完成；必须证明默认路径和需要保留的覆盖路径都符合验收矩阵。

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
- 仅与任务相关的产物摘录；不得默认附完整对话或完整 change 文档。
- 允许文件和禁止文件。
- `base_ref` 和审查 diff 命令。
- 必需测试。
- 返回格式。
- 是否允许写文件。
- 所属批次、批次验收命令和即时审查触发条件。

成功返回使用紧凑证据：命令、退出码、通过/失败计数、变更摘要。失败返回额外包含首个根因、关键原始输出和复现命令。只有定位问题确有需要时才继续索取完整日志或更大上下文。

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
