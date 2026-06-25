# 修复 DeepSeek OpenAI-compatible 配置

## 背景

当前 OpenAI-compatible Provider 会把配置的 `base-url` 自动补成 `/v1`，并且聊天与 embedding 共用同一组 `base-url` 和 `api-key`。DeepSeek 官方 OpenAI-compatible 配置使用 `https://api.deepseek.com` 作为基础地址，聊天端点为 `/chat/completions`，且未提供 embedding 端点。

这会导致两类问题：

- DeepSeek 的聊天请求被错误地发送到 `/v1/chat/completions`。
- 即使聊天模型可用，资料导入和 RAG 检索仍会因为 embedding 请求发往 DeepSeek 而失败。

## 目标

- 让 OpenAI-compatible Provider 按用户配置的基础地址精确拼接端点，不再隐式追加 `/v1`。
- 支持聊天和 embedding 使用不同的基础地址与 API Key。
- 保留旧配置 `suilearn.ai.base-url` 与 `suilearn.ai.api-key` 的向后兼容。
- 更新 README，明确 DeepSeek 只能作为聊天模型时还需要单独配置 embedding Provider。

## 非目标

- 不新增非 embedding 的检索降级模式。
- 不修改 RAG 检索排序策略。
- 不改变对外 API 契约字段。

## 验收标准

- `https://api.deepseek.com` 会请求 `/chat/completions`，不会被自动改为 `/v1/chat/completions`。
- `https://api.openai.com/v1` 仍可请求 `/v1/chat/completions` 和 `/v1/embeddings`。
- 支持 `chat-base-url/chat-api-key` 与 `embedding-base-url/embedding-api-key` 分别配置。
- 后端状态接口不暴露任何 API Key 原文。
