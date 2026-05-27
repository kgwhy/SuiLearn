package com.suilearn.api.dto;

import jakarta.validation.constraints.NotBlank;

public record RenameKnowledgeBaseRequest(
    @NotBlank String name,
    String description
) {
}
