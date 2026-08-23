package com.suilearn.api.agent.runtime;

import java.util.Objects;

/**
 * Server-validated mandatory scope for an Agent turn.
 *
 * <p>At least one of knowledgeBaseId or materialId is required. If both are present,
 * callers must ensure the material belongs to the knowledge base; the existence and
 * learner-visibility check remains owned by the retrieving domain service.
 */
public record StudyScope(String knowledgeBaseId, String materialId) {
    public StudyScope {
        knowledgeBaseId = normalize(knowledgeBaseId);
        materialId = normalize(materialId);
        if (knowledgeBaseId == null && materialId == null) {
            throw new IllegalArgumentException("knowledgeBaseId or materialId is required");
        }
    }

    public boolean hasKnowledgeBase() {
        return knowledgeBaseId != null;
    }

    public boolean hasMaterial() {
        return materialId != null;
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
