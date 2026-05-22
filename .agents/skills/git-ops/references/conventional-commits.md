# Conventional Commits Reference

Source: https://www.conventionalcommits.org/zh-hans/v1.0.0/

## Required Shape

Use this structure:

```text
<type>[optional scope][optional !]: <description>

[optional body]

[optional footer(s)]
```

The type and description are required. Scope, body, and footers are optional.

In this project, write the description, body, and explanatory footer values in Chinese. Keep `type`, optional `scope`, `!`, and footer tokens such as `BREAKING CHANGE` in English for compatibility with Conventional Commits tooling.

## Semantic Meaning

- `fix` maps to a SemVer patch-level change.
- `feat` maps to a SemVer minor-level change.
- A breaking change maps to a SemVer major-level change and can be attached to any type.

## Breaking Changes

Use one of two forms:

```text
feat(parser)!: 拒绝存在歧义的课程语法
```

```text
BREAKING CHANGE: 存在歧义的课程语法现在会被拒绝，而不是自动规范化。
```

If the `!` marker is used, still include a footer when extra migration context is useful.

## Footer Rules

- Footers follow git trailer style: `Token: value` or `Token #value`.
- `BREAKING CHANGE` may also be written with a space in the token.
- Use footers for issue links, review metadata, and migration notes.

Examples:

```text
Refs: #123
Reviewed-by: Name <name@example.com>
BREAKING CHANGE: 客户端必须发送已签名的课程作答记录。
```

## Good Examples

```text
feat(android): 添加课程进度缓存
```

```text
fix(server): 拒绝已过期的会话令牌
```

```text
docs(product): 明确新手引导范围
```

```text
test(android): 覆盖测验重试状态
```

```text
refactor(content): 拆分练习规范化逻辑
```

```text
chore(gradle): 更新包装器元数据
```

## Message Quality

- Use lowercase type names.
- Keep the Chinese subject concise and action-oriented.
- Avoid vague subjects such as `更新代码`, `修改内容`, or `修复问题`.
- Do not end the subject with Chinese or English punctuation.
- Explain why in the Chinese body when the subject alone cannot carry the intent.
