# 验证

## 当前状态

- 阶段：Spec
- 本轮验证结论：已创建方案文档，未进入 Build。

## 本轮验证

本轮只新增 OpenSpec 中文方案文档，没有修改业务代码、测试代码、契约或当前事实文档，因此模块测试不适用。

已执行或需执行的检查：

- `git diff 3b8aababf1e49294a32a41eb8ed1780632364ad5 --stat`
- 文件范围核对：仅应包含 `openspec/changes/upgrade-text-only-rag-pipeline/**` 作为本轮新增文件；工作区既有未提交变更不属于本轮。

## Build 阶段必跑验证

后端：

```powershell
mvn -f services/api/pom.xml test -q
```

工作流：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/check-suilearn-workflow.ps1 -BaseRef 3b8aababf1e49294a32a41eb8ed1780632364ad5 -ClosingChange upgrade-text-only-rag-pipeline
```

## 功能验收用例

### Chunk

- 标题不会单独成为孤立 chunk。
- 短段落会合并到相邻正文。
- 长段落按语义边界拆分，并保留 overlap。
- 代码块和表格不会被无意义截断。

### Retrieval

- 中文 query 可以命中中文资料。
- 英文 query 可以命中英文资料。
- `knowledgeBaseId` 和 `materialId` scope 严格生效。
- 删除资料不参与检索。
- PostgreSQL FTS 可用时不走全量 Java 扫描主路径。

### Context

- Top evidence 使用完整 chunk content。
- 命中 chunk 自动扩展相邻 chunk。
- 超预算时仍保留最高分核心 chunk。

### Grounding

- 回答中的引用编号必须存在。
- `uncertain=false` 时至少有一个 citation。
- 每个 statement 至少绑定一个 citation。
- 证据不足时返回不确定回答。

## 未验证项

- 真实大规模资料库性能。
- 不同中文分词策略的召回差异。
- 客户端 statement-level citation 展示体验。
