# 前向测试

只用于验证 workflow skill 的**行为变化**。不是格式检查，也不是每次归档的必选项。

## 何时执行

- 修改 `SKILL.md` 路由、不可协商项或加载表后。
- 修改 reference 中会改变 Agent 行为的规则后。
- 用户报告 Agent 跳步、误加载或绕过门禁后。

## 通过标准

- Agent 识别并使用 `suilearn-workflow`。
- 只加载当前场景需要的 reference。
- 编辑前要求角色、change 产物、base_ref 和文件范围。
- 不绕过 active change，不自证完成。
- 完成前要求测试、diff stat 和范围核对。

## 单 Agent 环境

无法派发真实新 Agent 时，用干净会话跑同一 prompt 并记录 `forward_test_mode: single-session`。不能把格式检查冒充为前向测试。
