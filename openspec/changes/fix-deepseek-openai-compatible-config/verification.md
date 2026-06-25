# 验证

状态：已通过

## 已运行命令

```powershell
mvn -f services/api/pom.xml "-Dtest=AiProviderStatusServiceTest,OpenAiCompatibleAiProviderTest" test -q
```

结果：通过，命令无输出，退出码 0。

```powershell
mvn -f services/api/pom.xml test-compile -q
```

结果：通过，命令无输出，退出码 0。

```powershell
mvn -f services/api/pom.xml test -q
```

结果：未通过。失败原因是本机 PostgreSQL 测试库不可用，启动 `SuiLearnV2ServiceTest` 时返回：

```text
FATAL: database "suilearn_test" does not exist
```

该失败发生在 Spring Boot 测试上下文获取 JDBC 连接阶段，早于本次 OpenAI-compatible 配置测试逻辑。
