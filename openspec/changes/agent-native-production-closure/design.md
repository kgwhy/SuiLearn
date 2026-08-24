# 生产收口设计（change-closure）

## Decisions

- **同环分流**：`TurnOrchestrator` 对三个内置能力调用同一个 `AgentLoop`；差异只在 manifest 与 `PromptBlockAssembler` 选择的 policy resource。
- **终态后记忆**：`MemoryTurnRecorder` 在 `AgentLoop.run` 返回后记录；所有异常被吞掉并 log，避免终态后新失败。
- **pipeline 后向兼容**：`RagService`/`SearchService` 保留 Retriever 构造器；存在 `RagPipeline` bean 时 Spring 选择 pipeline 构造器，缺 bean 的测试路径回退 `PgvectorHybridRagPipeline(retriever)`。
- **index version 随 embedding 成功写入**：`EmbeddingIndexVersionRecorder` 检查当前签名，同签名不重复建版本；新签名 beginVersion 并在 chunks 替换成功后 markReady。
- **ParseEngineRegistry 只提供统一解析 IR**：material revision 主路径继续使用 `DocumentParser`，避免丢失结构化 revision/block 语义。

## Alternatives considered

- 见 Agent Note。

## Data flow

```text
capability: study_agent|rag_qa|question_generation
  -> TurnOrchestrator -> AgentLoop -> ToolDispatcher -> ToolRegistry
  -> LoopResult -> MemoryTurnRecorder -> L1 trace / snapshot / command

RagController/SearchController
  -> RagService/SearchService -> RagPipeline
  -> PgvectorHybridRagPipeline -> KeywordRetriever

MaterialImportService embedding success
  -> EmbeddingIndexVersionRecorder
  -> EmbeddingSignature(status) -> beginVersion -> markReady
```
