# 验证计划与记录

状态：Build 进行中（Owner: Test Agent；最终汇总 Owner: Leader Agent）。

Build 入口基线已执行；下列功能、故障和运行态矩阵仍不得在没有真实证据时描述为已通过。

## 基线与范围

- base_ref：`ff08b45e58b50ae3cef15c6f96c8d8874dbce0b0`
- 隔离 worktree：`D:\SuiLearn\.worktrees\build-resilient-knowledge-pipeline`，分支 `codex/build-resilient-knowledge-pipeline`。
- 2026-07-10 Build 入口基线：
  - `mvn -f services/api/pom.xml test -q`：退出码 0；测试日志包含预期的迁移异常场景 WARN 和 Mockito/JDK 动态 agent 警告。
  - `npm --prefix apps/web test`：退出码 0，9/9 通过。
  - `npm --prefix apps/web run build`：退出码 0，TypeScript 与 Vite production build 成功。
  - `.\gradlew.bat :app:testDebugUnitTest --no-daemon`：首次因 worktree 缺少已忽略的 `local.properties` 未启动；复用主 checkout 的 Android SDK 路径后重跑退出码 0。
  - `docker compose config`：退出码 0；Docker CLI 报告用户级 `config.json` 无读取权限的环境警告，不影响静态配置展开。
- 每个 Build 任务执行前后必须记录 `tasks.md` 指定命令的原始结果。
- 最终范围只能落在 `policy.md` 最大边界与具体任务 Allowed files 交集内。
- 2026-07-14 用户批准范围纠正：真实二进制 OLE `.doc` 支持需要在既有 `poi-ooxml` 之外使用 `org.apache.poi:poi-scratchpad`；Task 3.1 必须使用最小化、生成或版权安全的真实 OLE `.doc` fixture，改名 RTF 无效；Task 3.2 被明确授权仅为此在 `services/api/pom.xml` 新增该依赖。

## 自动化验证命令

```powershell
mvn -f services/api/pom.xml test -q
npm --prefix apps/web test
npm --prefix apps/web run build
.\gradlew.bat :app:testDebugUnitTest --no-daemon
.\gradlew.bat :app:assembleDebug --no-daemon
docker compose config
powershell -ExecutionPolicy Bypass -File scripts/check-suilearn-workflow.ps1 -BaseRef ff08b45e58b50ae3cef15c6f96c8d8874dbce0b0
git diff ff08b45e58b50ae3cef15c6f96c8d8874dbce0b0 --stat
```

Owner: 各模块 Test Agent；最终复核 Leader/Reviewer。

## Task 2.1 配置验收矩阵

### Batch B 已验证证据（2026-07-14）

- `docker compose config` 与 retry 缺失、空值、legacy-only、canonical-only、双非空矩阵均退出码 0；Compose 保持新旧键无默认透传。
- `mvn -f services/api/pom.xml test -q` 退出码 0，129 tests、0 failures、0 errors。
- 独立 Test、Spec Review、Code Review 均完成；最终 P0/P1/P2 为 0。
- 已验证 Outbox confirm 后 ACK、bounded retry/DLQ、DLQ 受控重放 confirm、两级幂等、恢复、私有 MinIO 流式存储与 promotion 恢复、异步关闭时导入前 503 准入、adapter retry 运行态 0/1 次数语义和脱敏错误持久化。
- 未执行 `docker compose up` 与故障注入；端到端导入/OCR、Broker/MinIO 故障矩阵仍由 Batch C/Task 6.3 按本文件运行态计划完成，不得将静态配置或单元测试表述为运行态通过。

本节只定义配置与后续运行态验收计划；尚未执行 `docker compose up` 或故障注入，不得据此宣称中间件可运行。

| 场景 | `docker compose config` 的期望 | 状态/证据 Owner |
| --- | --- | --- |
| 默认本地编排 | `postgres`、`rabbitmq`、`minio`、`api`、`web` 均存在；三个持久卷与各服务健康检查存在；API 依赖三个健康服务 | 待执行 — Leader Agent |
| RabbitMQ/MinIO 覆盖 | RabbitMQ host/port/user/password/vhost、MinIO endpoint/access/secret/bucket 及宿主机端口均可由环境覆盖 | 待执行 — Leader Agent |
| retry 键缺失 | 两个 retry 键均不得被 Compose 注入非空默认；Backend 后续采用应用层默认 `0` | 待执行 — Leader Agent + Server Backend Agent |
| retry 键为空 | Compose 仅传递空值，Backend 后续按未提供处理；不得转换为 Compose 默认 | 待执行 — Leader Agent + Server Backend Agent |
| 仅旧 retry 键 | Compose 只透传 `SUILEARN_AI_MAX_RETRIES`，不补 canonical 键；Backend 2.2 负责兼容映射和诊断 | 待执行 — Leader Agent + Server Backend Agent |
| 仅 canonical retry 键 | Compose 只透传 `SUILEARN_ADAPTER_MAX_RETRIES`；Backend 2.2 负责 `0..1` 校验 | 待执行 — Leader Agent + Server Backend Agent |
| 两个 retry 键同时非空 | Compose 原样透传两键；Backend 2.2 必须以 `SUILEARN_RETRY_CONFIG_CONFLICT` fail-fast | 待执行 — Leader Agent + Server Backend Agent |

