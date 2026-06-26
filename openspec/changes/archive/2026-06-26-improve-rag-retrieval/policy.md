# 策略

## 变更

- 名称：`improve-rag-retrieval`
- 等级：Tiny
- base_ref：`cc8b0c1c5172088229e37948fa2989f868f5a831`
- Worktree 模式：serial

## 允许路径

- `services/api/src/main/java/com/suilearn/api/retrieval/KeywordRetriever.java`
- `services/api/src/test/java/com/suilearn/api/service/SuiLearnV2ServiceTest.java`

## 禁止路径

- `apps/**`
- `contracts/**`
- `docs/proposals/**`
- `docs/superpowers/**`
- `docs/product-requirements.md`
- `docs/tech-selection.md`

## 基线

- `mvn -f services/api/pom.xml test -q 2>&1`
- 状态：unavailable
- 原因：本地 Spring Boot 测试启动时连接 `localhost:5432` 被拒绝，当前环境没有可用 PostgreSQL。
