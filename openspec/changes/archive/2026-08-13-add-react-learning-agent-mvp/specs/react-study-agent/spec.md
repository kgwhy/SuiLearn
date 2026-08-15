## ADDED Requirements

### Requirement: Supervisor 必须使用有界 ReAct 编排学习任务
系统 MUST 通过一个 Supervisor ReAct Agent 理解用户学习请求，并在固定步骤、工具调用和超时预算内决定调用知识研究 SubAgent、练习辅导 SubAgent或结束任务。

#### Scenario: 资料讲解并生成练习
- **WHEN** 用户在有效知识库范围内请求讲解主题并生成练习
- **THEN** Supervisor 先取得有效 Evidence Bundle，再调用练习辅导 SubAgent，并在预算内返回讲解、引用和临时练习题

#### Scenario: 执行预算耗尽
- **WHEN** Supervisor 或 SubAgent 达到配置的 step、tool call 或 timeout 上限
- **THEN** 系统停止后续调用并返回 `BUDGET_EXHAUSTED` 或等价稳定状态、预算使用和已验证的部分结果

### Requirement: SubAgent 必须隔离上下文和能力
系统 MUST 固定提供 KnowledgeResearchSubAgent 与 PracticeCoachSubAgent；每个 SubAgent 只能接收完成职责所需的最小结构化上下文、工具和预算，且 MUST NOT 创建更多 Agent 或访问未授权 Memory/Store。

#### Scenario: 知识研究 SubAgent 执行检索
- **WHEN** Supervisor 委派一个带知识库或资料 scope 的研究任务
- **THEN** KnowledgeResearchSubAgent 只能调用知识检索和证据读取工具，并返回结构化 Evidence Bundle

#### Scenario: 练习 SubAgent 尝试直接读取数据库
- **WHEN** PracticeCoachSubAgent 的模型输出请求未注册的数据访问工具
- **THEN** 运行时拒绝该 action，且不得扩大工具集合或跨 Agent 复用隐藏工具

### Requirement: Agent 回答必须受知识范围和证据约束
系统 MUST 要求运行请求至少包含 `knowledgeBaseId` 或 `materialId`，并 MUST 复用现有 scope、删除状态和来源校验；回答与练习不得引用 scope 外或已删除资料。

#### Scenario: 单资料范围提问
- **WHEN** 用户指定一个有效 materialId 运行 Agent
- **THEN** 所有 Evidence 和 Citation 只来自该资料及其有效 revision/block

#### Scenario: 没有有效证据
- **WHEN** 指定范围内没有可验证 Evidence
- **THEN** 系统返回 `uncertain=true` 和空引用，并且不得生成声称基于资料的练习题

### Requirement: Agent REST 输出必须结构化且不泄露思维链
系统 SHALL 提供同步运行 API，返回 run/session 标识、回答、不确定状态、引用、临时练习、下一步建议、记忆状态、预算使用和 action trace；响应 MUST NOT 包含模型原始 reasoning、完整 Prompt 或原始模型响应。

#### Scenario: 成功运行 Agent
- **WHEN** 用户提交合法请求且依赖可用
- **THEN** 响应符合 OpenAPI 和 JSON Schema，action trace 只包含 step、agent/tool、status、duration 等执行元数据

#### Scenario: 模型输出不符合 Schema
- **WHEN** 模型首次返回缺字段、未知 action、空白必填字段或越界引用
- **THEN** 系统最多执行一次结构修复，仍无效则返回 `INVALID_MODEL_OUTPUT` 且不保存生成内容

### Requirement: 练习结果必须保持临时且不可污染正式题库
PracticeCoachSubAgent 产生的题目 MUST 只存在于本次 Agent 响应和允许的 session 摘要中，MUST NOT 写入 QuestionStore、GeneratedContentStore、正式任务或学习统计。

#### Scenario: Agent 生成练习题
- **WHEN** PracticeCoachSubAgent 返回结构化题目
- **THEN** 用户可在响应中查看题目、答案和解析，但现有正式题目列表、草稿审核列表和统计保持不变

### Requirement: Agent 依赖故障必须明确失败
Agent 未启用、模型不可用、Redis 不可用、scope 无效、预算耗尽或输出无效时，系统 MUST 返回稳定错误/降级状态，MUST NOT 伪造回答、引用、记忆或练习。

#### Scenario: AI 未配置
- **WHEN** Agent 已启用但没有可用 ChatModel
- **THEN** Agent readiness 降级且运行接口返回 `AGENT_MODEL_UNAVAILABLE` 或等价 503 错误

#### Scenario: Redis 在运行前不可用
- **WHEN** Agent 已启用但 session memory 无法连接 Redis
- **THEN** 运行接口返回 `AGENT_SESSION_MEMORY_UNAVAILABLE`，不得静默改用进程内会话

### Requirement: Agent 可观测性必须低基数且可验证
系统 MUST 记录运行、SubAgent、工具、上下文和记忆的受控指标与结构化执行元数据，同时 MUST NOT 在 metric tag 或普通日志中记录 learnerId、sessionId、正文、Prompt、原始响应或思维链。

#### Scenario: Agent 成功调用两个 SubAgent
- **WHEN** 一次运行完成研究和练习
- **THEN** 对应 run、subagent、tool、context 和 memory 指标增加，且 tag 只来自受控枚举
