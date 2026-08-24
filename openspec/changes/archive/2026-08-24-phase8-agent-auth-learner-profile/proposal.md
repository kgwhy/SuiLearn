# Phase 8：Agent 鉴权、learner 隔离与技能/人物 Prompt

## Why

Phase 8 是改造计划最后一个可选阶段。当前 `learnerId` 是调用方任意字符串，Agent turn/events/memory 没有身份边界；skills/persona 也没有持久化与注入路径。

## What Changes

- 引入 `spring-boot-starter-security`，新增 `suilearn.auth.enabled`（默认 false）与 token -> learnerId 绑定。
- 开启时保护 `/api/v2/agent/**` REST 与 `/api/v2/ws`；`LearnerTokenHandshakeInterceptor` 支持 header/query token。
- REST/WS 将 principal learnerId 作为权威 learner，所有 turn/session 资源做 learner 归属校验，跨 learner 返回 not found。
- 新增 `learner_profile` JPA 表、`AgentLearnerProfileController` GET/PUT 端点与 OpenAPI schema。
- `PromptBlockAssembler` 在 profile 存在时增加 `persona`、`skills` PromptBlock；AgentLoop 经 ContextBuilder 注入。

## Non-Goals

- 不引入 JWT/OAuth2/账号注册；不做密码登录。
- 不把现有知识库工作台资源伪装成多租户；它们仍是 trusted single-user，后续单独 change。
- 不修改 Android/Web 客户端。

## Acceptance Criteria

- 鉴权关闭时 403 既有后端测试全绿。
- 鉴权开启时无 token 401、错 token 403、跨 learner turn 不可见。
- profile CRUD 与 prompt block 注入有定向测试。
- OpenAPI 与全量后端回归通过。
