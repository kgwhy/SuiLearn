package com.suilearn.api.agent.context;

import java.util.Objects;

public record EvidenceItem(
    String stableId,
    String sourceRef,
    String content,
    double relevance,
    boolean verified
) {
    public EvidenceItem {
        stableId = requireText(stableId, "stableId");
        sourceRef = requireText(sourceRef, "sourceRef");
        content = requireText(content, "content");
        if (!Double.isFinite(relevance) || relevance < 0.0d || relevance > 1.0d) {
            throw new IllegalArgumentException("relevance must be between 0 and 1");
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return Objects.requireNonNull(value).strip();
    }
}
