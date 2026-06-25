# 验证

状态：已通过

## 已运行命令

```powershell
mvn -f services/api/pom.xml test-compile -q
```

结果：通过，命令无输出，退出码 0。

```powershell
mvn -f services/api/pom.xml "-Dtest=AiProviderStatusServiceTest,KeywordRetrieverTest,OpenAiCompatibleAiProviderTest" test -q
```

结果：通过，退出码 0。命令输出包含 Mockito / Byte Buddy 动态 agent 警告：

```text
Mockito is currently self-attaching to enable the inline-mock-maker.
WARNING: Dynamic loading of agents will be disallowed by default in a future release
```

```powershell
mvn -f services/api/pom.xml "-Dtest=SuiLearnV2ServiceTest" test -q
```

结果：未通过。失败原因是本机 PostgreSQL 测试库不可用，启动测试上下文时返回：

```text
FATAL: database "suilearn_test" does not exist
```

```powershell
mvn -f services/api/pom.xml test -q
```

结果：未通过。失败原因同上：

```text
FATAL: database "suilearn_test" does not exist
```
