# 验证

## 结果

状态：已通过。

## 基线

进入 Build 前的基线命令：

```powershell
mvn -f services/api/pom.xml test -q
```

此前在本地 PostgreSQL 不可用时记录为 `unavailable`，以 `mvn -f services/api/pom.xml test -DskipTests -q` 验证编译通过。

## 最终验证

命令：

```powershell
mvn -f services/api/pom.xml test -q
```

结果：通过，退出码 0（2026-06-26）。

环境：PostgreSQL 16.14，`localhost:5432` 可达。此前阻塞的 `SuiLearnV2ServiceTest`
（29 个测试）现已可启动并通过。完整后端套件共 53 个测试，0 失败、0 错误，覆盖
RAG grounded answer 编排：检索到证据时调用 AI Provider 生成 `answer` 并返回
`uncertain=false` 与 citations；无证据时返回 `uncertain=true` 与空证据。

相关测试类：

```text
OpenAiCompatibleAiProviderTest   Tests run: 5
CitationValidatorTest            Tests run: 3
KeywordRetrieverTest             Tests run: 3
SuiLearnV2ServiceTest            Tests run: 29
合计：53 个测试，0 失败、0 错误、0 跳过。
```

## 备注

- Maven 在 JDK 21 下输出 Mockito dynamic-agent 警告；测试仍通过。
