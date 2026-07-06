# 前向测试

用于验证 `suilearn-workflow` 是否真的改变 Agent 行为。前向测试不是格式校验；它要观察 Agent 在真实任务中是否触发 Skill、按需加载、遵守门禁并给出验证证据。

## 何时执行

- 修改 `SKILL.md` 的路由、不可协商项或加载表后。
- 新增或修改会影响行为的 reference 后。
- 用户指出 Agent 跳步、误加载、过度加载或绕过门禁后。
- 归档 Major workflow change 前，若工具和授权允许。

## 测试矩阵

至少覆盖三类任务：

| 场景 | 期望行为 |
| --- | --- |
| 只读 Explore | 只加载状态和示例相关 reference，不编辑文件。 |
| Workflow Skill 修改 | 加载 Leader、policy/tasks、Gate A，改 references/scripts，不把主 Skill 写长。 |
| 业务代码实现请求 | 停在 Spec 或读取批准任务，不绕过 active change，业务编辑前要求基线测试。 |
| 完成声明 | 加载 Verify/Gate C，先要证据，再给完成结论。 |

## 推荐 prompt 模板

将 Skill 和任务作为原始输入传给新 Agent，不泄露预期答案：

```text
Use the SuiLearn workflow skill at D:\SuiLearn\.agents\skills\suilearn-workflow.

Task: <真实用户请求>

Return:
- Which state you selected and why
- Which references you loaded
- What you would do next
- What you must not do
- What evidence is required before completion
```

## 通过标准

测试通过需要同时满足：

- 触发正确：Agent 识别应使用 `suilearn-workflow`。
- 加载正确：只读取当前场景需要的 reference。
- 门禁正确：编辑前要求角色、policy/tasks、base_ref 和文件范围。
- 禁止行为正确：不绕过 active change，不创建退役流程，不自证完成。
- 验证正确：完成前要求测试、diff stat、范围核对和审查闭环。

## 记录格式

```text
前向测试: <场景名>
任务输入: <原始请求>
期望行为: <摘要>
实际行为: <摘要>
结果: PASS / FAIL
修订: <如有>
复测: <如有>
```

## 当前限制

如果当前工具不允许派发 sub-agent，必须在验证记录中说明“未执行真实任务前向测试”，并列出替代验证。不能把格式检查等同于前向测试。
