# Agent-Native Capability/Tool Registry 设计（change-2）

## Context

基线 `24720c1`。change-1 已提供 `TurnContext/StudyScope/TurnEventSink` 和每回合事件通道，但 executor 是统一的 `UnavailableTurnExecutor`。旧 MVP 仍用 `AgentToolCatalog.fixedMvp()` 在类内硬编码 Supervisor/SubAgent 动作白名单。

## Goals / Non-Goals

**Goals:**

- 能力与工具成为 Spring bean 注册表，新增工具只实现 `Tool` 并声明 `ToolDefinition`。
- 权限来自 `CapabilityManifest.ownedTools()` 与 `ToolDefinition.requiredScopes()` 的交集，运行时不得扩权。
- 契约先暴露能力与工具 schema，Web/Android/测试可从同一端点生成消费代码或校验 golden files。
- 旧 Agent 路径不变；新 runtime 在 change-3 前继续诚实 unavailable。

**Non-Goals:**

- 不在本轮调用真实 LLM，不实现循环调度。
- 不删除旧 ReactAgent/旧 catalog（change-3）。
- 不新增正式内容写入工具。

## Decisions

### 1. 注册表放在 `agent/runtime`，工具实现放在 `agent/tool`

```text
CapabilityRegistry(Map<String, Capability>)
ToolRegistry(Map<String, Tool>)
TurnOrchestrator implements TurnExecutor
```

**Alternative**: 全部放 `agent/capability`。否决：registry 是 runtime 编排职责，capability 包只放能力契约与内置能力。
**Alternative**: 复用 Spring `ApplicationContext.getBeansOfType` 动态扫描。否决：构造注入 `Map<String, Tool>` 更可测，且 bean name 和 `ToolDefinition.name()` 的一致性可在构造器校验。

### 2. Capability 接口保持 `manifest()` 单方法

内置能力是只读 manifest；change-3 由 `LoopCapability`/`AgentLoop` 增加执行协议，不在本轮给 `Capability` 加执行方法。

**Alternative**: 本轮给 `Capability` 加 `execute`。否决：change-2 没有 LLM 循环，提前执行协议会产生死代码。

### 3. 六个工具的真实性边界

| 工具 | 依赖 | 本轮行为 |
|---|---|---|
| `search_knowledge` | `EvidenceSearchPort` | 调现有 `RetrievalEvidenceTools` 的 scope 校验检索 |
| `read_evidence` | `EvidenceReadPort` | 按 stableId/sourceRef 读取并再次校验 scope/删除状态 |
| `generate_practice` | `PracticeCoachSubAgent`（可选） | 依赖不可用时返回 `success=false, code=AGENT_MODEL_UNAVAILABLE`；可用时只生成临时练习 |
| `recall_memory` | `MemoryManager`（可选） | 返回会话/语义召回摘要或 `MEMORY_UNAVAILABLE` |
| `persist_memory` | `MemoryManager`（可选） | 走既有 promotion policy，只写 Agent semantic memory |
| `ask_user` | 无 | 返回 `pauseForUser`，由 change-3 AgentLoop 解释为 WAITING_INPUT |

**Alternative**: `generate_practice` 直接调用 `PracticeModelPort`。否决：绕过 `PracticeCoachSubAgent` 会失去证据/引用校验，重复实现现有安全边界。
**Alternative**: `persist_memory` 直接写领域 snapshot。否决：计划 Phase 5 才引入领域 snapshot，本轮仍使用现有两层记忆。

### 4. OpenAI schema 形状

`ToolRegistry.openAiSchemas()` 返回：

```json
[{"type":"function","function":{"name":"search_knowledge","description":"...","parameters":{...}}}]
```

`parameters` 为 JSON Schema object，`requiredScopes` 不进入模型 schema，只用于服务端权限。

**Alternative**: 只输出裸 parameters map。否决：计划验收明确要求与 Spring AI `ToolCallback` 一致，function wrapper 是通用协议。

### 5. `/api/v2/agent/capabilities` 响应

```json
{
  "capabilities": [{"name":"study_agent","description":"...","ownedTools":[...]}],
  "tools": [{"name":"search_knowledge","description":"...","parameters":{...},
             "deferred":false,"requiredScopes":["kb","material"]}]
}
```

同时返回两个列表，客户端可做能力-工具关联，也便于单独消费 schema。端点受 `suilearn.agent.enabled` 门禁，和回合端点同一语义。

**Alternative**: 按 capability 内嵌 tools。否决：同一工具被多能力拥有时会重复展开，契约 diff 更脆。

### 6. TurnOrchestrator 本阶段终态

```text
stage_start(source=capability)
progress(capability manifest resolved)
error(code=TURN_EXECUTOR_UNAVAILABLE)
failed
```

这保留 change-1 的诚实 unavailable，同时验证能力路由。真实循环在 change-3 替换 executor。

**Alternative**: `rag_qa` 本轮直接同步 RAG 回答。否决：会把 change-3 的检索-证据-生成循环提前分散到能力实现，违反“通用循环优于固定图”。

### 7. requiredScopes 语义

- `kb`：工具调用必须提供 `knowledgeBaseId`，且必须等于 `TurnContext.scope.knowledgeBaseId()`（或 scope 允许）。
- `material`：工具调用必须提供 `materialId`，且必须在 scope 内。
- `learner`：只允许当前 `TurnContext.learnerId()`。
- `turn`：允许读取当前 turn/session 状态。
- `memory`：允许访问 Agent 记忆层。

工具实现统一用 `ToolArguments` 做 scope/长度校验，不把原始 scope 直接透传给模型参数。

## Risks / Trade-offs

- 工具依赖可选时，注册表仍展示 schema；调用方必须处理 `success=false` 的 `ToolResult`，这是计划内降级语义。
- `question_generation` 的 `generate_questions` durable 工具延后，能力清单先用 deferred `generate_practice` 占位，需在 change-3 或后续 generation change 明确收口。
- 旧 `AgentToolCatalog` 与旧 SubAgent 仍在旧路径存在，两套权限机制并存一个 change 周期；change-3 删除旧路径，避免长期双轨。
