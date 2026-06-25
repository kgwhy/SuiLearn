# 设计

## 当前行为

`RagService.ask` 和 `SuiLearnV2Workflow.ask` 会：

1. 校验 `knowledgeBaseId` 或 `materialId` 范围。
2. 调用 `Retriever.retrieveEvidence(..., 3)`。
3. 若有证据，返回固定回答文案和引用片段。

## 新行为

在步骤 3 中改为：

1. 将用户问题、知识库范围、资料范围和检索到的 `SourceRef` 传入 `AiProvider.answerQuestion`。
2. Provider 返回 `GeneratedAnswer(answer, uncertain)`。
3. `RagAnswer` 使用 Provider 生成的 `answer` 和 `uncertain`，并继续返回原 citations/evidenceChunks。

## Prompt 约束

OpenAI-compatible Provider 使用 JSON 输出：

- 输入包括 `task=answer_question`、`question`、`knowledgeBaseId`、`materialId`、`sourceRefs`。
- 输出字段为 `answer` 和 `uncertain`。
- 要求模型只使用 sourceRefs/excerpts 中的事实，不得补充无来源信息。
- 证据不足时 `uncertain=true`，回答以“不确定：”开头并说明缺少依据。
- 回答中应以 `[1]`、`[2]` 形式标注引用序号。

## API 和数据影响

- 不改变 `AskQuestionRequest`。
- 不改变 `RagAnswer` 字段。
- 不改变持久化结构。

## 风险

- Provider 故障会像其他 AI 生成能力一样向上抛出错误；本变更不静默降级为固定文案。
- 生成质量依赖模型遵循 JSON 和引用约束，测试使用 deterministic provider 覆盖服务编排，不覆盖真实模型质量。
