# 验证计划与记录

状态：待执行（Owner: Test Agent；最终汇总 Owner: Leader Agent）。

当前仅完成 Spec 文档，不得把下列计划描述为已通过。

## 基线与范围

- base_ref：`ff08b45e58b50ae3cef15c6f96c8d8874dbce0b0`
- Spec 阶段业务代码未编辑，模块基线测试不适用。
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

状态：待执行（Owner: Leader Agent + Reviewer Agent）。

## Review 闭环

- Spec Review：未执行（Owner: Reviewer Agent）。
- Code Review：未执行（Owner: Reviewer Agent）。
- P0/P1 处置：当前无实现发现；Build 后据实记录。
- reviewer-style 自审：Spec 收尾时先执行一次，Build/Verify 收尾再执行一次。
