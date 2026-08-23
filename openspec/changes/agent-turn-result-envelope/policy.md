# TurnResult 信封策略

Status: Approved
批准者: 用户
批准日期: 2026-08-23
批准依据: 用户指令继续且 Android 延后。

- Change: `agent-turn-result-envelope`
- 级别: Major
- base_ref: `d5554b6c7f0f9c2c8b263aa4fe0c8f878d7b4c6d`

## 允许修改文件

- `openspec/changes/agent-turn-result-envelope/**`
- `.agents/notes/**`
- `contracts/openapi/suilearn-v2.yaml`
- `services/api/src/main/java/com/suilearn/api/agent/controller/**`
- `services/api/src/test/java/com/suilearn/api/agent/**`

## 禁止修改文件

- `apps/**`（Android 明确延后；Web not affected）
- 其他后端模块。

## 历史提交路径覆盖（非本 change 修改）

- `contracts/**`
- `services/api/pom.xml`
- `services/api/config/local.properties.example`
- `services/api/src/main/resources/application.properties`
- `services/api/src/main/java/com/suilearn/api/agent/**`
- `services/api/src/main/java/com/suilearn/api/rag/**`
- `services/api/src/main/resources/agents/**`
- `services/api/src/test/java/com/suilearn/api/agent/**`
- `services/api/src/test/java/com/suilearn/api/rag/**`
- `services/api/src/test/resources/agent-turn/**`
