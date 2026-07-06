# 门禁

## Gate A：编辑前

- 加载活动角色文件。
- 将变更判定为 Tiny、Normal 或 Major。
- 记录 `base_ref`。
- 检查 lock/worktree 要求。
- 在需要 active change home 时确认其存在。
- 列出计划修改文件，并逐项对照角色规则和 `policy.md` 范围。
- 业务代码编辑前运行基线测试。
- 对纯文档、纯工作流或只读审查，记录模块测试不适用的原因。

对于配置、启动、集成、端口、CORS、Docker/Compose、反向代理、环境变量、
CI wrapper、数据库或服务地址变更，进入 Build 前必须写清验收矩阵、默认值语义、
覆盖口、残留扫描项和运行态验证计划。

## Gate B：编辑期间

每个编辑批次前声明：

```text
📝 本次修改: <file list>
```

如果需要新增文件，先停止并声明扩展范围，再编辑。

## Gate C：完成前

- 运行验证，或说明为什么不适用。
- 运行 `git diff <base_ref> --stat`。
- 对照允许范围检查已修改文件。
- 声明关闭前检查 active change 产物中是否有陈旧的 `In progress`、`Status: open`
  或无 Owner 的 `pending` 状态。
- 对 Major 或跨角色工作，记录最终审查闭环。
- 提供 reviewer-style 自审。

## Gate D：自我审查

以 reviewer-style 扫描收尾：

```text
🔍 自我审查
[P0/P1/P2] issue — file
无阻塞问题 / 发现 N 个问题
```

## 退役路径

不要在以下路径下创建新文件：

- `docs/proposals/**`
- `docs/superpowers/specs/**`
- `docs/superpowers/plans/**`
