## 背景

当前 Web Docker 镜像使用 Nginx 托管构建后的静态资源，并在 `location /api/` 中代理后端。全栈 Compose 下 `api` 是 Compose 网络里的后端服务名；混合开发时，Web 容器需要代理宿主机 Maven 后端。此前新增的 `web-local-backend` 能解决连接问题，但会让 Docker Desktop 中出现两个 Web service。旧的 `services/api/compose.local.yml` 也会让开发者误以为项目有第二个 Compose 入口。

## 目标与非目标

**目标：**

- 同一个 Web 镜像支持运行时配置 API 上游。
- 同一个 API 镜像支持运行时配置数据库 URL。
- 根 Compose 只保留 `postgres`、`api`、`web` 三个 service；开发者通过 service 名组合运行，默认端口下无需切换环境变量。
- Docker Web 和本地 Vite 默认访问端口统一为 `5174`。
- 删除旧 `services/api/compose.local.yml`，根目录 `compose.yml` 成为唯一 Compose 入口。
- README 给出可直接复制的 PowerShell 命令和启动矩阵。

**非目标：**

- 不增加新的业务 API、页面或数据库行为。
- 不引入额外反向代理容器。
- 不自动删除用户机器上已有的旧容器或旧数据卷。

## 决策

1. 使用 Nginx 官方镜像的 `/etc/nginx/templates/*.template` 机制。
   - 方案：把 Web Nginx 配置改为模板，使用 `${SUILEARN_API_UPSTREAM}` 注入上游。
   - 理由：无需维护两份 Nginx 配置，也不需要启动额外 HTTP 中转容器。
   - 备选：新增专门 proxy 容器。缺点是增加容器、日志和健康检查排错点。

2. `web` 是唯一 Web service，默认 `SUILEARN_API_UPSTREAM=http://host.docker.internal:8080`。
   - 方案：在 Dockerfile 中设置默认值，Compose 的 `web` 服务也显式传入宿主机发布端口上游。
   - 理由：Docker API 和本地 Maven 后端默认都暴露在宿主机 `8080`，Web 容器走同一个入口即可自动兼容两种运行形态。

3. `api` 是唯一后端 service，数据库 URL 可通过 `SUILEARN_DB_URL` 覆盖。
   - 方案：默认连接 `jdbc:postgresql://host.docker.internal:<SUILEARN_POSTGRES_PORT>/<db>`；非默认数据库地址时再通过 `SUILEARN_DB_URL` 覆盖。
   - 理由：Docker PostgreSQL 和本地 PostgreSQL 默认都暴露在宿主机 `5432`，API 容器走同一个入口即可自动兼容两种运行形态。

4. 去掉 `web` 对 `api`、`api` 对 `postgres` 的强制 `depends_on`。
   - 方案：各组件可独立 `docker compose up -d <service>`；API 增加 `restart: unless-stopped`，避免全栈启动时数据库短暂未就绪导致一次性退出。
   - 理由：Compose 不支持“同一个 service 在不同启动组合下切换依赖”。为了单 service 组合启动，默认连接目标统一为宿主机发布端口，环境变量只作为覆盖口。

5. 删除 `services/api/compose.local.yml`。
   - 方案：所有 Docker 组合都通过根目录 `compose.yml` 启动，例如 `docker compose up -d postgres`。
   - 理由：一个项目保留一个 Compose 入口，避免用户在根 Compose 和后端子目录 Compose 之间做选择。

## 风险与取舍

- [Risk] `host.docker.internal` 在非 Docker Desktop Linux 环境可能不可用。→ 为 `api` 和 `web` 增加 `extra_hosts: host.docker.internal:host-gateway`，覆盖常见 Linux Docker Engine。
- [Risk] 全栈启动早期 Web 请求可能短暂 502。→ README 说明首次启动需等待，API 配置 `restart: unless-stopped` 以等待数据库恢复。
- [Risk] PowerShell 会话里残留自定义 `SUILEARN_API_UPSTREAM` 或 `SUILEARN_DB_URL` 会影响下一次启动。→ README 明确仅在非默认端口时覆盖，切回默认前清理环境变量。
- [Risk] 旧容器仍会显示在 Docker Desktop 中。→ 最终说明给出只删容器不删卷的清理命令。
