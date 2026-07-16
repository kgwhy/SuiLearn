package com.suilearn.api.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(name = "outbox_events", uniqueConstraints = @UniqueConstraint(columnNames = "idempotencyKey"))
public class OutboxEventEntity {
    @Id private String id;
    private String aggregateType;
    private String aggregateId;
    private String eventType;
    private String idempotencyKey;
    @Column(columnDefinition = "text") private String payload;
    private String state;
    private Integer attemptCount;
    private Integer retryCount;
    private Instant nextRetryAt;
    private Instant createdAt;
    private Instant publishedAt;

    protected OutboxEventEntity() { }

    public static OutboxEventEntity pending(
        String id, String taskId, String stage, String idempotencyKey, String payload, Instant createdAt
    ) {
        return pending(id, taskId, stage, idempotencyKey, payload, createdAt, 0);
    }

    public static OutboxEventEntity pending(
        String id, String taskId, String stage, String idempotencyKey, String payload, Instant createdAt, int retryCount
    ) {
        var event = new OutboxEventEntity();
        event.id = id;
        event.aggregateType = "ProcessingTask";
        event.aggregateId = taskId;
        event.eventType = stage;
        event.idempotencyKey = idempotencyKey;
        event.payload = payload;
        event.state = "PENDING";
        event.attemptCount = 0;
        event.retryCount = Math.max(retryCount, 0);
        event.createdAt = createdAt;
        return event;
    }

    public String id() { return id; }
    public String taskId() { return aggregateId; }
    public String stage() { return eventType; }
    public String payload() { return payload; }
    public String state() { return state; }
    public int attemptCount() { return attemptCount == null ? 0 : attemptCount; }
    public int retryCount() { return retryCount == null ? 0 : retryCount; }
    public Instant createdAt() { return createdAt; }
    public Instant nextRetryAt() { return nextRetryAt; }
    public Instant publishedAt() { return publishedAt; }

    public void markPublished(Instant publishedAt) { this.state = "PUBLISHED"; this.publishedAt = publishedAt; this.nextRetryAt = null; }
    public void scheduleRetry(Instant nextRetryAt) { this.state = "RETRY_WAIT"; this.attemptCount = attemptCount() + 1; this.nextRetryAt = nextRetryAt; }
    public void markDeadLetter() { this.state = "DEAD_LETTER"; this.attemptCount = attemptCount() + 1; this.nextRetryAt = null; }
}
