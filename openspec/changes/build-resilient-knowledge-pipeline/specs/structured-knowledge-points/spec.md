## ADDED Requirements

### Requirement: 知识点是结构化学习内容
系统 SHALL 将资料知识点生成为包含标题、简短总结、定义、核心原理、应用场景、易错点和来源引用的结构化内容，而不是只有关键词或统一占位描述。

#### Scenario: AI 返回完整知识点
- **WHEN** READY 资料的证据足以支持知识点生成且 AI 返回通过 schema 校验的结果
- **THEN** 系统保存结构化知识点草稿，并在列表展示标题与简短总结

#### Scenario: AI 返回不完整结构
- **WHEN** AI 结果缺少必需总结、定义、原理、场景、易错点或可追溯来源
- **THEN** 生成任务失败或拒绝该条结果，系统不以占位文字补成正式知识点

### Requirement: 禁止关键词 fallback
系统 MUST NOT 在 AI 未配置、调用失败或无可用结果时使用本地词项抽取和统一描述创建知识点。

#### Scenario: AI 失败不污染知识点
- **WHEN** 已配置 AI 调用失败或返回无可用知识点
- **THEN** 任务进入 FAILED 并显示原因，现有知识点保持不变，且不新增关键词记录

### Requirement: 知识点审核门禁
新生成知识点 SHALL 以 DRAFT 状态保存；用户 SHALL 能查看、编辑、确认或拒绝，且只有 CONFIRMED 知识点 SHALL 进入正式搜索、专项学习和知识点出题。

#### Scenario: 用户确认知识点
- **WHEN** 用户审阅并确认一个结构化知识点草稿
- **THEN** 系统将其标记为 CONFIRMED，并允许正式搜索、学习和面试题生成

#### Scenario: 用户拒绝知识点
- **WHEN** 用户拒绝一个知识点草稿
- **THEN** 系统保留必要审计状态但不把它暴露为正式知识点或出题来源

### Requirement: 自动生成与手动重新生成
资料首次进入 READY 后系统 SHALL 默认创建独立知识点生成任务，并 SHALL 允许用户手动重新生成而不覆盖已确认知识点。

#### Scenario: 资料处理完成
- **WHEN** 新资料首次成功进入 READY 且知识点自动生成开启
- **THEN** 系统创建可追踪的知识点生成任务，用户仍可立即阅读资料

#### Scenario: 用户重新生成知识点
- **WHEN** 用户对已有资料触发重新生成
- **THEN** 系统创建一批新 DRAFT，不删除、不修改已有 CONFIRMED 知识点

### Requirement: 来源版本可追溯
每个知识点 SHALL 引用生成时的 material revision 和具体 page/block/excerpt；资料产生新 revision 后旧知识点 SHALL 保持可见并标记来源待复核。

#### Scenario: 资料更新后查看旧知识点
- **WHEN** 已确认知识点引用的 revision 不再是资料当前 revision
- **THEN** 系统显示来源已更新/待复核状态，并允许用户比较新生成草稿

### Requirement: 旧知识点兼容
系统 SHALL 保留现有知识点并标记 legacy 来源，且 SHALL NOT 自动把旧关键词补写成未经审核的结构化知识点。

#### Scenario: 部署后读取旧知识点
- **WHEN** 迁移前知识点只有 name、description 和旧来源引用
- **THEN** 迁移后该知识点仍可查看，系统明确其 legacy 状态并允许用户重新生成结构化草稿
