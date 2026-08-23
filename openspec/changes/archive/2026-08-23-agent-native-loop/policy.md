# Agent-Native Loop 策略

Status: Approved
批准者: 用户
批准日期: 2026-08-23
批准依据: 用户指令“继续执行change3”，并按已声明的推荐方案执行 3a。

- Change: `agent-native-loop`
- 级别: Major
- base_ref: `e801f849cb464d7f4498616d89d19baabc5fbad1`
- 当前阶段: Spec -> Build
- 执行模式: serial（L3）
- 决策记录: `.agents/notes/implemented/architecture/2026-08-23-agent-native-loop.md`

## 角色归属

- Server Backend Agent：`services/api/**` 实现与相邻测试。
- Test Agent：干净 shell 独立执行定向验证。
- Reviewer Agent：单人自审，`review_mode: single-agent`。
- Leader Agent：任务卡、文件范围、归档。

## 允许修改文件

- `openspec/changes/agent-native-loop/**`
- `.agents/notes/implemented/architecture/2026-08-23-agent-native-loop.md`
- `.agents/notes/implemented/architecture/2026-08-23-agent-native-loop.md`
- `services/api/src/main/java/com/suilearn/api/agent/llm/**`
- `services/api/src/main/java/com/suilearn/api/agent/loop/**`
- `services/api/src/main/java/com/suilearn/api/agent/runtime/**`
- `services/api/src/main/java/com/suilearn/api/agent/controller/**`
- `services/api/src/main/java/com/suilearn/api/agent/tool/**`
- `services/api/src/main/resources/agents/agent-loop/**`
- `services/api/src/test/java/com/suilearn/api/agent/llm/**`
- `services/api/src/test/java/com/suilearn/api/agent/loop/**`
- `services/api/src/test/java/com/suilearn/api/agent/runtime/**`
- `services/api/src/test/java/com/suilearn/api/agent/controller/**`
- `services/api/src/test/java/com/suilearn/api/agent/tool/**`

## 禁止修改文件

- `apps/**`
- `contracts/**`
- 旧 `LearningAgentPort.java`、`LearningAgentController.java`、`SpringAiAlibabaLearningAgentAdapter.java`、旧 ReactAgent 拓扑与 `spring-ai-alibaba-agent-framework` 依赖（3b follow-up）
- `docs/**`
- Redis、RabbitMQ、MinIO、正式内容存储模块
- 其他 active change 目录

## 基线测试

- 基线提交 `e801f84` 已含 change-1/2：38 个定向测试全绿。
- 编辑前运行 38 测试回归；完整 `mvn test` 仍为 35 个无 PostgreSQL 环境 errors。

## 验收矩阵

| 场景 | 期望 |
|---|---|
| LlmClient SSE | content/tool_calls/usage 聚合正确 |
| LlmClient 非 2xx | 稳定失败，不返回部分成功 |
| AgentLoop 工具循环 | tool_call -> tool_result -> RESULT -> DONE |
| 空回答 | 最多 2 次 nudge，仍空 INVALID_MODEL_OUTPUT |
| 预算 | BUDGET_EXHAUSTED + FAILED |
| 重复/缺参/越权 | 去重执行一次/可修复错误/服务端拒绝 |
| ask_user | WAIT_FOR_INPUT；submitReply 后继续并 DONE |
| 非等待 submitReply | AGENT_TURN_NOT_WAITING_FOR_INPUT |
| study_agent | 不出现 TURN_EXECUTOR_UNAVAILABLE（fake LLM） |
| 未接线能力 | 显式 unavailable failed |
| 离线 Eval | 默认 profile 不访问公网/真实模型 |

## 高风险事件立即审查

- LLM 协议解析、工具权限、并发/去重、暂停恢复状态转换。
- 任何写入语义或事件终态变化。

## 审查重点

- LlmClient/loop 不依赖 Spring AI 类型。
- 事件 metadata 不含 prompt、原文、模型原始输出或 key。
- 旧路径在本 change 不得修改。
- 3b legacy 删除必须具名 follow-up，不得静默延期。
