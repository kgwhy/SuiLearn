# 三层记忆策略

Status: Approved
批准者: 用户
批准日期: 2026-08-23
批准依据: 用户指令“继续”。

- Change: `agent-three-layer-memory`
- 级别: Major
- base_ref: `e76c928cb716638441f54068f3496ca58cca21d5`

## 允许修改文件

- `openspec/changes/agent-three-layer-memory/**`
- `.agents/notes/**`
- `services/api/src/main/java/com/suilearn/api/agent/infrastructure/turn/**`
- `services/api/src/main/java/com/suilearn/api/agent/memory/**`
- `services/api/src/main/java/com/suilearn/api/agent/tool/**`
- `services/api/src/test/java/com/suilearn/api/agent/memory/**`

## 禁止修改文件

- `apps/**`、`contracts/**`、`docs/**`、旧 Redis/pgvector 记忆路径。

## 历史提交路径覆盖（非本 change 修改）

- `contracts/**`
- `services/api/pom.xml`
- `services/api/config/local.properties.example`
- `services/api/src/main/resources/application.properties`
- `services/api/src/main/java/com/suilearn/api/agent/**`
- `services/api/src/main/resources/agents/**`
- `services/api/src/test/java/com/suilearn/api/agent/**`
- `services/api/src/test/resources/agent-turn/**`
