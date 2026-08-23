# Agent Usage 与可观测（change-6a）

## Why

change-3 已采集 usage 但未折算成本或做白名单日志。6b 再做客户端切换与完整 TurnResult 信封扩展。

## What Changes

- 新增 `UsageTracker`：按模型价格表累计 prompt/completion token 与 cost。
- AgentLoop 接入 tracker，RESULT metadata 增加 usageCostUsd、promptTokens、completionTokens。
- 新增白名单诊断：只记录 usage/outcome/errorCode，不记录正文/Prompt/原始输出/key。

## Acceptance Criteria

- fake LLM 下 usage cost 与价格表一致；缺价格表使用默认价。
- 日志/指标不含敏感正文。
- 既有 71 定向测试通过。
