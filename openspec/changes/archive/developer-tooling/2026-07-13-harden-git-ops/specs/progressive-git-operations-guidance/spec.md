## ADDED Requirements

### Requirement: Git 操作指引必须渐进式加载
`git-ops/SKILL.md` MUST 只保留触发条件、核心安全规则、固定执行顺序和一层直达的 reference 路由。提交前检查、提交信息、分支与发布细节 MUST 位于独立 reference，且 Skill 不得要求每次 Git 操作读取全部 reference。

#### Scenario: 用户只请求查看 Git 状态
- **WHEN** 用户请求 `git status`、diff 或日志等只读操作
- **THEN** Skill MUST 不要求加载提交预检或发布 reference

### Requirement: 提交路径必须先加载安全预检
当用户请求暂存、提交、推送前提交或检查待提交内容时，`git-ops` MUST 在暂存审阅后、提交前加载并执行提交预检 reference 中的步骤。

#### Scenario: 用户请求创建提交
- **WHEN** 用户要求暂存并提交变更
- **THEN** Skill MUST 在运行 `git commit` 前执行安全预检

### Requirement: 默认提交模板不得包含验证或风险段落
`git-ops` 的默认提交正文模板 MUST 只包含可选的“变更摘要”段落，不得包含“验证”“风险与备注”或语义等价的强制段落，也不得要求它们作为 commit body 的内容。

#### Scenario: 用户要求常规提交
- **WHEN** Skill 提供默认 Conventional Commit 模板
- **THEN** 模板 MUST 不显示验证或风险说明段落

### Requirement: 交接报告与提交模板职责分离
Skill MAY 在交接报告中说明已运行的验证、未处理的风险或遗留的脏文件，但这些信息 MUST 不作为提交消息模板或提交前置条件。

#### Scenario: 提交完成后需要交接
- **WHEN** `git-ops` 汇报提交结果
- **THEN** 它 MAY 单独报告验证和风险信息，而不改变已创建提交的正文模板
