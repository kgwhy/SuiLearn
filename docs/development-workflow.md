# SuiLearn Leader 驱动开发流程

## 目标

本流程用于解决手动多 Agent 开发中的五类问题：

- 身份遗忘：每个任务都显式声明当前角色、职责和禁止事项。
- 重复修改冲突：任务开始前锁定文件，禁止并行修改同一文件。
- 代码质量不稳：实现后必须经过测试和审查清单。
- Bug 返工多：修复前先补复现测试或明确复现步骤。
- Token 消耗大：每个 Agent 只接收本任务需要的上下文。

## 总流程

```text
用户需求
  ↓
Leader 识别任务类型、角色归属和风险
  ↓
Leader 同步项目事实源，确认当前文档、代码和 Git 状态
  ↓
Leader 拆分任务、锁定文件、列出决策点
  ↓
需要人工确认时先问用户
  ↓
执行 Agent 按边界实现
  ↓
测试 Agent 验证
  ↓
审查 Agent 检查需求一致性、职责边界和质量
  ↓
Leader 汇总结果，决定完成、返工或重新拆分
```

## Leader 职责

- 单一入口：跨角色、多步骤或不确定任务先由 Leader 接收。
- 任务拆分：把需求拆成可独立验证的小任务。
- 文件锁：每个任务只能有一个 owner 修改同一文件。
- 决策收口：执行 Agent 不直接向用户追问，阻塞问题交给 Leader 汇总。
- 上下文裁剪：给执行 Agent 的上下文只包含相关需求、相关文档、文件范围、禁止事项和验收标准。
- 质量门禁：没有完成测试和审查前，不视为完成。

## 上下文同步

Leader 不依赖长对话记忆来判断项目事实。每次开始开发任务前，必须按任务需要同步外部事实源。

必读事实源：

- `AGENTS.md`
- `agents/leader.md`
- `docs/development-workflow.md`
- 本次任务对应的角色文件，例如 `agents/android.md`、`agents/server-backend.md` 或 `agents/web-frontend.md`
- 当前 `git status`，确认已有未提交改动

按需读取事实源：

- 产品范围：`docs/product-requirements.md`
- 技术约束：`docs/tech-selection.md`
- 架构边界：`docs/architecture.md`
- 灵感背景：`docs/chat.md`，只读参考，不直接当作已确认需求
- 相关代码、测试、错误日志和 CodeGraph 查询结果

同步后，Leader 需要在任务卡中写明“上下文来源”。如果上下文来源之间冲突，先列为决策点，不直接选择其中一方实现。

## 外部记忆规则

重要结论不能只留在聊天里，必须写回项目资产：

- 产品决策写入 `docs/product-requirements.md`，由产品 Agent 维护。
- 技术决策写入 `docs/tech-selection.md` 或 `docs/architecture.md`，由架构 Agent 维护。
- 协作流程写入 `docs/development-workflow.md`，由 Leader Agent 维护。
- 角色边界写入 `agents/*.md` 或 `AGENTS.md`。
- 代码行为由测试和实现共同固化，不能只靠口头约定。

如果某个决策还没有写回文档，Leader 在后续任务中只能把它当作“待确认上下文”，不能当作稳定事实。

## AI First 文档架构

SuiLearn 文档优先服务于 AI 开发上下文消费：让执行 Agent 快速理解“当前系统长什么样、本次改什么、影响哪里”。详细 Proposal 规则以 `docs/proposals/README.md` 为准，本流程只规定 Leader 如何把这些规则纳入任务卡、派发、审查和完成定义。

当前阶段采用以下结构：

```text
docs/chat.md                  # 灵感材料，只读参考
docs/product-requirements.md  # 当前产品规格，产品真相源
docs/architecture.md          # 当前系统架构与模块边界
docs/tech-selection.md        # 当前技术选型与约束
docs/development-workflow.md  # 多 Agent 协作流程
docs/proposals/*.md           # 可选：尚未并入当前规格的变更提案
```

规则：

