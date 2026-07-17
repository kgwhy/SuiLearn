# Efficient batch workflow

## Purpose

Define risk-adaptive execution, verification, and evidence controls for SuiLearn change delivery.

## Requirements

# 风险自适应批次工作流增量规格

## ADDED Requirements

### Requirement: Build 必须按风险自适应批次执行

Leader MUST 按依赖关系、文件重叠和风险域划分批次。批次内任务串行实现并执行任务内 TDD 或明确复现步骤及局部测试；独立 Test、Spec Review 和 Code Review 默认在批次末各执行一次。Implementer 不得把“实现完成、待批次审查”声明为最终完成。P0/P1/P2 发现 MUST 按主工作流要求修复并复审、迁移到具名 follow-up change 或取得用户显式接受，不得因批次化而消失。

#### Scenario: 同一风险域的连续任务

- **WHEN** 多个任务共享实现上下文且不存在需要即时审查的风险事件
- **THEN** Leader 应复用批次上下文串行实现，并在批次末统一组织独立验证和审查

#### Scenario: 批次内发生高风险事件

- **WHEN** 修改契约、迁移、权限或安全边界、并发或事务语义、跨模块公共接口，或者局部测试出现无法解释的失败
- **THEN** Leader 必须暂停批次并立即组织相应 Test、Spec Review 或 Code Review，不得把风险推迟到批次末

### Requirement: 验证必须按影响范围分层

任务实现和审查修复 MUST 运行受影响的最小可信测试集。批次关闭必须运行批次验收命令；跨模块、共享配置或测试基础设施变更必须扩大回归范围。最终 Verify 仍必须运行 change 规定的全量验证，局部测试不得替代最终证据。

#### Scenario: 审查修复范围局部

- **WHEN** 修复只影响已识别的单一模块且依赖边界未改变
- **THEN** 修复轮只需运行失败测试及该模块相关回归，并在批次关闭时补跑批次验收命令

### Requirement: 批次证据复用必须由指纹保护

Leader MUST 以 `base_ref`、任务清单、受影响路径/当前 diff、验证命令和环境前提组成证据指纹。仅当整个指纹一致时，才可复用已完成的独立 Test、Spec Review 或 Code Review；任一项变化 MUST 使复用结果失效。最终 Verify MUST 按 change 的最终验证要求执行，不得由复用证据替代。

#### Scenario: 未变化的批次验收证据

- **WHEN** 当前批次的证据指纹与已记录的独立验收完全一致
- **THEN** Leader 可引用该验收结论而不重复运行同一全量命令，并记录复用的证据标识

#### Scenario: 证据指纹发生变化

- **WHEN** 任务范围、diff、验证命令或环境前提任一变化
- **THEN** Leader 必须使旧证据失效，并按受影响范围重新验证

### Requirement: 上下文和证据必须紧凑且可追溯

子 Agent MUST 默认只接收任务卡、相关规格摘录、允许/禁止路径、受影响符号或文件、当前 diff 和验证命令，不得重复传递完整对话或无关规格。成功验证记录必须包含命令、退出码、通过/失败计数和必要摘要；只有失败、间歇性问题或审计要求时才附关键原始日志片段或日志位置。

#### Scenario: 验证成功

- **WHEN** 命令退出码为 0 且无异常警告需要调查
- **THEN** Test Agent 返回紧凑证据，不回传完整重复日志

#### Scenario: 验证失败

- **WHEN** 命令非零退出或出现需要调查的异常
- **THEN** Test Agent 返回首个根因、关键原始输出、失败用例和可复现命令，并省略与根因无关的重复日志

### Requirement: 取消、日志和 worktree 必须受控

当用户缩小范围、暂停或取消时，Leader MUST 停止新派发、中断相关子 Agent、等待其写入/测试进程退出和文件锁释放，再处理取消任务独占的未验收改动。并发测试产生的报告 MUST 丢弃或隔离，后续验证必须从干净报告目录开始。完整验证命令与 `git diff <base_ref> --stat` MUST 执行并可追溯，但成功回传 MUST 只包含退出码、计数、摘要和记录位置。创建或首次复用 worktree 时，Leader MUST 使用 `git -c safe.directory=<absolute-worktree>`，且 MUST NOT 修改用户全局 Git 配置。

#### Scenario: 取消正在测试的任务

- **WHEN** 用户在子 Agent 写入或运行测试期间缩小范围或取消任务
- **THEN** Leader 等待相关进程退出、隔离旧报告并只撤销该任务独占的未验收改动，随后再验证保留范围

