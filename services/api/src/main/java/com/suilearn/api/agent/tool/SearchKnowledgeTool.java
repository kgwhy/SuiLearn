package com.suilearn.api.agent.tool;

import com.suilearn.api.agent.runtime.TurnContext;
import java.util.LinkedHashMap;
import java.util.StringBuilder;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SearchKnowledgeTool implements Tool {
    private final EvidenceSearchPort searchPort;

    public SearchKnowledgeTool(EvidenceSearchPort searchPort) {
        this.searchPort = searchPort;
    }

    @Override
    public ToolDefinition definition() {
        return new ToolDefinition(AgentToolNames.SEARCH_KNOWLEDGE,
            "Search knowledge base or material evidence within the turn scope.",
            Map.of("type", "object", "properties", Map.of(
                "query", Map.of("type", "string", "minLength", 1),
                "limit", Map.of("type", "integer", "minimum", 1, "maximum", 20, "default", 5)
            ), "required", List.of("query")),
            false, Set.of("turn"));
    }

    @Override
    public ToolResult execute(TurnContext context, Map<String, Object> args) {
        String query = ToolArguments.requiredString(args, "query", 2000);
        int limit = ToolArguments.integer(args, "limit", 5, 1, 20);
        if (searchPort == null) {
            return new ToolResult("Knowledge search is unavailable.", List.of(),
                Map.of("code", "SEARCH_UNAVAILABLE"), false, null);
        }
        try {
            var pointers = searchPort.search(new EvidenceSearchPort.SearchRequest(
                query, context.scope(), limit));
            var metadata = new LinkedHashMap<String, Object>();
            metadata.put("count", pointers.size());
            metadata.put("pointers", pointers.stream().map(SearchKnowledgeTool::pointer).toList());
            var sources = pointers.stream()
                .map(pointer -> new ToolCitation(pointer.stableId(), pointer.sourceRef()))
                .toList();
            return new ToolResult(describe(pointers), sources, metadata, true, null);
        } catch (RuntimeException exception) {
            return new ToolResult("Knowledge search failed.", List.of(),
                Map.of("code", "SEARCH_FAILED"), false, null);
        }
    }

    private static String describe(List<EvidencePointer> pointers) {
        var content = new StringBuilder();
        content.append("Found ").append(pointers.size()).append(" evidence pointer(s). ")
            .append("Call read_evidence with one stableId (or sourceRef):");
        for (int index = 0; index < pointers.size(); index++) {
            EvidencePointer pointer = pointers.get(index);
            content.append("\n- stableId=").append(pointer.stableId())
                .append(", sourceRef=").append(pointer.sourceRef())
                .append(", relevance=").append(pointer.relevance());
            if (pointer.excerpt() != null && !pointer.excerpt().isBlank()) {
                content.append(", excerpt=").append(pointer.excerpt());
            }
        }
        return content.toString();
    }

    private static Map<String, Object> pointer(EvidencePointer pointer) {
        var item = new LinkedHashMap<String, Object>();
        item.put("stableId", pointer.stableId());
        item.put("sourceRef", pointer.sourceRef());
        item.put("knowledgeBaseId", pointer.knowledgeBaseId());
        item.put("materialId", pointer.materialId());
        item.put("relevance", pointer.relevance());
        item.put("revisionId", pointer.revisionId());
        item.put("pageNumber", pointer.pageNumber());
        item.put("blockId", pointer.blockId());
        item.put("excerpt", pointer.excerpt());
        return item;
    }
}
