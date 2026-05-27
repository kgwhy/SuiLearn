package com.suilearn.api.model;

public record MaterialChunk(
    String id,
    String materialId,
    String content,
    int ordinal,
    SourceRef sourceRef
) {
}
