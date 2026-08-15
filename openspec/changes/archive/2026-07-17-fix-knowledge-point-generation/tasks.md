## 1. 后端结构化响应恢复

- [x] 1.1 **Owner: Server Backend Agent — 编写可修复与终态无效输出的 RED 测试。** 允许文件：`services/api/src/test/java/com/suilearn/api/knowledgepoint/application/KnowledgePointServiceTest.java`、`services/api/src/test/java/com/suilearn/api/ai/OpenAiCompatibleAiProviderTest.java`。禁止：`apps/web/**`、`apps/android/**`、`contracts/**`、`docs/product-requirements.md`。新增聚焦测试，证明：受支持别名/fence 输出可被接受；首次无效结构化结果只触发一次修复请求；第二次无效结果持久化为带安全 `AI_STRUCTURED_OUTPUT_INVALID` 分类的 `FAILED`；不保存草稿，错误中不包含原始模型输出。先运行聚焦 Maven 测试，确认新测试在修改生产代码前失败。审查重点：测试真实校验/任务边界，而非仅验证 mock 调用次数。

- [x] 1.2 **Owner: Server Backend Agent — 实现无损归一化与一次语义修复。** 允许文件：`services/api/src/main/java/com/suilearn/api/ai/AiProvider.java`、`services/api/src/main/java/com/suilearn/api/ai/OpenAiCompatibleAiProvider.java`、`services/api/src/main/java/com/suilearn/api/knowledgepoint/application/KnowledgePointService.java`、`services/api/src/main/java/com/suilearn/api/runtimefixture/RuntimeFixtureAiProvider.java`，以及任务 1.1 测试和 `services/api/src/test/java/com/suilearn/api/service/SuiLearnV2ServiceTest.java` 中的既有测试替身。禁止：`apps/web/**`、`apps/android/**`、`contracts/**`、数据库实体/迁移、配置默认值。仅为接口兼容调整 runtime fixture/测试替身，不得新增或启用 fixture 能力。保持传输重试不变；只处理已记录别名/fence；使用安全校验类别重新生成一次；第二次无效后返回终态持久化任务失败。运行 `mvn -f services/api/pom.xml "-Dtest=KnowledgePointServiceTest,OpenAiCompatibleAiProviderTest" test -q`。审查重点：语义修复恰好一次且独立于 adapter retry，证据白名单仍为必需条件，非预期 Provider 故障继续原有路由。

- [x] 1.4 **Owner: Server Backend Agent — 修复第一轮审查的修复契约、引用白名单与重试边界。** 允许文件：任务 1.2 所列后端实现与测试文件。禁止范围同任务 1.2。补充 RED 测试并实现：修复提示明确 `{ "knowledgePoints": [...] }` 顶层对象；任一越界 citation（包括与有效 citation 混合）均触发修复；持久化的 citation 使用冻结证据中的规范引用；即使应用层 `maxRetries=1`，语义修复方法也至多调用一次，HTTP/IO 重试仅由适配器承担。运行 `mvn -f services/api/pom.xml "-Dtest=KnowledgePointServiceTest,OpenAiCompatibleAiProviderTest" test -q`。审查重点：不吞掉越界 citation、不重试第二次语义修复、修复提示不暴露原始响应。

- [x] 1.3 **Owner: Test Agent — 独立验证后端回归覆盖。** 不允许修改生产文件；测试修复必须退回 Server Backend Agent。禁止：`services/api/src/main/**`、`apps/web/**`、`contracts/**`。运行 `mvn -f services/api/pom.xml test -q`。审查重点：有效输出、归一化输出、修复成功、两次无效以及无 fallback/原始响应泄露。

## 2. Web 持久化任务流程

