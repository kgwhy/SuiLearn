# 策略

## 等级

Normal

原因：本变更影响后端配置解析和运行时 Provider 行为，涉及多个后端文件与 README，但不改变外部 API 契约和存储模型。

## 角色

- 协调：Leader Agent
- 实现：Server Backend Agent
- 文档：Leader Agent 按用户要求更新 README

## base_ref

`3b8aababf1e49294a32a41eb8ed1780632364ad5`

## 文件锁

串行修改以下路径：

- `services/api/src/main/java/com/suilearn/api/config/**`
- `services/api/src/main/java/com/suilearn/api/ai/OpenAiCompatibleAiProvider.java`
- `services/api/src/main/java/com/suilearn/api/retrieval/OpenAiCompatibleEmbeddingProvider.java`
- `services/api/src/main/java/com/suilearn/api/service/AiProviderStatusService.java`
- `services/api/config/local.properties`
- `services/api/src/main/resources/application.properties`
- `services/api/config/local.properties.example`
- `services/api/src/test/java/com/suilearn/api/ai/OpenAiCompatibleAiProviderTest.java`
- `services/api/src/test/java/com/suilearn/api/service/AiProviderStatusServiceTest.java`
- `README.md`

## 基线测试

已运行：

```powershell
mvn -f services/api/pom.xml "-Dtest=AiProviderStatusServiceTest,OpenAiCompatibleAiProviderTest" test -q
```

结果：通过，命令无输出，退出码 0。
