# 任务

## 1. 改进 RAG 知识检索

- 状态：已完成
- Owner：Server Backend Agent
- 允许文件：`services/api/src/main/java/com/suilearn/api/retrieval/KeywordRetriever.java`、`services/api/src/test/java/com/suilearn/api/service/SuiLearnV2ServiceTest.java`
- 禁止文件：`apps/**`、`contracts/**`、`docs/proposals/**`、`docs/superpowers/**`
- 验证：`mvn -f services/api/pom.xml test -q`

### 范围

- 保持现有 API、数据模型和存储结构不变。
- 将现有检索调整为更稳定的混合检索：关键词召回、向量相似度、词项覆盖度、短片段轻量加权、同资料去重。
- 补充回归测试，覆盖语义召回排序和 RAG 证据去重。

### 验证记录

- `mvn -f services/api/pom.xml test -DskipTests -q 2>&1`：通过，编译和测试编译成功。
- `mvn -f services/api/pom.xml test -q 2>&1`：未通过，原因是本地 PostgreSQL 测试库不可用，Spring Test 启动时连接 `localhost:5432` 被拒绝。
