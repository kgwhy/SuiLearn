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

## 禁止修改文件

- 其他业务代码、契约、`apps/**`、`docs/**`。

## 验证

- Docker 启动 PostgreSQL/Redis/RabbitMQ/MinIO 后运行完整 `mvn test`。
- 目标：除 Testcontainers 无法访问 Docker socket 的环境用例外，无新失败。
