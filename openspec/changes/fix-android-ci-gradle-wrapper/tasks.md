# 修复 Android CI Gradle Wrapper 校验失败

## 任务说明

- Owner: Leader 协调，Android CI/build-infra 修复。
- 级别: Tiny Fast Track。
- 问题: Android CI 在 `gradle/actions/setup-gradle@v4` 阶段校验 `gradle/wrapper/gradle-wrapper.jar`，发现 hash `4f63236158ec4c97799ba1e592e53e54a635cec7b618a2fa82e8655a0d53bc4f` 不属于官方 Gradle Wrapper JAR。
- 根因: 仓库提交的是 Gradle 8.7 分发包中的 `lib/plugins/gradle-wrapper-8.7.jar`，该插件 JAR 内部才包含真正的 `gradle-wrapper.jar`，因此 setup-gradle wrapper validation 判定为 unknown。Windows `gradlew.bat` 还使用了非标准 wrapper classpath，直接替换 JAR 会与旁路 JAR 产生类冲突。
- 非目标: 不修改 Android 业务代码、产品范围、后端、Web 或契约。

## 待办

- [x] 复现并记录当前 wrapper JAR hash 与 JAR 内容。
- [x] 使用 Gradle 8.7 官方 `wrapper` task 重生成 wrapper JAR 与启动脚本。
- [x] 将 GitHub Actions 中的官方 actions 升到 Node 24 兼容主版本，移除 Node 20 deprecation 风险。
- [x] 运行 Android 单元测试和 debug 构建验证。
- [x] 运行 workflow policy 检查和 diff 范围核对。

## 验证记录

- `gradle/wrapper/gradle-wrapper.jar` SHA-256: `cb0da6751c2b753a16ac168bb354870ebb1e162e9083f116729cec9c781156b8`
- `.\gradlew.bat :app:testDebugUnitTest --no-daemon`: 通过。
- `.\gradlew.bat :app:assembleDebug --no-daemon`: 通过。
- `bash ./gradlew --version`: 本机 WSL 未安装 Linux 发行版，无法在本地 Windows 环境验证 POSIX 脚本。

## 验证命令

- `.\gradlew.bat :app:testDebugUnitTest --no-daemon`
- `.\gradlew.bat :app:assembleDebug --no-daemon`
- `powershell -ExecutionPolicy Bypass -File scripts/check-suilearn-workflow.ps1 -BaseRef 5f5564f5bdf5a635e99b21f2f8536b25b215f9b6 -ClosingChange fix-android-ci-gradle-wrapper`
- `git diff 5f5564f5bdf5a635e99b21f2f8536b25b215f9b6 --stat`
