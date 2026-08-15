# 触发示例

## 只读探索

- 状态：Explore。
- 加载：`state-machine.md`。
- 不写文件；结束说明“只读探索，无文件变更”。

## 小型修复或文档调整

- 状态：Spec/Build。
- 等级：Light 或 Standard。
- Light 只需 `tasks.md`；Standard 需 `tasks.md + policy.md`，新功能再补 proposal。
- 完成后使用统一 `STATUS` 格式。

## 用户可见功能

- 状态：Spec -> Build。
- 等级：Standard；跨角色/契约/存储时 Major。
- 已批准且任务可独立提交时可选 L2 Auto。
- 必须有独立 Test；Standard 单角色低风险允许合并 Review。

## 工作流修改

- 等级：Major，除非只是措辞修正。
- 修改 references 或 scripts，不让 `SKILL.md` 变长。
- 完成后运行 `python3 scripts/check_workflow_skill.py` 和 `python3 scripts/check_suilearn_workflow.py --base-ref <ref>`。
