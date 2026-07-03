## 背景

当前 `compose.yml` 主要面向全栈 Docker 运行，Web 容器此前硬编码代理到 Compose 内部后端服务。开发者切换到“Docker 跑数据库/前端，本地跑后端”时容易误启动额外 Web service、手动切换环境变量，或留下旧的 `suilearn-api-local` 数据库容器，Docker Desktop 中会出现两套数据库/前端容器。

本变更让 SuiLearn 的本地运行形态统一为一个 Compose project、一个数据库 service、一个后端 image、一个前端 image；前端、后端和数据库都可以选择 Docker 启动或本地直连/本地命令启动。

## 变更内容

- Web Nginx 上游从硬编码 `http://api:8080` 改为运行时环境变量配置，默认走 `http://host.docker.internal:8080`，兼容 Docker API 和本地 Maven 后端。
- 根 `compose.yml` 只保留 `postgres`、`api`、`web` 三个组件 service；移除额外的 `web-local-backend` service。
- Docker API 的数据库 URL 可通过 `SUILEARN_DB_URL` 覆盖，默认走 `jdbc:postgresql://host.docker.internal:5432/suilearn`，兼容 Docker PostgreSQL 和宿主机数据库。
- 删除旧 `services/api/compose.local.yml`，统一使用根目录 `compose.yml` 启动全栈或单组件服务，避免继续维护第二个 Compose 入口。
- `.env.example`、本地 Vite 配置和 README 统一 Web 默认端口为 `5174`，并新增本地/Docker 组合启动说明。

非目标：

- 不改变后端 API 契约、数据库模型、Web 业务页面或前端 API client。
- 不新增独立 HTTP 中转容器；当前规模下由 Web Nginx 直接选择上游即可。
- 不删除历史旧容器或数据卷；清理由用户按需执行。

验收标准：

- `docker compose up --build -d` 的默认全栈语义保持不变，仍只使用 `postgres`、`api`、`web`。
- `docker compose up --build -d web` 默认可同时兼容宿主机 Maven 后端和 Docker API 暴露的 `8080` 端口，不需要手动设置 `SUILEARN_API_UPSTREAM`。
- `docker compose up --build -d api` 默认可同时兼容宿主机数据库和 Docker PostgreSQL 暴露的 `5432` 端口，不需要手动设置 `SUILEARN_DB_URL`。
- 仓库只保留根目录 `compose.yml` 作为 Compose 入口，不再要求开发者记住两个 Compose 文件。
- README 明确单一 Compose 入口、组件启动矩阵、端口占用处理和仅在非默认端口时才需要的环境变量覆盖方式。

受影响当前事实文档：

- README 运行说明需要同步。
- 不影响 `docs/product-requirements.md`、`docs/architecture.md`、`docs/tech-selection.md` 或 `contracts/**`。

## 能力变更

### 新增能力

- `mixed-docker-local-stack`: 数据库、后端和 Web 可在同一 Compose project 下按组件启动，默认经宿主机发布端口自动兼容 Docker 服务和本地服务；环境变量仅用于非默认端口或特殊网络覆盖。

### 修改能力

- 无。

## 影响范围

- `apps/web/Dockerfile`
- `apps/web/nginx.conf.template`
- `apps/web/vite.config.ts`
- `compose.yml`
- `.env.example`
- `README.md`
- `docs/tech-selection.md`
- `services/api/config/local.properties.example`
- `openspec/changes/support-mixed-docker-local-stack/**`
