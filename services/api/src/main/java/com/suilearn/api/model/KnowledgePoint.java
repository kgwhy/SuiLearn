package com.suilearn.api.model;

import java.util.List;

public record KnowledgePoint(
    String id,
    String knowledgeBaseId,
    String name,
    String description,
    String sourceMaterialId,
    List<SourceRef> sourceRefs,
    String title,
    String shortSummary,
    String definition,
    List<String> principles,
    List<String> applicationScenarios,
    List<String> pitfalls,
    KnowledgePointReviewStatus reviewStatus,
    boolean sourceOutdated,
    boolean legacy
) {
    public KnowledgePoint(
        String id, String knowledgeBaseId, String name, String description, String sourceMaterialId, List<SourceRef> sourceRefs
    ) {
        this(id, knowledgeBaseId, name, description, sourceMaterialId, sourceRefs,
            name, description, null, List.of(), List.of(), List.of(), KnowledgePointReviewStatus.CONFIRMED, false, true);
    }
}
