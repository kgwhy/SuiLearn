# 设计

## 配置模型

`SuiLearnAiProperties` 保留旧字段：

- `baseUrl`
- `apiKey`

新增可选字段：

- `chatBaseUrl`
- `chatApiKey`
- `embeddingBaseUrl`
- `embeddingApiKey`

有效配置按以下优先级解析：

- 聊天基础地址：`chatBaseUrl` 优先，否则使用 `baseUrl`。
- 聊天 API Key：`chatApiKey` 优先，否则使用 `apiKey`。
- Embedding 基础地址：`embeddingBaseUrl` 优先，否则使用 `baseUrl`。
- Embedding API Key：`embeddingApiKey` 优先，否则使用 `apiKey`。

这样旧配置无需修改即可继续使用；DeepSeek 场景可以只把聊天配置指向 DeepSeek，把 embedding 配置指向支持 `/embeddings` 的 OpenAI-compatible 服务。

## 请求地址

Provider 只负责去掉末尾多余 `/`，不再自动追加 `/v1`。是否带 `/v1` 由配置决定。

示例：

- DeepSeek 聊天：`https://api.deepseek.com` + `/chat/completions`
- OpenAI 聊天：`https://api.openai.com/v1` + `/chat/completions`
- OpenAI Embedding：`https://api.openai.com/v1` + `/embeddings`

## 状态接口

现有状态契约保持不变。`baseUrl` 返回有效聊天基础地址，`apiKeyEnvName` 保留旧环境变量名并在 README 中说明可使用分离环境变量。

## 风险

- 配置错误时失败点会从隐式 `/v1` 变为用户配置的实际地址，需要 README 明确说明。
- DeepSeek 仅配置聊天模型仍不能完成资料导入，因为当前 RAG 需要 embedding；状态检查会要求聊天和 embedding 都完整配置。
