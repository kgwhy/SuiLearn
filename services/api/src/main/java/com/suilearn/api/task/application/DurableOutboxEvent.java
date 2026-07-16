package com.suilearn.api.task.application;

import java.time.Instant;

record DurableOutboxEvent(
    String id,
    String taskId,
    String stage,
    String payload,
    OutboxDeliveryState state,
    int attempts,
    Instant createdAt,
    Instant nextRetryAt,
    Instant publishedAt,
    int retryCount
) {
    DurableOutboxEvent(
        String id, String taskId, String stage, String payload, OutboxDeliveryState state, int attempts,
        Instant createdAt, Instant nextRetryAt, Instant publishedAt
    ) {
        this(id, taskId, stage, payload, state, attempts, createdAt, nextRetryAt, publishedAt, 0);
    }

    DurableOutboxEvent retryAt(Instant nextRetryAt) {
        return new DurableOutboxEvent(id, taskId, stage, payload, OutboxDeliveryState.RETRY_WAIT,
            attempts + 1, createdAt, nextRetryAt, null, retryCount);
    }

    DurableOutboxEvent deadLetter() {
        return new DurableOutboxEvent(id, taskId, stage, payload, OutboxDeliveryState.DEAD_LETTER,
            attempts + 1, createdAt, null, null, retryCount);
    }

    DurableOutboxEvent publishedAt(Instant publishedAt) {
        return new DurableOutboxEvent(id, taskId, stage, payload, OutboxDeliveryState.PUBLISHED,
            attempts, createdAt, null, publishedAt, retryCount);
    }
}
