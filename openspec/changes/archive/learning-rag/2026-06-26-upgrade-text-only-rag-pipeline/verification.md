# 验证

Status: passed.

## 当前状态

- 阶段：Verify
- 状态：已通过。
- Status: passed.
- 本轮验证结论：核心 text-only RAG 已实现，完整后端测试已通过。

## 本轮验证

本轮已修改后端 RAG 业务代码和测试。

已执行或需执行的检查：

- `docker compose -f services/api/compose.local.yml up -d postgres`：通过，`suilearn-postgres` 已运行。
- `docker exec suilearn-postgres createdb -U suilearn suilearn_test`：通过，测试库已创建。
- `mvn -f services/api/pom.xml test-compile -q`：通过。
- `mvn -f services/api/pom.xml "-Dtest=DefaultMaterialChunkerTest,KeywordRetrieverTest,CitationValidatorTest,OpenAiCompatibleAiProviderTest" test -q`：通过，存在 Mockito dynamic agent warning。
- `mvn -f services/api/pom.xml "-Dtest=SuiLearnV2ServiceTest" test -q`：通过，存在 Mockito dynamic agent warning。
- `mvn -f services/api/pom.xml test -q`：通过，存在 Mockito dynamic agent warning。
- `mvn -f services/api/pom.xml "-Dtest=KeywordRetrieverTest" test -q`：通过，覆盖 embedding endpoint 返回 404/异常时自动降级 text-only 检索。
- `mvn -f services/api/pom.xml test -q`：通过，确认运行时降级修复后完整后端测试仍通过。
- `git diff 3b8aababf1e49294a32a41eb8ed1780632364ad5 --stat`：完成前执行。

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
