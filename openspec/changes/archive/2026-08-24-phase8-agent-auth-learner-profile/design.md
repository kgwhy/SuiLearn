# Phase 8 设计

## Decisions

- **Bearer static token registry**：`suilearn.auth.tokens` JSON 绑定 `[{token,learnerId}]`，内存索引 token，不落库。
- **Security 默认关闭**：`suilearn.auth.enabled=false` 时 `permitAll` 且不装 Bearer filter，保持 single-user 兼容。
- **learner 权威来自 principal**：开启后请求 body/命令里的 learnerId 被 principal 覆盖；查询类命令用 `requireTurn(turnId, learnerId)` 校验归属。
- **WS 认证在手写握手拦截器完成**：读取 `Authorization` header 或 `access_token` query，设置 `learnerId` session attribute；WS handler 每个命令校验并回 sanitized error。
- **Profile 持久化**：`learner_profile(learner_id PK, persona text, skills_json text)`；skills 为有界字符串列表，长度/数量 fail-fast。
- **Prompt 注入**：`ContextBuilder` 增加 profile 参数，`PromptBlockAssembler` 只对非空 profile 插入 `persona`/`skills` blocks；空 profile 保持旧顺序。

## Data flow

```text
REST Bearer -> OncePerRequestFilter -> SecurityContext(learnerId)
  -> AgentTurnController(principal learnerId)
  -> TurnRuntimeService.requireTurn(turnId, learnerId)

WS handshake -> LearnerTokenHandshakeInterceptor -> session.learnerId
  -> AgentTurnWebSocketHandler -> runtime scoped methods

AgentLoop -> LearnerProfileService.get(learnerId)
  -> ContextBuilder.build(..., profile)
  -> PromptBlockAssembler(persona + skills blocks)
```