- `docs/product-requirements.md` 只表达当前已确认的产品规格，不承载历史版本库，也不堆叠大量需求索引。
- 未来变更先进入 `docs/proposals/*.md`，具体状态、命名、门禁、关闭和 `Spec Key` 规则见 `docs/proposals/README.md`。
- AI 开发任务的文档输入包优先使用“当前规格 + 已批准 Proposal + 相关技术/架构约束”，避免一次性注入无关历史文档。

### Proposal 实现门禁

Leader 派发引用 `docs/proposals/*.md` 的实现任务前，必须按 `docs/proposals/README.md` 检查 Proposal 状态、当前规格影响、角色和文件影响、验收标准、实现后关闭方式。未通过门禁的 Proposal 只能用于讨论、探查或原型验证，不能作为已确认实现依据。

## 任务卡模板

每个开发任务开始前，Leader 先生成任务卡。

```text
任务名称：
背景：
上下文来源：
文档输入包：
  Current Spec:
  Approved Proposal:
  Architecture:
  Tech Constraints:
  Excluded Docs:
目标：
归属角色：
子 Agent 策略：
隔离模式：none / worktree
可修改文件：
禁止修改文件：
依赖任务：
前置任务（已完成）：
需要用户确认：
实现要求：
测试要求：
审查重点：
完成定义：
```

如果任务很小，可以压缩任务卡，但必须保留：目标、归属角色、文档输入包、隔离模式、可修改文件、禁止修改文件、完成定义。

文档输入包规则：

- `Current Spec` 必须列出当前实现依据，例如 `docs/product-requirements.md` 的相关章节。
- `Approved Proposal` 只允许列出状态为 `Approved` 的 `docs/proposals/*.md`；如果没有，写“无”。
- `Architecture` 列出相关架构文档，例如 `docs/architecture.md` 或 `contracts/**`。
- `Tech Constraints` 列出相关技术约束，例如 `docs/tech-selection.md`。
- `Excluded Docs` 列出本任务不得当作实现依据的材料，例如 `docs/chat.md`、`Draft Proposal` 或历史 diff。

## 子 Agent 调用决策

Leader 每次开始任务前，必须在任务卡中写明子 Agent 策略：

```text
子 Agent 策略：不调用 / 可选调用 / 必须调用
理由：
```

### 不调用

满足以下条件时，Leader 默认不调用子 Agent，直接执行：

- 单一角色任务。
- 修改文件不超过 3 个。
- 不涉及共享文件或跨端契约。
- 不引入新依赖、新存储、新架构边界。
- 不需要独立审查即可通过明确测试验证。
- 任务可以在当前上下文内完整完成。

典型例子：

- 修正文档中的一处角色引用。
- 移动少量文件并修正 package / import。
- 修复一个有明确测试覆盖的小 bug。

### 可选调用

满足以下条件时，Leader 可以调用子 Agent，但必须说明为什么调用或为什么不调用：

- 单一角色任务，但修改文件较多。
- 需要较多代码阅读，但实现仍由一个角色完成。
- 需要测试 Agent 独立补测试或 Reviewer Agent 独立审查。
- Leader 当前上下文较重，继续塞入实现细节会影响后续判断。

可选调用时，Leader 可以选择：

- 自己实现，另派 Reviewer / Test 子 Agent 做验证。
- 派一个执行子 Agent 实现，Leader 负责整合和最终验证。
- 不调用子 Agent，但在最终结果中说明原因。

### 必须调用

满足以下任一条件时，Leader 必须调用子 Agent，或先说明无法调用的原因并请求用户确认：

- 任务跨两个或以上长期角色，例如 Android + Content、Server Backend + Contracts、Web + Server Backend。
- 涉及 `contracts/**`、根 Gradle 配置、数据迁移、存储方案或技术选型变更。
- 需要并行探索多个互相独立的问题。
- 实现和审查都很关键，且同一上下文内自写自审风险较高。
- 修改超过 8 个文件，或会影响多个用户主流程。
- 用户明确要求使用子 Agent、并行 Agent 或多 Agent 协作。

必须调用时，Leader 至少拆出以下一种子任务：

- 实现子任务：由对应执行 Agent 修改明确文件范围。
- 测试子任务：由测试 Agent 补测试或跑验证清单。
- 审查子任务：由审查 Agent 做需求一致性和质量审查。
- 探查子任务：只读分析并返回阻塞问题，不写代码。

