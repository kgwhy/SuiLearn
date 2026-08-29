package com.suilearn.api.agent.tool;

import com.suilearn.api.agent.runtime.TurnContext;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ReadEvidenceTool implements Tool {
    private final EvidenceReadPort readPort;

    public ReadEvidenceTool(EvidenceReadPort readPort) {
        this.readPort = readPort;
    }

    @Override
    public ToolDefinition definition() {
        return new ToolDefinition(AgentToolNames.READ_EVIDENCE,
            "Read one verified evidence record by stable id or sourceRef inside the turn scope.",
            Map.of("type", "object", "properties", Map.of(
                "stableId", Map.of("type", "string", "minLength", 1),
                "sourceRef", Map.of("type", "string", "minLength", 1)
            ), "anyOf", List.of(Map.of("required", List.of("stableId")), Map.of("required", List.of("sourceRef")))),
            false, Set.of("turn"));
    }

    @Override
    public ToolResult execute(TurnContext context, Map<String, Object> args) {
        String stableId = ToolArguments.optionalString(args, "stableId", 256);
        String sourceRef = ToolArguments.optionalString(args, "sourceRef", 256);
        if (stableId == null && sourceRef == null) {
            throw new IllegalArgumentException("stableId or sourceRef is required");
        }
        String effectiveStableId = stableId == null ? sourceRef : stableId;
        String effectiveSourceRef = sourceRef == null ? stableId : sourceRef;
        if (readPort == null) {
            return new ToolResult("Evidence reading is unavailable.", List.of(),
                Map.of("code", "EVIDENCE_READ_UNAVAILABLE"), false, null);
        }
        try {
            var pointer = new EvidencePointer(effectiveStableId, effectiveSourceRef,
                context.scope().knowledgeBaseId(), context.scope().materialId(), 0.0d);
            return readPort.read(new EvidenceReadPort.ReadRequest(effectiveSourceRef, pointer,
                context.scope()))
                .filter(record -> !record.deleted())
                .map(record -> {
                    var metadata = new LinkedHashMap<String, Object>();
                    metadata.put("stableId", record.stableId());
                    metadata.put("sourceRef", record.sourceRef());
                    metadata.put("knowledgeBaseId", valueOrEmpty(record.knowledgeBaseId()));
                    metadata.put("materialId", valueOrEmpty(record.materialId()));
                    metadata.put("revisionId", valueOrEmpty(record.revisionId()));
                    metadata.put("pageNumber", record.pageNumber() == null ? "" : record.pageNumber());
                    metadata.put("blockId", valueOrEmpty(record.blockId()));
                    metadata.put("excerpt", valueOrEmpty(record.excerpt()));
                    metadata.put("content", record.content());
                    return new ToolResult(record.content(), List.of(new ToolCitation(record.stableId(), record.sourceRef())),
                        metadata, true, null);
                })
                .orElseGet(() -> new ToolResult("No readable evidence found for this source.", List.of(),
                    Map.of("code", "EVIDENCE_NOT_FOUND"), false, null));
        } catch (RuntimeException exception) {
            return new ToolResult("Evidence reading failed: " + exception.getMessage(), List.of(),
                Map.of("code", "EVIDENCE_READ_FAILED", "message", exception.getMessage() == null ? "" : exception.getMessage()),
                false, null);
        }
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
