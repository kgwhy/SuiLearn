# Proposal：容器化完整服务

## 做什么

为 SuiLearn 后端和 Web 工作台新增容器镜像构建配置，并提供根目录 `compose.yml`，使开发者可以通过一条 Docker Compose 命令启动 PostgreSQL、API 服务和 Web 服务。

## 为什么做

当前 `services/api/compose.local.yml` 只启动 PostgreSQL。后端测试和本地联调依赖数据库，Web 也需要稳定代理到 API。缺少完整编排会导致本地复现 CI 和端到端联调成本偏高。

## 范围

- 新增后端 Spring Boot Dockerfile。
- 新增 Web 静态构建 Dockerfile 和 Nginx 反向代理配置。
- 新增根目录 `compose.yml`，编排 `postgres`、`api`、`web` 三个服务。
- 新增必要的 `.dockerignore`，避免把构建产物、依赖目录和本地配置打进镜像。

## 非目标

- 不修改后端业务代码、Web UI、API 契约或数据库模型。
- 不引入 Redis、队列、独立网关或额外生产基础设施。
- 不处理本机 Docker 权限问题；Compose 文件提供标准启动入口，但执行仍依赖用户环境可访问 Docker。

## 验收标准

- 根目录执行 `docker compose up --build` 可构建并启动完整服务。
- Web 容器通过 Nginx 服务静态资源，并将 `/api/` 代理到 API 容器。
- API 容器使用 compose 内部 PostgreSQL 连接，不依赖宿主机 `localhost:5432`。
- 本地可用验证命令通过；Docker 不可用时记录准确原因。
