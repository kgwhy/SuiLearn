# Policy: read_evidence 容错增强与 embedding dimensions 回填

## 变更信息

- 变更：`fix-agent-evidence-fallback-embedding-dims`
- 状态：`Status: Approved`
- 批准者：用户（2026-08-28 反馈 Evidence reading failed 与 Embedding Dimensions=0）
- 等级：Standard
- Owner：Server Backend Agent
- base_ref：`b26e942`
- Build 循环：L2

## 允许的文件

- `openspec/changes/fix-agent-evidence-fallback-embedding-dims/**`
- `services/api/src/main/java/com/suilearn/api/agent/tool/RetrievalEvidenceTools.java`
- `services/api/src/test/java/com/suilearn/api/agent/tool/RetrievalEvidenceToolsTest.java`
- `services/api/src/main/java/com/suilearn/api/retrieval/OpenAiCompatibleEmbeddingProvider.java`
- `.agents/notes/implemented/architecture/2026-08-28-agent-evidence-fallback-embedding-dims.md`

## 禁止的文件

- `apps/**`、`contracts/**`、`docs/**`、CLI。

## 验证计划

- 静态检查 `git diff --check`
- 本环境无 Java/Maven；待用户本地执行：
  ```bash
  mvn -f services/api/pom.xml -Dtest=RetrievalEvidenceToolsTest test
  mvn -f services/api/pom.xml test -q
  ```
