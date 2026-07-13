# 任务

## 状态

- [x] Owner: Server Backend Agent，新增 `TEXT_ONLY` chunk 状态和 embedding 能力探测。
- [x] Owner: Server Backend Agent，调整资料导入，无 embedding 时保存文本索引 chunk。
- [x] Owner: Server Backend Agent，调整 `KeywordRetriever` 和 `SourceService`，让 `TEXT_ONLY` chunk 可检索、可引用。
- [x] Owner: Server Backend Agent，调整 Provider 状态，使聊天配置即可进入文本 RAG 模式。
- [x] Owner: Server Backend Agent，补充无 embedding 模式测试。
- [x] Owner: Leader Agent，记录验证结果并关闭变更。

## 允许修改文件

- `services/api/src/main/java/com/suilearn/api/model/EmbeddingStatus.java`
- `services/api/src/main/java/com/suilearn/api/retrieval/EmbeddingProvider.java`
- `services/api/src/main/java/com/suilearn/api/retrieval/OpenAiCompatibleEmbeddingProvider.java`
- `services/api/src/main/java/com/suilearn/api/retrieval/KeywordRetriever.java`
- `services/api/src/main/java/com/suilearn/api/material/application/MaterialImportService.java`
- `services/api/src/main/java/com/suilearn/api/service/internal/SuiLearnV2Workflow.java`
- `services/api/src/main/java/com/suilearn/api/source/application/SourceService.java`
- `services/api/src/main/java/com/suilearn/api/config/SuiLearnAiProperties.java`
- `services/api/src/main/java/com/suilearn/api/service/AiProviderStatusService.java`
- `services/api/src/test/java/com/suilearn/api/service/SuiLearnV2ServiceTest.java`
- `services/api/src/test/java/com/suilearn/api/service/AiProviderStatusServiceTest.java`
- `services/api/src/test/java/com/suilearn/api/retrieval/KeywordRetrieverTest.java`
- `openspec/changes/enable-text-only-rag/**`

## 禁止修改文件

- `apps/android/**`
- `apps/web/**`
- `contracts/**`
- `docs/product-requirements.md`
- `docs/tech-selection.md`

## 验证命令

```powershell
mvn -f services/api/pom.xml test-compile -q
mvn -f services/api/pom.xml "-Dtest=AiProviderStatusServiceTest,OpenAiCompatibleAiProviderTest" test -q
mvn -f services/api/pom.xml "-Dtest=SuiLearnV2ServiceTest" test -q
```
