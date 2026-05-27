package com.suilearn.api.model;

import java.time.Instant;
import java.util.List;

public record SavedAiNote(
    String id,
    String knowledgeBaseId,
    AiNoteType type,
    String title,
    String content,
    List<SourceRef> sourceRefs,
    Instant savedAt
) {
}
