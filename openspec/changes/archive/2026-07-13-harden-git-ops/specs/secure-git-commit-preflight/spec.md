## ADDED Requirements

### Requirement: 已暂存提交快照必须经过凭据预检
在执行 `git commit` 前，`git-ops` SHALL 在主入口明确要求运行 `scan-staged-secrets.ps1`，检查待提交代码中的 key、secret 或 access token；该扫描器对 `git diff --cached --name-only -z --diff-filter=ACMR` 返回的每个文件的完整暂存 blob 进行预检。预检 MUST 覆盖新增、复制、修改及重命名后的待提交文件，且不得仅检查 diff hunks。

#### Scenario: 修改文件中存在未触及的泄露 token
- **WHEN** 待提交文件的暂存 blob 含有疑似 token，但该行不在本次 diff hunk 中
- **THEN** 预检 MUST 将该文件标记为命中并阻止提交

### Requirement: 命中结果不得回显凭据
扫描器和 `git-ops` 输出 MUST 仅报告规则标识、文件路径和安全的下一步，不得打印、记录或在交接中复述疑似凭据的匹配值或匹配行。

#### Scenario: 扫描器发现 access token
- **WHEN** 暂存 blob 匹配 access token 规则
- **THEN** 输出 MUST 不包含该 token 的任何子串

### Requirement: 可疑凭据必须阻止提交并交由用户决定
扫描器发现疑似真实 key、secret 或 access token 时 MUST 以非零状态结束，`git-ops` MUST 停止提交。任何继续操作、忽略模式或将其视为测试数据的例外 MUST 由用户明确确认；Skill 不得静默跳过。

#### Scenario: 预检命中疑似 API key
- **WHEN** 提交前扫描返回非零状态
- **THEN** `git-ops` MUST 不运行 `git commit`，并请求用户确认后续处置

### Requirement: 无命中才允许进入提交
当扫描器以零状态完成时，`git-ops` SHALL 继续执行既有的有意暂存和提交流程。

#### Scenario: 已暂存内容不含疑似凭据
- **WHEN** 预检成功完成
- **THEN** `git-ops` MUST 将提交流程推进到提交信息准备阶段
