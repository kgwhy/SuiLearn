package com.suilearn.api.model;

import java.util.List;

public record RagAnswer(
    String answer,
    boolean uncertain,
    List<SourceRef> citations,
    List<MaterialChunk> evidenceChunks,
    String savedAiNoteId
) {
}
