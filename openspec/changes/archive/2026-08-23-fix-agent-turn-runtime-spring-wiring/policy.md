# 修复 Agent Turn Runtime Spring 装配策略

Status: Approved
批准者: 用户
批准日期: 2026-08-23
批准依据: 用户提供 Docker CLI 后运行完整回归发现的装配错误，属 change-3a follow-up 修复。

- Change: `fix-agent-turn-runtime-spring-wiring`
- 级别: Standard
- base_ref: `e15bdd644db728256eee7907ef7aa6c69be54f34`

## 允许修改文件

- `services/api/src/main/java/com/suilearn/api/agent/controller/AgentTurnController.java`
- `services/api/src/main/java/com/suilearn/api/agent/runtime/AgentTurnRuntimeConfiguration.java`
- `openspec/changes/fix-agent-turn-runtime-spring-wiring/**`

## 历史提交路径覆盖（非本 change 修改）

- `contracts/**`
- `services/api/pom.xml`
- `services/api/config/local.properties.example`
- `services/api/src/main/resources/application.properties`
- `services/api/src/main/java/com/suilearn/api/agent/**`
- `services/api/src/main/resources/agents/agent-loop/**`
- `services/api/src/test/java/com/suilearn/api/agent/**`
- `services/api/src/test/resources/agent-turn/**`

以上为 change-1/2/3a 已提交并归档的文件；本 change 不修改它们，仅用于归档前 workflow checker 在未 push 时的历史 diff 覆盖。

## 禁止修改文件

- 其他业务代码、契约、`apps/**`、`docs/**`。

## 验证

- Docker 启动 PostgreSQL/Redis/RabbitMQ/MinIO 后运行完整 `mvn test`。
- 目标：除 Testcontainers 无法访问 Docker socket 的环境用例外，无新失败。
