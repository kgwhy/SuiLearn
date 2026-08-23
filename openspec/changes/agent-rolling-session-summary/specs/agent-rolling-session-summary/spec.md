## ADDED Requirements

### Requirement: 会话摘要必须持久化并水位推进
系统 MUST 在 session_summary 表保存摘要与 summary_up_to_message_id；只有摘要生成成功后才推进水位。

#### Scenario: 摘要幂等
- **WHEN** 连续两次 ensure 且无新消息
- **THEN** 第二次不调用 LLM，返回同一摘要

### Requirement: 反漂移必须从原文重建
当未摘要原始消息数不超过半窗口时，系统 MUST 忽略旧摘要并从这些原文重建。

#### Scenario: 短会话重建
- **WHEN** 已有摘要且总消息数 ≤ halfWindow
- **THEN** LLM 收到全部原文消息，水位更新到最后消息

### Requirement: 摘要失败不得阻塞回合
摘要 LLM 不可用时，系统 MUST 返回旧摘要或空摘要，并继续 AgentLoop。

#### Scenario: 摘要失败
- **WHEN** LlmClient 抛错
- **THEN** ensure 不抛异常，返回已有摘要
