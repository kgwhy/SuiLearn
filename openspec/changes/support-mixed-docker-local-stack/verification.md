# 验证

状态：已通过。

## 命令结果

### `openspec validate support-mixed-docker-local-stack --strict`

```text
Change 'support-mixed-docker-local-stack' is valid
```

### `docker compose config --services`

```text
api
postgres
web
```

说明：根 Compose 只保留一个数据库 service、一个后端 service、一个 Web service。

### `docker compose config`

结果：退出码 0。

关键输出：

```text
api:
  image: suilearn-api:local
  environment:
    SUILEARN_DB_URL: jdbc:postgresql://host.docker.internal:5432/suilearn
web:
  image: suilearn-web:local
  environment:
    SUILEARN_API_UPSTREAM: http://host.docker.internal:8080
  ports:
    - published: "5174"
```

说明：默认全栈 Compose 使用同一组 `postgres` / `api` / `web` service；Web 默认发布到宿主机 `5174`，并经宿主机 `8080` 访问后端；API 默认经宿主机 `5432` 访问数据库。因此 Docker 服务和本地服务在默认端口下无需切换环境变量。

### `rg -n "5173|5174|suilearn.web.cors.allowed-origins|SUILEARN_WEB_CORS_ALLOWED_ORIGINS" services\api\src\main compose.yml README.md .env.example openspec\changes\support-mixed-docker-local-stack`

结果：退出码 0。

关键输出：

```text
compose.yml:30:      SUILEARN_WEB_CORS_ALLOWED_ORIGINS: http://localhost:${SUILEARN_WEB_PORT:-5174},http://127.0.0.1:${SUILEARN_WEB_PORT:-5174}
services\api\src\main\resources\application.properties:10:suilearn.web.cors.allowed-origins=${SUILEARN_WEB_CORS_ALLOWED_ORIGINS:http://localhost:5174,http://127.0.0.1:5174}
services\api\src\main\java\com\suilearn\api\config\WebCorsConfig.java:23:        @Value("${suilearn.web.cors.allowed-origins:http://localhost:5174,http://127.0.0.1:5174}")
```

说明：Docker API 和本地 Maven API 的默认 CORS 白名单都已包含 `localhost:5174`。

### `rg -n "5173" .env.example README.md compose.yml apps\web\vite.config.ts`

结果：退出码 1，符合预期。

关键输出：

```text
<no matches>
```

说明：真实运行配置和 README 中不再保留旧默认端口 `5173`。

### `rg -n "5174|SUILEARN_WEB_PORT" .env.example README.md compose.yml apps\web\vite.config.ts`

结果：退出码 0。

关键输出：

```text
compose.yml:30:      SUILEARN_WEB_CORS_ALLOWED_ORIGINS: http://localhost:${SUILEARN_WEB_PORT:-5174},http://127.0.0.1:${SUILEARN_WEB_PORT:-5174}
compose.yml:65:      - "${SUILEARN_WEB_PORT:-5174}:80"
apps\web\vite.config.ts:17:      port: 5174,
README.md:82:| Web 工作台 | http://localhost:5174 |
README.md:152:如果默认 Web 端口 `5174` 已被占用，可在 `.env` 中设置其他端口，例如 `SUILEARN_WEB_PORT=5175`。
README.md:224:Vite 开发服务默认监听 `http://localhost:5174`，并把 `/api` 代理到 `http://localhost:8080`。构建 Web 应用：
.env.example:4:SUILEARN_WEB_PORT=5174
```

说明：Docker Web 和本地 Vite 的默认端口均为 `5174`。

### `docker compose up --build -d --force-recreate web`

结果：退出码 0。

关键输出：

```text
Image suilearn-web:local Built
Container suilearn-web-1 Recreated
Container suilearn-web-1 Started
```

说明：当前运行中的 Web 容器已使用新镜像和新默认上游。

### `docker inspect --format '{{range .Config.Env}}{{println .}}{{end}}' suilearn-web-1`

结果：退出码 0。

关键输出：

```text
SUILEARN_API_UPSTREAM=http://host.docker.internal:8080
```

说明：运行中的 Web 容器默认代理到宿主机 `8080`，兼容本地 Maven 后端和 Docker API 暴露端口。

### `curl.exe -i http://localhost:8080/api/v2/ai/provider-status`

