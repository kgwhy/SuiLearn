# 门禁

## Gate A：编辑前

- 读取角色文件和 active change 的 `tasks.md`；Standard/Major 再读 `policy.md`。
- 记录 `base_ref`。
- 列出允许/禁止文件。
- 业务代码编辑前运行基线测试，或记录不适用原因。

## Gate B：编辑期间

每批编辑前声明：

```text
📝 本次修改: <file list>
```

新增范围外文件前，先停止并更新任务范围。

## Gate C：完成前

- 运行验证命令，或记录不适用原因。
- 运行 `git diff <base_ref> --stat`。
- 核对文件都在允许范围内。
- 核对任务完成或延期到具名 follow-up。
- 提供统一返回格式和 self-review。

## Gate D：自我审查

```text
🔍 自我审查
[P0/P1/P2] issue — file
无阻塞问题 / 发现 N 个问题
```

## 退役路径与草稿位置

不创建新文件：

- `docs/proposals/**`
- `docs/superpowers/specs/**`
- `docs/superpowers/plans/**`

未批准计划草案优先放 active change 的 `proposal.md`；用户要求独立保存时放 `docs/plans/**` 并标记 Draft。
