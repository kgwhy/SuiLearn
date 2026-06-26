# 任务

## 1. 架构和契约对齐

- 状态：已完成
- Owner：Architect Agent
- 允许文件：`docs/architecture.md`、`docs/tech-selection.md`、`contracts/**`、`openspec/changes/complete-v2-ai-rag-release/**`
- 禁止文件：`apps/**`、`services/**`
- 验证：契约 diff 审查和受影响模块测试计划

## 2. 真实 AI Provider 路径

- 状态：已完成后端 MVP HTTP 路径
- Owner：Server Backend Agent
- 允许文件：`services/api/**`
- 禁止文件：`apps/**`；Task 1 契约更新完成前禁止改 `contracts/**`
- 验证：`mvn -f services/api/pom.xml test -q`
- 证据：OpenAI-compatible chat generation 和 embeddings 使用本地 HTTP server 测试，无外部网络依赖。

## 3. 后端学习记录和统计

- 状态：已完成
- Owner：Server Backend Agent
- 允许文件：`services/api/**`
- 禁止文件：`apps/**`；Task 1 契约更新完成前禁止改 `contracts/**`
- 验证：`mvn -f services/api/pom.xml test -q`

## 4. 语义检索实现

- 状态：已完成
- Owner：Server Backend Agent
- 允许文件：`services/api/**`
- 禁止文件：`apps/**`；Task 1 契约更新完成前禁止改 `contracts/**`
- 验证：`mvn -f services/api/pom.xml test -q`
- 证据：真实 embedding provider 路径已存在；retriever 在存储非 fake embeddings 时按向量相似度排序，fake 模式保留 keyword-only，避免虚假 RAG 证据。

## 5. Android 保存 AI 内容进入学习闭环

- 状态：已完成
- Owner：Android Agent
- 允许文件：`apps/android/**`
- 禁止文件：`services/**`、`apps/web/**`；Task 1 契约更新完成前禁止改 `contracts/**`
- 验证：`.\gradlew.bat :app:testDebugUnitTest --no-daemon`

## 6. Web 工作台测试

- 状态：已完成
- Owner：Web Frontend Agent 和 Test Agent
- 允许文件：`apps/web/**`
- 禁止文件：`services/**`、`apps/android/**`；Task 1 契约更新完成前禁止改 `contracts/**`
- 验证：`npm --prefix apps/web run build` 加本任务新增的已批准 Web 测试命令

## 7. 工作流文档漂移清理

- 状态：本变更内已完成
- Owner：Leader Agent 协调 Architect/Product 审查
- 允许文件：拥有角色已批准的当前事实文档
- 禁止文件：实现代码
- 验证：工作流检查器和角色审查

## 8. 工作流关闭门禁硬化

- 状态：已完成
- Owner：Leader Agent
- 允许文件：`AGENTS.md`、`docs/development-workflow.md`、`scripts/check-suilearn-workflow.ps1`、`openspec/changes/complete-v2-ai-rag-release/**`
- 禁止文件：实现代码
- 验证：`powershell -ExecutionPolicy Bypass -File scripts/check-suilearn-workflow.ps1 -BaseRef 3f3fe48b8c940ed3be2d922e6739d143c7e122c1 -ClosingChange complete-v2-ai-rag-release`
- 证据：完成规则现在要求 Review Agent 处置记录、关闭状态扫描、当前事实文档退役流程引用扫描，以及主/子 active change 关系说明。
