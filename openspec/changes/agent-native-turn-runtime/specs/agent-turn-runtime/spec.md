## ADDED Requirements

### Requirement: TurnRuntimeService 必须管理回合生命周期
系统 MUST 通过 `TurnRuntimeService` 提供 `startTurn / subscribe / resumeFrom / cancelTurn / submitReply / checkActiveTurn`，并 MUST 在同一事务内持久化 turn、用户消息和首个 `turn_started` 事件。

#### Scenario: 创建并启动回合
- **WHEN** 调用 `startTurn` 且请求合法
- **THEN** turn 记录、session_message 与 seq=1 `turn_started` 原子落库，随后异步执行器获得已提交上下文

#### Scenario: 同一会话存在活动回合
- **WHEN** 同一 `sessionId` 已有 `RUNNING` 或 `WAITING_INPUT` 回合且再次启动
- **THEN** 系统返回 `AGENT_TURN_ACTIVE_CONFLICT`，不创建第二个活动回合

### Requirement: 事件必须按 turn 连续、幂等重放
系统 MUST 按 `(turnId, seq)` 唯一持久化事件，`seq` 从 1 单调递增不跳号；`subscribe`/`resumeFrom` 按 `afterSeq` 先重放持久化事件，再接入实时流。

#### Scenario: 断线续流
- **WHEN** 客户端断开后携带 `afterSeq=3` 重新订阅
- **THEN** 客户端先收到 seq 4..N 的持久化事件，再收到后续实时事件，且不重复收到 1..3

#### Scenario: 并发发布
- **WHEN** 多个执行线程同时发布同一 turn 事件
- **THEN** 每个事件获得唯一连续 seq，`turn_events` 不出现重复 `(turnId, seq)`

### Requirement: 实时推送必须有界且尽力而为
系统 MUST 为每个回合创建独立 `TurnEventBus`，内存队列容量固定；队列满时丢弃实时帧但保留已落库事件，并 MUST NOT 阻塞执行器或无限增长内存。

#### Scenario: 慢消费者
- **WHEN** WS subscriber 不消费且队列超过容量
- **THEN** publisher 不阻塞，内存占用不随事件数持续增长，后续 `resume_from` 仍能获得完整事件

### Requirement: 终态必须唯一且不可追加
系统 MUST 保证每个 turn 只发布一个终态（`done / cancelled / failed`）；终态后 `TurnEventBus` 拒绝任何新事件并唤醒同步等待者。

#### Scenario: 终态后写入
- **WHEN** 执行器在终态后尝试 publish
- **THEN** 事件被拒绝，turn 状态和 lastSeq 不变化

### Requirement: 取消必须停止后续副作用
系统 MUST 允许取消 `CREATED / RUNNING / WAITING_INPUT` 回合；取消后执行器不得再执行工具调用，且 MUST 发布 `cancelled` 终态。

#### Scenario: 运行中取消
- **WHEN** 执行器发布 progress 后收到 cancel
- **THEN** turn 状态变为 `CANCELLED`，后续 publish 被拒绝，同步等待者立即返回取消结果

### Requirement: submitReply 只允许 WAITING_INPUT
系统 MUST 只接受 `WAITING_INPUT` 回合的 `submit_user_reply`；其他状态返回 `AGENT_TURN_NOT_WAITING_FOR_INPUT`。

#### Scenario: 非等待回合投递回复
- **WHEN** 调用方对 `RUNNING` 或终态回合投递回复
- **THEN** 系统返回稳定错误，不改变回合状态或事件流

### Requirement: 孤儿恢复必须幂等
系统 MUST 在应用启动和 `check_active_turn` 时，把残留 `RUNNING` 回合标记为 `FAILED_ORPHANED`，并写入唯一终态事件；重复执行不得产生重复终态。

#### Scenario: 应用重启
- **WHEN** 数据库中仍有 `RUNNING` 回合
- **THEN** 这些回合被标记为 `FAILED_ORPHANED`，并产生 `failed` 终态事件；已终态回合不变

### Requirement: 阶段占位执行器不得伪造成功
本阶段默认 `TurnExecutor` MUST 只发布显式 unavailable 错误与 `failed` 终态，MUST NOT 生成回答、引用、练习、记忆或写入任何正式内容存储。

#### Scenario: 启动 Agent 回合
- **WHEN** 新运行时收到合法请求
- **THEN** 事件流以 `turn_started` 开始，以 `failed(TURN_EXECUTOR_UNAVAILABLE)` 结束，且无 result/done

### Requirement: 事件持久化必须 sanitized 且有界
系统 MUST 限制单条事件 payload UTF-8 编码后不超过 64 KiB；`metadata` 与日志 MUST NOT 包含用户正文、Prompt、原始模型输出或 API key。超限事件必须拒绝发布并转为错误终态。

#### Scenario: 超大 payload
- **WHEN** 执行器尝试发布超过 64 KiB 的事件
- **THEN** 系统拒绝该事件，记录低基数原因，并以 sanitized error 终止回合

### Requirement: 新运行时不得改变旧 Agent 路径
本 change MUST NOT 修改 `LearningAgentController`、`LearningAgentPort`、ReactAgent 拓扑、旧 REST 行为或 Redis/语义记忆路径；旧路径继续作为 change-3 前的现有入口。

#### Scenario: 旧路径回归
- **WHEN** 运行旧 Agent 相关既有测试
- **THEN** 所有既有旧路径断言保持通过（或失败仅来自既有环境原因，与本 change diff 无关）

### Requirement: 能力与工具协议只定义不注册
系统 MUST 提供 `Capability`、`CapabilityManifest`、`Tool`、`ToolDefinition`、`ToolResult` 的稳定 Java 接口/record，但本 change MUST NOT 注册 Spring bean 或让这些协议参与运行路径。

#### Scenario: 协议编译
- **WHEN** 编译 agent/runtime 与 agent/capability 类型
- **THEN** 接口签名与契约示例一致，且不依赖 Spring AI 类型
