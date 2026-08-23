## ADDED Requirements

### Requirement: PromptBlock 必须稳定分块
系统 MUST 用 PromptBlockAssembler 按 general/policy/capability/memory/tools/skills 顺序组装 system prompt；相同输入的字节与估算 token MUST 稳定。

#### Scenario: 同一 capability 两次组装
- **WHEN** 使用相同 manifest 与 memory 输入
- **THEN** 输出文本、估算 token 与块顺序一致

### Requirement: ContextBuilder 必须守卫会话窗口
系统 MUST 将历史预算限制为 contextMaxTokens×0.35，超限时优先裁剪旧 tool 消息，并 MUST 在保留历史前插入截断标记。

#### Scenario: 超窗
- **WHEN** 历史估算超过历史预算
- **THEN** 旧 tool 消息被裁剪，保留 user/assistant，截断标记存在且不进入 tool role

### Requirement: AgentLoop 必须发布真实请求预算
AgentLoop MUST 累计 LlmClient 返回的 prompt/completion tokens，并在 progress 事件中同时报告 estimatedContextTokens 与 actualPromptTokens。

#### Scenario: 预算报表
- **WHEN** fake LLM 返回 usage
- **THEN** progress/result metadata 的 actualPromptTokens 等于累计值，估算值来自 ContextBuilder

### Requirement: 4a 不新增存储表
本 change MUST NOT 新增数据库表或迁移；滚动摘要与 summary watermark 由 4b 实现。

#### Scenario: scope 检查
- **WHEN** 查看 diff
- **THEN** 无新 JPA entity/repository 表或 schema 变更
