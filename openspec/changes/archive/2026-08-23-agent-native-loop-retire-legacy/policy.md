# 退役旧 ReactAgent 策略

Status: Approved
批准者: 用户
批准日期: 2026-08-23
批准依据: 用户指令“继续”，且 change-3a 固定 Eval 与 Docker 完整回归已提供删除门禁证据。

- Change: `agent-native-loop-retire-legacy`
- 级别: Major
- base_ref: `9dfa79724f0091da6fbdc75a0ee77e0d1d730181`

## 允许修改文件

- `openspec/changes/agent-native-loop-retire-legacy/**`
- `.agents/notes/**`
- `contracts/openapi/suilearn-v2.yaml`
- `services/api/pom.xml`
- `services/api/src/main/java/com/suilearn/api/agent/**`
- `services/api/src/test/java/com/suilearn/api/agent/**`

## 历史提交路径覆盖（非本 change 修改）

- `contracts/**`
- `services/api/pom.xml`
- `services/api/config/local.properties.example`
- `services/api/src/main/resources/application.properties`
- `services/api/src/main/java/com/suilearn/api/agent/**`
- `services/api/src/main/resources/agents/**`
- `services/api/src/test/java/com/suilearn/api/agent/**`
- `services/api/src/test/resources/agent-turn/**`

以上为 change-1/2/3a 及 fix 已提交文件；本 change 不修改这些历史覆盖声明，仅用于未 push 时 workflow checker 的历史 diff 覆盖。

## 禁止修改文件

- `apps/**`
- `docs/**`
- 其他 active change
- 非 agent 后端模块（generation/material/knowledgebase/task 等）

## 基线测试

- 干净 shell 54 tests 通过；Docker 下完整回归 416 tests / 0 errors（排除 Testcontainers socket 环境用例）。

## 验收矩阵

| 场景 | 期望 |
|---|---|
| 源码残留 | 无 ReactAgent/LearningAgentPort/旧 controller |
| OpenAPI | 无 /api/v2/agents/study 与 StudyAgent* |
| 依赖 | 无 spring-ai-alibaba-agent-framework |
| 工具行为 | search/read/practice 测试全绿 |
| 新 runtime | AgentLoop Eval 全绿 |
| 完整回归 | Docker 下除 Testcontainers socket 环境外全绿 |

## 高风险事件立即审查

- 契约删除、依赖删除、Spring 装配、工具类型迁移。
