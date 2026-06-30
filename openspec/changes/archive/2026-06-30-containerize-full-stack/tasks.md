# 任务：容器化完整服务

## 状态

- [x] 新增后端 Dockerfile 和 `.dockerignore`。
- [x] 新增 Web Dockerfile、Nginx 配置和 `.dockerignore`。
- [x] 新增根目录完整 `compose.yml`。
- [x] 新增根 `.env.example`，列出端口、数据库和 AI Provider 配置。
- [x] 为 API/Web 编排增加 healthcheck 启动顺序。
- [x] 运行本地可用验证命令，并记录 Docker 不可用项。

## 验证摘要

- `docker compose config`：通过，可解析 `postgres`、`api`、`web` 三个服务。
- `mvn -f services/api/pom.xml -DskipTests package -q`：通过。
- `npm --prefix apps/web run build`：通过。
- `docker compose build`：本机 Docker/Buildx 权限阻塞，错误为 `CreateFile C:\Users\youku\.docker\buildx\instances: Access is denied.`
- `docker compose up --build --detach`：本机 Docker API 权限阻塞，错误为 `open //./pipe/docker_engine: Access is denied.`

## Owner

- Leader Agent 协调跨角色范围。
- Server Backend Agent 范围：`services/api/Dockerfile`、`services/api/.dockerignore`。
- Web Frontend Agent 范围：`apps/web/Dockerfile`、`apps/web/.dockerignore`、`apps/web/nginx.conf`。
- Leader/架构协调范围：根目录 `compose.yml` 和本 change 产物。

## 允许修改文件

- `compose.yml`
- `.env.example`
- `.gitignore`
- `services/api/Dockerfile`
- `services/api/.dockerignore`
- `apps/web/Dockerfile`
- `apps/web/.dockerignore`
- `apps/web/nginx.conf`
- `openspec/changes/2026-06-30-containerize-full-stack/**`

## 禁止修改文件

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

## 验证命令

- `npm --prefix apps/web run build`
- `mvn -f services/api/pom.xml test -q`
- `docker compose config`
- `docker compose build`
- `docker compose up --build`
- `powershell -ExecutionPolicy Bypass -File scripts/check-suilearn-workflow.ps1 -BaseRef 2379f957b6e8c047aed8f09d4993a0627c70dcde`

## 审查重点

- Compose 入口必须位于根目录，支持一条命令启动完整服务。
- API 容器不得连接宿主机 `localhost` 数据库。
- Web 容器必须代理 `/api/`，避免浏览器直连容器内部主机名。
- Docker 镜像不得复制本地密钥、`node_modules`、`target`、`dist` 或 `local.properties`。
- `.env.example` 只能包含示例值或空占位，不写入真实密钥。
