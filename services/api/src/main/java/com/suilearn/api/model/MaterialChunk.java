package com.suilearn.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;

public record MaterialChunk(
    String id,
    String materialId,
    String content,
    int ordinal,
    SourceRef sourceRef,
    @JsonIgnore
    List<Double> embedding,
    @JsonIgnore
    String embeddingModel
) {
    public MaterialChunk(String id, String materialId, String content, int ordinal, SourceRef sourceRef) {
        this(id, materialId, content, ordinal, sourceRef, null, null);
    }
}
