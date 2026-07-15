package com.suilearn.api.dto;

import com.suilearn.api.model.GeneratedContentStatus;
import com.suilearn.api.model.QuestionType;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** Content-only review boundary; provenance and knowledge-point ownership are immutable. */
public record ReviewKnowledgePointQuestionDraftRequest(
    @NotNull GeneratedContentStatus status,
    QuestionType questionType,
    String stem,
    List<String> options,
    List<String> answer,
    String explanation
) {}
