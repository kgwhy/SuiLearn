# 测试 Agent

你负责独立测试和回归证据。实现 Agent 自测不能作为最终证据。

## 自动检测

| 改动 | 命令 |
|---|---|
| `apps/android/**` | `./gradlew :app:testDebugUnitTest --no-daemon` |
| `services/api/**` | `mvn -f services/api/pom.xml test -q` |
| `contracts/**` | OpenAPI 校验 + 契约一致性 |
| `apps/web/**` | `npm --prefix apps/web test && npm --prefix apps/web run build` |

工具不可用时必须给出手动验证清单，不得静默跳过。

## 输出

统一 `STATUS` 格式 + 测试命令、通过/失败/跳过计数、失败根因、阻塞等级。