结果：退出码 0。

关键输出：

```text
HTTP/1.1 200
```

说明：宿主机本地后端正在 `8080` 提供 API。

### `curl.exe -i http://localhost:5174/api/v2/ai/provider-status`

结果：退出码 0。

关键输出：

```text
HTTP/1.1 200
Server: nginx/1.27.5
```

说明：Docker Web 经 Nginx 代理到宿主机后端成功，原先的 502 已复现并修复。

### `docker compose up -d --build api web` with `SUILEARN_WEB_PORT=5174`

结果：退出码 0。

关键输出：

```text
api, web containers rebuilt and restarted
```

说明：与资料导入/知识点提取联调时使用同一组 `api` / `web` service 和同一个 Web 端口 `5174`，未创建第二套前端或后端 service。

### `POST http://localhost:5174/api/v2/materials/{materialId}/extract-knowledge-points`

结果：退出码 0。

关键输出：

```text
HTTP 200
```

说明：资料导入后的知识点提取请求可通过 Docker Web 端口进入后端；该运行环境验证统一记录在本 Docker/本地混合启动 change 中。

### `Test-Path services\api\compose.local.yml`

结果：退出码 0。

关键输出：

```text
False
```

说明：旧本地数据库 Compose 入口已删除，根目录 `compose.yml` 是唯一 Compose 入口；单独启动数据库使用 `docker compose up -d postgres`。

### `npm --prefix apps/web run build`

结果：退出码 0。

关键输出：

```text
> suilearn-web@0.1.0 build
> tsc -b && vite build

✓ 1591 modules transformed.
✓ built in 1.63s
```

### `mvn -f services/api/pom.xml -Dtest=WebCorsConfigTest test -q`

结果：退出码 0。

关键输出：

```text
Started WebCorsConfigTest
```

说明：新增聚焦测试验证默认 CORS 配置允许 `Origin: http://localhost:5174` 的 POST 预检请求。

### `mvn -f services/api/pom.xml test -q`

结果：退出码 0。

关键输出：

```text
Started WebCorsConfigTest
Started SuiLearnV2ServiceTest
```

说明：后端测试通过。测试输出包含 `PostgresChunkSearchIndexMigrationTest` 故意触发的 `RuntimeException: boom` 日志，但 Maven 退出码为 0，属于既有测试场景。

### `docker compose build web`

结果：退出码 0。

关键输出：

```text
naming to docker.io/library/suilearn-web:local done
Image suilearn-web:local Built
```

### `docker compose build api`

结果：退出码 0。

关键输出：

```text
naming to docker.io/library/suilearn-api:local done
Image suilearn-api:local Built
```

### `docker run --rm suilearn-web:local nginx -T`

结果：退出码 0。

关键输出：

```text
set $api_upstream http://host.docker.internal:8080;
nginx: configuration file /etc/nginx/nginx.conf test is successful
```

### `docker run --rm -e SUILEARN_API_UPSTREAM=http://host.docker.internal:8080 suilearn-web:local nginx -T`

结果：退出码 0。

关键输出：

```text
set $api_upstream http://host.docker.internal:8080;
nginx: configuration file /etc/nginx/nginx.conf test is successful
```

## 注意事项

- Docker/Compose 命令输出包含 `C:\Users\youku\.docker\config.json: Access is denied` warning，但命令退出码为 0，配置解析和构建均完成。
- Git 命令输出包含 `C:\Users\youku/.config/git/ignore: Permission denied` warning，不影响本次文件范围检查。
