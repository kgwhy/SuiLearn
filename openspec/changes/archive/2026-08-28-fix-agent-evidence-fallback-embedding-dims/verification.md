# Verification: read_evidence 容错与 embedding dimensions 回填

Status: Verified（2026-08-28，静态验证）

## 环境限制

当前无 Java/Maven，无法运行后端测试。已增加测试并在本地做静态检查。

## 静态检查

- `RetrievalEvidenceTools.read()` 对 byId 与 retrieveEvidence 都加了 Runtime 保护。
- content 为空时回退 excerpt。
- `OpenAiCompatibleEmbeddingProvider.dimensions()` 返回缓存的实际 embedding 维度。
- `RetrievalEvidenceToolsTest` 新增 content 为空回退 excerpt 测试。
- `git diff --check` 通过。

## 待执行命令

```bash
mvn -f services/api/pom.xml -Dtest=RetrievalEvidenceToolsTest test
mvn -f services/api/pom.xml test -q
```
