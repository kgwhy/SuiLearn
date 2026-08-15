## ADDED Requirements

### Requirement: 原始资料文件导入
系统 SHALL 允许用户通过 multipart 上传 Markdown、TXT、PDF、DOC 和 DOCX 原始文件，并 SHALL 保存文件类型、MIME、大小、校验值、文件名和知识库归属。

#### Scenario: 成功上传受支持文件
- **WHEN** 用户向一个存在的知识库上传受支持且通过校验的原始文件
- **THEN** 系统保存原件，创建资料和异步处理任务，并立即返回 materialId 与 taskId

#### Scenario: 拒绝伪造或超限文件
- **WHEN** 文件扩展名、MIME、文件签名不一致，或大小/页数超过配置上限
- **THEN** 系统拒绝导入并返回可操作的错误原因，且不创建虚假的 READY 资料

### Requirement: 原件与阅读版双视图
系统 SHALL 永久保留未删除资料的原始文件，并 SHALL 为成功处理的资料提供完整统一阅读版、原始文件查看能力和原件下载能力。

#### Scenario: 查看完整资料
- **WHEN** 用户打开 READY 资料详情
- **THEN** 系统默认展示完整阅读版，并允许切换到原始文件或下载原件

#### Scenario: 旧资料没有原件
- **WHEN** 用户打开由旧 content 迁移而来且没有原始资产的资料
- **THEN** 系统展示完整 legacy 阅读版并明确说明原始文件不可用

### Requirement: 按格式解析与按页 OCR
系统 SHALL 为 Markdown/TXT/PDF/DOC/DOCX 使用对应解析适配器；对 PDF SHALL 先直接提取文本，并仅对文本不足页面执行 OCR。

#### Scenario: 文本型 PDF 不触发 OCR
- **WHEN** PDF 每个页面均能提取达到阈值的有效文本
- **THEN** 系统直接生成页面映射和阅读版，且不调用 OCR

#### Scenario: 扫描或混合 PDF 自动 OCR
- **WHEN** PDF 全部或部分页面的可提取文本低于阈值
- **THEN** 系统只对文本不足页面执行 OCR，并将结果合并到对应页面的阅读版

#### Scenario: OCR 无法完成
- **WHEN** 资料没有足够文本且 OCR 不可用或最终失败
- **THEN** 资料处理任务失败但原件仍可查看、下载和重试，系统不得返回虚假的 READY

### Requirement: 不可变解析版本与结构化正文
系统 SHALL 为每次成功处理创建不可变 DocumentRevision 和有顺序、章节、页码映射的 DocumentBlock；重新处理 SHALL 创建新 revision 而非覆盖旧结果。

#### Scenario: 重新处理资料
- **WHEN** 用户对已有资料触发重新处理且处理成功
- **THEN** 系统创建新 revision 并将其设为当前阅读版，同时保留旧 revision 供已有来源引用追溯

### Requirement: 精确来源定位
系统 SHALL 允许知识点和题目来源引用指向 material、revision、page/block 和 excerpt，并 SHALL 允许用户从引用跳转到对应阅读位置。

#### Scenario: 从知识点引用定位资料
- **WHEN** 用户点击知识点详情中的来源引用
- **THEN** 系统打开对应资料 revision 并定位到引用的页码或正文 block

### Requirement: 兼容旧文本资料
系统 SHALL 将现有资料正文增量映射为 LEGACY_TEXT revision，并 SHALL 保留现有资料状态、知识点和已保存题目。

#### Scenario: 部署迁移后读取旧资料
- **WHEN** 变更部署前存在只有 content 的 READY 资料
- **THEN** 部署后该资料仍可完整阅读，且不会因缺少原始文件而丢失知识点或题目
