package com.suilearn.api.model;

import java.time.Instant;
import java.util.List;

public record AiNoteDraft(
    String id,
    String knowledgeBaseId,
    String generationTaskId,
    AiNoteType type,
    String title,
    String content,
    List<SourceRef> sourceRefs,
    Instant createdAt
) {
}
