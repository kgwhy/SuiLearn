# Policy：容器化完整服务

## 变更等级

Normal。

理由：该变更跨 Server Backend、Web Frontend 和根级编排文件，但不修改产品范围、API 契约、存储模型或业务代码。

## base_ref

`2379f957b6e8c047aed8f09d4993a0627c70dcde`

## 关联 active change

- `2026-06-30-add-github-actions-ci`：并行 active change，不属于本次容器化审查范围。

## 角色归属

- Leader Agent：跨角色协调、任务卡和最终门禁。
- Server Backend Agent：后端 Dockerfile 和后端 Docker ignore。
- Web Frontend Agent：Web Dockerfile、Nginx 配置和 Web Docker ignore。
- Architect/Leader 协调：根 `compose.yml` 的服务边界。

## 文件锁

模式：serial。

锁定路径：

- `compose.yml`
- `.env.example`
- `.gitignore`
- `services/api/Dockerfile`
- `services/api/.dockerignore`
- `apps/web/Dockerfile`
- `apps/web/.dockerignore`
- `apps/web/nginx.conf`
- `openspec/changes/2026-06-30-containerize-full-stack/**`

当前检查：未发现 `.agents/locks` 中的 active lock 冲突。

## 允许路径

- `compose.yml`
- `.env.example`
- `.gitignore`
- `services/api/Dockerfile`
- `services/api/.dockerignore`
- `apps/web/Dockerfile`
- `apps/web/.dockerignore`
- `apps/web/nginx.conf`
- `openspec/changes/2026-06-30-containerize-full-stack/**`

## 禁止路径

- `apps/android/**`
- `services/api/src/**`
- `apps/web/src/**`
- `contracts/**`
- `docs/product-requirements.md`
- `docs/architecture.md`
- `docs/tech-selection.md`
- `docs/proposals/**`
- `docs/superpowers/specs/**`
- `docs/superpowers/plans/**`

## 基线状态

- Web 基线：passed，`npm --prefix apps/web run build` 成功。
- Backend 基线：unavailable，`mvn -f services/api/pom.xml test -q` 因本机 `localhost:5432` PostgreSQL 不可用失败；本变更提供 compose 数据库启动入口。

## 验收标准

- `docker compose config` 能解析完整服务编排。
- 根 `.env.example` 提供可复制的 Docker Compose 配置入口。
- Web 等待 API healthcheck 通过后启动，减少首启 502。
- `docker compose up --build` 在 Docker 可用环境中能启动 `postgres`、`api`、`web`。
- 本地无法访问 Docker API 时，最终报告记录原始错误和替代验证。
