# Single-agent workflow governance

## Purpose

Define long-term decision records, diff-scoped verification, single-agent review, and UI evidence rules for SuiLearn.

## Requirements

### Requirement: 长期决策必须写入 Agent Notes

Major 变更 MUST 新增或更新至少一条 `.agents/notes/` 决策记录；Standard 变更只要涉及行为取舍、架构判断、配置默认值语义或可复用契约，MUST 新增或更新决策记录。记录 MUST 包含 `## Alternatives considered`，`Status` MUST 与目录生命周期一致。`implemented/` 记录 MUST 使用现在时，并在文件、类名、默认值变化时同改动更新事实。

#### Scenario: Major 变更无决策记录

- **WHEN** 归档前 `check_agent_notes.py` 发现对应决策目录为空
- **THEN** 关闭变更失败，直到补上符合条件的 Agent Note

#### Scenario: implemented 记录包含提案时代 section

- **WHEN** `implemented/` 出现 `## Proposal` 或 `## Acceptance criteria`
- **THEN** `check_agent_notes.py` 必须失败

### Requirement: 验证范围必须来自变更范围

完成门禁 MUST 先运行 `scripts/change_scope.py --base <base_ref>`，再按 `references/verification-selection.md` 选择最小验证。不得默认运行全量测试，也不得以未覆盖受影响路径的部分测试伪装完成验证。

#### Scenario: 变更只触及 workflow 文档

- **WHEN** change-scope 只包含 `AGENTS.md`、`docs/**`、`.agents/**`、`scripts/**`
- **THEN** 最小验证为 workflow checker、Agent Notes checker 和 unittest，业务模块测试可记录不适用原因

#### Scenario: 变更触及后端与契约

- **WHEN** change-scope 包含 `services/api/**` 和 `contracts/**`
- **THEN** 必须运行后端测试与 OpenAPI 校验，并评估受影响消费端回归

### Requirement: 单人项目默认独立验证而不是独立 Agent

单人项目 MUST 默认以干净 shell 独立执行测试并保留原始输出；Review MUST 使用新会话、延迟自审或用户确认，并记录 `review_mode: single-agent`。不得把实现 Agent 的同一次运行当作独立验证。

#### Scenario: 无子 Agent 可用

- **WHEN** 环境无法派发独立子 Agent
- **THEN** 同一会话切换到 Test/Reviewer 角色执行，但命令独立运行且自审与实现之间有时间间隔或新会话边界

### Requirement: 当前事实文档只写当前事实

`docs/architecture.md`、`docs/tech-selection.md`、`docs/product-requirements.md` MUST 只包含已实现且可验证的事实。计划、目标和实施状态 MUST 只存在于 active OpenSpec change；目标只有在验证与 Review 闭环后才能改写为未标注的当前事实。

#### Scenario: 已归档变更的 Build 目标仍标注在当前事实文档

- **WHEN** `docs/architecture.md` 或 `docs/tech-selection.md` 出现“已批准 Build 目标”
- **THEN** 文档必须迁移：已实现内容改写为现在时当前事实，未实现内容移回 active change

### Requirement: 用户可见 UI 变更必须附真实证据

用户可见 UI 变更 MUST 附真实运行截图、录屏或 GIF，并记录 commit SHA、启动命令和运行条件。证据 MUST 来自本次 tree 的真实构建与真实后端；任何 mock、fixture 或测试钩子 MUST 在证据旁声明。二进制证据 MUST NOT 提交进主分支历史。

#### Scenario: Web 或 Android UI 变更缺证据

- **WHEN** 完成门禁或自审发现用户可见 UI 变更没有真实运行证据
- **THEN** 按 P1 处理，修复前不得关闭变更
