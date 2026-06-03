package com.suilearn.api.model;

import java.time.Instant;

public record TaskStatus(
    String id,
    TaskKind kind,
    TaskLifecycleStatus status,
    String knowledgeBaseId,
    String materialId,
    String generatedContentId,
    AiProviderType providerType,
    String model,
    Integer progressPercent,
    String currentStep,
    String errorCode,
    String errorMessage,
    Integer retryCount,
    TaskResultRef resultRef,
    Instant createdAt,
    Instant startedAt,
    Instant finishedAt,
    Instant updatedAt
) {
}
