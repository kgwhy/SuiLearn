package com.suilearn.api.model;

public record SourceRef(
    SourceType type,
    String id,
    String knowledgeBaseId,
    String title,
    String materialId,
    String chunkId,
    boolean deleted,
    String excerpt,
    String revisionId,
    Integer pageNumber,
    String blockId
) {
    public SourceRef(
        SourceType type, String id, String knowledgeBaseId, String title, String materialId,
        String chunkId, boolean deleted, String excerpt
    ) {
        this(type, id, knowledgeBaseId, title, materialId, chunkId, deleted, excerpt, null, null, chunkId);
    }
}
