## 1. Web 容器上游配置

- [x] 1.1 Owner: Web Frontend Agent。将 Web Nginx 配置改为运行时模板，允许文件：`apps/web/Dockerfile`、`apps/web/nginx.conf.template`；禁止文件：`apps/web/src/**`、`services/api/src/**`；测试命令：`npm --prefix apps/web run build`、Docker Web 镜像构建；审查重点：默认上游为 `http://host.docker.internal:8080`，兼容 Docker API 和本地 Maven 后端，不影响静态资源托管。

## 2. 统一 Compose 服务模型

- [x] 2.1 Owner: Leader/Architect 协调。移除额外 `web-local-backend` service，让 `postgres`、`api`、`web` 成为唯一组件服务，允许文件：`compose.yml`；禁止文件：`apps/web/src/**`、`services/api/src/**`、`contracts/**`；测试命令：`docker compose config`；审查重点：前后端各只有一个 service/image，服务可按名称单独启动。
- [x] 2.2 Owner: Leader/Server Backend 协调。让 Docker API 默认经宿主机发布端口连接数据库，并删除旧本地数据库 Compose 文件，允许文件：`compose.yml`、`services/api/compose.local.yml`、`services/api/config/local.properties.example`；禁止文件：`contracts/**`；测试命令：`docker compose config`、确认旧 Compose 文件不存在；审查重点：默认兼容 Docker PostgreSQL 和本地 PostgreSQL，仓库只保留一个 Compose 入口。

## 3. 文档与环境变量

- [x] 3.1 Owner: Leader/Architect 协调。更新 `.env.example` 和 README 组合启动说明，允许文件：`.env.example`、`README.md`；禁止文件：`docs/proposals/**`、`docs/superpowers/**`；测试命令：人工核对命令与 Compose 变量一致；审查重点：说明 Docker/本地组合、端口占用、环境变量清理和旧容器清理边界。
- [x] 3.1a Owner: Leader/Architect 协调。更新当前事实文档中的开发数据库入口，允许文件：`docs/tech-selection.md`；禁止文件：`docs/proposals/**`、`docs/superpowers/**`；测试命令：人工核对不再指向 `services/api/compose.local.yml`；审查重点：当前事实文档只指向根 `compose.yml`。
- [x] 3.2 Owner: Web Frontend Agent。将本地 Vite 默认端口与 Compose Web 默认端口统一为 `5174`，允许文件：`apps/web/vite.config.ts`；禁止文件：`apps/web/src/**`；测试命令：`npm --prefix apps/web run build`；审查重点：Docker Web 和本地 Vite 默认访问地址一致。
- [x] 3.3 Owner: Server Backend Agent。将本地 Maven 后端默认 CORS 白名单与 Web 默认端口统一为 `5174`，允许文件：`services/api/src/main/resources/application.properties`、`services/api/src/main/java/com/suilearn/api/config/WebCorsConfig.java`、`services/api/src/test/java/com/suilearn/api/config/WebCorsConfigTest.java`；禁止文件：其他 `services/api/src/**`；测试命令：`mvn -f services/api/pom.xml test -q`、带 `Origin: http://localhost:5174` 的 API 调用；审查重点：本地后端不需要手动设置 `SUILEARN_WEB_CORS_ALLOWED_ORIGINS` 即可接受 Web 请求。
- [x] 3.4 Owner: Leader。运行最终验证并更新任务状态，允许文件：`openspec/changes/support-mixed-docker-local-stack/tasks.md`、`openspec/changes/support-mixed-docker-local-stack/verification.md`；禁止文件：其他 active change；测试命令：最终验证命令；审查重点：任务状态与实际验证一致。
