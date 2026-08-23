# Agent Note: 在固定 Eval 与 Docker 回归通过后删除旧 ReactAgent 路径
Status: implemented

## Problem

change-3a 后新旧 Agent 路径并存。计划要求旧 ReactAgent/旧 REST/Alibaba 依赖删除，但删除必须在固定 Eval 通过后执行。

## Decision

- 已删除旧 LearningAgentPort/ReactAgent/controller/REST/依赖。
- 检索与练习工具类型迁移到 StudyScope/PracticeDifficulty。
- 新增 LlmPracticeModelPort 与新基础设施配置，保留记忆 bean 供工具使用。
- OpenAPI 删除旧 /api/v2/agents/study 与 StudyAgent* schema。

## Alternatives considered

- **保留 deprecated 旧 REST**：否决，计划明确不保留 legacy 双跑。
- **一并删除 Prompt/Context/Memory 旧类**：否决，Phase 4/5 再演进，避免 3b 范围膨胀。
- **练习生成直接内联到 AgentLoop**：否决，复用确定性 helper 与引用校验更安全。

## Consequences

- 新 runtime 是唯一 Agent 路径；旧 REST 客户端必须切换新契约。
- 55 个新路径测试全绿；Docker 完整回归除 Testcontainers socket 环境外全绿。
- 旧 Prompt/Context/Memory 类暂留为死代码，Phase 4/5 清理。
