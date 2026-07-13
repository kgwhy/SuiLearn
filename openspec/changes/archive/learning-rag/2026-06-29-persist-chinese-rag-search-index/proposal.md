# 提案：持久化中文文本 RAG 检索索引

## 做什么

为 SuiLearn 的 text-only RAG 检索建立**真正生效的中文召回索引**，并消除当前检索主路径上的全表扫描与 O(N²) 打分。本变更落地上一变更 `upgrade-text-only-rag-pipeline` 归档时记录的延期项：持久化 `search_text`、`tsvector` 与 reindex。

核心动作：

- 新增 chunk 持久化字段 `search_text`：在 chunk 写入时由应用层中英文 tokenizer（中文 n-gram + 英文词元）生成。
- 基于 `search_text` 建立 PostgreSQL 生成列 `search_tsv`（`to_tsvector('simple', search_text)`）和 GIN 索引。
- 检索候选召回改为走 GIN 索引的 tsquery 查询，中文与英文都能真正命中；不再以全量 `findAll()` Java 扫描作为主路径。
- BM25 打分改为每次查询对候选集**预计算一次**语料统计（df / 平均长度 / 文档长度），消除按候选重复分词的 O(N²)。
- 新增运行时迁移组件，为存量 chunk 回填 `search_text` 并建立索引（reindex），沿用既有 `PostgresLargeObjectTextMigration` 的 `ApplicationRunner` 模式。
- 清理死代码 `containsAnyKeyword`，统一传入 FTS 的 query 归一化。

## 为什么做

当前实现已通过功能测试，但检索召回层存在三个测试抓不到、规模一大就暴露的真实问题：

- **中文 FTS 名存实亡**：`MaterialChunkJpaRepository.searchText` 用 `to_tsvector('simple', content)`。`simple` 分词器只按空格/标点切词，中文整段被当成一个 token，`plainto_tsquery` 几乎永远匹配不上 → 返回空 → `KeywordRetriever.candidateChunks` 退回 `scopedChunks` 全量候选。对一个以中文为主的学习产品，「不以全量 Java 扫描为主路径」的目标实际未达成。
- **BM25 O(N²)**：`combinedScore → textOnlyScore → bm25Score` 对每个候选都重新遍历整个语料计算 `averageDocumentLength` 和 `documentFrequency`，同一段内容被重复分词成千上万次。叠加上一条，单次问答近似语料规模的平方。
- **入口仍是全表 `findAll()`**：`retrievableChunks` 走 `store.listChunks()`（`chunks.findAll()`），把所有知识库的全部 chunk 拉进内存再按 scope 过滤，并对每个 chunk 单独 `findMaterial`（N+1）。

上一变更在 `archive.md` 中已把这些显式记为非阻塞延期项，Owner 为 Server Backend Agent。本变更即该具名 follow-up。

## 范围

本变更覆盖后端检索与持久化：

- chunk 写入路径生成并持久化 `search_text`。
- 新增 `search_tsv` 生成列与 GIN 索引（运行时迁移）。
- 存量 chunk 的 `search_text` 回填与索引建立（reindex）。
- `KeywordRetriever` 候选召回改用索引查询；BM25 改用预计算语料统计。
- 按 scope 限定的 chunk 查询替代全表 `findAll()`。
- 死代码清理与 query 归一化一致性。
- 对应后端测试与必要的迁移/检索单测。

## 非目标

- 不引入 pg_jieba / zhparser 等需自定义镜像的中文分词扩展（当前镜像 `pgvector/pgvector:pg16` 不含，作为后续可选基础设施升级，见 `design.md`）。
- 不引入 embedding、向量索引或外部 rerank。
- 不改动 RAG 回答、citation、grounding 校验逻辑与对外契约字段。
- 不改动移动端和 Web。

## 范围内的关键决策

中文分词采用**应用层 n-gram → `to_tsvector('simple')` + GIN**（零扩展、当前镜像可跑），而非 pg_jieba。该决策、权衡与 pg_jieba 备选见 `design.md`「分词方案决策」。若用户在 Approval Gate 要求 pg_jieba，需要额外授权自定义 Docker 镜像与部署变更，本变更范围据此调整。

## 成功标准

- 中文 query 通过 GIN 索引召回候选，不再退回全量 chunk Java 扫描。
- 英文 query 仍能命中。
- 单次问答的 BM25 打分对候选集只计算一次语料统计，分词次数与候选数线性相关，而非平方。
- 存量 chunk 经迁移后获得 `search_text` 与索引，可被新召回路径命中。
- 新导入资料在 chunk 写入时即生成 `search_text`，无需手动 reindex。
- scope（`knowledgeBaseId` / `materialId`）与删除资料过滤在新路径下严格生效。
- 数据库不可用或测试隔离时，仍可降级到 Java BM25 fallback 并保留 scope 过滤。
- 后端测试覆盖：中文召回命中、英文召回命中、迁移回填、scope 过滤、BM25 预计算等价性、fallback。

## 相关变更

- `upgrade-text-only-rag-pipeline`（已归档）：建立 text-only RAG 主路径，并显式延期 `search_text`/`tsvector`/reindex；本变更落地该延期项。
- `enable-text-only-rag`、`implement-grounded-rag-answer`、`improve-rag-retrieval`（已归档）：前置基础，本变更不修改其结论。
