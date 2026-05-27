package com.suilearn.api.model;

public record SourceRef(
    SourceType type,
    String id,
    String knowledgeBaseId,
    String title,
    String materialId,
    String chunkId,
    boolean deleted,
    String excerpt
) {
}
