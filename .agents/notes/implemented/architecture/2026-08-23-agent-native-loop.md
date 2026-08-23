# Agent Note: 采用 LlmClient 流式端口与通用 AgentLoop，legacy 删除拆到 3b
Status: implemented

## Problem

change-1/2 后的新 runtime 仍 unavailable。计划 change-3 要求一次完成 LlmClient、AgentLoop、ToolDispatcher、暂停恢复并同阶段删除旧 ReactAgent；沙箱无真实模型和 PostgreSQL，单批次替换无法用完整回归对照，风险不可控。

## Decision

- change-3 拆为 3a/3b。3a 已新增 LlmClient 端口、OpenAI-compatible SSE adapter、AgentLoop、ToolDispatcher、ask_user 暂停恢复，并用离线 fake LLM fixed Eval 证明循环。
- 3a 未修改旧路径；3b 在真实 runtime fixture 冒烟与回归具备后删除旧 ReactAgent/旧 REST/Alibaba 依赖。
- LlmClient 默认聚合流式 chunk，业务层不依赖 Spring AI 类型。
- ask_user 使用每回合回复队列阻塞虚拟线程，submitReply 后原位继续。

## Alternatives considered

- **严格单批次删除 legacy**：否决，缺少真实模型和完整后端回归，失去对照。
- **复用 Spring AI ChatClient**：否决，重新引入框架图/agent 边界。
- **ask_user 终态化回合、下次重启 loop**：否决，计划明确不重跑整个回合。
- **文本标签回退作为默认**：否决，计划要求原生 function calling 优先。

## Consequences

- study_agent 在 fake LLM 下完成 RESULT+DONE，不再 unavailable。
- 54 个定向测试全绿；完整回归仍为 35 个无 PostgreSQL 环境 errors。
- 新旧 Agent 路径并存一个周期；3b `agent-native-loop-retire-legacy` 是具名 follow-up。
- fake LLM 不证明真实模型质量；3b 前需要真实 adapter 冒烟。
