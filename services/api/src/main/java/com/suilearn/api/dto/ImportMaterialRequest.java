package com.suilearn.api.dto;

import com.suilearn.api.model.MaterialSourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ImportMaterialRequest(
    @NotBlank String title,
    String fileName,
    @NotNull MaterialSourceType sourceType,
    @NotBlank String content
) {
}
