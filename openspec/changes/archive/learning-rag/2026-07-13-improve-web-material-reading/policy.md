# 策略记录

## 基本信息

- Change: `improve-web-material-reading`
- 级别: Normal，已扩展为 Web + Server Backend 修复
- base_ref: `5ec2fafe308afc71032ffa8284ff9c4c4abea3ad`
- 当前阶段: Verify，知识点提取来源可观测性和已配置 chat AI 失败策略修复已完成
- 执行模式: serial
- 审批依据: 用户在 2026-07-01 明确要求优化导入资料后的内容查看和知识点可读性。
- 文件锁: 未发现 `.agents/locks` 目录，未发现可见锁冲突。

## 角色与范围

- Leader Agent: 负责变更载体、门禁、验证和收口。
- Web Frontend Agent: 负责 Web UI、状态流、Web 测试和构建验证。
- Server Backend Agent: 负责知识点提取在 AI 未配置时的服务端 fallback 和后端单元测试。

## 允许修改文件

- `openspec/changes/improve-web-material-reading/proposal.md`
- `openspec/changes/improve-web-material-reading/design.md`
- `openspec/changes/improve-web-material-reading/tasks.md`
- `openspec/changes/improve-web-material-reading/policy.md`
- `apps/web/package.json`
- `apps/web/src/App.tsx`
- `apps/web/src/styles.css`
- `apps/web/src/types.ts`
- `apps/web/src/api.contract.test.mjs`
- `apps/web/src/workbench-ui.test.mjs`
- `services/api/src/main/java/com/suilearn/api/knowledgepoint/application/KnowledgePointService.java`
- `services/api/src/main/java/com/suilearn/api/service/internal/SuiLearnV2Workflow.java`
- `services/api/src/main/java/com/suilearn/api/service/SuiLearnV2Service.java`
- `services/api/src/test/java/com/suilearn/api/knowledgepoint/application/KnowledgePointServiceTest.java`
- `services/api/src/test/java/com/suilearn/api/service/SuiLearnV2ServiceTest.java`

## 禁止修改文件

- `contracts/**`
- `apps/android/**`
- `docs/product-requirements.md`
- `docs/architecture.md`
- `docs/tech-selection.md`
- `docs/proposals/**`
- `docs/superpowers/**`

## 基线测试

- `npm --prefix apps/web test`: 通过，3 个测试全部通过。
- `mvn -f services/api/pom.xml "-Dtest=KnowledgePointCandidateExtractorTest,SuiLearnV2ServiceTest#extractKnowledgePointsFiltersSeparatorsDuplicatesAndSentenceFragments" test -q`: 未通过，原因是本地 PostgreSQL 缺少 `suilearn_test` 数据库，Spring 集成测试无法建立连接；非本次代码回归。

## Review 策略

- 由于当前工具策略不允许在用户未显式要求时派发子 Agent，本次由主 Agent 串行执行并在完成前做 reviewer-style 自审。
- 若后续范围扩大到后端、契约或 Android，立即回到 Spec 并升级任务拆分。
- 2026-07-06 用户反馈导入资料提取知识点时片段均显示 `model 未返回` / `dim 未返回`；本次限定为 Web 对后端 `TEXT_ONLY` embedding 状态的展示修复，不改契约和后端。
- 2026-07-06 用户要求区分 AI 提取和本地 fallback，并要求已配置 chat API 时 AI 失败不再静默降级；本次限定在已允许的知识点服务、Web 展示和测试范围内。
- 2026-07-06 发现 legacy `SuiLearnV2Workflow` 仍保留不一致的本地 fallback 路径；允许同步修复 legacy workflow 与测试，避免不同服务入口行为分叉。
