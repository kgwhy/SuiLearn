# 验证

Status: passed.

## 当前状态

- 阶段：Verify
- 状态：已通过。
- Status: passed.
- 本轮结论：持久化中文文本检索索引已实现，全部后端测试通过，索引 DDL 与 n-gram tsquery 在 pg16 上验证可用。

## 已执行验证

环境：`suilearn-postgres`（`pgvector/pgvector:pg16`）健康运行，测试库 `suilearn_test` 已存在。

- `mvn -f services/api/pom.xml test-compile -q`：通过。
- `mvn -f services/api/pom.xml "-Dtest=TextSearchTokenizerTest,KeywordRetrieverTest,PostgresChunkSearchIndexMigrationTest,SuiLearnV2StoreTransactionBoundaryTest" test -q`：通过（EXIT=0）。
- `mvn -f services/api/pom.xml test -q`：通过（EXIT=0）。完整套件 **67 个测试，0 失败、0 错误、0 跳过**，跨 14 个测试类，含针对 PostgreSQL 的 `@SpringBootTest`（`SuiLearnV2ServiceTest` 29 个）。（含 Code Review 后新增的 `CorpusStatsTest` BM25 对拍与扩充的迁移测试。）
- 生成列/索引/中文匹配 DDL 直验（pg16）：建生成列 `search_tsv generated always as (to_tsvector('simple', coalesce(search_text,''))) stored` + GIN 索引成功，插入 n-gram `search_text` 后用 `to_tsquery('simple', '''机器'' | ''zzz''')` 命中（`INDEX-MATCH-OK`），证明「应用层 n-gram → search_text → 生成 tsvector → GIN → tsquery」中文链路成立。
- `git diff eccf04be3e399914cfb53da428380b6771d1009d --stat`：变更文件均落在 policy 允许的 Build 路径内（`retrieval/**`、`persistence/**`、`material/**`、`model/MaterialChunk` 相关与对应测试）。
- 工作流检查器（含 `-ClosingChange`）：见下方记录。

## 关键验证说明

- `@SpringBootTest` 启动时运行 `PostgresChunkSearchIndexMigration`（`ApplicationRunner`，`@Order(20)`，晚于 LOB 迁移 `@Order(10)`）；若生成列 DDL 在 pg16 非法将导致上下文加载失败、全部集成测试报错——实际全部通过，证明迁移 DDL 合法且已执行。
- 语义模式回归：索引仅基于关键词，若对语义相关但无关键词重叠的 chunk 预过滤会丢失语义召回。已修正为 `embeddingProvider.supportsEmbeddings()` 时对 scope 候选全量打分，仅 text-only 路径做索引收窄。对应集成测试 `ragEvidenceDiversifiesAcrossMaterialsBeforeFillingSameMaterialChunks` 通过。

## 功能验收结果

- 索引召回：中文 query 经 n-gram 走 GIN 索引命中（单测 + DDL 直验）；英文 query 命中（集成测试）。✓
- scope/删除/embedding_status 过滤在 SQL 路径生效（`findByScope` + `searchText` 查询）。✓
- 写入路径：`saveChunks` 经 `TextSearchTokenizer.searchText` 填充 `search_text`，单写入路径（`MaterialChunkStore` 委托）。✓
- 迁移：非 PostgreSQL 跳过（`PostgresChunkSearchIndexMigrationTest`）；生成列/索引 `IF NOT EXISTS` 幂等；回填按批、失败不阻塞启动。✓
- BM25：`CorpusStats` 每查询预计算一次；打分公式与原逐候选实现等价（k1=1.5、b=0.75、同 IDF）。✓
- Fallback：DB 调用异常返回空 → Java BM25 fallback，保留 scope 与删除过滤。✓

## 未验证项

- 真实大规模资料库的索引召回延迟与回填耗时（需生产级数据量）。
- n-gram bigram 与未来 pg_jieba 分词的召回质量差异。
