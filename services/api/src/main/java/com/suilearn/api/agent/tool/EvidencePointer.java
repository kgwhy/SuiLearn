package com.suilearn.api.agent.tool;

public record EvidencePointer(
    String stableId,
    String sourceRef,
    String knowledgeBaseId,
    String materialId,
    double relevance,
    String revisionId,
    Integer pageNumber,
    String blockId,
    String excerpt
) {
    public EvidencePointer {
        stableId = RequiredText.value(stableId, "stableId");
        sourceRef = RequiredText.value(sourceRef, "sourceRef");
        if (!Double.isFinite(relevance) || relevance < 0.0d || relevance > 1.0d) {
            throw new IllegalArgumentException("relevance must be between 0 and 1");
        }
    }

    public EvidencePointer(String stableId, String sourceRef, String knowledgeBaseId, String materialId,
                           double relevance) {
        this(stableId, sourceRef, knowledgeBaseId, materialId, relevance, null, null, null, null);
    }
}