- [x] 2.1 **Owner: Web Frontend Agent — 为资料提取提交编写 RED 回归测试。** 允许文件：`apps/web/src/workbench-ui.test.mjs`、`apps/web/src/api.contract.test.mjs`，测试必需时可修改 `apps/web/src/App.tsx` 或 `apps/web/src/api.ts`。禁止：`services/api/**`、`apps/android/**`、`contracts/**`、`docs/product-requirements.md`。断言资料提取操作使用 `generateMaterialKnowledgePoints`、保留返回任务供展示，且不再调用 `extractKnowledgePoints`。运行 `npm --prefix apps/web test` 并确认新断言在修改应用代码前失败。审查重点：测试可观察的任务提交/状态行为，而非只匹配实现字符串。

- [x] 2.2 **Owner: Web Frontend Agent — 将提取操作切换为持久化任务 API。** 允许文件：`apps/web/src/App.tsx`、确有必要时的 `apps/web/src/api.ts`，以及任务 2.1 测试。禁止：`services/api/**`、`apps/android/**`、`contracts/**`、API 端点定义。提交现有异步请求，保存/查看返回任务，刷新资料和工作台状态，并保留任务卡错误展示。仅移除过时的主调用方；除非测试证明无人使用，否则不删除已弃用 API 客户端兼容方法。运行 `npm --prefix apps/web test` 和 `npm --prefix apps/web run build`。审查重点：HTTP 202 任务流、失败任务消息可见、无同步 500 路径。

- [x] 2.4 **Owner: Web Frontend Agent — 修复第一轮审查的知识点任务终态刷新。** 允许文件：`apps/web/src/App.tsx`、`apps/web/src/knowledgePointExtractionTask.ts`、`apps/web/src/knowledgePointExtractionTask.test.mjs`、`apps/web/src/workbench-ui.test.mjs`、`apps/web/package.json`（仅更新现有测试文件清单）。禁止：`services/api/**`、`apps/android/**`、`contracts/**`、新端点或依赖。为资料详情中的知识点任务提供现有任务状态刷新入口；将任务提交/终态编排抽为可执行模块，真实模拟 `QUEUED → FAILED/SUCCEEDED`；当任务成功时重新加载当前资料详情和知识点列表；当任务失败时保持并展示安全错误。运行 `npm --prefix apps/web test` 与 `npm --prefix apps/web run build`。审查重点：可观察状态流，而非只检查源码字符串。

- [x] 2.3 **Owner: Test Agent — 独立验证 Web 回归覆盖。** 不允许修改生产文件；测试修复必须退回 Web Frontend Agent。禁止：`services/api/**`、`apps/android/**`、`contracts/**`。运行 `npm --prefix apps/web test` 与 `npm --prefix apps/web run build`。审查重点：手动提取创建任务、成功后刷新结果、失败保持在任务范围内。

## 3. 变更验证与审查闭环

- [x] 3.1 **Owner: Leader Agent — 运行验收与范围检查。** 允许文件：仅可在 `openspec/changes/fix-knowledge-point-generation/**` 记录证据/任务状态。禁止：应用代码、契约、Android、当前事实文档。运行 `openspec validate fix-knowledge-point-generation --strict`、`mvn -f services/api/pom.xml test -q`、`npm --prefix apps/web test`、`npm --prefix apps/web run build`、`git -c safe.directory=D:/SuiLearn/.worktrees/fix-knowledge-point-generation diff d2926b7533765c88290d8ac729e820cb75224b49 --stat` 与 `git -c safe.directory=D:/SuiLearn/.worktrees/fix-knowledge-point-generation diff --check`。审查重点：无原始模型输出、无未记录端点/schema 变更、全部改动文件均在批准角色范围内。

- [x] 3.2 **Owner: Reviewer Agent — 先完成独立规格审查，再完成代码审查。** 允许文件：仅在本变更目录记录审查发现/证据；生产修复必须退回所属 Agent。禁止：全部生产代码与契约。对照 proposal/design/specs/tasks/policy 与任务 1.3、2.3 的独立测试证据，记录 reviewer-style P0/P1/P2 结论。审查重点：用户批准的有限重试语义、失败安全性、Web 移除同步弃用路径、文件边界合规。