如果工具环境暂时不支持调用子 Agent，Leader 必须在任务卡中写明：

```text
子 Agent 策略：必须调用，但当前无法调用
降级方式：
风险：
是否需要用户确认：
```

## 文件锁规则

- 同一时刻，一个文件只能被一个任务锁定。
- 涉及共享文件时，任务必须串行执行。
- 如果实现过程中发现必须修改未授权文件，执行 Agent 停止扩大修改，向 Leader 返回“越界申请”。
- Leader 判断越界是否合理；必要时重新拆任务或请求用户确认。
- 共享文件变更必须在结果中说明影响哪些角色。

### 文件锁风险等级

| 风险等级 | 判定条件 | 隔离要求 |
|---|---|---|
| 低 | 各 Agent 修改文件无交集，且不在同一模块内 | 可以在同一 worktree 内并行 |
| 中 | Agent 之间无直接共享文件，但有跨模块边界（如 Android 与 Web） | 推荐 worktree 隔离，也可串行 |
| 高 | 涉及 `contracts/**`、根 `build.gradle.kts`、`docs/*.md`、或同一目录下的多 Agent 修改 | **必须** worktree 隔离或严格串行 |

高风险任务如果选择串行，Leader 必须在任务卡中说明为什么不使用 worktree 隔离。

## Worktree 隔离

当任务卡 `隔离模式` 为 `worktree` 时，子 Agent 在独立 git worktree 中工作，Leader 负责回收合并。
`worktree` 是隔离能力要求，不绑定具体工具名；如果当前 Codex/Agent 环境提供 `EnterWorktree` 等封装入口，可以使用封装入口，否则使用 `git worktree` 原生命令。

### 使用规则

- Leader 派发前必须为子 Agent 创建或指定独立 worktree，并在任务卡中写明 worktree 路径和分支名。
- 子 Agent 在 worktree 中修改文件，完成后由 Leader `git merge` 回收。
- 回收时冲突由 Leader 人工仲裁，必要时请求用户确认。
- 同一 worktree 内可串行执行多个低风险子任务；不同 worktree 之间完全隔离。
- 以下场景**必须**使用 worktree 隔离：
  - 跨角色并行任务涉及共享文件（`contracts/**`、根 `build.gradle.kts`、根 `settings.gradle.kts`）
  - 两个及以上执行 Agent 并行修改同一模块
  - 契约稳定后，多个消费端适配任务并行执行

契约变更本身不得与消费端适配并行；只有在契约变更已合入并被 Leader 确认为稳定后，消费端适配任务才可以并行。

### 原生命令模板

如果没有封装入口，Leader 使用以下 git 原生命令创建隔离 worktree：

```bash
git worktree list
git worktree add -b codex/<task-slug> ../SuiLearn-worktrees/<task-slug> HEAD
```

如果分支已经存在：

```bash
git worktree add ../SuiLearn-worktrees/<task-slug> codex/<task-slug>
```

回收合并完成后：

```bash
git worktree remove ../SuiLearn-worktrees/<task-slug>
git worktree prune
```

如果当前环境不能创建 sibling worktree，Leader 必须选择以下之一：

- 请求用户授权创建 worktree。
- 将任务卡改为 `隔离模式: none`，并声明“严格串行”及原因。
- 停止任务并报告无法满足 worktree 隔离。

### 不需要 worktree 隔离的场景

- 单一角色、单一模块、无共享文件冲突的串行任务
- 只读探查任务
- 文件修改范围完全不重叠的低风险并行任务

## 当前文件归属

