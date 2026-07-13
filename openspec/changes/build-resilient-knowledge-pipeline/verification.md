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

- Spec Review：未执行（Owner: Reviewer Agent）。
- Code Review：未执行（Owner: Reviewer Agent）。
- P0/P1/P2 处置：Task 1.1/Spec 修订 Review 已发现并修复配置、幂等、安全与任务边界问题；后续 Build 继续逐任务据实记录，任何 P2 也必须修复或经用户批准迁移到具名 follow-up change。
- reviewer-style 自审：Spec 收尾时先执行一次，Build/Verify 收尾再执行一次。
