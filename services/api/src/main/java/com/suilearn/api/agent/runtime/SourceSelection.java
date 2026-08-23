package com.suilearn.api.agent.runtime;

import java.util.Objects;

/**
 * Structured source selection for capabilities such as question_generation.
 * It is carried by {@link TurnContext} but is not interpreted by the change-1 runtime.
 */
public record SourceSelection(SourceKind kind, String sourceId) {
    public SourceSelection {
        Objects.requireNonNull(kind, "kind");
        sourceId = requireText(sourceId, "sourceId");
    }

    public enum SourceKind {
        KNOWLEDGE_BASE,
        MATERIAL,
        KNOWLEDGE_POINT,
        QUESTION,
        WRONG_QUESTION,
        SAVED_QUESTION,
        GENERATION_TASK
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.strip();
    }
}
