# 验证

状态：已通过。

## 命令

- `docker compose config`
- `mvn -f services/api/pom.xml -DskipTests package -q`
- `npm --prefix apps/web run build`
- `npm --prefix apps/web test`
- `powershell -ExecutionPolicy Bypass -File scripts/check-suilearn-workflow.ps1 -BaseRef 2379f957b6e8c047aed8f09d4993a0627c70dcde`
- `docker compose build`
- `docker compose up --build --detach`

## 结果

- `docker compose config` 通过，解析出 `postgres`、`api`、`web` 三个服务。
- 后端 jar 构建通过：`mvn -f services/api/pom.xml -DskipTests package -q` 退出码 0。
- Web 测试通过：3 个测试通过，0 失败。
- Web 构建通过：`vite` production build 成功。
- 工作流检查通过。
- Docker 镜像构建在本机因 Docker/Buildx 权限阻塞：`CreateFile C:\Users\youku\.docker\buildx\instances: Access is denied.`
- Docker 启动在本机因 Docker API 权限阻塞：`open //./pipe/docker_engine: Access is denied.`

## 追加验证

- 用户实际运行发现 Web Nginx 启动时报 `host not found in upstream "api"`。
- 已修复为 Docker DNS 运行时解析：`resolver 127.0.0.11 valid=10s ipv6=off` + 变量形式 `proxy_pass`。
- 修复后重新运行 `docker compose config`、Web 构建和工作流检查，均通过。

## 结论

完整服务容器化配置已完成。剩余未能本机实跑 `docker compose build/up` 的原因是当前机器 Docker 权限，不是 compose 解析或非 Docker 构建链路问题。
