package com.suilearn.api.model;

import java.time.Instant;
import java.util.List;

public record GeneratedQuestionDraft(
    String id,
    String knowledgeBaseId,
    String generationTaskId,
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
    Instant updatedAt,
    String knowledgePointId,
    String materialId,
    String revisionId,
    String evidenceExcerpt
) {
    public GeneratedQuestionDraft(
        String id, String knowledgeBaseId, String generationTaskId, GeneratedContentStatus status, List<SourceRef> sourceRefs,
        SourceType sourceType, String sourceId, QuestionType questionType, String categoryId, String categoryName,
        List<String> knowledgePointIds, String stem, List<String> options, List<String> answer, String explanation,
        String savedQuestionId, Instant savedAt, Instant createdAt, Instant updatedAt
    ) {
        this(id, knowledgeBaseId, generationTaskId, status, sourceRefs, sourceType, sourceId, questionType, categoryId,
            categoryName, knowledgePointIds, stem, options, answer, explanation, savedQuestionId, savedAt, createdAt,
            updatedAt, first(knowledgePointIds), firstMaterial(sourceRefs), firstRevision(sourceRefs), firstExcerpt(sourceRefs));
    }

    private static String first(List<String> values) {
        return values == null || values.isEmpty() ? null : values.getFirst();
    }

    private static String firstMaterial(List<SourceRef> refs) {
        return refs == null || refs.isEmpty() ? null : refs.getFirst().materialId();
    }

    private static String firstRevision(List<SourceRef> refs) {
        return refs == null || refs.isEmpty() ? null : refs.getFirst().revisionId();
    }

    private static String firstExcerpt(List<SourceRef> refs) {
        return refs == null || refs.isEmpty() ? null : refs.getFirst().excerpt();
    }
}
