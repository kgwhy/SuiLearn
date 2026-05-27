package com.suilearn.api.model;

import java.time.Instant;

public record KnowledgeBase(
    String id,
    String name,
    String description,
    Instant createdAt,
    Instant updatedAt
) {
}
