# Agent Note: 采用 Capability/Tool 双层注册表替代固定动作白名单
Status: implemented

## Problem

change-1 已建立回合运行时，但工具面仍是旧 `AgentToolCatalog.fixedMvp()` 类内 allowlist；新增能力或工具需要修改循环/拓扑，无法做到“新增工具只注册并声明权限”。

## Decision

change-2 建立两层注册表：

- `CapabilityRegistry` 注册 study_agent/rag_qa/question_generation，默认 study_agent，未知 capability 拒绝。
- `ToolRegistry` 注入全部 Tool bean，输出 OpenAI function-calling 兼容 schema，并按 capability ownedTools 计算权限。
- 六个工具 search_knowledge/read_evidence/generate_practice/recall_memory/persist_memory/ask_user 复用现有确定性边界；依赖不可用时返回结构化失败。
- `TurnOrchestrator` 本阶段只做能力路由并保留 `TURN_EXECUTOR_UNAVAILABLE + failed` 终态，真实 AgentLoop 留给 change-3。
- 新增 `GET /api/v2/agent/capabilities` additive 契约端点。

## Alternatives considered

- **给 Capability 接口直接加 execute 方法**：否决，本轮无 LLM 循环，会产生死代码。
- **ToolRegistry 动态扫描 ApplicationContext**：否决，构造注入 Map 更可测，且能校验重复名。
- **generate_practice 直接调 PracticeModelPort**：否决，绕过 PracticeCoachSubAgent 的证据/引用校验。
- **rag_qa 本轮直接同步 RAG 回答**：否决，会把通用循环逻辑分散到能力实现，违背计划架构。
- **按 capability 内嵌 tools 返回契约**：否决，共享工具重复展开，契约脆弱。

## Consequences

- 能力与工具清单由注册表可枚举，schema OpenAI 兼容。
- 越权工具在 runtime 拒绝；工具不写正式内容；模型/记忆不可用时结构化失败。
- 旧 Agent 路径保持不变；`AgentToolCatalog` 与新注册表并存到 change-3。
- `question_generation` 的 durable generate_questions 工具保持 deferred，清单使用 generate_practice/ask_user 占位。
