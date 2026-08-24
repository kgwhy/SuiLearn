# Agent Note: Phase 8 先保护 Agent 运行时与 learner 资源，遗留工作台不冒充多租户
Status: implemented

## Problem

Agent learnerId 可被任意调用方指定，turn/events/memory 没有身份边界；缺少 skills/persona prompt 注入。

## Decision

- Spring Security Bearer token 认证，`suilearn.auth.enabled` 默认 false，开启后保护 `/api/v2/agent/**` 与 WS。
- token -> learnerId 由配置绑定；REST/WS 所有 learner 资源按 principal 校验，不存在的他人资源返回 not found。
- 新增 `learner_profile` 表与 GET/PUT profile API；PromptBlockAssembler 注入 persona/skills block。
- 现有知识库工作台资源仍为 trusted single-user，不在本次伪造多租户。

## Alternatives considered

- **JWT/OAuth2 全功能身份系统**：否决，当前无账号系统，先做有界 Bearer token。
- **全库所有表加 learner_id**：否决，会一次改动全部既有工作台契约与迁移，风险超出 Phase 8 边界。
- **profile 放 TurnContext metadata**：否决，无法持久化和跨 turn 管理。

## Consequences

- 定向 46 tests 与 Docker 全量 419 tests 全绿。
- 鉴权开启时 Agent 资源具备 learner 隔离；鉴权关闭时旧行为不变。
- 知识库工作台多租户与客户端鉴权适配仍为具名 follow-up。
