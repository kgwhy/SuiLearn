package com.suilearn.api.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateKnowledgePointRequest(
    @NotBlank String name,
    @NotBlank String description
) {
}
