# 归档记录

Status: archived

Owner: Leader

## 归档内容

- 实现引用：本 change 的 proposal、design、specs、tasks、policy、verification。
- 验证摘要：Phase 8 定向 46 tests 全绿；Docker 全量后端 419 tests 全绿。
- 当前事实同步：
  - `docs/product-requirements.md`：新增需求 8 与 `SPEC-AGENT-AUTH-LEARNER`。
  - `docs/architecture.md`：新增 Agent security、learner_profile、prompt 注入与隔离边界。
  - `docs/tech-selection.md`：新增 spring-boot-starter-security 与 auth 配置。
  - `contracts/openapi/suilearn-v2.yaml`：新增 profile 端点、schema、BearerAuth security scheme。
  - `contracts/schemas/suilearn-ws.yaml`：新增 `AGENT_AUTH_REQUIRED`。
- 决策记录：已迁移为 `.agents/notes/implemented/architecture/2026-08-24-phase8-agent-auth-learner-profile.md`。

Deferred items:
- JWT/OAuth2、账号注册与刷新令牌 -> 后续身份系统 change。
- 现有知识库工作台资源 learner_id 化 -> 具名 follow-up `knowledge-workbench-multitenancy`。
- Android/Web 客户端接入新鉴权 -> 用户此前延后 Android，Web 另行确认。
- 真实生产 token 轮换/撤销 -> 部署侧后续 change。

## 审查摘要

- review_mode: single-agent
- Spec Review 先行：Phase 8 范围与策略已对照计划与用户指令复核。
- Code Review：token 不落日志/响应、跨 learner 返回 not found、WS header/query 双通道、profile 有界校验、prompt 注入空 profile 不改变旧顺序。
- P0/P1: 0；P2: 0 未关闭（上述 deferred items 均具名）。
