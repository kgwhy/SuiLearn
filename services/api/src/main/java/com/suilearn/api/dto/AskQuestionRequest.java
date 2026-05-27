package com.suilearn.api.dto;

import jakarta.validation.constraints.NotBlank;

public record AskQuestionRequest(
    String knowledgeBaseId,
    String materialId,
    @NotBlank String question
) {
}
