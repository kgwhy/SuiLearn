# RAG 检索索引规格

## 新增需求：持久化中文文本检索索引

SuiLearn 后端必须为 chunk 建立持久化的 text-only 检索索引，使中文与英文 query 都能通过数据库索引召回候选，而不以全量 chunk Java 扫描作为主路径。

### 场景：chunk 写入时生成 search_text

- Given 用户导入资料并生成 chunk
- When 系统持久化 chunk
- Then 每个 chunk 应写入 `search_text` 字段
- And `search_text` 应由统一 tokenizer 从 `content` 生成（中文 n-gram + 英文词元）
- And 无需手动 reindex 即可被检索命中

### 场景：中文 query 通过索引召回

- Given 知识库中存在中文资料 chunk 且已建立检索索引
- When 用户用中文 query 提问
- Then 系统应通过 GIN 索引的 tsquery 召回候选
- And 不应退回全量 chunk Java 扫描作为主路径

### 场景：英文 query 通过索引召回

- Given 知识库中存在英文资料 chunk 且已建立检索索引
- When 用户用英文 query 提问
- Then 系统应通过索引召回相关候选

### 场景：scope 与删除过滤在索引路径生效

- Given 多个知识库与资料存在 chunk
- When 系统执行索引召回
- Then 候选应严格限定在请求的 `knowledgeBaseId` 与 `materialId` 范围内
- And 已删除资料的 chunk 不应进入候选
- And 仅 `embedding_status` 为 `READY` 或 `TEXT_ONLY` 的 chunk 参与召回

### 场景：数据库索引不可用时降级

- Given 当前数据库不支持该索引查询（如测试隔离或非 PostgreSQL）
- When 系统执行 text-only 检索
- Then 系统应降级到 Java BM25 fallback
- And fallback 必须保留 scope 过滤与删除资料过滤

## 新增需求：BM25 打分按候选集单次预计算

系统的 BM25 打分必须在每次查询时对候选集只计算一次语料统计，避免按候选重复分词。

### 场景：单次查询的语料统计只构建一次

- Given 一次查询的候选集包含 N 个 chunk
- When 系统对候选进行 BM25 打分
- Then 文档词频、文档长度、平均文档长度与 document frequency 应基于候选集预计算一次
- And 不应对每个候选重新遍历整个候选集分词

### 场景：预计算与原打分等价

- Given 同一查询与同一候选集
- When 分别用预计算 BM25 与逐候选 BM25 打分
- Then 两者对每个 chunk 的分数应一致（相同 k1、b 与 IDF 公式）

## 新增需求：存量 chunk 迁移与 reindex

系统必须为已存在的 chunk 回填 `search_text` 并建立检索索引。

### 场景：启动时回填存量 chunk

- Given 数据库中存在 `search_text` 为空的旧 chunk
- When 应用启动执行迁移
- Then 系统应为这些 chunk 计算并回填 `search_text`
- And 应创建 `search_tsv` 生成列与 GIN 索引（若不存在）
- And 迁移应幂等，重复执行不产生重复列或索引

### 场景：迁移仅在 PostgreSQL 运行

- Given 当前数据库不是 PostgreSQL
- When 应用启动执行迁移
- Then 迁移应跳过且不影响应用启动

### 场景：迁移失败不阻塞启动

- Given 回填过程中某批次失败
- When 迁移执行
- Then 应记录该批次错误并继续
- And 未回填的旧 chunk 在召回时走 fallback
