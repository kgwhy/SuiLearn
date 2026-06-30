# Design：容器化完整服务

## 总体方案

新增根目录 `compose.yml` 作为完整服务入口：

- `postgres`：沿用 `pgvector/pgvector:pg16`，默认数据库 `suilearn`。
- `api`：从 `services/api/Dockerfile` 构建 Spring Boot jar，运行时通过 `SUILEARN_DB_URL` 指向 `postgres:5432`。
- `web`：从 `apps/web/Dockerfile` 构建静态资源，Nginx 监听 `80`，宿主机映射到 `${SUILEARN_WEB_PORT:-5173}`。

## 后端镜像

后端使用 Maven + Temurin 21 多阶段构建：

1. build stage 复制 `pom.xml` 和 `src/` 后执行 `mvn -B -DskipTests package`。
2. runtime stage 使用 `eclipse-temurin:21-jre`，只复制生成的 jar。
3. 运行时通过环境变量注入数据库和 AI Provider 配置。

构建时跳过测试，原因是镜像构建只产出可运行 artifact；测试由 Maven 本地命令和 CI job 承担。

## Web 镜像

Web 使用 Node 22 + Nginx 多阶段构建：

1. build stage 执行 `npm ci` 和 `npm run build`。
2. runtime stage 使用 Nginx 服务 `dist/`。
3. `apps/web/nginx.conf` 将 `/api/` 反向代理到 `api:8080`，与前端默认 `VITE_API_BASE_URL=/api/v2` 对齐。

## 数据和端口

- PostgreSQL 数据持久化到 `suilearn-postgres-data` volume。
- API 宿主机端口默认 `${SUILEARN_API_PORT:-8080}`。
- Web 宿主机端口默认 `${SUILEARN_WEB_PORT:-5173}`。

## 风险

- 本机 Docker 权限不可用时无法执行 compose 验证。
- 后端未提供专用 health endpoint，因此 `api` 仅依赖 `postgres` healthcheck 后启动；Web 代理会在 API 启动完成后自然恢复。
