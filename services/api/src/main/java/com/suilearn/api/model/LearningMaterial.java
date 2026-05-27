package com.suilearn.api.model;

import java.time.Instant;

public record LearningMaterial(
    String id,
    String knowledgeBaseId,
    String title,
    MaterialSourceType sourceType,
    MaterialStatus status,
    String content,
    Instant createdAt,
    Instant deletedAt
) {
}
