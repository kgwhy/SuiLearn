## ADDED Requirements

### Requirement: PipelineFactory 必须提供默认混合管道
系统 MUST 默认返回 `pgvector-hybrid`，并允许按名称选择；未知名称 MUST 失败。

#### Scenario: 默认管道
- **WHEN** 未指定 pipeline
- **THEN** 返回包装现有 KeywordRetriever 的 pipeline

### Requirement: EmbeddingSignature 必须稳定并区分配置
系统 MUST 由 binding/model/dim/baseUrl/apiVersion 计算稳定 SHA-256；任一字段变化签名不同。

#### Scenario: 换模型
- **WHEN** 模型名或维度改变
- **THEN** 签名不同且 IndexVersionManager 报告 needs_reindex

### Requirement: index_versions 必须版本化且 ready 才可读
系统 MUST 持久化 kb_id/signature/version_no/storage_ref/ready；查询签名匹配且 ready 的最新版本。

#### Scenario: 旧索引未 ready
- **WHEN** 新签名版本尚未 ready
- **THEN** 继续返回旧 ready 版本并提示 needs_reindex

### Requirement: ParseEngine 注册表必须按媒体类型路由
系统 MUST 注册 text/markdown 引擎并返回 ParsedDocument IR；未知类型返回显式不支持。

#### Scenario: text 解析
- **WHEN** 输入 text/markdown
- **THEN** 返回 normalized text 与媒体类型