### 默认值语义与覆盖口

- `.env.example` 仅保存非生产、非敏感的本地编排默认值；部署凭据必须由部署环境覆盖，且不得写入日志、响应或本变更记录。
- RabbitMQ/MinIO 的 API 连接变量、处理开关、并发、文件/页数限制、任务和 adapter 超时/retry、熔断参数均以同名 `SUILEARN_*` 环境变量覆盖；Compose 将其逐项映射给 API。
- `SUILEARN_ADAPTER_MAX_RETRIES=0` 是根环境示例的唯一 retry 默认。deprecated `SUILEARN_AI_MAX_RETRIES` 不得出现在 `.env.example`。
- Compose 对两个 retry 键分别使用无默认值透传 `${SUILEARN_ADAPTER_MAX_RETRIES-}` 与 `${SUILEARN_AI_MAX_RETRIES-}`。缺失或空值均不代表显式 retry 输入；应用层绑定、legacy 映射和双键冲突处理由 Task 2.2 验证。

### Task 2.1 残留扫描

- `compose.yml`、`.env.example`、启动脚本和 CI 中不存在 `SUILEARN_AI_MAX_RETRIES=2` 或其他旧 retry 默认。
- Compose 中不存在 `${SUILEARN_ADAPTER_MAX_RETRIES:-0}`、legacy retry 的非空默认，或 Redis/独立 Worker 服务。
- RabbitMQ、MinIO 与 PostgreSQL 均有持久卷、健康检查；API `depends_on` 以健康条件连接这些依赖。
- `.env.example` 不含真实生产凭据、API key 或 MinIO/RabbitMQ 的生产 secret。

### Task 2.1 运行态验证计划

1. 运行 `docker compose up -d --build` 和 `docker compose ps`，确认 PostgreSQL、RabbitMQ、MinIO 与 API 的健康状态；记录实际镜像拉取或启动阻塞。
2. 暂停并恢复 RabbitMQ，验证 API 重启后未发送 Outbox 可恢复；Task 2.4/6.3 实现后记录真实消息、ACK、DLQ 证据。
3. 使 MinIO 不可用，验证新上传明确失败且不返回虚假 READY；Task 2.5/6.3 实现后记录对象清理与恢复证据。
4. 在缺失、空值、legacy-only、canonical-only、both-nonempty 五种 retry 输入下运行 Compose 配置矩阵；Task 2.2 实现后补充启动/diagnostic 的真实结果。

## 格式验收矩阵

| 场景 | 期望 | 状态/证据 Owner |
| --- | --- | --- |
| Markdown/TXT | 保存原件、完整阅读版、结构化知识点草稿 | 待执行 — Test Agent |
| DOC/DOCX | 保存原件、阅读版和高保真预览 | 待执行 — Test Agent |
| 文本 PDF | 直接提取且 OCR 调用为 0 | 待执行 — Test Agent |
| 扫描 PDF | 按页 OCR 并保留页码引用 | 待执行 — Test Agent |
| 混合 PDF | 仅 OCR 文本不足页面 | 待执行 — Test Agent |
| 损坏文件 | 永久失败、原件/错误语义符合策略 | 待执行 — Test Agent |
| 伪造扩展/MIME/签名 | 上传拒绝，不产生虚假 READY | 待执行 — Test Agent |
| 50 MB/500 页边界 | 默认值与环境覆盖均按契约执行 | 待执行 — Test Agent |
| Markdown 不可信内容 | raw HTML 转义/禁用，危险或未知 scheme 拒绝，远程资源默认不自动加载；受控代理验证 SSRF 边界 | 待执行 — Test Agent |

## 异步与故障恢复矩阵

