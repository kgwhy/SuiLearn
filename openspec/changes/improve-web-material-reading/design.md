# 设计

## 方案

保持后端和契约不变，只优化 Web 工作台的状态流和展示层。

资料导入完成后，若后端返回 READY 资料，前端先把生成来源设为该资料，再调用资料详情读取接口并切到资料页。这样用户完成导入后立即看到刚导入的正文、任务状态和派生内容，不需要再从列表中猜测要点哪里。

资料详情新增“资料正文”阅读块，优先使用 `MaterialDetail.content`。如果后端只返回 `contentPreview`，阅读块显示预览并标识为预览；两者都为空时展示空状态。正文使用保留换行的阅读样式，避免长文本被压缩成一段。

知识点展示保留 chip 作为选择入口，但新增可读详情列表：每个知识点展示名称、描述、来源资料 ID、来源标题和摘录。资料详情中的 `extractedKnowledgePoints` 不再只渲染为关键词 chip，而是渲染为 `KnowledgePointSummaryList`，用户无需跳转即可理解知识点内容。总览中选中 chip 后仍展示 `KnowledgePointDetail`，该组件也补充来源摘录。

## 数据流

- `importMaterial` 成功后：
  - READY：`setSection("materials")`，设置生成来源，调用 `openMaterial(material.id)`，再提取知识点并刷新工作台。
  - 非 READY：刷新工作台，不强制打开详情。
- `openMaterial` 继续使用 `api.getMaterial(materialId)`。
- 知识点详情只消费已有 `KnowledgePoint.sourceRefs` 和 `sourceMaterialId`。

## 风险

- 若后端仅返回 `contentPreview`，用户仍只能看到预览；前端会明确标识“预览”。
- 长资料正文可能较长，因此阅读块设置最大高度和滚动，避免撑坏资料页布局。
