package com.suilearn.api.model;

import java.time.Instant;

public record KnowledgeBaseDetail(
    String id,
    String name,
    String description,
    Instant createdAt,
    Instant updatedAt,
    int materialCount,
    int knowledgePointCount,
    int questionCount,
    int generatedContentCount,
    int aiNoteCount
) {
}
