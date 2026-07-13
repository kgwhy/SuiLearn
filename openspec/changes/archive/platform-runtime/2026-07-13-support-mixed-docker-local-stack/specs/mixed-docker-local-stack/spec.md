## ADDED Requirements

### Requirement: Web 容器运行时 API 上游

Web Docker image 必须（SHALL）允许通过运行时配置选择 Nginx `/api` 代理使用的 API 上游，并保持 `http://host.docker.internal:8080` 作为默认值。

#### Scenario: 默认使用宿主机网关上游

- 当 Web image 未设置自定义 API 上游时
- Nginx 必须把 `/api` 请求代理到 `http://host.docker.internal:8080`

#### Scenario: 混合本地后端上游

- 当 Web image 使用 `SUILEARN_API_UPSTREAM=http://host.docker.internal:8080` 运行时
- Nginx 必须把 `/api` 请求代理到配置的宿主机后端上游

### Requirement: Docker 与本地后端混合 Compose 入口

根 Compose 文件必须（SHALL）为每个组件只提供一个 service：`postgres`、`api` 和 `web`。为了让 Web 容器连接宿主机后端，不得（MUST NOT）要求第二个 Web service。

#### Scenario: Docker Web 连接宿主机后端

- 当开发者在未设置 `SUILEARN_API_UPSTREAM` 的情况下启动 `web` 时
- Compose 必须启动同一个 `web` service 和 image，并把 `/api` 代理到宿主机 `8080` 端口暴露的后端

#### Scenario: 默认全栈模式保持组件服务不变

- 当开发者运行默认全栈 Compose 命令时
- Compose 必须使用 `postgres`、`api` 和 `web` services 启动 PostgreSQL、API 和 Web

### Requirement: Docker API 运行时数据库目标

API Compose service 必须（SHALL）允许通过运行时配置选择 JDBC 数据库 URL，并保持宿主机网关 PostgreSQL 端口作为默认目标。

#### Scenario: Docker API 默认使用宿主机网关数据库

- 当开发者在未设置自定义数据库 URL 的情况下启动 `api` 时
- API 必须默认使用 `jdbc:postgresql://host.docker.internal:5432/suilearn`

#### Scenario: Docker API 使用宿主机数据库

- 当开发者使用 `SUILEARN_DB_URL=jdbc:postgresql://host.docker.internal:5432/suilearn` 启动 `api` 时
- API container 必须连接宿主机数据库，而不是强制依赖 Compose PostgreSQL service

### Requirement: 单一 Compose 入口

仓库必须（SHALL）只保留根目录 `compose.yml` 作为开发 Compose 入口。

#### Scenario: 启动单独数据库不需要第二个 Compose 文件

- 当开发者只需要 Docker PostgreSQL 时
- 必须可以通过 `docker compose up -d postgres` 从根目录启动
