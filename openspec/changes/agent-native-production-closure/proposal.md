# Agent-Native 生产收口（change-closure）

## Why

上一轮归档后仍有四个具名 follow-up：`rag_qa`/`question_generation` 未接线、三层记忆在线生产者未挂接、RAG engine 未进入生产检索主路径、真实运行态验证受环境限制。本 change 在离线 deterministic 边界内收口前三个，第四个以离线 Eval 扩展补齐。

## What Changes

- `TurnOrchestrator` 将 `study_agent`、`rag_qa`、`question_generation` 全部路由到同一 `AgentLoop`；`PromptBlockAssembler` 按 capability 选择 general/policy prompt，工具面仍由 manifest 控制。
- 新增 `MemoryTurnRecorder`：AgentLoop 终态后记录 L1 trace、`turn` surface snapshot，并提交幂等 consolidation command；记忆失败只记录日志，不改变回合终态。
- `RecallMemoryTool` 在线注入 L2/L3 repository，将最近 L2 docs 与 L3 slots 并入 recall metadata。
- `RagService`/`SearchService` 经 `RagPipeline` 调用检索；新增 Spring 配置装配 `PgvectorHybridRagPipeline`、`PipelineFactory`、`IndexVersionManager`、`ParseEngineRegistry`。
- `MaterialImportService` 在 embedding 成功后调用 `EmbeddingIndexVersionRecorder`，按 embedding binding/model/dim/baseUrl/apiVersion 记录 `index_versions` ready 版本；换签名保持 needs_reindex 语义。
- 扩展离线 Eval：rag_qa 检索闭环、question_generation 练习闭环、记忆记录幂等、pipeline 服务路径。

## Non-Goals

- 不修改 `apps/**`、不切换 Android 新协议客户端。
- 不启动 Phase 8。
- 不在缺 Docker socket / 真实模型配置的沙箱伪造真实运行态证据。
- 不用 ParseEngineRegistry 替换 material revision/block 流水线。

## Acceptance Criteria

- 三个内置能力在 fake LLM 下均能完成 loop，rag_qa/question_generation 不再产生 `TURN_EXECUTOR_UNAVAILABLE`。
- 回合终态后 L1/snapshot/command 可验证；重复提交幂等；记忆失败不影响回合终态。
- RagService/SearchService 的检索调用经过 RagPipeline；embedding 成功写 ready index version，换签名 needs_reindex。
- 相关定向测试全绿，文件范围不越界。
