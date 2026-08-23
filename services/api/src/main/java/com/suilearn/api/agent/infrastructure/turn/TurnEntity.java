package com.suilearn.api.agent.infrastructure.turn;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

@Entity
@Table(
    name = "turn",
    indexes = @Index(name = "idx_turn_session_created", columnList = "session_id, created_at")
)
public class TurnEntity {
    @Id
    private String id;
    private String sessionId;
    private String learnerId;
    private String capability;
    private String status;
    @Column(columnDefinition = "text")
    private String scopeJson;
    @Column(columnDefinition = "text")
    private String sourceSelectionJson;
    private String inputMessageId;
    private long lastSeq;
    private Instant createdAt;
    private Instant startedAt;
    private Instant finishedAt;
    private Instant updatedAt;
    @Version
    private Long version;

    protected TurnEntity() {
    }

    public TurnEntity(String id, String sessionId, String learnerId, String capability, String status,
                      String scopeJson, String sourceSelectionJson, String inputMessageId, long lastSeq,
                      Instant createdAt, Instant startedAt, Instant finishedAt, Instant updatedAt) {
        this.id = id;
        this.sessionId = sessionId;
        this.learnerId = learnerId;
        this.capability = capability;
        this.status = status;
        this.scopeJson = scopeJson;
        this.sourceSelectionJson = sourceSelectionJson;
        this.inputMessageId = inputMessageId;
        this.lastSeq = lastSeq;
        this.createdAt = createdAt;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.updatedAt = updatedAt;
    }

    public String getId() { return id; }
    public String getSessionId() { return sessionId; }
    public String getLearnerId() { return learnerId; }
    public String getCapability() { return capability; }
    public String getStatus() { return status; }
    public String getScopeJson() { return scopeJson; }
    public String getSourceSelectionJson() { return sourceSelectionJson; }
    public String getInputMessageId() { return inputMessageId; }
    public long getLastSeq() { return lastSeq; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }

    public void markStatus(String newStatus, Instant now) {
        this.status = newStatus;
        this.updatedAt = now;
        if (finishedAt == null && isTerminalStatus(newStatus)) {
            this.finishedAt = now;
        }
    }

    public void markLastSeq(long seq, Instant now) {
        this.lastSeq = seq;
        this.updatedAt = now;
    }

    private static boolean isTerminalStatus(String status) {
        return "COMPLETED".equals(status) || "CANCELLED".equals(status)
            || "FAILED".equals(status) || "FAILED_ORPHANED".equals(status);
    }
}
