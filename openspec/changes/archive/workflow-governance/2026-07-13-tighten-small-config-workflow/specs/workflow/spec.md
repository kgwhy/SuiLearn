## ADDED Requirements

### Requirement: 配置与运行态变更门禁

SuiLearn 工作流必须（SHALL）为配置、启动和集成类变更增加额外验收与验证门禁，覆盖端口、CORS、Docker/Compose、反向代理、环境变量、CI wrapper、数据库 URL 和外部服务端点等。

#### Scenario: 必须提供验收矩阵

- 当变更修改配置、启动或集成行为时
- 变更产物必须包含验收矩阵，覆盖默认行为、支持的运行组合，以及是否允许或禁止手动配置

#### Scenario: 必须提供运行态证据

- 当配置、启动或集成类变更进入 Verify 时
- 静态验证必须补充运行态边界证据，例如 HTTP 请求、预检请求、容器环境检查、服务健康检查，或等价的手工验证记录

### Requirement: 同一问题链路使用单一 active change

SuiLearn 工作流必须（SHALL）把同一用户问题链路的后续修复保留在同一个 active change home 中，除非后续工作明确属于独立的产品、架构、契约或平台问题。

#### Scenario: 同链路后续问题

- 当后续问题与某个 active change 共享同一根因链路时
- 任务和验证必须追加到该 active change，而不是创建或填充无关 change

#### Scenario: 相关但独立的变更

- 当另一个 active change 只需要主配置变更的运行态证据时
- 它应链接到主 change，而不是复制配置验证细节

### Requirement: 返工先回到 Spec

当第一次返工暴露实现行为与用户预期语义不一致时，SuiLearn 工作流必须（SHALL）先回到 Spec。

#### Scenario: 用户纠正语义

- 当用户指出当前实现不是预期效果时
- Agent 必须先更新 proposal、design、tasks 或验收标准，再继续实现

#### Scenario: 验证暴露默认行为缺失

- 当验证显示默认路径仍需要手动配置，且这与验收标准冲突时
- Agent 必须先更新 Spec 产物，然后再修改实现
