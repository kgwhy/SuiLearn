# Conventional Commits 参考

来源：[Conventional Commits 1.0.0](https://www.conventionalcommits.org/zh-hans/v1.0.0/)

## 基本格式

```text
<type>[optional scope][optional !]: <description>

[optional body]

[optional footer(s)]
```

`type` 和标题说明必填；scope、正文与 footer 可选。标题、正文和解释性 footer 使用中文；`type`、scope、`!` 与 `BREAKING CHANGE` 等工具标记保留英文。

## 类型选择

- `feat`：新增面向用户或 API 的能力。
- `fix`：修复缺陷。
- `docs`：只改文档。
- `style`：不改变行为的格式调整。
- `refactor`：既非功能也非修复的代码调整。
- `perf`：性能改进。
- `test`：新增或修正测试。
- `build`：构建系统或依赖变更。
- `ci`：CI 配置或流水线变更。
- `chore`：不属于上述类型的维护。
- `revert`：撤销以前的提交。

scope 用于澄清影响区域，例如 `android`、`server`、`docs`、`gradle`、`ui`、`auth` 或包/模块名。

## 破坏性变更

使用标题中的 `!`，或添加 `BREAKING CHANGE` footer：

```text
feat(parser)!: 拒绝存在歧义的课程语法
```

```text
BREAKING CHANGE: 存在歧义的课程语法现在会被拒绝，而不是自动规范化。
```

## 质量要求

- `type` 使用小写，中文标题保持简短、明确且以动作描述改动。
- 不使用“更新代码”“修改内容”“修复问题”等含义不明的标题。
- 标题末尾不加中英文标点。
- 标题不足以说明原因时，再添加简短正文。
