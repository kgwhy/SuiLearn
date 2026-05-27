package com.suilearn.api.model;

import java.time.Instant;
import java.util.List;

public record QuestionSummary(
    String id,
    String knowledgeBaseId,
    QuestionType questionType,
    String stem,
    String categoryId,
    String categoryName,
    Integer difficulty,
    List<String> knowledgePointIds,
    int answeredCount,
    double correctRate,
    List<SourceRef> sourceRefs,
    Instant createdAt,
    Instant savedAt
) {
}
