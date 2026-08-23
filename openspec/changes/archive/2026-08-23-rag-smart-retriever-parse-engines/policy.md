# SmartRetriever/ParseEngine 策略

Status: Approved
批准者: 用户
批准日期: 2026-08-23
批准依据: 用户指令“按流程完成改造计划剩余部分”。

- Change: `rag-smart-retriever-parse-engines`
- 级别: Major
- base_ref: `5936b5ca9ae7ee76f2e96bdd88ff39df25c2e195`

## 允许修改文件

- `openspec/changes/rag-smart-retriever-parse-engines/**`
- `.agents/notes/**`
- `services/api/src/main/java/com/suilearn/api/rag/**`
- `services/api/src/test/java/com/suilearn/api/rag/**`

## 禁止修改文件

- `apps/**`、`contracts/**`、`docs/**`、现有 material 解析行为。

## 历史提交路径覆盖（非本 change 修改）

- `contracts/**`
- `services/api/pom.xml`
- `services/api/config/local.properties.example`
- `services/api/src/main/resources/application.properties`
- `services/api/src/main/java/com/suilearn/api/agent/**`
- `services/api/src/main/resources/agents/**`
- `services/api/src/test/java/com/suilearn/api/agent/**`
- `services/api/src/test/resources/agent-turn/**`
