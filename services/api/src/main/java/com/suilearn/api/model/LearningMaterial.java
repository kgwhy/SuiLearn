package com.suilearn.api.model;

import java.time.Instant;

public record LearningMaterial(
    String id,
    String knowledgeBaseId,
    String title,
    MaterialSourceType sourceType,
    MaterialStatus status,
    String importTaskId,
    String embeddingTaskId,
    String errorMessage,
    String content,
    Instant createdAt,
    Instant deletedAt
) {
    public LearningMaterial(
        String id,
        String knowledgeBaseId,
        String title,
        MaterialSourceType sourceType,
        MaterialStatus status,
        String content,
        Instant createdAt,
        Instant deletedAt
    ) {
        this(id, knowledgeBaseId, title, sourceType, status, null, null, null, content, createdAt, deletedAt);
    }
}
