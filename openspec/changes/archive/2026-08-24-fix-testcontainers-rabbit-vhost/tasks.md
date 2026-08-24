# 修复 Testcontainers Rabbit vhost 测试配置

Status: Approved
批准者: 用户
批准日期: 2026-08-24
批准依据: 用户指令完成 Docker 全量未运行测试。

- Change: `fix-testcontainers-rabbit-vhost`
- Owner: Test / Server Backend
- 级别: Light
- 基线引用: `069cfce`

## 待办

- [x] 1.1 在 Testcontainers 集成测试中显式设置 Rabbit 默认 vhost `/`
  - Allowed: `services/api/src/test/java/com/suilearn/api/integration/DurablePipelineTestcontainersIntegrationTest.java`
  - Forbidden: 业务代码、契约、`apps/**`
  - Test: `mvn -B -f services/api/pom.xml test`
  - 完成定义：Docker 全量后端测试 0 failures/0 errors。

## 历史提交路径覆盖（非本 change 修改）

- `contracts/**`
- `services/api/**`
- `docs/architecture.md`
- `docs/product-requirements.md`
- `docs/tech-selection.md`
- `.agents/**`
- `openspec/**`
