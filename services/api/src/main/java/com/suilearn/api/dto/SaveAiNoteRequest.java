package com.suilearn.api.dto;

import com.suilearn.api.model.AiNoteType;
import com.suilearn.api.model.SourceRef;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record SaveAiNoteRequest(
    String draftId,
    @NotBlank String knowledgeBaseId,
    @NotNull AiNoteType type,
    @NotBlank String title,
    @NotBlank String content,
    @NotEmpty List<SourceRef> sourceRefs
) {
}
