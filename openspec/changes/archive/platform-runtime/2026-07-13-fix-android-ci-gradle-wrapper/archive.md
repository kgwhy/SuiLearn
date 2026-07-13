# 归档

Status: passed

## 变更名称

修复 Android CI Gradle Wrapper 校验失败。

## 实现引用

working tree:

- `.github/workflows/ci.yml`
- `gradlew`
- `gradlew.bat`
- `gradle/wrapper/gradle-wrapper.jar`
- `openspec/changes/fix-android-ci-gradle-wrapper/tasks.md`
- `openspec/changes/fix-android-ci-gradle-wrapper/policy.md`
- `openspec/changes/fix-android-ci-gradle-wrapper/verification.md`
- `openspec/changes/fix-android-ci-gradle-wrapper/archive.md`

## 验证摘要

- `gradle/wrapper/gradle-wrapper.jar` 从误提交的 `gradle-wrapper-8.7.jar` 插件 JAR 修复为官方 wrapper 启动 JAR。
- Windows wrapper 脚本由 Gradle 8.7 官方 `wrapper` task 重生成，移除了会与正确 wrapper JAR 冲突的多 JAR classpath。
- Android 单元测试和 debug 构建已通过。
- 本机没有可用 WSL Linux 发行版，因此 POSIX `gradlew` 启动未能在本地 Windows 环境验证；CI Ubuntu runner 会直接使用 Linux shell。

## 当前事实同步

本变更不修改产品、架构、技术选型、契约或业务代码事实文档。

## 延期项

Deferred items: none

## 最终审查

无 P0/P1/P2 阻塞问题。
