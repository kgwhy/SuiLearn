# 设计

## 后端

`MaterialImportService` 保持现有流程：解析和切块成功后，如果 embedding 未配置，直接写入 `TEXT_ONLY` chunks。新增行为是：如果 embedding 已配置但 embedding 子任务在写入向量时失败，则记录 embedding 任务失败，并把资料 chunk 降级保存为 `TEXT_ONLY`，随后让导入任务成功完成。

这样可以区分两件事：

- 资料导入、解析、切块是否可用。
- embedding 配置或远端服务是否可用。

`KnowledgePointService.extractKnowledgePoints` 增加资料状态检查，只允许 `READY` 资料继续提取。失败、删除、解析中或索引中状态都拒绝。

## 前端

`importMaterial` 在导入返回后检查 `material.status`。只有 `READY` 时才自动调用知识点提取。非 `READY` 状态只刷新工作台，让用户看到任务状态和错误。

## 风险

- embedding 失败后资料变为 `READY`，但 chunk 是 `TEXT_ONLY`，检索质量依赖关键词检索。
- 旧的失败资料仍保留为 `FAILED`，不会自动迁移为 `READY`。
