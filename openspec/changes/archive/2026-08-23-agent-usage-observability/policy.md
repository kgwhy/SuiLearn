# Agent Usage 策略

Status: Approved
批准者: 用户
批准日期: 2026-08-23
批准依据: 用户指令“按流程完成改造计划剩余部分”。

- Change: `agent-usage-observability`
- 级别: Major
- base_ref: `e10b91f2427dc83fb2f9fa8086fcec603165dc45`

## 允许修改文件

- `openspec/changes/agent-usage-observability/**`
- `.agents/notes/**`
- `services/api/src/main/java/com/suilearn/api/agent/llm/**`
- `services/api/src/main/java/com/suilearn/api/agent/loop/**`
- `services/api/src/test/java/com/suilearn/api/agent/llm/**`
- `services/api/src/test/java/com/suilearn/api/agent/loop/**`

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

以上为已提交历史文件；本 change 不修改，仅用于未 push 时 workflow checker 覆盖。

## 禁止修改文件

- `apps/**`、`contracts/**`、`docs/**`、非上述后端文件。
