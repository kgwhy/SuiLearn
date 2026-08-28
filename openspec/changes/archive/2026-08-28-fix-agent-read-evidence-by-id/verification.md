# Verification: 修复 Agent read_evidence 找不到可读证据

Status: Verified（2026-08-28，静态验证；Java 测试未能执行）

## 当前环境限制

本环境没有 `java` 且 Maven 无执行权限，因此无法运行 `mvn test`。已通过代码静态检查与逻辑审查确认改动路径。

## 静态检查

- `git diff --check`：通过。
- `RetrievalEvidenceTools` 优先 `MaterialChunkStore.find(chunkId)` 读取 material chunk，再回退 `retrieveEvidence`。
- `AgentInfrastructureConfiguration` 注入 `MaterialChunkStore`。
- 新增 `RetrievalEvidenceToolsTest`，覆盖 `search -> read` 按 chunkId 读取完整内容的场景。

## 待 Java 环境执行的验收命令

```bash
mvn -f services/api/pom.xml -Dtest=RetrievalEvidenceToolsTest test
mvn -f services/api/pom.xml test -q
```

## 范围

只修改 `services/api/**` 中 Agent evidence read 相关 3 个文件；未改 CLI/Web/Android/契约。