| 故障 | 期望 | 状态/证据 Owner |
| --- | --- | --- |
| RabbitMQ 发布中断 | Outbox 保留，恢复后投递 | 待执行 — Test Agent |
| 消费者 ACK 前崩溃 | 消息重投且不重复写 | 待执行 — Test Agent |
| 重复消息 | revision/知识点/题目均幂等 | 待执行 — Test Agent |
| MinIO 上传中断 | 不返回虚假成功；可恢复/清理临时对象 | 待执行 — Test Agent |
| AI 未配置 | 资料可读，无关键词/占位知识点 | 待执行 — Test Agent |
| AI/OCR 超时 | 退避重试、熔断、上限后 DLQ | 待执行 — Test Agent |
| 应用重启 | 未发 Outbox/卡住任务恢复 | 待执行 — Test Agent |
| 删除资料 | 资产/索引异步清理，已保存内容保留删除来源 | 待执行 — Test Agent |
| 部分 OCR 页面后重投/重启 | 已成功页复用持久化 operation result，调用次数不增加；只继续未完成/可重试失败页 | 待执行 — Test Agent |
| Adapter retry 配置四态 | 缺失/空值使用应用默认 0；仅新键校验；仅旧键有界映射并诊断；双非空/非法值 fail-fast | 待执行 — Test Agent |
| Legacy retry tombstone | removal change 的非空旧键以 `SUILEARN_RETRY_CONFIG_REMOVED` fail-fast；只有无残留证据后 cleanup change 才删除透传/detector | 待执行 — Test Agent |
| 指标标签基数 | IDs 仅进入日志/trace/exemplar；metric tags 只使用受控低基数枚举 | 待执行 — Test Agent |

## Web 验收

- multipart 拖放/选择上传，不对 PDF 调用 `file.text()`。
- 上传后可离开页面，状态持续更新；错误显示原因和重试。
- 完整阅读版、原始文件和下载入口均可达，引用可定位页/段落。
- 知识点列表显示标题+短总结，详情结构完整并可审核。
- 默认一键生成 1 道中等简答题；更多设置支持题型/难度/数量。
- 未审核知识点不可正式出题，未保存题目不进入题库。
- 桌面/平板/手机层级、键盘、aria、44px 目标和非颜色状态表达通过。

状态：待执行（Owner: Web Frontend Agent + Test Agent）。

## 运行态验证

```powershell
docker compose up -d --build
docker compose ps
```

必须实际完成：

1. 全格式端到端导入。
2. 处理中重启 API。
3. 暂停/恢复 RabbitMQ。
4. MinIO 不可用时上传。
5. 重复消息和 DLQ/人工重试。
6. AI/OCR 超时与熔断。
7. 删除资料后的对象/索引/来源残留检查。
8. Actuator/Micrometer 的依赖健康、队列、Outbox、任务、OCR/AI 指标。
9. 关闭或缺失 Backend/RabbitMQ/MinIO/AI 时 Android 本地学习闭环。

状态：待执行（Owner: Test Agent）。如果环境权限阻塞，必须记录原始错误、已尝试步骤和未被替代的剩余风险，不得用 `docker compose config` 冒充运行态通过。

## 残留扫描

完成前至少扫描：

- Web PDF `file.text()`。
- “粘贴正文/已转成文本 PDF”文案。
- 契约中真实 PDF/Office 解析不支持说明。
- `KnowledgePointCandidateExtractor` 或统一占位 description fallback 的主路径引用。
- `runManagedTask` 等同步资料/知识点/题目主路径。
- 日志/响应中的密钥、完整正文、原始模型响应、object key 或永久授权信息。
- 旧 retry 默认 `2`、`${SUILEARN_ADAPTER_MAX_RETRIES:-0}` 等 Compose 默认注入、Provider SDK/手写 retry 第二计数器。
- `SUILEARN_AI_MAX_RETRIES` 只允许出现在当前兼容期的 Compose 可选透传、Backend legacy detector/映射及迁移测试；tombstone/cleanup 阶段按具名 change 的 allowlist 扫描。
- correlationId/taskId/materialId 等 ID 被用作 Micrometer tags。
- Markdown raw HTML 未转义、`javascript:`/`data:`/`file:` 等危险 scheme 或远程图片默认自动加载。
- 已成功 OCR 页或其他 adapter operation 在任务重投/重启后被重复调用。

状态：待执行（Owner: Leader Agent + Reviewer Agent）。

## Review 闭环

- 审查策略：用户于 2026-07-10 明确要求减少时间和 Token 消耗，后续改为 Batch A–F 里程碑批次审查；任务内 TDD/局部测试保留，独立 Test/Spec Review/Code Review 在批次末统一执行。

### Task 1.1：技术与架构基线

- 状态：完成（2026-07-10）。
- 实现引用：`591d51e`、`25fbfbe`、`79dac27`。
- Implementer：Architect Agent；修改 `docs/architecture.md`、`docs/tech-selection.md` 及经用户批准的同一 active change design/spec/policy/tasks/verification 修订。
- Test：纯文档任务，模块测试不适用；`openspec validate build-resilient-knowledge-pipeline --strict`、SuiLearn Workflow checker、`git diff --check` 均退出码 0，并独立验证 retry 四态、Compose 可选透传、ProcessingOperation/OCR 页级恢复、CommonMark 安全、metric tag 基数、任务文件边界和残留矩阵。
- Spec Review：最终 `✅ Spec compliant`；用户批准 retry 配置 Spec 修订后，design/spec/tasks/policy 已同步且 OpenSpec 状态为 ready。
- Code Review：最终 `Ready to merge: Yes`，P0/P1/P2 均为 0；先前发现的当前事实误写、指标高基数、Markdown 安全、韧性默认/迁移、operation 幂等、tombstone、任务范围和验收矩阵问题均已修复并复审。
- 文件范围：Task 1.1 只修改 Architect 文档与本 active change 产物；未修改业务代码、契约、Compose 或 `.env.example`。

