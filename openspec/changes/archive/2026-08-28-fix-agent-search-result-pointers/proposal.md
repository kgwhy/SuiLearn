# Proposal: 让 search_knowledge 返回模型可用的证据指针内容

## 背景

修复 read_evidence 后，用户仍遇到 `BUDGET_EXHAUSTED`。verbose 日志显示模型只反复调用 `search_knowledge`，从不调用 `read_evidence`。

根因：AgentLoop 只把 `ToolResult.content` 发回模型，而 `search_knowledge` 的 content 只有 `Found N evidence pointer(s).`；真正可用的 `stableId/sourceRef` 在 metadata 里，模型看不到，因此无法调用 `read_evidence`。

## 变更内容

- `SearchKnowledgeTool` 的 content 直接列出每个指针的 `stableId`、`sourceRef`、`relevance` 和 `excerpt`。
- 更新 `AgentDeclarativeToolsTest`，断言 content 包含可读取指针。
- 不改动工具契约、metadata 或检索逻辑。

## 验收标准

- 模型在调用 search_knowledge 后能从 tool content 中获得 `stableId/sourceRef`，并能调用 read_evidence。
