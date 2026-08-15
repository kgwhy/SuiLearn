# 完成分支

任务完成并验证后：

1. 运行相关测试和质量检查。
2. 检查 `git status --short --branch` 和 `git diff <base_ref> --stat`。
3. 用户选择处理方式：
   - 提交到当前分支；
   - 合并回 `dev`；
   - 推送并创建 PR；
   - 保留 worktree；
   - 丢弃未验收改动。
4. 合并顺序：`feature -> dev -> main`；`main` 只通过 PR 合并。
5. 清理 worktree 前确认没有未提交且需要保留的文件。