| 范围 | 默认 owner | 说明 |
| --- | --- | --- |
| `docs/product-requirements.md` | 产品 Agent | 当前产品规格和验收标准 |
| `docs/chat.md` | 只读参考 | 灵感材料，不直接当作 PRD |
| `docs/proposals/**` | 产品 Agent / 架构 Agent | 变更提案；产品行为归产品 Agent，技术或架构变更归架构 Agent |
| `docs/tech-selection.md` | 架构 Agent | 技术选型与阶段约束 |
| `docs/architecture*.md` | 架构 Agent | 模块边界、数据演进、接口契约 |
| `apps/android/**` | Android Agent | 第一版 Android App 全部客户端实现，包括 UI、ViewModel、domain、data、本地导入和 Android 测试代码 |
| `services/api/**` | Server Backend Agent | 第二版及后续 Java Spring Boot 服务端 |
| `apps/web/**` | Web Frontend Agent | 第三版及后续 React Web 前端 |
| `contracts/**` | 架构 Agent | API 契约、JSON schema、跨端模型对齐 |
| 根目录 `build.gradle.kts`、`settings.gradle.kts`、`gradle.properties` | 共享文件 | 跨模块依赖、插件、构建配置，必须说明影响范围 |

如果后续新增题库源文件或内容规范，默认归内容 Agent。Android 内部可以按 presentation / domain / data 分层，但默认不拆成长期独立 Agent；只有大任务需要并行时，Leader 才在任务卡中临时拆分 Android UI、Android Domain/Data、Android Test 子任务。

## Contracts 协作规则

`contracts/**` 是跨端契约的单点真相，由架构 Agent 拥有和维护。

默认职责：

- 架构 Agent：设计和修改 OpenAPI、JSON schema、跨端模型契约。
- Server Backend Agent：按契约实现服务端 API、DTO、数据库映射和服务端校验。
- Android Agent：按契约消费服务端 API 或解析内容 schema，不自行改变契约。
- Web Frontend Agent：按契约消费 API，维护 Web 端 API client 和 TypeScript 类型消费层。
- Content Agent：按 schema 维护题库内容，发现 schema 不足时向 Leader / 架构 Agent 提出变更请求。
- 测试 Agent / 审查 Agent：验证实现、测试和内容是否与契约一致。

修改规则：

- 任何 Agent 发现需要修改 `contracts/**`，必须先向 Leader 返回越界申请或阻塞问题。
- Leader 将契约变更交由架构 Agent 处理；涉及产品行为变化时，先交产品 Agent 确认。
- 契约变更完成后，再派发 Server Backend、Android、Web 或 Content 的实现/适配任务。
- 不允许实现 Agent 为了本端方便直接改契约并同步修改自己的实现。

### 时序约束：Contracts 先行

契约变更与消费端实现**不得并行执行**。Leader 必须保证以下执行顺序：

1. 架构 Agent **先行、单独**完成契约变更，Leader 确认 merge。
2. Leader 确认契约稳定后，再派发消费端（Server Backend、Android、Web Frontend、Content）的适配任务。
3. 消费端适配任务可以并行（使用 worktree 隔离）。
4. 不允许 "一边改契约一边改实现" —— 契约变更合入之前，消费端任务不得开始写代码。

涉及共享文件（根 `build.gradle.kts`、`settings.gradle.kts` 等）同理：修改任务必须串行，或通过 worktree 隔离后由 Leader 统一 merge。

## 人工决策点

以下情况必须先问用户，不能由执行 Agent 静默决定：

- 改变产品范围、用户流程、版本优先级。
- 改变技术栈、引入新依赖、改变存储方案。
- 删除或迁移用户数据。
- 改变题库内容标准、答案口径或知识点分类。
- 一个任务需要跨多个职责域并修改共享文件。
- 当前文档和实现冲突，且无法判断应改文档还是改实现。

以下情况可以由执行 Agent 做保守假设，但必须在结果中标明：

- 局部变量命名、函数拆分等低风险实现细节。
- 不改变行为的测试辅助代码。
- 与现有风格一致的 UI 微调。
- 可回退、可验证、不影响产品范围的小型实现选择。

## 子 Agent 派发规则

只有任务卡的子 Agent 策略为“可选调用”且 Leader 决定调用，或策略为“必须调用”时，才进入本节派发流程。

Leader 派发任务时，任务描述必须包含：

