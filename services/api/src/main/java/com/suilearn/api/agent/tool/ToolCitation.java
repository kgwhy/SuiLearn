package com.suilearn.api.agent.tool;

public record ToolCitation(String stableId, String sourceRef) {
    public ToolCitation {
        stableId = requireText(stableId, "stableId");
        sourceRef = requireText(sourceRef, "sourceRef");
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.strip();
    }
}
