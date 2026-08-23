# RAG Pipeline/Index 策略

Status: Approved
批准者: 用户
批准日期: 2026-08-23
批准依据: 用户指令“继续”。

- Change: `rag-pipeline-index-versioning`
- 级别: Major
- base_ref: `87b9493c18f591297186e31d8ce2a7a632ad900a`

## 允许修改文件

- `openspec/changes/rag-pipeline-index-versioning/**`
- `.agents/notes/**`
- `services/api/src/main/java/com/suilearn/api/rag/**`
- `services/api/src/test/java/com/suilearn/api/rag/**`

## 禁止修改文件

- `apps/**`、`contracts/**`、`docs/**`、现有 retrieval/material 行为。

## 历史提交路径覆盖（非本 change 修改）

- `contracts/**`
- `services/api/pom.xml`
- `services/api/config/local.properties.example`
- `services/api/src/main/resources/application.properties`
- `services/api/src/main/java/com/suilearn/api/agent/**`
- `services/api/src/main/resources/agents/**`
- `services/api/src/test/java/com/suilearn/api/agent/**`
- `services/api/src/test/resources/agent-turn/**`
