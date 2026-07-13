# 设计：持久化中文文本 RAG 检索索引

## 总体决策

在不引入向量、不引入需自定义镜像的 PG 扩展的前提下，建立**应用层 n-gram tokenization → 持久化 `search_text` → `to_tsvector('simple')` 生成列 + GIN 索引**的检索主路径，并把 BM25 排序改为基于候选集的单次预计算。检索召回从「全表 `findAll()` + 内存扫描」转为「索引查询 + 候选打分」。

## 运行环境约束（接地）

- 数据库镜像：`pgvector/pgvector:pg16`（`services/api/compose.local.yml:5`）。stock PG16 + pgvector，**不含 pg_jieba / zhparser**，无法 `CREATE EXTENSION` 直接安装。
- schema 管理：Hibernate `spring.jpa.hibernate.ddl-auto=update`（`application.properties:7`），**无 Flyway/Liquibase**；`services/api/src/main/resources/db/**` 不存在。
- 既有迁移模式：运行时 `ApplicationRunner` 组件执行原始 DDL，先例为 `PostgresLargeObjectTextMigration`（在 `ApplicationRunner.run` 中按表执行 `alter table`）。
- 结论：本变更的索引、生成列、回填必须由**新的 `ApplicationRunner` 迁移组件**完成；Hibernate 只负责把实体新增字段 `search_text` 生成为普通列。

## 分词方案决策

| 方案 | 中文召回质量 | 是否需扩展/换镜像 | 部署风险 | 本变更采用 |
|---|---|---|---|---|
| A. 应用层 n-gram + `to_tsvector('simple')` + GIN | 中（bigram 召回，偏召回率） | 否，当前镜像直接可跑 | 低 | **是** |
| B. pg_jieba / zhparser + GIN | 高（真正分词） | 是，需自定义镜像并重建部署 | 高 | 否（列为后续可选升级） |
| C. pg_trgm 三元组索引 | 中低，且对 CJK 短查询噪声大 | pg_trgm 为 contrib，未必随镜像启用 | 中 | 否 |

采用方案 A 的理由：当前镜像零改动即可落地「真正的索引召回」，消除全表扫描这一最严重问题；n-gram bigram 对中文召回足够支撑第一阶段，且与现有 `KeywordRetriever.keywords()` 已生成的「Han 单字 + 相邻双字 + 拉丁词元」完全一致，可直接复用。pg_jieba 召回更准但属基础设施级变更，记为 `design.md`「后续演进」与非目标，须用户在 Approval Gate 单独授权。

## 目标检索链路

```text
chunk 写入
  -> TextSearchTokenizer.searchText(content)   // 中文 n-gram + 英文词元，空格连接
  -> 持久化 material_chunks.search_text
  -> 生成列 search_tsv = to_tsvector('simple', search_text)
  -> GIN(search_tsv)

RagService / SearchService
  -> QueryNormalizer（复用归一化）
  -> TextSearchTokenizer.tsquery(query)        // 同一 tokenizer，OR 语义
  -> store.searchChunksText（GIN 索引召回 + ts_rank_cd 排序，scope/删除过滤在 SQL）
  -> KeywordRetriever 候选打分（BM25 预计算 + 语义可选 + 邻接扩展）
```

## Tokenizer 设计

新增 `TextSearchTokenizer`（`retrieval` 包），抽取自现有 `KeywordRetriever.keywords()`：

- `List<String> tokens(String text)`：归一化（小写、trim）后按 code point 扫描：
  - HAN：输出单字，并与前一个 HAN 字组成 bigram。
  - 字母/数字：累积成词元 flush。
  - 其他：分隔。
- `String searchText(String content)`：`String.join(" ", tokens(content))`，写入 `search_text` 列。
- `String tsquery(String query)`：对 `tokens(query)` 去重、转义后用 ` | ` 连接，供 `to_tsquery('simple', :q)`；OR 语义保召回，最终排序交给 BM25/`ts_rank_cd`。token 仅含小写字母数字与单个 CJK 字符，转义后不含 tsquery 操作符，避免注入与语法错误。

`KeywordRetriever.keywords()` 改为委托 `TextSearchTokenizer`，保证「写入分词」「查询分词」「BM25 分词」三处口径一致。

## 数据库设计

`material_chunks` 新增：

```text
search_text   text                       -- 应用层写入；实体字段，Hibernate 建列
search_tsv    tsvector GENERATED ALWAYS AS (to_tsvector('simple', coalesce(search_text, ''))) STORED
                                          -- 迁移组件创建，Hibernate 不管理
索引：CREATE INDEX IF NOT EXISTS idx_material_chunks_search_tsv
        ON material_chunks USING gin(search_tsv)
```

实体只声明 `search_text`（`@Column(columnDefinition = "text")`）；`search_tsv` 与 GIN 索引由迁移组件用原始 DDL 创建，并标注 `IF NOT EXISTS` 保证幂等。

## 迁移与 reindex 设计

新增 `PostgresChunkSearchIndexMigration implements ApplicationRunner`（`persistence` 包），仅在 PostgreSQL 上运行（复用 `isPostgres` 判断）：

