# 策略

## 等级

Normal。

原因：本变更跨 `apps/web/**`、根 `compose.yml`、`.env.example`、README 和少量开发配置文档，影响开发启动方式，但不改变产品行为、API 契约、数据库模型或业务代码。

## 角色与 Owner

- Web Frontend Agent：`apps/web/Dockerfile`、`apps/web/nginx.conf.template`、`apps/web/vite.config.ts`。
- Server Backend Agent：`services/api/src/main/resources/application.properties`、`services/api/src/main/java/com/suilearn/api/config/WebCorsConfig.java`、`services/api/src/test/java/com/suilearn/api/config/WebCorsConfigTest.java`、`services/api/config/local.properties.example`。
- Leader/Architect 协调：根 `compose.yml`、`.env.example`、README 运行说明、`docs/tech-selection.md` 开发数据库入口说明、OpenSpec change 产物。

## base_ref

`5ec2fafe308afc71032ffa8284ff9c4c4abea3ad`

## 文件锁

串行执行，不创建持久锁。

## 允许修改文件

- `apps/web/Dockerfile`
- `apps/web/nginx.conf.template`
- `apps/web/vite.config.ts`
- `compose.yml`
- `.env.example`
- `services/api/compose.local.yml`（删除旧入口）
- `services/api/config/local.properties.example`
- `services/api/src/main/resources/application.properties`
- `services/api/src/main/java/com/suilearn/api/config/WebCorsConfig.java`
- `services/api/src/test/java/com/suilearn/api/config/WebCorsConfigTest.java`
- `README.md`
- `docs/tech-selection.md`
- `openspec/changes/support-mixed-docker-local-stack/**`

## 禁止修改文件

- `apps/web/src/**`
- `services/api/src/**`，但允许列表中明确列出的 CORS 配置和测试文件除外。
- `contracts/**`
- `docs/proposals/**`
- `docs/superpowers/**`
- 其他 active change 目录

## 基线与验证

基线：

- `docker compose config`
- `docker compose config --services`

最终验证：

- `docker compose config`
- `docker compose config --services`
- 确认仓库不再保留 `services/api/compose.local.yml`
- `npm --prefix apps/web run build`
- 如 Docker 权限允许，构建 `web` 和 `api` 镜像，并用临时 `docker run` 验证 Nginx 模板展开。

模块测试说明：

- 本变更不修改业务逻辑。TDD 对配置默认值属于例外项，本任务以 Compose 配置解析、Web 构建、Docker 镜像构建、后端测试和带 `Origin: http://localhost:5174` 的 API 调用作为验证。
