# 验证

状态：已通过

## 命令

- `mvn -f services/api/pom.xml -Dtest=SuiLearnV2ServiceTest test -q`
- `npm --prefix apps/web run build`
- `mvn -f services/api/pom.xml test -q`
- `git diff 46472606357a68dfb92966851213228fbbaa2541 --stat`
- `powershell -ExecutionPolicy Bypass -File scripts/check-suilearn-workflow.ps1 -BaseRef 46472606357a68dfb92966851213228fbbaa2541`

## 结果

最终验证通过。`mvn` 完整后端测试输出中包含一个测试刻意触发的 `PostgresChunkSearchIndexMigration` warning stack trace，命令退出码为 0。
