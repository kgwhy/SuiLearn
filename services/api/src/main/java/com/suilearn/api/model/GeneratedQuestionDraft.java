package com.suilearn.api.model;

import java.time.Instant;
import java.util.List;

public record GeneratedQuestionDraft(
    String id,
    String knowledgeBaseId,
    GeneratedContentStatus status,
    List<SourceRef> sourceRefs,
    SourceType sourceType,
    String sourceId,
    QuestionType questionType,
    String categoryId,
    String categoryName,
    List<String> knowledgePointIds,
    String stem,
    List<String> options,
    List<String> answer,
    String explanation,
    String savedQuestionId,
    Instant savedAt,
    Instant createdAt,
    Instant updatedAt
) {
}
