# Verification: search_knowledge 返回可读证据指针

Status: Verified（2026-08-28，静态验证）

## 环境限制

当前执行环境无 Java/Maven，不能运行后端测试。已新增/更新测试断言，待用户在 Windows 本地构建后执行。

## 静态检查

- `SearchKnowledgeTool` content 现在包含每个指针的 stableId、sourceRef、relevance、excerpt。
- `AgentDeclarativeToolsTest` 断言 content 包含 `stableId=stable-1` 与 `sourceRef=ref-1`。
- `git diff --check` 通过。

## 待执行命令

```bash
mvn -f services/api/pom.xml -Dtest=AgentDeclarativeToolsTest test
```
