# Agent Note: 生产收口采用“同环分流 + 失败不阻断回合”策略
Status: implemented

## Problem

rag_qa/question_generation 仍 unavailable；L1/snapshot 未挂在线回合；RAG pipeline 未接入生产检索主路径。

## Decision

- 复用同一 AgentLoop，按 capability 切换 prompt policy 与工具面，不在 loop 内新增分支。
- TurnOrchestrator 在 loop 终态后记录 L1 trace、turn snapshot 并提交 consolidation 命令；记忆失败只诊断不阻断回合。
- RagService/SearchService 经 RagPipeline 调用；embedding 成功后在 MaterialImportService 记录 index_versions ready 版本。
- ParseEngineRegistry 提供为 Spring bean，但 material revision 路径保留 DocumentParser，避免丢失 revision/block 语义。

## Alternatives considered

- **为 rag_qa/question_generation 新写独立 loop**：否决，增加重复调度/预算/暂停逻辑。
- **在 AgentLoop 内记录记忆**：否决，把跨切面职责塞进循环且终态前记录会放大失败面。
- **用 @Primary Retriever 全局替换**：否决，会影响全部既有消费者并造成 bean 覆盖不确定性。
- **material import 改用 ParseEngineRegistry**：否决，ParsedDocument 是纯文本 IR，无法替代 revision/block 结构化流水线。

## Consequences

- 88 个定向测试全绿；三能力均不再 unavailable。
- 回合终态后记忆写入幂等且失败不改变终态。
- RagService/SearchService 生产检索经 pgvector-hybrid pipeline；embedding 成功写 ready index version。
- 真实运行态与 Android 客户端仍为具名 follow-up。
