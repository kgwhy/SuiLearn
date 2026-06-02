---
name: to-prd
description: 将当前对话上下文整理成中文 PRD。Use when the user wants to create a PRD from the current context, convert discussion into a product requirements issue, or prepare implementation-ready issue-tracker work with the ready-for-agent label.
---

# To PRD

This skill takes the current conversation context and codebase understanding and produces a Chinese PRD. Do not interview the user; synthesize what is already known.

The issue tracker and triage label vocabulary should have been provided. If not, run `/setup-matt-pocock-skills` if that command is available in the harness.

## Process

1. Explore the repo enough to understand the current state of the codebase, if you have not already.
   - Follow `AGENTS.md` and the current role file before modifying project files.
   - Use SuiLearn's existing product docs as PRD fact sources.
   - Use `docs/architecture.md` only to avoid conflicting with established architecture; do not duplicate or replace architecture decisions in the PRD.
   - Use the project's own vocabulary from those docs instead of restating a hard-coded glossary in the PRD.
   - If a feature needs new architecture, mark it as needing Architecture Agent follow-up instead of deciding it inside the PRD.

2. Sketch the seams where the feature should be tested.
   - Prefer existing seams to new ones.
   - Use the highest seam possible.
   - If new seams are needed, propose them at the highest point you can.
   - Include the seams in the PRD's Testing Decisions. If the user explicitly asked to review seams first, pause for confirmation; otherwise continue without interviewing.

3. Write the PRD using the template below.
   - Write the PRD in Chinese by default. Only use another language if the user explicitly asks for it.
   - Keep the PRD user-centered.
   - Keep user stories under each requirement concise but complete.
   - Do not include specific file paths or code snippets unless a prototype produced a concise decision-rich shape that prose would obscure.

4. Publish the PRD to the project issue tracker.
   - Apply the `ready-for-agent` triage label.
   - No additional triage is needed.
   - If publishing is blocked by missing auth, missing remote, missing tracker setup, or missing label permissions, leave the PRD body ready to publish and report the blocker.

## PRD Template

```markdown
## 背景与目标

### 背景

说明为什么现在要做这个产品，以及它要解决什么用户问题或产品问题。

### 目标

说明本 PRD 希望达成的结果，优先从用户视角描述。

### 非目标

说明本 PRD 明确不解决什么，避免范围膨胀。

## 功能需求

### 需求 1：<功能名称>

#### 用户故事

为这个需求列出最少但完整的用户故事。每条用户故事使用以下格式：

1. 作为<角色>，我希望<能力或行为>，以便<收益或目标>

#### 需求点

- 用户可感知的行为、状态、规则和边界情况。
- 从产品视角定义的数据、内容或文案预期。
- 仅在必要时说明跨角色交付边界。

#### 验收标准

- 可观察、可验证的完成标准。
- 优先描述用户可见行为和稳定的产品事实，不写实现细节。

### 需求 2：<功能名称>

每个功能需求都重复以上结构。

## 产品决策

列出已经确定的产品决策。可以包括：

- 产品范围和优先级决策
- 用户流程决策
- 内容、文案或数据含义决策
- 从产品视角定义的兼容或迁移预期
- 需要交给架构 Agent 写入 `docs/architecture.md` 的决策点

不要在 PRD 中写详细架构、数据库 schema、API 契约、具体文件路径或代码片段。架构内容属于 `docs/architecture.md`。

## 测试决策

列出已经确定的测试决策。包括：

- 好测试的判断标准：只测试外部行为，不测试实现细节。
- 需要测试哪些模块或行为边界。
- 代码库中是否已有类似测试模式可参考。

## 不在范围内

说明本 PRD 明确不包含的内容。

## 补充说明

记录风险、假设、待确认事项或发布 issue 时需要附带的信息。
```
