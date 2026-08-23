## ADDED Requirements

### Requirement: 旧 ReactAgent 运行时必须完全删除
系统 MUST NOT 包含 `LearningAgentPort`、`SpringAiAlibabaLearningAgentAdapter`、`ReactAgent`、`AgentFrameworkConfiguration` 或 `spring-ai-alibaba-agent-framework` 依赖；新 runtime 是唯一 Agent 路径。

#### Scenario: 残留扫描
- **WHEN** 扫描 `services/api` 源码与依赖树
- **THEN** 无旧类/依赖引用，且新工具测试全绿

### Requirement: 旧 Agent REST 必须删除
系统 MUST 删除 `/api/v2/agents/study/runs` 与 `/api/v2/agents/study/learners/{learnerId}/memories` 及 `StudyAgent*` OpenAPI components。

#### Scenario: 契约扫描
- **WHEN** 校验 OpenAPI
- **THEN** 不包含旧路径或旧 schema，新 `/api/v2/agent/**` 路径保持不变

### Requirement: 工具端口必须使用运行时类型
`EvidenceSearchPort`/`EvidenceReadPort` MUST 使用 `StudyScope`；`PracticeModelPort`/`GeneratePracticeTool` MUST 使用 `PracticeDifficulty`，MUST NOT 依赖 `LearningAgentPort`。

#### Scenario: 工具执行
- **WHEN** 执行检索与练习工具测试
- **THEN** 行为与 change-2 等价，仅类型边界变更

### Requirement: 练习 helper 不得依赖旧 catalog/budget
`PracticeCoachSubAgent` MUST NOT 使用 `AgentToolCatalog`/`SharedAgentBudget`/`AgentAction`/`AgentRole`；其校验与引用规则保留。

#### Scenario: 引用越界
- **WHEN** 练习模型返回 scope 外 citation
- **THEN** helper 拒绝并返回稳定错误

### Requirement: 新基础设施配置必须维持工具可用
系统 MUST 提供 `RetrievalEvidenceTools`、`PracticeModelPort`、`SessionMemoryService`、`MemoryManager` 等新工具依赖 bean；无模型配置时 `generate_practice` 返回稳定 `AGENT_MODEL_UNAVAILABLE`。

#### Scenario: 无 ChatModel
- **WHEN** LlmClient 未配置或不可用
- **THEN** 练习工具返回失败 ToolResult，不抛到 runtime
