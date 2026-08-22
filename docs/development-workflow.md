# SuiLearn 工作流（精简版）

SuiLearn 使用一个原生状态机管理所有产品、架构、契约和代码变更。本文是工作流的唯一完整政策源；`AGENTS.md` 和 workflow skill 只做路由摘要，不再复制完整规则。

```text
Explore -> Spec --[Approval Gate]--> Build -> Verify --[Sync Gate]--> Archive
             ^                           |          |
             |        spec issue --------+          |
             +---- verify fail: 小问题返 Build，范围/规格问题返 Spec
             +---- archive 前 sync 不通过返 Spec 或 Build
```

## 原则

- 新会话或上下文被压缩后，先重读 `AGENTS.md` 和 `suilearn-workflow/SKILL.md` 路由，再继续执行。

- 一个用户问题链路只有一个 active change home。
- 业务代码实现必须来自已批准任务。
- 契约先于消费端实现。
- 文件归属和允许路径必须先于编辑确认。
- 业务代码变更必须有测试或明确复现证据。
- 单人项目默认独立验证而不是独立 Agent：Test 用干净 shell 独立执行并保留原始输出；Review 用新会话/延迟自审或用户确认，并记录 `review_mode: single-agent`。
- 证据先于完成声明。
- 长期决策理由必须写入 `.agents/notes/`，不依赖聊天上下文或 Git 历史考古。
- 当前事实文档只写已落地事实；计划、目标和状态留在 active change。
- 用户可见 UI 变更必须附真实运行证据。
- 不在 `docs/proposals/**` 下创建新文件。

## 变更等级

| 等级 | 适用 | 最低产物 | 决策记录 | Build 循环 |
|---|---|---|---|---|
| Light | 单角色、不改变产品/架构/契约/存储/行为，通常不超过 3 个文件 | `tasks.md` | 可选 | L1 |
| Standard | 用户可见行为、多文件实现、普通 bug、常规配置 | `tasks.md`、`policy.md`；新功能或跨模块时补 `proposal.md`，数据/架构影响明显时补 `design.md` | 有取舍时必写 Agent Note | L2 或 L2 Auto |
| Major | 跨角色、共享文件、契约、存储、架构、工作流、安全或高风险变更 | `proposal.md`、`design.md`、`specs/**`、`tasks.md`、`policy.md`、`verification.md`、`archive.md` | 必写 Agent Note | L3 |

Light 允许不创建 OpenSpec change，但必须有一个可追溯的 `tasks.md`，内容至少包含：

```text
任务名称、Owner、允许文件、禁止文件、验证命令、完成定义
```

Standard 和 Major 必须进入 `openspec/changes/<change-name>/**`。

## 状态：Explore

允许阅读、比较、提问和风险分析。禁止写业务代码。退出条件是问题已表述清楚，或决定停止。

## 状态：Spec

- Light：写 `tasks.md`，Leader 或用户批准后进入 Build。
- Standard：写 `tasks.md`、`policy.md`；新功能或跨模块时补 `proposal.md`，需要时写 `design.md`。
- Major：写完整 proposal、design、specs、tasks、policy、verification、archive。

## Approval Gate

进入 Build 前必须满足：

- 范围和验收标准明确；
- `base_ref` 已记录；
- 允许/禁止文件已记录；
- 每个任务有 Owner 和验证命令；
- 决策记录已就绪：Major 或 Standard 有取舍时，在 `policy.md` 写明对应 `.agents/notes/` 路径；
- 批准状态已记录：在 `policy.md` 或 `tasks.md` 顶部写 `Status: Approved` / `状态：已批准`，并写批准者和日期；
- 配置/启动/集成类变更默认 Standard；发布关键路径（端口、CORS、Docker、数据库、服务地址）必须写清验收矩阵和默认值语义，Major 才要求完整残留扫描与运行态验证计划。

## 状态：Build

```text
L1 Light:    Implement -> Verify
L2 Standard: Implement -> Test -> Review -> Fix when needed
L2 Auto:     一次批准 proposal/tasks 后，逐任务 TDD、逐任务提交，失败或高风险步骤暂停，结束做一次 Review
L3 Major:    Batch[Implement + local tests] -> Test -> Spec Review -> Code Review -> Fix
```

- L2 的 Review 在单角色且低风险时允许合并为一次审查；跨角色、契约或数据变化必须独立。
- L2 Auto 只适用于已明确批准 proposal/tasks、每任务可独立验证和提交的 Standard 变更；用户取消或连续失败时退回普通 L2。
- L3 的 Spec Review 必须先于 Code Review。
- 高风险事件（契约、迁移、安全、并发/事务/幂等、跨模块接口、无法解释的失败）必须立即审查，不等待批次末。
- 子 Agent 只接收任务卡、相关规格摘录、允许/禁止路径、diff 和命令。
- 成功验证只回传命令、退出码、计数和摘要；失败才附首个根因和关键原始输出。
- 同一任务同一文件三轮修复仍失败，停止并返回 Spec。
- 用户取消时，停止新派发，等待进程退出，再处理未验收改动。