1. `alter table material_chunks add column if not exists search_tsv ... generated ...`（若不存在）。
2. `create index if not exists idx_material_chunks_search_tsv ...`。
3. 回填：分批 `select id, content from material_chunks where search_text is null`，用 `TextSearchTokenizer.searchText` 计算后 `update ... set search_text = ?`；生成列与索引自动随之更新。
4. 全程幂等：已回填的行（`search_text is not null`）跳过；重启可重复执行。

排序与时序：`ApplicationRunner` 在 Hibernate schema update 之后运行，确保 `search_text` 列已存在。回填失败按批记录日志，不阻塞应用启动；未回填的旧 chunk 在召回时自然走 fallback。

## 写入路径

chunk 持久化处（`SuiLearnV2Store` / `MaterialChunkStore` 的 save）在落库前用 `TextSearchTokenizer.searchText(content)` 填充 `search_text`，使新导入资料即时具备索引，无需依赖迁移回填。

## 检索改造

### 候选召回（替代全表扫描）

`store.searchChunksText` 的 native query 改为基于生成列与 GIN：

```sql
select c.* from material_chunks c
join learning_materials m on m.id = c.material_id
where (:knowledgeBaseId is null or m.knowledge_base_id = :knowledgeBaseId)
  and (:materialId is null or c.material_id = :materialId)
  and m.status <> 'DELETED'
  and c.embedding_status in ('READY', 'TEXT_ONLY')
  and c.search_tsv @@ to_tsquery('simple', :tsquery)
order by ts_rank_cd(c.search_tsv, to_tsquery('simple', :tsquery)) desc
limit :limit
```

`KeywordRetriever.candidateChunks`：用 tokenizer 生成 `tsquery` 传入；命中则候选 = 索引结果，不再回退全量 `scopedChunks`。仅当查询为空或 DB 调用异常时才回退 fallback。

**语义模式例外**：当 `embeddingProvider.supportsEmbeddings()` 为真时，候选 = 全部 scope 内 chunk，不做索引收窄。原因：索引召回仅基于关键词，会丢弃「语义相关但无关键词重叠」的 chunk，破坏语义召回。索引收窄只应用于 text-only 主路径（本变更消除全表扫描的目标场景）；语义模式沿用既有对 scope 候选的全量打分，行为不变。

### scope 限定查询（替代 findAll）

新增 `listChunksByScope(knowledgeBaseId, materialId)` 仓库方法（SQL 内按 KB/material/status 过滤），替代 `retrievableChunks` 中的 `store.listChunks()` 全表加载。邻接扩展所需的同资料 chunk 也由 scope 查询提供。

### BM25 预计算（消除 O(N²)）

引入 `CorpusStats`（每次查询构建一次）：

```text
CorpusStats.of(candidateCorpus):
  docTermFreqs: Map<chunkId, Map<term,int>>   // 每个候选分词一次
  docLengths:   Map<chunkId, int>
  avgDocLength: double
  docFreq:      Map<term, int>                 // term -> 含该 term 的文档数
```

`bm25Score(chunk, terms, stats)` 直接查表，不再对每个候选重新遍历语料。语料范围取本次候选集（索引召回结果，规模有界），而非全库。打分数学与现实现等价（同 k1=1.5、b=0.75、IDF 公式），用单测对拍保证等价。

## Fallback 与测试隔离

- native query 与生成列在 H2/非 PG 环境不可用：保留现有 `try/catch → 返回空 → Java BM25 fallback` 行为，fallback 仍用 `TextSearchTokenizer` 和 `CorpusStats`，保留 scope 与删除过滤。
- 迁移组件非 PG 直接跳过。

## 兼容性

- 不改 `RagAnswer`、`AiProvider` 输入输出、citation/grounding 逻辑与对外契约字段。
- `MaterialChunk` 模型新增可选 `searchText` 仅用于持久化与检索，不进入 API 响应。
- 旧客户端不受影响。

## 替代方案

- **维持现状只修 BM25**：成本低，但中文仍全表扫描，未解决最严重问题。
- **pg_jieba/zhparser**：召回最准，但需自定义镜像与部署变更，超出当前阶段，列为后续可选升级。
- **改 `simple` 为 n-gram 但不持久化**（查询时实时 `to_tsvector`）：仍是顺序扫描、无法用 GIN，未解决性能问题。

## 后续演进（非本变更）

- 引入 pg_jieba/zhparser 自定义镜像，将 `search_text` 改为真正分词，提升中文召回精度。
- 引入 embedding 混合召回。
- `search_text` 增加标题路径加权、字段加权（`setweight`）。

## 风险

- n-gram bigram 召回偏向召回率，可能引入低相关候选：由 BM25/`ts_rank_cd` 排序与 `MIN_RETRIEVAL_SCORE` 阈值控制。
- 生成列与 GIN 增加写入与存储成本：chunk 写入量级可控，可接受。
- 迁移回填大表耗时：分批执行、幂等、失败不阻塞启动。
- ddl-auto=update 与原始 DDL 协同：生成列与索引用 `IF NOT EXISTS` 保证幂等，避免与 Hibernate 冲突。
