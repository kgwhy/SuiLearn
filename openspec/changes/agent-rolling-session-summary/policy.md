# 滚动会话摘要策略

Status: Approved
批准者: 用户
批准日期: 2026-08-23
批准依据: 用户指令“继续”。

- Change: `agent-rolling-session-summary`
- 级别: Major
- base_ref: `faf4241abe242c7ac6471350c92b3002239f4a39`

## 允许修改文件

- `openspec/changes/agent-rolling-session-summary/**`
- `.agents/notes/**`
- `services/api/src/main/java/com/suilearn/api/agent/infrastructure/turn/**`
- `services/api/src/main/java/com/suilearn/api/agent/context/**`
- `services/api/src/main/java/com/suilearn/api/agent/loop/**`
- `services/api/src/main/java/com/suilearn/api/agent/runtime/**`
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

以上为已提交历史文件；本 change 不修改，仅用于未 push 时 workflow checker 覆盖。

## 禁止修改文件

- `apps/**`、`contracts/**`、`docs/**`、非 agent 后端模块。
- 不新增 memory 表或删除 Redis。
