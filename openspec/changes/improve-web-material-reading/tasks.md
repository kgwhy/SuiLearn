# 优化 Web 导入资料后的内容查看和知识点可读性

## 任务说明

- Owner: Leader 协调，Web Frontend Agent 执行。
- 级别: Normal。
- 用户需求: 导入资料后用户可查看资料内容；知识点不要做成无法查看的关键词。
- 非目标: 不改后端、契约、Android、产品事实文档或知识点提取算法。

## 待办

- [x] 补充 Web 回归测试，覆盖资料详情正文优先展示、导入后自动打开详情、资料知识点可读详情列表。
- [x] 调整 `apps/web/src/App.tsx`，让 READY 资料导入后自动打开详情并切到资料页。
- [x] 调整 `apps/web/src/App.tsx`，新增资料正文阅读块和知识点详情列表。
- [x] 调整 `apps/web/src/styles.css`，保证正文和知识点详情在桌面/移动端可读。
- [x] 运行 Web 测试、Web 构建、workflow 检查和 diff 范围核对。
- [x] 修复资料正文/片段展示中的 URL 编码噪声，避免前端直接显示 `%E5...`。
- [x] 修复 AI 未配置时 `extract-knowledge-points` 500，改为服务端本地 fallback 提取。
- [x] 补充并运行对应 Web/后端回归测试。

## 验证记录

- `npm --prefix apps/web test`: 通过，6 个测试全部通过。
- `npm --prefix apps/web run build`: 通过，`tsc -b && vite build` 完成。
- `powershell -ExecutionPolicy Bypass -File scripts/check-suilearn-workflow.ps1 -BaseRef 5ec2fafe308afc71032ffa8284ff9c4c4abea3ad`: 通过。
- `git diff 5ec2fafe308afc71032ffa8284ff9c4c4abea3ad --stat`: 已运行；未跟踪新文件需结合 `git status --short` 查看。
- 浏览器验证: 前台短跑 `npm --prefix apps/web run dev -- --host 127.0.0.1 --port 5273 --strictPort` 可启动 Vite；后台隐藏进程在当前桌面沙箱中未保活，未能完成应用内浏览器交互验证。
- `mvn -f services/api/pom.xml "-Dtest=KnowledgePointServiceTest,KnowledgePointCandidateExtractorTest" test -q`: 通过。
- `mvn -f services/api/pom.xml -DskipTests package -q`: 通过。
- 运行环境验证已归并到 `openspec/changes/support-mixed-docker-local-stack/verification.md`；本 change 仅保留资料阅读和知识点可读性相关的 Web/后端功能验证。
- `mvn -f services/api/pom.xml test -q`: 通过。日志中 `PostgresChunkSearchIndexMigrationTest` 会输出一个预期内的 `boom` 栈，用于验证迁移失败时不中断。
- `npm --prefix apps/web test`: 通过，7 个测试全部通过。

## 验证命令

- `npm --prefix apps/web test`
- `npm --prefix apps/web run build`
- `powershell -ExecutionPolicy Bypass -File scripts/check-suilearn-workflow.ps1 -BaseRef 5ec2fafe308afc71032ffa8284ff9c4c4abea3ad`
- `git diff 5ec2fafe308afc71032ffa8284ff9c4c4abea3ad --stat`

## 审查重点

- 资料详情必须优先渲染完整 `content`，不能只显示短预览。
- 导入 READY 资料后必须自动进入可查看详情的路径。
- 知识点必须有可阅读描述和来源，不应只停留在关键词 chip。
- 文件变更必须限制在允许范围。
