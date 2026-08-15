# 验证与关闭

## 必需证据

- 测试/构建/检查命令输出，或精确的不适用原因。
- `git diff <base_ref> --stat`。
- 文件范围核对。
- 任务完成或延期到具名 follow-up 的核对。
- P0/P1 修复并复审；P2 修复、延期或用户接受。

## 关闭检查

- tasks 没有 open/in progress/pending 状态。
- 批准状态存在且未撤回。
- Major 的 `archive.md` 不得停留在 open。
- 当前事实已同步或记录 `not affected`。

## 完成格式

```text
STATUS: DONE / DONE_WITH_CONCERNS / NEEDS_CONTEXT / BLOCKED
Changed files:
Tests:
Summary:
Assumptions:
Blockers:
```
