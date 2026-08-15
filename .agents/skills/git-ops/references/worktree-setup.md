# Worktree 隔离

仅在需要并行处理多个任务或隔离变更时使用。

1. 从 `dev` 创建分支。
2. 创建 worktree 后先执行：

```bash
git -c safe.directory=<absolute-worktree> status --short
```

3. 在 worktree 内运行项目 setup 和基线测试，确认未改代码前测试通过。
4. 所有 Git 写操作沿用同一 `safe.directory` 参数，不修改用户全局 Git 配置。
