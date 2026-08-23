# Agent-Native Capability 与声明式工具注册表（改造计划 change-2）

## Why

change-1 已落地回合运行时、事件流和 WS/REST 入口，但 `TurnRuntimeService` 仍使用统一 `UnavailableTurnExecutor`，没有能力路由和工具面。按 `docs/plans/suilearn-refactor-plan.md`，change-2 必须把固定 Agent 拓扑的能力/工具边界改为注册表模型，并在 change-3 接入 `AgentLoop` 前先稳定清单、权限与契约。

## What Changes

- 新增 `ToolRegistry`：扫描 Spring 容器中全部 `Tool` bean，按 name 建索引，输出 OpenAI function-calling 兼容 JSON Schema，并校验重复名/缺失 schema。
- 新增 `CapabilityRegistry`：注册 `study_agent`、`rag_qa`、`question_generation` 三个内置能力；未指定 capability 默认 `study_agent`，未知 capability 返回 `AGENT_CAPABILITY_UNKNOWN`。
- 新增 `TurnOrchestrator`：作为 `TurnExecutor` 按 `TurnContext.capability()` 路由；本阶段仍不执行真实 LLM 循环，只发布 capability 路由元数据与显式 `TURN_EXECUTOR_UNAVAILABLE + failed` 终态。
- 把旧固定动作改造成六个声明式 Tool bean：`search_knowledge`、`read_evidence`、`generate_practice`、`recall_memory`、`persist_memory`、`ask_user`。
- 工具权限从旧 `AgentToolCatalog` 类内 allowlist 迁移到 `CapabilityManifest.ownedTools()` + `ToolDefinition.requiredScopes()`；`ToolRegistry` 在运行时拒绝 capability 未拥有的工具。
- 新增 `GET /api/v2/agent/capabilities`：枚举能力清单、owned tools 与每个工具的 OpenAI 兼容 JSON Schema；受 `suilearn.agent.enabled` 总开关约束。
- `question_generation` 本轮只注册清单并拥有 `generate_practice`（计划允许的 deferred 选择）与 `ask_user`；`generate_questions` durable task 工具与 `SourceSelection` 校验边界放到 change-3/后续 generation change。

## Capabilities

### New

- `agent-capability-registry`: 能力/工具双层注册、OpenAI 工具 schema、路由默认值、权限交集和契约端点。

### Modified

- `agent-turn-runtime`: `TurnRuntimeService` 的默认 executor 改为 `TurnOrchestrator`（事件仍以 unavailable 失败终态结束，生命周期行为不变）。

## Impact

- `contracts/openapi/suilearn-v2.yaml`：新增 additive `GET /api/v2/agent/capabilities` 与 schema；旧端点语义不变。
- `services/api/src/main/java/com/suilearn/api/agent/{runtime,capability,tool,controller}/**`：新增注册表、六个 Tool bean、orchestrator 与 controller。
- `services/api/src/test/java/com/suilearn/api/agent/**`：新增 registry/tool/orchestrator/controller/契约测试。
- 不修改 `apps/android/**`、`apps/web/**`、旧 `LearningAgentPort`/ReactAgent、旧 REST 行为、Redis/语义记忆或正式内容写入路径。

## Non-Goals

- 不实现 `AgentLoop`、`LlmClient`、`ToolDispatcher` 的 LLM 循环调用（change-3）。
- 不让 Agent 工具写入正式题库、生成内容 store 或学习统计；`persist_memory` 只写既有 Agent 语义记忆，不写领域实体。
- 不实现真实 `rag_qa` 回答或 `question_generation` durable task 提交。
- 不删除旧 `AgentToolCatalog`/旧 SubAgent；它们继续服务旧同步 Agent 路径，change-3 验收后统一删除。
- 不引入 MCP、动态插件加载或运行时扩权。

## Acceptance Criteria

- `/api/v2/agent/capabilities` 在 Agent 启用时可枚举 3 个 capability 与 6 个工具；关闭时返回 `AGENT_FEATURE_DISABLED`。
- `ToolRegistry.openAiSchemas()` 输出 OpenAI function-calling 兼容结构（name/description/parameters）。
- `CapabilityRegistry` 默认 `study_agent`；`study_agent` 拥有全部 6 个工具，`rag_qa` 只拥有检索/证据工具，`question_generation` 只拥有其清单声明工具。
- `ToolRegistry.requireTool(capability, tool)` 拒绝 capability 未拥有的工具与未知工具，不依赖旧 `AgentToolCatalog` 的类内 allowlist。
- 六个 Tool bean 的确定性路径有单元测试；AI 或 memory 依赖不可用时返回结构化失败 `ToolResult`，不抛到 runtime 假装成功。
- `TurnOrchestrator` 按 capability 发布路由元数据并保持 change-1 的 unavailable 终态语义。
- 旧 Agent 路径与 change-1 的 27 个测试保持通过。
