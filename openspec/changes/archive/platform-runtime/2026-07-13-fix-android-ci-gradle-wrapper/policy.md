# 策略记录

## 基本信息

- Change: `fix-android-ci-gradle-wrapper`
- 级别: Tiny Fast Track
- base_ref: `5f5564f5bdf5a635e99b21f2f8536b25b215f9b6`
- 当前阶段: Verify
- 执行模式: serial
- 文件锁: 未发现 `.agents/locks` 目录，未发现可见锁冲突。

## 允许修改文件

- `openspec/changes/fix-android-ci-gradle-wrapper/tasks.md`
- `openspec/changes/fix-android-ci-gradle-wrapper/policy.md`
- `openspec/changes/fix-android-ci-gradle-wrapper/verification.md`
- `openspec/changes/fix-android-ci-gradle-wrapper/archive.md`
- `.github/workflows/ci.yml`
- `gradlew`
- `gradlew.bat`
- `gradle/wrapper/gradle-wrapper.jar`
- `gradle/wrapper/gradle-wrapper.properties`

## 禁止修改文件

- `apps/android/**` 业务代码
- `services/api/**`
- `apps/web/**`
- `contracts/**`
- `docs/product-requirements.md`
- `docs/architecture.md`
- `docs/tech-selection.md`
- `docs/proposals/**`
- `docs/superpowers/**`

## 基线测试

- `.\gradlew.bat :app:testDebugUnitTest --no-daemon`: 跳过。当前 wrapper JAR 已被 CI 判定为 unknown，直接执行该二进制不适合作为安全基线。

## 审查重点

- wrapper JAR 必须是 Gradle 8.7 官方 wrapper 启动 JAR，而不是 `gradle-wrapper-8.7.jar` 插件 JAR。
- CI action 升级不得改变 job 拓扑、测试命令或产品行为。
- 不引入 `ACTIONS_ALLOW_USE_UNSECURE_NODE_VERSION`。
