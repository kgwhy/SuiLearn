# 验证与关闭

在声明完成或关闭变更前使用。

## 必需证据

- 必需测试、构建或检查命令输出，或精确的不适用原因。
- `git diff <base_ref> --stat`.
- 对照角色 policy 和 active change `policy.md` 核对已修改文件。
- 任务状态核对：每个任务已完成，或延期到具名 follow-up change。
- Major 或跨角色工作的审查闭环。

## 工作流产物检查

声明关闭前，检查 active change 产物中是否存在陈旧状态：

- `In progress`
- `Status: open`
- 无 Owner 的 `pending`

如果变更有意保持打开，最终报告必须说明。

## 配置 / 启动 / 集成

对于端口、CORS、Docker/Compose、反向代理、环境变量、CI wrapper、数据库连接、
服务地址或本地/容器启动方式：

- 报告验收矩阵的运行态结果
- 报告旧默认值、旧端口、旧 service 名或旧文档的残留扫描
- 解释每个不可运行的运行态检查

这类变更不能只依赖静态分析。

## 完成格式

```text
✅ 完成
改了什么: <summary>
测试结果: <raw output or not-applicable reason>
文件核对: <N files, all in scope / out-of-scope files: X>
Review 闭环: <无发现 / 已修复 Pn / 已延期到 change id>
```
