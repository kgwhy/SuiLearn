# 策略

## 等级

Normal

原因：本变更影响后端资料导入、检索、RAG 和 Provider 状态，多文件实现但不改变 API 字段结构和数据库表结构。

## 角色

- 协调：Leader Agent
- 实现：Server Backend Agent

## base_ref

`3b8aababf1e49294a32a41eb8ed1780632364ad5`

## 文件锁

串行修改：

- `services/api/src/main/java/com/suilearn/api/model/EmbeddingStatus.java`
- `services/api/src/main/java/com/suilearn/api/retrieval/**`
- `services/api/src/main/java/com/suilearn/api/material/application/MaterialImportService.java`
- `services/api/src/main/java/com/suilearn/api/service/internal/SuiLearnV2Workflow.java`
- `services/api/src/main/java/com/suilearn/api/source/application/SourceService.java`
- `services/api/src/main/java/com/suilearn/api/config/SuiLearnAiProperties.java`
- `services/api/src/main/java/com/suilearn/api/service/AiProviderStatusService.java`
- `services/api/src/test/java/com/suilearn/api/service/SuiLearnV2ServiceTest.java`
- `services/api/src/test/java/com/suilearn/api/service/AiProviderStatusServiceTest.java`
- `services/api/src/test/java/com/suilearn/api/retrieval/KeywordRetrieverTest.java`

## 基线测试

已运行：

```powershell
mvn -f services/api/pom.xml test-compile -q
```

结果：通过，命令无输出，退出码 0。

已运行：

```powershell
mvn -f services/api/pom.xml "-Dtest=SuiLearnV2ServiceTest" test -q
```

结果：未通过，本机 PostgreSQL 测试库不存在：

```text
FATAL: database "suilearn_test" does not exist
```
