# 渐进式加载工作流规格

## ADDED Requirements

### Requirement: Workflow context MUST load progressively

SuiLearn Agent 工作流 MUST 支持按任务状态渐进式加载上下文，避免每个任务都读取完整工作流说明。

#### Scenario: Exploratory task

- **WHEN** 用户提出探索、设计讨论或方案比较问题
- **THEN** Agent 只需要加载常驻规则、workflow skill 路由和 Explore 相关 reference
- **AND** 不需要加载 Build、Verify、Archive 的完整执行细节

#### Scenario: File edit task

- **WHEN** Agent 准备修改文件
- **THEN** Agent 必须加载 Gate A、活动角色文件、active change 的 `policy.md` 和 `tasks.md`
- **AND** 必须声明计划修改文件与角色策略核对结果

#### Scenario: Completion claim

- **WHEN** Agent 准备声明任务完成
- **THEN** Agent 必须加载 Verify/Gate C 相关 reference
- **AND** 必须提供验证命令、diff stat、文件范围核对和 reviewer-style 自审

### Requirement: Workflow authority MUST be layered

SuiLearn 工作流规则 MUST 明确区分 ruler、Skill、doc 和自动化检查的职责。

#### Scenario: Agent routing

- **WHEN** Agent 需要判断下一步工作流动作
- **THEN** `suilearn-workflow` Skill 必须作为轻量路由器提供状态判断和 reference 导航
- **AND** 不应要求 Agent 每次全量读取 `docs/development-workflow.md`

#### Scenario: Human audit

- **WHEN** 人类或 Reviewer 需要理解完整制度
- **THEN** `docs/development-workflow.md` 必须保留完整工作流说明和背景
- **AND** 可以作为审查、培训和后续细分规则归属的事实源
