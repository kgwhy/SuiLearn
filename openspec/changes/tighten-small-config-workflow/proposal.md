## 背景

本次 Docker/本地混合启动改动代码量不大，但多次返工暴露出工作流缺口：小型配置变更没有强制先写清真实验收语义，静态配置验证替代了运行态验证，且同一问题链路的后续修复容易散落到多个 active change 中。

需要把这些教训沉淀到 SuiLearn 工作流中，避免后续端口、CORS、Compose、代理、环境变量、CI wrapper 等“小改动”继续以补丁方式返工。

## 变更内容

- 在工作流中新增“配置 / 启动 / 集成类变更附加门禁”。
- 要求这类变更即使很小，也必须在 Spec 阶段写验收矩阵、默认值策略和残留扫描项。
- 要求同一用户问题链路复用一个 active change home，后续 502、CORS、端口等同根因修复不得散落到无关 change。
- 要求 Verify 阶段包含运行态边界验证，不能只依赖 `docker compose config`、构建或静态扫描。
- 要求第一次返工暴露语义偏差时先回到 Spec 更新验收标准，再继续 Build。

非目标：

- 不修改产品需求、架构、技术选型、契约或业务代码。
- 不改变现有 active Docker / Web 功能变更的实现内容。
- 不新增自动化脚本；本次先把门禁写入事实工作流。

## 验收标准

- `docs/development-workflow.md` 明确记录配置类变更的验收矩阵、单一 change home、运行态验证和返工回 Spec 规则。
- `AGENTS.md` 的 Gate A / Gate C 能提示未来 Agent 执行这些规则。
- `openspec validate tighten-small-config-workflow --strict` 通过。
- 工作流检查器通过。
