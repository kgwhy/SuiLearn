# Agent Note: search_knowledge 的工具内容必须包含可读取的证据指针
Status: implemented

## Problem

AgentLoop 只把工具结果的 content 传回模型。search_knowledge 之前只返回 “Found N evidence pointer(s).”，真正的 stableId/sourceRef 在 metadata 中，模型看不到，因此无法调用 read_evidence，只能反复 search 直到预算耗尽。

## Decision

search_knowledge 的 content 直接列出每个证据指针的 stableId、sourceRef、relevance 与 excerpt。metadata 仍保留结构化指针，给 UI/追踪使用。

## Alternatives considered

- **修改 AgentLoop 拼接 metadata**：所有工具都要处理格式，且容易把大段内容塞进模型上下文。
- **只在 prompt 中说明去 metadata 读取**：模型无法访问 metadata，不可行。
- **改成结构化 JSON content**：可读性更好，但当前纯文本列表已足够模型调用 read_evidence，改动更小。

## Consequences

- search tool result 会稍长，但单次最多 20 个指针，仍远低于事件载荷上限。
- 模型能直接从工具结果取得 read_evidence 所需参数。
