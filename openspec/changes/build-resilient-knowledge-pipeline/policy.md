# Policy：可靠的资料知识流水线

## 变更信息

- Change：`build-resilient-knowledge-pipeline`
- 状态：Spec，等待用户完成书面 Spec Review 和 Build Approval Gate
- 等级：Major
- 默认循环：L3（Implementer → Test → Spec Review → Code Review → Fix）
- base_ref：`ff08b45e58b50ae3cef15c6f96c8d8874dbce0b0`
- Worktree/锁模式：共享契约、配置、持久化与事实文档串行；契约稳定后 Backend 与 Web 仍按任务卡和文件锁顺序执行
- 业务基线：Spec 文档阶段不编辑业务代码，模块基线测试不适用；进入每个 Build 任务前按 `tasks.md` 执行该任务测试命令

## 单一 active change home

本用户问题链路唯一变更目录为：

`openspec/changes/build-resilient-knowledge-pipeline/**`

`improve-web-material-reading` 是本变更之前已完成任务的历史上下文，不是当前实现授权，不得在本链路中继续扩写或与本 change 并行承载需求。

## 角色归属

- Leader Agent：范围、顺序、文件锁、任务卡、验证和 Review 闭环。
- Leader Agent 在本变更中经用户明确中间件授权，额外拥有根 `compose.yml` 与 `.env.example` 的串行编排任务；不因此获得业务代码所有权。
- Product Agent：用户行为、验收标准和稳定产品事实同步。
- Architect Agent：技术依赖、模块/数据流、OpenAPI、架构/技术事实。
- Server Backend Agent：中间件接入、存储、解析/OCR、任务、知识点、题目和后端测试。
- Web Frontend Agent：上传、阅读、知识点和题目交互及 Web 测试。
- Android Agent：仅验证/适配远程契约兼容，不改变本地学习闭环产品范围。
- Test Agent：格式语料、故障注入、集成与运行态验收。
- Reviewer Agent：独立 Spec Review/Code Review；Implementer 不自证完成。

## 顺序与锁规则

1. `docs/architecture.md`、`docs/tech-selection.md` 和 OpenAPI 先由 Architect 串行稳定。
2. OpenAPI 未稳定前，Backend/Web/Android 不得按猜测修改消费模型。
3. 根 `compose.yml`、`.env.example` 由 Leader 串行编排；`services/api/pom.xml`、持久化模型和任务基础设施由 Backend 串行锁定，两个任务不得互相越界写入。
4. Parser/OCR、知识点、题目模块只在底座与契约完成后执行；同一实现文件不得并行写。
5. Web 只在契约与对应 Backend 行为稳定后适配。
6. Test 与 Reviewer 不与 Implementer 共用完成声明；发现产品/架构/契约歧义立即返回 Spec。
7. 当前 `.agents/locks` 不存在；进入 Build 时 Leader 必须重新检查并记录真实锁状态。

## 允许路径

具体任务只能使用 `tasks.md` 中自己的 Allowed files。整个 Major change 的最大边界为：

- `openspec/changes/build-resilient-knowledge-pipeline/**`
- `docs/product-requirements.md`
- `docs/architecture.md`
- `docs/tech-selection.md`
- `contracts/openapi/suilearn-v2.yaml`
- `services/api/**`
- `apps/web/**`
- `apps/android/**`（仅任务 6.4 明确的远程契约兼容测试/必要适配）
- `compose.yml`
- `.env.example`
- `scripts/**`（仅 Leader 另行声明的验证脚本）

## 禁止路径与行为

- `docs/proposals/**`
- `docs/superpowers/**`
- 其他 `openspec/changes/**` 或 `openspec/changes/archive/**`
- `AGENTS.md`、`agents/**`、`.agents/skills/**`、`docs/development-workflow.md`
- 未经扩展声明修改 Android 本地题库、Room、刷题 UI、答题/错题/收藏/统计逻辑
- 新建独立微服务/Worker 项目、引入 Redis、账号、多租户、云同步或知识库市场
- 用同步请求线程、`@Async` 内存队列、关键词 fallback 或虚假 READY 绕过已批准设计
- 在日志、API、任务表、文档示例中写入密钥、完整资料正文、原始模型响应或永久 MinIO 凭据

## 配置与集成门禁

进入 Build 前必须保持以下已批准语义：

- RabbitMQ + PostgreSQL Outbox 提供持久化异步任务，MinIO 保存原件/衍生资产。
- 异步处理默认开启；显式关闭时禁用新文件上传，不回退同步路径。
- OCR 默认开启且只处理文本不足页面；默认最大文件 50 MB、PDF 500 页、处理并发 2、OCR 并发 1、最大尝试 3。
- AI 未配置/失败时资料仍可阅读，但不得生成关键词/占位知识点。
- 环境变量提供覆盖口；`.env.example` 只含非敏感默认值。
- `verification.md` 的格式、依赖故障、恢复、重复投递、删除清理和运行态矩阵必须执行或记录真实阻塞。
- 完成前执行 design 的 Residual Scan，不得以单元测试代替 Compose 运行态证据。

## Build Approval Gate

当前未授权业务代码实现。只有用户审阅本 change 的 proposal/design/specs/tasks/policy 后明确批准 Build，Leader 才能按任务顺序派发实现。

## 完成门禁

- 所有 tasks 已完成且 Owner/验证证据齐全。
- `verification.md` 从“待执行”更新为真实结果，禁止预填通过。
- Product/Architecture/Tech/Contracts 的 Sync Gate 已完成或明确记录不受影响。
- `git diff ff08b45e58b50ae3cef15c6f96c8d8874dbce0b0 --stat` 与文件范围核对通过。
- 无陈旧 In progress/open 或无 Owner pending；所有 P0/P1 Review 发现闭环。
- Implementer、Test、Spec Reviewer、Code Reviewer 职责分离并记录。
