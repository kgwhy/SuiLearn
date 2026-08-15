# 验证

Status: passed.

## 命令

- `Get-FileHash -Algorithm SHA256 -Path gradle\wrapper\gradle-wrapper.jar`
- `C:\Users\youku\.gradle\wrapper\dists\gradle-8.7-bin\bhs2wmbdwecv87pi65oeuq5iu\gradle-8.7\bin\gradle.bat wrapper --gradle-version 8.7 --distribution-type bin --no-daemon`
- `.\gradlew.bat :app:testDebugUnitTest --no-daemon`
- `.\gradlew.bat :app:assembleDebug --no-daemon`
- `bash ./gradlew --version`
- `powershell -ExecutionPolicy Bypass -File scripts/check-suilearn-workflow.ps1 -BaseRef 5f5564f5bdf5a635e99b21f2f8536b25b215f9b6 -ClosingChange fix-android-ci-gradle-wrapper`
- `git diff 5f5564f5bdf5a635e99b21f2f8536b25b215f9b6 --stat`

## 结果

- wrapper JAR 已由 Gradle 8.7 官方 `wrapper` task 重生成，SHA-256 为 `cb0da6751c2b753a16ac168bb354870ebb1e162e9083f116729cec9c781156b8`。
- Android 单元测试通过: `BUILD SUCCESSFUL in 9s`。
- Android debug 构建通过: `BUILD SUCCESSFUL in 8s`。
- POSIX wrapper 本地验证未运行成功，原因是 Windows 环境中的 `bash` 入口依赖 WSL，但 WSL 未安装 Linux 发行版。
- SuiLearn workflow policy 检查通过: `SuiLearn Workflow policy check passed.`。

## 结论

Android CI 的 Gradle Wrapper 校验失败已修复；Node 20 deprecation 风险通过升级官方 actions 主版本处理，未引入 `ACTIONS_ALLOW_USE_UNSECURE_NODE_VERSION`。
