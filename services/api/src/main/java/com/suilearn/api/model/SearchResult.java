package com.suilearn.api.model;

import java.util.List;

public record SearchResult(
    String id,
    SearchResultType type,
    String title,
    String summary,
    double score,
    String knowledgeBaseId,
    List<String> knowledgePointIds,
    List<SourceRef> sourceRefs
) {
}
