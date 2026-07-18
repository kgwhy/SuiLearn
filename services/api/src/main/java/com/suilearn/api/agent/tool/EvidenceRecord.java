package com.suilearn.api.agent.tool;

public record EvidenceRecord(
    String stableId,
    String sourceRef,
    String knowledgeBaseId,
    String materialId,
    String content,
    boolean deleted,
    String revisionId,
    Integer pageNumber,
    String blockId,
    String excerpt
) {
    public EvidenceRecord {
        stableId = RequiredText.value(stableId, "stableId");
        sourceRef = RequiredText.value(sourceRef, "sourceRef");
        content = RequiredText.value(content, "content");
    }

    public EvidenceRecord(String stableId, String sourceRef, String knowledgeBaseId, String materialId,
                          String content, boolean deleted) {
        this(stableId, sourceRef, knowledgeBaseId, materialId, content, deleted, null, null, null, null);
    }
}

final class RequiredText {
    private RequiredText() {
    }

    static String value(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.strip();
    }
}
