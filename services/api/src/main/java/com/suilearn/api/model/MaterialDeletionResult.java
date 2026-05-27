package com.suilearn.api.model;

import java.time.Instant;

public record MaterialDeletionResult(
    String materialId,
    MaterialStatus status,
    DeletedMaterialSavedContentPolicy savedContentPolicy,
    DeletedMaterialPendingContentPolicy pendingContentPolicy,
    int invalidatedChunkCount,
    int deletedPendingGeneratedContentCount,
    int retainedSavedQuestionCount,
    int retainedAiNoteCount,
    Instant deletedAt
) {
}
