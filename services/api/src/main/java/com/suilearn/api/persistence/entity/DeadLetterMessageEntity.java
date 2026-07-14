package com.suilearn.api.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Durable dead-letter audit metadata. Deliberately excludes the broker message body. */
@Entity
@Table(name = "dead_letter_messages")
public class DeadLetterMessageEntity {
    @Id private String originalMessageId;
    private String taskId;
    private String stage;
    private String originalQueue;
    private Integer retryCount;
    private String failureKind;
    private String errorCode;
    private String errorMessage;
    private Integer replayCount;
    private Instant deadLetteredAt;
    private Instant lastReplayedAt;

    protected DeadLetterMessageEntity() { }

    public static DeadLetterMessageEntity recorded(
        String originalMessageId,
        String taskId,
        String stage,
        String originalQueue,
        int retryCount,
        String failureKind,
        String errorCode,
        Instant deadLetteredAt
    ) {
        var entry = new DeadLetterMessageEntity();
        entry.originalMessageId = originalMessageId;
        entry.taskId = taskId;
        entry.stage = stage;
        entry.originalQueue = originalQueue;
        entry.retryCount = retryCount;
        entry.failureKind = failureKind;
        entry.errorCode = errorCode;
        entry.errorMessage = failureKind + "_FAILURE";
        entry.replayCount = 0;
        entry.deadLetteredAt = deadLetteredAt;
        return entry;
    }

    public void refresh(int retryCount, String failureKind, String errorCode, Instant deadLetteredAt) {
        this.retryCount = retryCount;
        this.failureKind = failureKind;
        this.errorCode = errorCode;
        this.errorMessage = failureKind + "_FAILURE";
        this.deadLetteredAt = deadLetteredAt;
    }

    public void markReplayed(Instant replayedAt) {
        this.replayCount = replayCount() + 1;
        this.lastReplayedAt = replayedAt;
    }

    public String originalMessageId() { return originalMessageId; }
    public String taskId() { return taskId; }
    public String stage() { return stage; }
    public String originalQueue() { return originalQueue; }
    public int retryCount() { return retryCount == null ? 0 : retryCount; }
    public String errorMessage() { return errorMessage; }
    public int replayCount() { return replayCount == null ? 0 : replayCount; }
}
