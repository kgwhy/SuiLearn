# Agent ContextBuilder 策略

Status: Approved
批准者: 用户
批准日期: 2026-08-23
批准依据: 用户指令“继续”。

- Change: `agent-context-builder`
- 级别: Major
- base_ref: `3376a6f6e106cb2894e1c4b055c449d8f355802f`

## 允许修改文件

- `openspec/changes/agent-context-builder/**`
- `.agents/notes/**`
- `services/api/src/main/java/com/suilearn/api/agent/context/**`
- `services/api/src/main/java/com/suilearn/api/agent/loop/**`
- `services/api/src/main/java/com/suilearn/api/agent/runtime/**`
- `services/api/src/main/java/com/suilearn/api/agent/infrastructure/turn/SessionMessageJpaRepository.java`
- `services/api/src/test/java/com/suilearn/api/agent/context/**`
- `services/api/src/test/java/com/suilearn/api/agent/loop/**`

## 历史提交路径覆盖（非本 change 修改）

- `contracts/**`
- `services/api/pom.xml`
- `services/api/config/local.properties.example`
- `services/api/src/main/resources/application.properties`
- `services/api/src/main/java/com/suilearn/api/agent/**`
- `services/api/src/main/resources/agents/**`
- `services/api/src/test/java/com/suilearn/api/agent/**`
- `services/api/src/test/resources/agent-turn/**`

以上为 change-1/2/3 已提交文件；本 change 不修改这些历史覆盖声明，仅用于未 push 时 workflow checker 的历史 diff 覆盖。

## 禁止修改文件

- `apps/**`、`contracts/**`、`docs/**`、非 agent 后端模块、新数据库表。

## 基线测试

- 55 个新 runtime 定向测试通过；Docker 完整回归除 Testcontainers socket 外通过。
