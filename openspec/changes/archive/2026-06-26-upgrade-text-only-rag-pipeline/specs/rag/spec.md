# RAG 规格

## 新增需求：Text-only RAG Pipeline

SuiLearn 后端必须提供不依赖 embedding 的 RAG Pipeline。该 Pipeline 必须以结构化 chunk、text-only 检索、上下文扩展、证据打包、带引用回答和回答校验为核心能力。

### 场景：导入资料生成语义 chunk

- Given 用户导入一份包含标题、段落、列表和代码块的资料
- When 系统完成资料解析和 chunk
- Then 系统生成的 chunk 应接近 `400-600 tokens`
- And 相邻 chunk 应保留约 `80 tokens` overlap
- And 标题应作为 `headingPath` 元数据进入相关 chunk
- And 标题本身不应成为孤立无正文 chunk

### 场景：无 embedding 配置下完成资料可检索

- Given 当前 AI 配置不支持 embedding
- When 用户导入资料
- Then 系统仍应保存可检索 chunk
- And chunk 应进入 text-only index
- And 资料状态应可达到 `READY`

### 场景：按知识库范围检索

- Given 知识库 A 和知识库 B 都存在资料
- When 用户在知识库 A 中提问
- Then 检索候选只应来自知识库 A
- And 删除资料不应进入候选

### 场景：按资料范围检索

- Given 用户指定 `materialId`
- When 系统执行 RAG 检索
- Then 检索候选只应来自指定资料
- And 相邻扩展也不得越过该资料范围

### 场景：命中 chunk 自动扩展上下文

- Given 检索最高分 chunk 为资料中的第 N 个 chunk
- When 系统构造 evidence
- Then 系统应尝试携带第 N-1 和第 N+1 个 chunk
- And 最终上下文不得超过 context budget

### 场景：LLM 收到完整 evidence content

- Given 系统已选出 evidence group
- When 调用 AI Provider 生成回答
- Then payload 应包含完整 evidence content
- And `SourceRef.excerpt` 只能作为引用摘要，不得作为唯一上下文

### 场景：回答必须包含合法引用

- Given LLM 返回 `uncertain=false`
- When 系统校验回答
- Then 回答必须至少包含一个合法 citation
- And 每个 statement 必须至少包含一个合法 citation
- And citation 编号必须存在于本次 evidence 列表

### 场景：证据不足时返回不确定回答

- Given 检索不到 evidence
- Or LLM 输出无法通过 citation validation
- When 系统返回 RAG answer
- Then `uncertain` 必须为 `true`
- And answer 应说明资料中没有足够证据

## 新增需求：Text-only Retrieval

系统必须提供不依赖向量的检索主路径。

### 场景：优先使用 PostgreSQL FTS

- Given PostgreSQL FTS 能力可用
- When 系统执行 text-only retrieval
- Then 系统应使用数据库索引召回候选
- And 不应以全量 chunk Java 扫描作为主路径

### 场景：fallback 可用于测试和降级

- Given PostgreSQL FTS 不可用或测试环境需要隔离数据库特性
- When 系统执行 text-only retrieval
- Then 系统可使用 Java BM25 fallback
- But fallback 必须保留 scope 过滤和删除资料过滤

## 新增需求：兼容现有 RAG 返回

系统应保持现有 RAG API 的基本兼容性。

### 场景：旧客户端读取 answer

- Given 客户端只读取 `answer`、`uncertain`、`citations` 和 `evidenceChunks`
- When 后端返回新版 RAG answer
- Then 旧客户端仍可正常显示回答和引用列表

### 场景：新客户端读取 statement citations

- Given 客户端支持 statement-level citation
- When 后端返回新版 RAG answer
- Then 客户端可以读取每个 statement 的 citations
- And 可以将每个结论定位到对应 evidence
