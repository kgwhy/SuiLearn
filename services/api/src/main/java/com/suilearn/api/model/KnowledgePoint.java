package com.suilearn.api.model;

import java.util.List;

public record KnowledgePoint(
    String id,
    String knowledgeBaseId,
    String name,
    String description,
    String sourceMaterialId,
    List<SourceRef> sourceRefs
) {
}
