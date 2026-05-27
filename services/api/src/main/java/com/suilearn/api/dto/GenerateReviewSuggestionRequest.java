package com.suilearn.api.dto;

import com.suilearn.api.model.SourceRef;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record GenerateReviewSuggestionRequest(
    @NotBlank String knowledgeBaseId,
    @NotEmpty List<SourceRef> sourceRefs,
    List<String> weakKnowledgePointIds,
    List<String> wrongQuestionIds,
    String prompt
) {
}
