package com.suilearn.api.model;

import java.time.Instant;

public record MaterialMetadata(
    String id,
    String knowledgeBaseId,
    String title,
    MaterialSourceType sourceType,
    MaterialStatus status,
    Instant createdAt,
    Instant deletedAt
) {
}