### Batch A / Task 1.2：OpenAPI 契约

- 状态：完成（2026-07-10）；实现引用：`3094ac7`、`253b437`。
- 契约：multipart 原始资料上传与 202 material/task、原件/阅读版/revision/block、结构化/legacy 知识点、审核门禁、按 task 寻址的知识点/题目批次、最多 10 道面试题、TaskStatus 状态变体及 deprecated legacy JSON 兼容。
- Test：Maven 74/74 通过；SnakeYAML duplicate-key 安全解析、264 个 `$ref` 闭包、路径/响应与 schema 正反例、敏感字段和同步主路径残留检查通过。
- Spec Review：`✅ Spec compliant`。
- Code Review：最终 `Ready to merge: Yes`，P0/P1/P2 均为 0；generic sourceRefs-only 草稿兼容修复已复审。
- 文件范围：`contracts/openapi/suilearn-v2.yaml` 及同 active change 的 design/spec/policy/tasks/verification；未修改 Backend、Web 或 Android 实现。
- 暂停点：按用户要求，Batch A 完成后暂停，不进入 Batch B。

### Workflow batch / Task 1.3：风险自适应批次工作流

- 状态：完成（2026-07-13）。
- 范围：稳定工作流、Leader 调度规则、轻量 Build reference、工作流检查器，以及同一 active change 的 task/policy/delta spec/verification；未修改 Backend、Web、Android、契约或产品事实。
- RED：新增检查器断言后，首次运行准确报告缺少“风险自适应批次、即时审查触发条件、紧凑证据、批次验收命令”并退出 1。
- Test：负向自测、正常 Workflow checker、`openspec validate build-resilient-knowledge-pipeline --strict`、`git diff --check` 最终均退出 0；纯工作流变更，业务模块测试不适用。
- Test 修复：Strict OpenSpec 首轮因 3 条 Requirement 缺少字面 `MUST/SHALL` 退出 1；按仓库既有模式补 `MUST` 后独立复测 3/3 通过。
- Spec/Code Review：首轮发现 1 个 P2——检查器只守四个关键词；已扩展为 TDD、独立三段审查、最终 Verify、P0/P1/P2 和 delta spec 核心语义断言，并加入逐条缺失计数的负向自测。复审结论：P0/P1/P2 均为 0，`Ready to merge: Yes`。
- 暂停点：按用户要求，本工作流批次完成并提交后暂停；不进入 Batch B。

- Spec Review：未执行（Owner: Reviewer Agent）。
- Code Review：未执行（Owner: Reviewer Agent）。
- P0/P1/P2 处置：Task 1.1/Spec 修订 Review 已发现并修复配置、幂等、安全与任务边界问题；后续 Build 继续逐任务据实记录，任何 P2 也必须修复或经用户批准迁移到具名 follow-up change。
- reviewer-style 自审：Spec 收尾时先执行一次，Build/Verify 收尾再执行一次。

### Task 3.1 fixture correction (2026-07-14)

- `mvn -f services/api/pom.xml -Dtest=DocumentParserContractTest test -q`: exit code 1; 8 tests RED, 8 failures, 0 errors. The Apache POI `Word6.doc` OLE fixture loaded successfully; every failure is the expected missing `com.suilearn.api.material.document.DocumentParser` contract awaiting Task 3.2.

### Batch C / Tasks 3.1–3.4 (2026-07-15)

- Independent final gate: approved; P0/P1/P2 = 0.
- `mvn -f services/api/pom.xml test -q`: 198 tests, 0 failures/errors/skips.
- `openspec validate build-resilient-knowledge-pipeline --strict`, workflow checker, and `git diff --check`: exit code 0.
- Verified parser failure routing/DLQ, OCR retry and operation reuse, reprocess and legacy revisions, bounded parsing, descendant-first process termination, and 1 MiB process-output caps.

### Task 1.3 workflow refinement (2026-07-15)

- User approved four workflow controls: evidence-fingerprint reuse, cancellation/report isolation, compact successful logs and Git summaries, and worktree-local `safe.directory` handling.
- Scope is limited to Leader workflow policy, subagent loop, workflow checker, and this active change's Task 1.3 artifacts; no application, contract, or product behavior is changed.
