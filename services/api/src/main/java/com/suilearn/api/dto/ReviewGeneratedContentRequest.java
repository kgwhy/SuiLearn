package com.suilearn.api.dto;

import com.suilearn.api.model.GeneratedContentStatus;
import com.suilearn.api.model.SourceRef;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ReviewGeneratedContentRequest(
    @NotNull GeneratedContentStatus status,
    String stem,
    List<String> options,
    List<String> answer,
    String explanation,
    String categoryId,
    String categoryName,
    List<String> knowledgePointIds,
    List<SourceRef> sourceRefs
) {
}
