# 任务

## 状态

- [x] Owner: Server Backend Agent，更新 OpenAI-compatible 配置模型与 Provider 请求地址。
- [x] Owner: Server Backend Agent，补充后端测试覆盖 DeepSeek 无 `/v1` 与分离配置。
- [x] Owner: Leader Agent，更新 README 的 DeepSeek 配置说明。
- [x] Owner: Leader Agent，运行验证命令并记录结果。

## 允许修改文件

- `services/api/src/main/java/com/suilearn/api/config/SuiLearnAiProperties.java`
- `services/api/src/main/java/com/suilearn/api/config/AppConfig.java`
- `services/api/src/main/java/com/suilearn/api/ai/OpenAiCompatibleAiProvider.java`
- `services/api/src/main/java/com/suilearn/api/retrieval/OpenAiCompatibleEmbeddingProvider.java`
- `services/api/src/main/java/com/suilearn/api/service/AiProviderStatusService.java`
- `services/api/config/local.properties`
- `services/api/src/main/resources/application.properties`
- `services/api/config/local.properties.example`
- `services/api/src/test/java/com/suilearn/api/ai/OpenAiCompatibleAiProviderTest.java`
- `services/api/src/test/java/com/suilearn/api/service/AiProviderStatusServiceTest.java`
- `README.md`
- `openspec/changes/fix-deepseek-openai-compatible-config/**`

## 禁止修改文件

- `apps/android/**`
- `apps/web/**`
- `contracts/**`
- `docs/product-requirements.md`
- `docs/tech-selection.md`

## 验证命令

```powershell
mvn -f services/api/pom.xml "-Dtest=AiProviderStatusServiceTest,OpenAiCompatibleAiProviderTest" test -q
```
