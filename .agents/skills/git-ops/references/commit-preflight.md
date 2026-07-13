# 提交前预检

仅在用户要求暂存、提交或检查待提交内容时读取本文件。

## 步骤

1. 查看 `git diff --cached -- <path>`，确认暂存内容与提交边界。
2. 在仓库根目录运行：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .agents/skills/git-ops/scripts/scan-staged-secrets.ps1
```

3. 退出码为 `0` 时，才可继续准备提交信息和执行 `git commit`。

## 命中或失败时

- 退出码非 `0` 表示发现疑似凭据，或无法安全读取暂存快照；立即停止提交。
- 只报告扫描器给出的规则名和文件路径，绝不查看、复制或回显匹配值、匹配行或二进制内容。
- 默认处理方式是从暂存区域移除疑似凭据并轮换真实凭据。若认为是示例或测试数据，必须由用户明确确认后才能继续；不得自行忽略。

## 扫描范围

扫描器使用 `git diff --cached --name-only -z --diff-filter=ACMR` 取得待提交路径，并读取每个路径的完整暂存 blob。因此它能发现同一文件中未被本次 diff 触及的泄露内容。二进制或非 UTF-8 暂存文件会被安全阻断，不会输出其内容。
