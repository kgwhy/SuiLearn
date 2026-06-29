# 归档记录

Status: passed.

## 变更名称

`persist-chinese-rag-search-index`（等级 Major）

## 状态

- Status: passed.
- 已实现并验证。落地上一变更延期的持久化 `search_text`/`tsvector`/reindex，并消除中文 FTS 失效、BM25 O(N²) 与全表 `findAll()` 三个检索层问题。

## 最终实现引用

- base_ref：`eccf04be3e399914cfb53da428380b6771d1009d`。
- 实现位于 working tree（尚未提交，等待用户决定提交时机）。
- 新增源文件：
  - `services/api/src/main/java/com/suilearn/api/retrieval/TextSearchTokenizer.java`
  - `services/api/src/main/java/com/suilearn/api/persistence/PostgresChunkSearchIndexMigration.java`
- 修改源文件：
  - `services/api/src/main/java/com/suilearn/api/retrieval/KeywordRetriever.java`（索引候选、`CorpusStats` 预计算、删除死代码与全表扫描）
  - `services/api/src/main/java/com/suilearn/api/persistence/SuiLearnV2Store.java`（写入 `search_text`、`listChunksByScope`、query→tsquery）
  - `services/api/src/main/java/com/suilearn/api/persistence/repository/MaterialChunkJpaRepository.java`（`search_tsv` 索引查询、`findByScope`）
  - `services/api/src/main/java/com/suilearn/api/persistence/entity/MaterialChunkEntity.java`（`search_text` 字段）
  - `services/api/src/main/java/com/suilearn/api/persistence/PostgresLargeObjectTextMigration.java`（`@Order(10)`）
- 新增/修改测试：`TextSearchTokenizerTest`、`PostgresChunkSearchIndexMigrationTest`、`KeywordRetrieverTest`、`SuiLearnV2ServiceTest`、`SuiLearnV2StoreTransactionBoundaryTest`。

## 验证摘要

- `mvn -f services/api/pom.xml test -q`：通过，EXIT=0，61 个测试 0 失败 0 错误 0 跳过（含 PostgreSQL `@SpringBootTest`）。
- pg16 生成列 + GIN + n-gram tsquery 直验：`INDEX-MATCH-OK`。
- 详见 `verification.md`。

## 同步到当前事实文档

- 架构事实：已更新 `docs/architecture.md` 检索边界规则——text-only 候选走持久化 `search_tsv` + GIN 索引、不以全表扫描为主路径；索引收窄仅用于 text-only，语义模式全量打分；BM25 按候选集预计算。
- 技术事实：已更新 `docs/tech-selection.md`——新增 text-only 检索索引选型（应用层 n-gram + `to_tsvector('simple')` + GIN，零扩展）与运行时 `ApplicationRunner` 迁移；pg_jieba 列为后续可选升级。
- 产品事实：不受影响（检索质量与性能增强，未改变产品范围）。
- 契约：不受影响（无对外字段变更，`search_text` 仅服务端持久化）。

## 延期项

延期项：pg_jieba 真正中文分词升级；真实大规模资料库召回延迟与回填耗时验证。

- pg_jieba/zhparser 真正中文分词：需自定义 Docker 镜像与部署变更，列为后续可选升级；schema（`search_text`/`search_tsv`/GIN）与本批一致，届时仅替换 tokenizer 并重跑 reindex。Owner：Server Backend Agent。
- 真实大规模资料库的索引召回延迟与回填耗时验证：需生产级数据量。

## 最终审查摘要

由子 Agent（Reviewer，sonnet）对本次 diff 做代码质量审查，结论无 P0，发现 1×P1、3×P2、2×P3，处置如下（全部已修复）：

- P0：无。
- P1（已修复）：BM25 等价缺对拍测试（design 明确要求）。已将 `CorpusStats` 提为包级顶层类并暴露 `bm25()`，新增 `CorpusStatsTest` 对教科书公式手算值断言（`isCloseTo(..., within(1e-9))`），含中文 bigram、未命中 term、空语料用例。
- P2-1（已修复）：`SuiLearnV2Store` 全限定类名 → 改为 `import com.suilearn.api.retrieval.TextSearchTokenizer`。
- P2-2（已修复）：迁移回填逐行 UPDATE → 改为 `jdbc.batchUpdate` 整批提交。
- P2-3（已修复）：迁移测试单薄 → 补「PostgreSQL 路径建生成列+GIN 后无行可回填即停」「SELECT 失败时不调用 batchUpdate 且不抛异常」用例。
- P3-1（已记录）：语义模式 `candidateChunks = scopedChunks` 对超大 KB 无候选上限——属设计有意决策（保语义召回），已在代码注释标注为已知瓶颈，留待后续。
- P3-2（已修复）：候选上限魔法数 `50` → 提为命名常量 `CANDIDATE_INDEX_LIMIT` 并加注释。
- 早前实现中自查发现并修复的语义模式回归（索引预过滤丢失语义召回，`ragEvidenceDiversifiesAcrossMaterials...`）亦已复测通过。
- 死代码 `containsAnyKeyword`、`isRetrievable`、全表 `findAll()` 主路径已清除。
