# 变更策略：fix-knowledge-point-generation

## 状态与批准

- 变更等级：Normal。
- 状态：在用户批准本变更的 proposal、design、delta specs、tasks 与 policy 前保持 Spec；只有记录批准后才进入 Build。
- 基线提交：`d2926b7533765c88290d8ac729e820cb75224b49`。
- 必需审查循环：Server Backend Implementer → Test → Web Implementer → Test → Spec Review → Code Review → 如有需要由所属 Agent 修复。

## 允许范围

| Owner | 允许路径 |
| --- | --- |
| Server Backend Agent | `services/api/src/main/java/com/suilearn/api/ai/AiProvider.java`、`services/api/src/main/java/com/suilearn/api/ai/OpenAiCompatibleAiProvider.java`、`services/api/src/main/java/com/suilearn/api/knowledgepoint/application/KnowledgePointService.java`、接口兼容所需的 `services/api/src/main/java/com/suilearn/api/runtimefixture/RuntimeFixtureAiProvider.java`、匹配的 `services/api/src/test/**` |
| Web Frontend Agent | `apps/web/src/App.tsx`、确有必要时的 `apps/web/src/api.ts`、`apps/web/src/knowledgePointExtractionTask.ts`、`apps/web/src/knowledgePointExtractionTask.test.mjs`、匹配的 `apps/web/src/**/*.test.*`、`apps/web/package.json`（仅更新现有测试文件清单） |
| Test Agent | 匹配的测试文件和验证证据 |
| Reviewer Agent | 仅限本变更目录中的审查证据与发现 |
| Leader Agent | 本变更产物和协调证据 |

## 禁止范围

- `apps/android/**`
- `contracts/**`
- 数据库实体和迁移
- `docs/product-requirements.md`、`docs/architecture.md`、`docs/tech-selection.md`
- 配置默认值、Compose、凭据、Provider 端点/模型选择，以及原始模型响应持久化
- 新增、启用或扩展 runtime fixture 能力
- 任何补造内容或引用的 fallback

## 验收矩阵

| 场景 | 预期结果 |
| --- | --- |
| 有效结构化模型输出 | 以白名单引用持久化 DRAFT 知识点。 |
| 受支持别名或 fenced JSON | 无损归一化后校验；不发起修复请求。 |
| 首次结构无效输出 | 恰好一次语义修复请求；不持久化部分结果。 |
| 修复输出仍无效 | 持久化任务为 FAILED，包含安全稳定错误；无原始模型输出。 |
| Provider/传输故障 | 保留原有重试/失败路由；无语义 fallback。 |
| Web 手动提取 | HTTP 202 任务提交；任务卡展示成功/失败，而非同步 500。 |

## 验证与残留扫描

- 业务代码编辑前的基线：`mvn -f services/api/pom.xml test -q`、`npm --prefix apps/web test`、`npm --prefix apps/web run build`。
- 定向 RED/GREEN 命令由 `tasks.md` 定义；最终验证必须独立重新运行全部命令。
- 扫描修改过的代码/测试：主要 Web 调用 `extractKnowledgePoints(materialId)`、`runManagedTask`、关键词 fallback、占位描述、原始模型响应/error body 日志、来源正文日志、对象键、凭据以及第二条语义修复路径。
- 本变更消费已有持久化端点且不修改依赖默认值，不需要 runtime fixture。环境可用时，运行态检查应提交一份资料并观察 202 任务后进入 SUCCEEDED 或安全的 FAILED 状态。

## 完成条件

- 不得存在未完成或无 Owner 的任务复选框。
- 已记录 `openspec validate fix-knowledge-point-generation --strict`、后端测试、Web 测试/构建、diff 检查、范围检查和独立 Test/Reviewer 证据。
- 完成前已审阅 `git diff d2926b7533765c88290d8ac729e820cb75224b49 --stat`。
