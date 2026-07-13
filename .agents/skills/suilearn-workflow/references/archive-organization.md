# 归档组织

当关闭 OpenSpec change 时，归档目录按技术能力域组织：

```text
openspec/changes/archive/<primary-domain>/YYYY-MM-DD-<change-name>/
```

允许的主域仅为：

- `workflow-governance`：工作流、角色、OpenSpec、Agent、policy。
- `platform-runtime`：CI、构建、Docker、Compose、运行时配置。
- `learning-rag`：资料导入、知识点、检索、RAG、学习体验。
- `developer-tooling`：Git 规则、开发工具和自动化。

每个 change 必须有且只有一个主域。跨域关联只记录为索引标签，严禁复制归档目录。

## 归档入口

在已完成 artifact/task 检查和 delta spec 同步决策后，使用项目内命令：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/archive-openspec-change.ps1 `
  -ChangeName <change-name> `
  -PrimaryDomain <workflow-governance|platform-runtime|learning-rag|developer-tooling> `
  -RelatedDomains <optional-domain-list>
```

该命令只接受 `openspec/changes/` 的直接子目录；会拒绝未知主域和已有目标，并在移动成功后维护
`openspec/changes/archive/README.md`。归档后运行 `openspec list --json`，确认该 change 不再是 active。

不要调用平铺路径的归档方式，也不要手工复制 change 目录。
