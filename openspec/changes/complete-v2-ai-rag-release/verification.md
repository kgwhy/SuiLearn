# 验证

状态：已通过。

## 后端 Provider/检索验证

命令：

```powershell
mvn -f services/api/pom.xml test -q
```

原始输出摘要：

```text
Found @SpringBootConfiguration com.suilearn.api.SuiLearnApiApplication for test class com.suilearn.api.service.SuiLearnV2ServiceTest
Started SuiLearnV2ServiceTest ...
Exit code: 0
```

备注：

- 命令同时运行了 `OpenAiCompatibleAiProviderTest`，该测试使用本地 `HttpServer` 验证 `/v1/chat/completions` 和 `/v1/embeddings`。
- 命令同时运行了 `AiProviderStatusServiceTest`，验证 OpenAI-compatible 配置完整时状态可用，且不会暴露配置的 key。
- Maven 在 JDK 21 下输出 Mockito dynamic-agent 警告；测试通过。

## 全量验证

命令：

```powershell
mvn -f services/api/pom.xml test -q
.\gradlew.bat :app:testDebugUnitTest --no-daemon
npm --prefix apps/web run build
npm --prefix apps/web run test
powershell -ExecutionPolicy Bypass -File scripts/check-suilearn-workflow.ps1 -BaseRef 3f3fe48b8c940ed3be2d922e6739d143c7e122c1
powershell -ExecutionPolicy Bypass -File scripts/check-suilearn-workflow.ps1 -BaseRef 3f3fe48b8c940ed3be2d922e6739d143c7e122c1 -ClosingChange complete-v2-ai-rag-release
```

原始输出摘要：

```text
Backend: Started SuiLearnV2ServiceTest ... Exit code: 0
Android: BUILD SUCCESSFUL in 20s
Web build: built in 1.65s
Web test: tests 3, pass 3, fail 0
Workflow: Protected paths changed; active OpenSpec change found.
Workflow: SuiLearn Workflow policy check passed.
关闭工作流: Protected paths changed; active OpenSpec change found.
关闭工作流: SuiLearn Workflow policy check passed.
```

## 契约审查

- `contracts/openapi/suilearn-v2.yaml` 已包含答题提交端点和服务端答题记录 schema。
- 搜索和 embedding 文案描述的是服务端 embedding 相似度行为，不承诺某个具体向量数据库。

## 当前事实文档审查

- `docs/product-requirements.md` 和 `docs/architecture.md` 已将后续产品/架构变更指向 `openspec/changes/<change-name>/**`，不再指向退役 proposal 文件。

## 工作流关闭审查

- `AGENTS.md` 和 `docs/development-workflow.md` 现在要求 Major 变更记录最终 Review Agent 处置结果。
- `scripts/check-suilearn-workflow.ps1` 现在支持 `-ClosingChange`，并检查已完成变更产物中的陈旧 open/in-progress 状态、缺失的延期项记录和缺失的审查处置记录。
