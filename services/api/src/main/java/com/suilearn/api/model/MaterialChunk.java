package com.suilearn.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;

public record MaterialChunk(
    String id,
    String knowledgeBaseId,
    String materialId,
    String content,
    int ordinal,
    SourceRef sourceRef,
    @JsonIgnore
    List<Double> embedding,
    EmbeddingStatus embeddingStatus,
    String embeddingModel,
    Integer embeddingDimensions
) {
    public MaterialChunk(String id, String materialId, String content, int ordinal, SourceRef sourceRef) {
        this(id, sourceRef == null ? null : sourceRef.knowledgeBaseId(), materialId, content, ordinal, sourceRef);
    }

    public MaterialChunk(String id, String knowledgeBaseId, String materialId, String content, int ordinal, SourceRef sourceRef) {
        this(id, knowledgeBaseId, materialId, content, ordinal, sourceRef, null, EmbeddingStatus.PENDING, null, null);
    }

    public MaterialChunk(
        String id,
        String knowledgeBaseId,
        String materialId,
        String content,
        int ordinal,
        SourceRef sourceRef,
        List<Double> embedding,
        String embeddingModel
    ) {
        this(
            id,
            knowledgeBaseId,
            materialId,
            content,
            ordinal,
            sourceRef,
            embedding,
            embedding == null ? EmbeddingStatus.PENDING : EmbeddingStatus.READY,
            embeddingModel,
            embedding == null ? null : embedding.size()
        );
    }
}
