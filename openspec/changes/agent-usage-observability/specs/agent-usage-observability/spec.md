## ADDED Requirements

### Requirement: UsageTracker 必须按价格表折算成本
系统 MUST 对 prompt/completion tokens 按模型价格表折算 USD，缺价格表使用默认价；累计 MUST 可查询。

#### Scenario: 成本
- **WHEN** 模型价格 2/4 USD per million，使用 500 prompt / 250 completion
- **THEN** cost=0.002
