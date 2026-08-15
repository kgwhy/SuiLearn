# 归档

Status: passed

## 变更名称

容器化完整服务。

## 实现引用

working tree：

- `compose.yml`
- `.env.example`
- `.gitignore`
- `services/api/Dockerfile`
- `services/api/.dockerignore`
- `apps/web/Dockerfile`
- `apps/web/.dockerignore`
- `apps/web/nginx.conf`
- `README.md`
- `openspec/changes/2026-06-30-containerize-full-stack/**`

## 验证摘要

- 根 `compose.yml` 编排 `postgres`、`api`、`web`。
- API 容器连接 compose 内部 PostgreSQL：`jdbc:postgresql://postgres:5432/...`。
- Web 容器使用 Nginx 静态服务，并将 `/api/` 代理到 `api:8080`。
- API healthcheck 使用 `/api/v2/ai/provider-status`；Web 等待 API healthy 后启动。
- `.env.example` 提供端口、数据库和 AI Provider 配置示例。
- `docker compose config`、后端 jar 构建、Web 测试/构建和工作流检查通过。

## 当前事实同步

README 已由用户确认同步 Docker 一键启动说明。本变更不修改产品范围、API 契约、数据库模型或业务代码。

## 延期项

延期项：无。

## 最终审查

无 P0/P1/P2 阻塞问题。用户审查提出的非阻塞优化中，已补 `.env.example`、API healthcheck、Web healthy 依赖和 CI push 分支过滤；`pgvector/pgvector:pg16` 保持与本地 compose 和技术基线一致。
