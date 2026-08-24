# Phase 8 鉴权 / learner 隔离 / 技能 Prompt 策略

Status: Approved
批准者: 用户
批准日期: 2026-08-24
批准依据: 用户指令“实现吧”。

- Change: `phase8-agent-auth-learner-profile`
- 级别: Major
- base_ref: `d6c7a3b`
- 执行模式: L3（单人执行）
- 决策记录: `.agents/notes/proposed/architecture/2026-08-24-phase8-agent-auth-learner-profile.md`

## 允许修改文件

- `openspec/changes/phase8-agent-auth-learner-profile/**`
- `.agents/notes/**`
- `services/api/pom.xml`
- `services/api/src/main/resources/application.properties`
- `services/api/config/local.properties.example`
- `.env.example`
- `contracts/openapi/suilearn-v2.yaml`
- `services/api/src/main/java/com/suilearn/api/agent/config/**`
- `services/api/src/main/java/com/suilearn/api/agent/controller/**`
- `services/api/src/main/java/com/suilearn/api/agent/learner/**`
- `services/api/src/main/java/com/suilearn/api/agent/runtime/**`
- `services/api/src/main/java/com/suilearn/api/agent/context/**`
- `services/api/src/main/java/com/suilearn/api/agent/loop/**`
- `services/api/src/main/java/com/suilearn/api/agent/infrastructure/**`
- `services/api/src/main/java/com/suilearn/api/security/**`
- `services/api/src/test/java/com/suilearn/api/agent/**`
- `services/api/src/test/java/com/suilearn/api/security/**`
- `services/api/src/test/java/com/suilearn/api/contract/**`
- `services/api/src/test/java/com/suilearn/api/controller/**`
- `services/api/src/test/java/com/suilearn/api/rag/**`
- 最终事实同步：`docs/product-requirements.md`、`docs/architecture.md`、`docs/tech-selection.md`

## 禁止修改文件

- `apps/**`
- 除上述外的其他后端业务模块与测试
- `docs/proposals/**`

## 验收矩阵

| 场景 | 默认值/覆盖语义 | 必需验证 |
|---|---|---|
| auth 关闭 | `suilearn.auth.enabled=false`，全部旧行为不变 | 既有 403 回归 |
| auth 开启 | `/api/v2/agent/**` 无 Bearer 401，错 token 403 | 安全测试 |
| learner 隔离 | turn/events/cancel/reply/active-turn 只能本人访问，跨 learner 返回 not found | TurnRuntimeService/Controller 测试 |
| WS 鉴权 | header 或 query token；缺失/错误发 sanitized error | WS 测试 |
| profile | GET/PUT 本人 profile，跨 learner 404 | Profile 测试 |
| prompt 注入 | persona/skills 成为独立 PromptBlock，空 profile 不改变既有 block 顺序 | PromptBlockAssembler 测试 |
| 全量回归 | Docker + Testcontainers 0 failures/errors | 完整 `mvn test` |

## 历史提交路径覆盖（非本 change 修改）

- `contracts/**`
- `services/api/**`
- `docs/architecture.md`
- `docs/product-requirements.md`
- `docs/tech-selection.md`
- `.agents/**`
- `openspec/**`
