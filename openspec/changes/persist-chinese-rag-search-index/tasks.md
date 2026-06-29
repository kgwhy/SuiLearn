# 任务

## 0. 变更分级

- 等级：Major
- 主 Owner：Leader Agent
- 架构 Owner：Architect Agent
- 后端 Owner：Server Backend Agent
- 测试 Owner：Test Agent
- 审查 Owner：Reviewer Agent
- Build 循环：L3
- base_ref：`eccf04be3e399914cfb53da428380b6771d1009d`

## 1. 架构与方案确认

- 状态：已完成
- Owner：Architect Agent
- 允许文件：
  - `openspec/changes/persist-chinese-rag-search-index/**`
  - `docs/architecture.md`
  - `docs/tech-selection.md`
- 禁止文件：
  - `apps/**`
  - `services/api/**`
  - `docs/proposals/**`
  - `docs/superpowers/**`
- 任务：
  - 确认分词方案 A（应用层 n-gram + `to_tsvector('simple')` + GIN）为本批方案，pg_jieba 记为后续可选升级。
  - 确认迁移走 `ApplicationRunner` 模式，不引入 Flyway/Liquibase。
  - 确认不改对外契约字段。
- 验证：
  - 审查 `proposal.md`、`design.md`、`specs/rag/spec.md` 一致性。

## 2. 实现统一 Tokenizer

- 状态：已完成
- Owner：Server Backend Agent
- 允许文件：
  - `services/api/src/main/java/com/suilearn/api/retrieval/**`
  - `services/api/src/test/java/com/suilearn/api/retrieval/**`
- 禁止文件：
  - `apps/**`
  - `docs/proposals/**`
  - `docs/superpowers/**`
- 任务：
  - 新增 `TextSearchTokenizer`，抽取自现有 `KeywordRetriever.keywords()`（HAN 单字 + 相邻 bigram + 拉丁词元）。
  - 提供 `tokens`、`searchText`、`tsquery` 方法；`tsquery` 转义后用 ` | ` 连接（OR 语义）。
  - `KeywordRetriever.keywords()` 改为委托该 tokenizer，保证写入/查询/BM25 口径一致。
- 验证：
  - 中文 bigram 与英文词元拆分正确。
  - `tsquery` 不含 tsquery 操作符注入，去重正确。
  - 委托后 `KeywordRetrieverTest` 仍通过。

## 3. 新增 search_text 字段与写入路径

- 状态：已完成
- Owner：Server Backend Agent
- 允许文件：
  - `services/api/src/main/java/com/suilearn/api/persistence/**`
  - `services/api/src/main/java/com/suilearn/api/material/**`
  - `services/api/src/main/java/com/suilearn/api/model/MaterialChunk.java`
  - `services/api/src/test/java/com/suilearn/api/persistence/**`
  - `services/api/src/test/java/com/suilearn/api/material/**`
- 禁止文件：
  - `apps/**`
  - `docs/proposals/**`
  - `docs/superpowers/**`
- 任务：
  - `MaterialChunkEntity` 新增 `search_text`（`@Column(columnDefinition = "text")`）。
  - chunk 落库前用 `TextSearchTokenizer.searchText(content)` 填充 `search_text`。
  - `MaterialChunk` 模型按需携带 `searchText`（不进入 API 响应）。
- 验证：
  - 新写入 chunk 的 `search_text` 非空且与 tokenizer 输出一致。
  - 现有 chunk 持久化测试不回归。

## 4. 实现迁移与 reindex

- 状态：已完成
- Owner：Server Backend Agent
- 允许文件：
  - `services/api/src/main/java/com/suilearn/api/persistence/**`
  - `services/api/src/test/java/com/suilearn/api/persistence/**`
- 禁止文件：
  - `apps/**`
  - `docs/proposals/**`
  - `docs/superpowers/**`
- 任务：
  - 新增 `PostgresChunkSearchIndexMigration implements ApplicationRunner`，仅在 PostgreSQL 运行。
  - 创建 `search_tsv` 生成列与 `idx_material_chunks_search_tsv` GIN 索引（`IF NOT EXISTS`，幂等）。
  - 分批回填 `search_text is null` 的旧 chunk；失败按批记录日志、不阻塞启动。
- 验证：
  - 非 PostgreSQL 跳过。
  - 重复执行不产生重复列/索引。
  - 回填后旧 chunk 可被索引召回。

## 5. 检索改造：索引召回 + scope 查询 + BM25 预计算

- 状态：已完成
- Owner：Server Backend Agent
- 允许文件：
  - `services/api/src/main/java/com/suilearn/api/retrieval/**`
  - `services/api/src/main/java/com/suilearn/api/persistence/**`
  - `services/api/src/test/java/com/suilearn/api/retrieval/**`
  - `services/api/src/test/java/com/suilearn/api/persistence/**`
- 禁止文件：
  - `apps/**`
  - `docs/proposals/**`
  - `docs/superpowers/**`
- 任务：
  - `MaterialChunkJpaRepository.searchText` 改为基于 `search_tsv @@ to_tsquery('simple', :tsquery)` + `ts_rank_cd` 排序，scope/删除/embedding_status 过滤在 SQL。
  - `KeywordRetriever.candidateChunks` 用 tokenizer 生成 tsquery；命中即用索引候选，不再回退全量 `scopedChunks`（仅空查询/异常时 fallback）。
  - 新增 `listChunksByScope`，替代 `retrievableChunks` 的 `store.listChunks()` 全表加载与 N+1。
  - 引入 `CorpusStats`，BM25 每查询预计算一次；`bm25Score` 查表。
  - 删除死代码 `containsAnyKeyword`；传入 FTS 的 query 与 BM25 归一化口径统一。
- 验证：
  - 中文/英文 query 走索引命中。
  - scope 与删除过滤生效。
  - BM25 预计算与逐候选打分对拍等价。
  - DB 异常时 fallback 可用且保留 scope。

## 6. API 客户端适配

- 状态：不适用
- Owner：Web Frontend Agent / Android Agent
- 任务：
  - 本变更不改对外契约字段，`search_text` 仅服务端持久化，不进入响应。
- 验证：
  - 现有问答与检索接口契约测试不回归。

## 7. 测试与审查

- 状态：已完成
- Owner：Test Agent / Reviewer Agent
- 验证命令：
  - `mvn -f services/api/pom.xml test -q`
  - `powershell -ExecutionPolicy Bypass -File scripts/check-suilearn-workflow.ps1 -BaseRef eccf04be3e399914cfb53da428380b6771d1009d -ClosingChange persist-chinese-rag-search-index`
- 审查：
  - Spec Review：确认 proposal、design、spec、tasks 一致。
  - Code Review：确认未重新引入全表扫描主路径、未引入 tsquery 注入、迁移幂等、BM25 等价、fallback 保留 scope、无死代码残留。