### 单人项目默认路径

SuiLearn 是单人项目，以下规则是默认，不是降级：

- Test：在干净 shell 独立运行命令，保留完整命令和原始输出；不得把实现 Agent 的同一次运行当作独立验证。
- Review：完成实现后开新会话或隔一段时间自审，或由用户确认；在 `verification.md` 或完成报告中记录 `review_mode: single-agent`。
- Major 的 Spec Review 先于 Code Review；用户明确参与审查也算有效 Review 证据。
- 子 Agent 不可用时，Leader 在同一会话中切换角色执行，但仍须遵守独立命令和延迟自审原则。

## 状态：Verify

完成声明前必须提供：

- `python3 scripts/change_scope.py --base <base_ref>` 输出，并按最小验证选择执行；
- 必需测试命令输出，或明确的不适用原因；
- `python3 scripts/check_agent_notes.py` 输出；
- `git diff <base_ref> --stat`；
- 文件范围核对；
- 任务完成或延期到具名 follow-up 的核对；
- P0/P1 阻塞问题必须已修复并复审；P2 必须修复、延期或由用户接受。

## Sync Gate

归档前同步：

- 产品结论 -> `docs/product-requirements.md`
- 架构结论 -> `docs/architecture.md`
- 技术结论 -> `docs/tech-selection.md`
- 契约 -> `contracts/**`
- 决策结论 -> `.agents/notes/`：`proposed/` 改写为 `implemented/`，Status 与目录一致
- 未同步项必须在 archive note 中记录 `not affected`。

## 状态：Archive

- 先运行 `python3 scripts/check_suilearn_workflow.py --closing-change <change>`。
- 使用项目命令 `python3 scripts/archive_openspec_change.py` 归档到 `archive/YYYY-MM-DD-<change-name>/`。
- 归档记录包含最终状态、实现引用、验证摘要、已同步事实、延期项和审查摘要。

## 当前事实文档

```text
docs/product-requirements.md  # 产品
docs/architecture.md          # 架构
docs/tech-selection.md        # 技术基线
contracts/**                  # 契约
```

`docs/chat.md` 是历史讨论，只读。`docs/proposals/**` 和 Superpowers 路径已退役，不创建新文件。未批准的计划草案优先放 active change 的 `proposal.md`；用户明确要求独立保存时放 `docs/plans/**` 并标记 Draft，`docs/plans/**` 不属于当前事实目录。

当前事实文档只写已实现且可验证的事实；“已批准 Build 目标”、实施进度、任务状态只存在于 active change。目标只有在验证和 Review 闭环后才改写为未标注的当前事实。

## 文件归属

| 范围 | 默认 Owner |
|---|---|
| `docs/product-requirements.md` | Product Agent |
| `docs/architecture*.md` | Architect Agent |
| `docs/tech-selection.md` | Architect Agent |
| `contracts/**` | Architect Agent |
| `apps/android/**` | Android Agent |
| `services/api/**` | Server Backend Agent |
| `apps/web/**` | Web Frontend Agent |
| 内容源文件 | Content Agent |
| 测试代码 | Test Agent 或任务所属实现 Agent |
| `AGENTS.md`、`docs/development-workflow.md` | Leader Agent |
| `.agents/notes/**` | Leader Agent |
| `openspec/changes/**` | Leader 协调，Owner 按 change 范围 |

## 测试命令

| 范围 | Windows | Unix |
|---|---|---|
| Android 单测 | `.\gradlew.bat :app:testDebugUnitTest --no-daemon` | `./gradlew :app:testDebugUnitTest --no-daemon` |
| Android 构建 | `.\gradlew.bat :app:assembleDebug --no-daemon` | `./gradlew :app:assembleDebug --no-daemon` |
| 后端 | `mvn -f services/api/pom.xml test -q` | 同左 |
| Web | `npm --prefix apps/web run build` | 同左 |
| 工作流 | `python3 -m unittest discover -s tests -p 'test_workflow_scripts.py'`；`python3 scripts/check_workflow_skill.py`；`python3 scripts/check_suilearn_workflow.py --base-ref <ref>`；`python3 scripts/check_agent_notes.py`；`python3 scripts/change_scope.py --base <ref>` | 同左 |

## 统一返回格式

所有子 Agent 和完成声明使用同一基础格式：

```text
STATUS: DONE / DONE_WITH_CONCERNS / NEEDS_CONTEXT / BLOCKED
Changed files:
Tests:
Summary:
Assumptions:
Blockers:
```

角色可追加字段，但不得替换或删除 `STATUS` 和 `Tests`。

## 审查严重级别

- P0：阻塞运行、数据破坏、安全问题或严重范围违规。
- P1：核心行为错误、重大架构违规或重要测试缺失。
- P2：可维护性、边界情况或中等回归风险。
- P3：风格、命名或轻微文档问题。

P0/P1 必须修复并复审。P2 必须修复、进入具名 follow-up 或由用户显式接受。
