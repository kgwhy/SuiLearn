## 1. 工作流事实文档

- [x] 1.1 Owner: Leader Agent。补充配置 / 启动 / 集成类变更附加门禁，允许文件：`docs/development-workflow.md`；禁止文件：业务代码、契约、产品事实文档；测试命令：人工核对、工作流检查器；审查重点：规则具体可执行，不引入第二套流程。
- [x] 1.2 Owner: Leader Agent。同步 `AGENTS.md` Gate A / Gate C 提示，允许文件：`AGENTS.md`；禁止文件：业务代码、契约、产品事实文档；测试命令：人工核对、工作流检查器；审查重点：未来 Agent 在修改前和完成前能看到关键门禁。

## 2. 变更产物与验证

- [x] 2.1 Owner: Leader Agent。记录本 change 的 policy 和 verification，允许文件：`openspec/changes/tighten-small-config-workflow/**`；测试命令：`openspec validate tighten-small-config-workflow --strict`、`powershell -ExecutionPolicy Bypass -File scripts/check-suilearn-workflow.ps1 -BaseRef 5ec2fafe308afc71032ffa8284ff9c4c4abea3ad`；审查重点：该 change 不与 Docker active change 混淆。
