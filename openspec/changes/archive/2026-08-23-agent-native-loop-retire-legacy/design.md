# 退役旧 ReactAgent 设计（change-3b）

## Context

基线 `9dfa797`。change-3a 已证明新循环；旧路径只用于旧同步 API。删除边界按“运行时唯一化”执行。

## Decisions

### 1. 练习工具保留确定性 helper，不保留 Agent 图

`PracticeCoachSubAgent` 改为无 catalog、无 budget 的确定性校验 helper，仅由 `GeneratePracticeTool` 调用。`KnowledgeResearchSubAgent` 删除，其职责已由 search/read 工具直接实现。

### 2. Scope/Difficulty 类型迁出 LearningAgentPort

- `StudyScope` 已是 runtime 值对象，检索工具统一使用。
- 新增 `PracticeDifficulty` 枚举，练习工具与模型端口使用。
- 删除 `LearningAgentPort`。

### 3. 新基础设施配置

新增 `agent/config/AgentInfrastructureConfiguration`：
- `RetrievalEvidenceTools` 由 `RetrievalPort` 构造。
- `LlmPracticeModelPort` 由 `LlmClient` + ObjectMapper 构造，加载受控 prompt 资源。
- 会话/语义记忆与 embedding provider 保留旧实现，作为 Phase 5 前的现状。
- 不再注册 `PromptRegistry`/ContextManager/AgentHealthIndicator/AgentRuntimeReadiness/MemoryCandidateExtractor。

### 4. OpenAPI 删除而非 deprecated

计划明确不提供 legacy 双跑；删除 `/api/v2/agents/study/runs`、memory deletion 与全部 `StudyAgent*` components。

### 5. 依赖删除

只删 `spring-ai-alibaba-agent-framework`。Spring AI BOM 保留，供既有 AI infrastructure stub 与未来边界使用。

## Risks / Trade-offs

- 删除旧 REST 是不可逆 API 变更，但计划已声明兼容性边界不保留旧接口。
- 旧记忆/上下文类暂留为死代码，Phase 4/5 会替换；3b 不扩大范围。
