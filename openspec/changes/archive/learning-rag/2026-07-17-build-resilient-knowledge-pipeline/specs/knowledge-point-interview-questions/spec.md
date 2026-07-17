## ADDED Requirements

### Requirement: 从已确认知识点直接生成面试题
系统 SHALL 在已确认知识点详情提供生成面试题操作，并 SHALL 使用该知识点结构化内容及其原文证据作为生成上下文。

#### Scenario: 知识点详情发起生成
- **WHEN** 用户在 CONFIRMED 知识点详情点击“生成面试题”
- **THEN** 系统创建异步题目生成任务，并保持用户可继续阅读资料或知识点

#### Scenario: 草稿知识点不可出题
- **WHEN** 用户尝试以 DRAFT、REJECTED 或 ARCHIVED 知识点作为正式出题来源
- **THEN** 系统拒绝请求并提示先确认有效知识点

#### Scenario: 旧版知识点必须重新生成
- **WHEN** 用户尝试从 legacy 知识点直接生成面试题
- **THEN** 系统返回稳定冲突错误，要求从该资料的现有 revision 重新生成结构化知识点并确认后再出题

#### Scenario: 来源已过期或引用无效
- **WHEN** 已确认知识点的 sourceOutdated 为 true，或没有至少一条当前有效的 material/revision/page-or-block 引用
- **THEN** 系统拒绝生成请求且不创建题目生成任务

### Requirement: 默认一键生成
系统 SHALL 为快捷操作默认生成 1 道中等难度简答题，且 SHALL NOT 要求用户填写 Prompt、分类 ID 或其他技术字段。

#### Scenario: 使用默认参数
- **WHEN** 用户直接点击主生成按钮且未展开更多设置
- **THEN** 请求使用数量 1、难度 MEDIUM、题型 SHORT_ANSWER，并创建待审核草稿

### Requirement: 渐进式高级设置
系统 SHALL 允许用户在“更多设置”中选择题型、难度和数量，并 SHALL 对数量和枚举值进行校验；单次生成数量 SHALL 为 1 至 10。

#### Scenario: 自定义出题参数
- **WHEN** 用户选择受支持题型、难度和有效数量后提交
- **THEN** 系统按选择创建生成任务，并返回对应数量的题目草稿结果

#### Scenario: 参数无效
- **WHEN** 数量为 11 或更大，或题型/难度不受支持
- **THEN** 系统返回 400 并在相关字段附近显示明确修正方式

### Requirement: 题目证据与知识点关联
生成的每道题 SHALL 保留 knowledgePointId、material/revision 来源引用和用于生成的证据摘录，并 SHALL 提供题干、答案和解析。

#### Scenario: 查看题目草稿来源
- **WHEN** 用户打开知识点生成的题目草稿
- **THEN** 系统展示关联知识点和可追溯原文证据，用户可从引用返回资料位置

#### Scenario: 按任务读取批量草稿
- **WHEN** 客户端轮询到题目生成任务成功
- **THEN** 任务结果提供 `/api/v2/tasks/{taskId}/question-drafts` 地址，并可从该地址读取本任务生成的全部待审核草稿

### Requirement: AI 内容确认门禁
题目生成结果 SHALL 保持 PENDING_REVIEW；用户 SHALL 能编辑、保存或丢弃，且未经保存的草稿 MUST NOT 进入正式题库、刷题、错题、收藏、搜索或统计。

#### Scenario: 保存审核后的题目
- **WHEN** 用户审阅或编辑题干、答案和解析后选择保存
- **THEN** 系统创建正式题目并保留知识点与来源引用，使其可进入现有学习闭环

#### Scenario: 审核不能篡改归属和证据
- **WHEN** 用户审核知识点面试题草稿并提交内容修改
- **THEN** 请求只允许修改审核动作和题目内容，不允许提交或修改 knowledgePointId、knowledgePointIds、sourceRefs、citations 或 generationTaskId

#### Scenario: 难度映射往返一致
- **WHEN** 用户保存 EASY、MEDIUM 或 HARD 的生成草稿并再次读取正式题目
- **THEN** 系统分别持久化为 2、3 或 4，并可无歧义映射回 EASY、MEDIUM 或 HARD

#### Scenario: 丢弃题目草稿
- **WHEN** 用户丢弃生成草稿
- **THEN** 草稿不得进入正式题库或影响学习统计

### Requirement: 题目生成失败隔离
题目生成失败 SHALL 只影响对应生成任务，不得改变资料 READY、知识点 CONFIRMED 或已有题目状态。

#### Scenario: AI 生成超时并最终失败
- **WHEN** 题目生成达到最大重试次数仍失败
- **THEN** 系统显示失败和重试入口，资料、知识点和已有题目保持不变

#### Scenario: AI 返回仅空白的必填内容
- **WHEN** AI 返回的题干、答案项、解析或其他必填生成文本在 trim 后为空
- **THEN** 对应题目生成任务失败且系统不持久化任何该无效题目草稿