- 角色：本次按哪个 Agent 执行。
- 目标：只描述本任务要完成的结果。
- 上下文来源：本任务依据哪些文档、代码、测试或错误日志。
- 文档输入包：当前规格、Approved Proposal、架构约束、技术约束和排除文档。
- 文件范围：允许修改和禁止修改的文件。
- 依据：相关 PRD、技术文档或架构规则。
- 验收：要跑的测试、要满足的断言或手动检查路径。
- 返回格式：改了什么、测试结果、假设、阻塞问题。
- 是否允许写文件：探查子任务必须明确只读；实现子任务必须明确可写范围。

子 Agent 遇到无法决策的问题时，不直接扩大实现；返回阻塞清单：

```text
阻塞问题：
1. 问题：
   影响：
   可选方案：
   推荐：
```

Leader 汇总阻塞问题后统一向用户确认，再继续派发执行任务。

## Bug 修复流程

修 Bug 时优先使用回归保护：

1. 测试 Agent 或对应实现 Agent 先写失败测试，或给出明确复现步骤。
2. 实现 Agent 做最小修复。
3. 跑目标测试，确认失败测试变为通过。
4. 跑相关回归测试，确认没有破坏已有流程。
5. 审查 Agent 检查是否只是掩盖问题、是否引入硬编码或越权修改。

如果无法自动化测试，必须记录手动验证步骤和无法自动化的原因。

## 审查清单

审查 Agent 每次至少检查：

- 是否符合 `docs/product-requirements.md`。
- 如果任务引用了 `docs/proposals/**`，该 Proposal 是否为 `Approved`，且实现后是否需要合并回当前规格文档。
- 如果本次实现完成了 Proposal 的全部范围，是否已把稳定结论合并回当前规格，并将 Proposal 状态更新为 `Implemented`，或明确记录未完成项。
- 是否符合 `docs/tech-selection.md` 和 `docs/architecture.md`。
- 是否越权修改职责外文件。
- 是否出现写死业务数据、魔法值或临时绕过逻辑。
- 是否缺少必要测试或回归验证。
- 是否引入未请求功能、过度抽象或无关重构。
- 是否破坏现有用户流程。

审查结论必须给出严重级别、文件位置、影响、建议修复和负责 Agent。

## 完成定义

任务满足以下条件才算完成：

- 任务卡中的目标已实现。
- 没有未确认的产品或技术决策。
- 修改文件没有超出锁定范围，或越界已被 Leader 批准。
- 相关测试或手动验证已完成。
- 审查清单没有 P0/P1 阻塞问题。
- 如果任务完成了 Approved Proposal，Proposal 已关闭或留下明确未完成项。
- 输出中说明了改动内容、验证结果、风险和后续建议。

## Token 控制

Leader 给执行 Agent 的上下文应控制在最小必要范围：

- 必给：任务卡、相关文档摘录、相关文件路径、验收标准。
- 可给：相关测试、已有错误日志、接口签名。
- 不给：完整历史聊天、无关文档、其他角色的探索过程。

执行 Agent 返回结果时只交付摘要、关键文件、测试结果、假设和阻塞问题，不回传完整思考过程。

如果任务跨越多个回合，Leader 在继续执行前要重新同步 `git status`、相关文档和相关文件，不默认上一回合的上下文仍然准确。

### Leader 上下文分流

Leader 自身不应把所有文档和子 Agent 输出全部注入上下文。分流策略：

| 工作 | 分流目标 | 说明 |
|---|---|---|
| 事实同步 | Explore 子 Agent（只读） | 读取 git status、最近 commit、相关文档摘要、代码路径清单、冲突点标注 |
| 代码审查 | Reviewer 子 Agent | 独立审查需求一致性、边界、质量；Leader 只看 P0/P1 清单 |
| 测试执行 | Test 子 Agent | 独立跑测试、汇总结果；Leader 只看通过/失败摘要 |

Leader 自身只持有：

- 任务卡
- 事实摘要（来自 Explore Agent，不超过一屏）
- 子 Agent 结果摘要（改了什么、测试结果、阻塞问题）
- P0/P1 问题清单

禁止注入 Leader 上下文的内容：

- 子 Agent 的完整思考过程
- 完整代码 diff（需要看具体改动时再读取对应文件）
- 无关角色的文档全文
- 历史聊天记录
