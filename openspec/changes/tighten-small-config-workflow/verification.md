# 验证

状态：已通过。

## 命令结果

### `openspec validate tighten-small-config-workflow --strict`

结果：退出码 0。

```text
Change 'tighten-small-config-workflow' is valid
```

### `powershell -ExecutionPolicy Bypass -File scripts/check-suilearn-workflow.ps1 -BaseRef 5ec2fafe308afc71032ffa8284ff9c4c4abea3ad`

结果：退出码 0。

```text
Protected paths changed; OpenSpec change record found.
SuiLearn Workflow policy check passed.
```

说明：命令输出包含 `C:\Users\youku/.config/git/ignore: Permission denied` 和 LF/CRLF warning，不影响检查结果。

### `rg -n "配置 / 启动 / 集成|验收矩阵|同一用户问题链路|第一次返工|运行态验证|残留扫描" docs\development-workflow.md AGENTS.md openspec\changes\tighten-small-config-workflow`

结果：退出码 0。

关键输出：

```text
AGENTS.md:34:...验收矩阵、默认值语义、覆盖口、残留扫描项和运行态验证计划；同一用户问题链路必须复用一个 active change home。
AGENTS.md:58:...报告验收矩阵对应的运行态验证结果、旧默认值残留扫描结果...
AGENTS.md:87:- 第一次返工暴露语义偏差时，停止继续补丁式实现...
docs\development-workflow.md:96:- 同一用户问题链路只能有一个 active change home...
docs\development-workflow.md:102:配置 / 启动 / 集成类变更附加要求：
docs\development-workflow.md:106:- 验收矩阵...
docs\development-workflow.md:110:- 运行态验证计划...
docs\development-workflow.md:184:- 第一次返工暴露语义偏差时停止补丁式实现...
```

### `git diff 5ec2fafe308afc71032ffa8284ff9c4c4abea3ad --stat`

结果：退出码 0。

说明：输出包含当前工作树中既有 Docker、Web、后端改动；本 change 的意图范围是 `AGENTS.md`、`docs/development-workflow.md` 和 `openspec/changes/tighten-small-config-workflow/**`。
