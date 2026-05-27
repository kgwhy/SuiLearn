package com.suilearn.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateKnowledgeBaseRequest(
    @NotBlank String name,
    String description
) {
}
