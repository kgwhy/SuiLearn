package com.suilearn.api.model;

import java.time.Instant;
import java.util.List;

public record MaterialDetail(
    String id,
    String knowledgeBaseId,
    String title,
    MaterialSourceType sourceType,
    MaterialStatus status,
    String content,
    String contentPreview,
    Instant createdAt,
    Instant deletedAt,
    List<MaterialChunk> chunks,
    List<KnowledgePoint> extractedKnowledgePoints
) {
}
