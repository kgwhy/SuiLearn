package com.suilearn.api.dto;

import com.suilearn.api.model.SourceRef;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record GenerateExplanationRequest(
    @NotBlank String knowledgeBaseId,
    @NotBlank String knowledgePointId,
    @NotEmpty List<SourceRef> sourceRefs,
    String prompt
) {
}
