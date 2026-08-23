package com.suilearn.api.agent.infrastructure.turn;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

@Entity
@Table(name = "session_summary")
public class SessionSummaryEntity {
    @Id
    private String sessionId;
    @Column(columnDefinition = "text")
    private String summary;
    private String summaryUpToMessageId;
    private Instant summaryUpToCreatedAt;
    private Instant updatedAt;
    @Version
    private Long version;

    protected SessionSummaryEntity() {}

    public SessionSummaryEntity(String sessionId, String summary, String summaryUpToMessageId,
                                Instant summaryUpToCreatedAt, Instant updatedAt) {
        this.sessionId = sessionId;
        this.summary = summary;
        this.summaryUpToMessageId = summaryUpToMessageId;
        this.summaryUpToCreatedAt = summaryUpToCreatedAt;
        this.updatedAt = updatedAt;
    }

    public String getSessionId() { return sessionId; }
    public String getSummary() { return summary; }
    public String getSummaryUpToMessageId() { return summaryUpToMessageId; }
    public Instant getSummaryUpToCreatedAt() { return summaryUpToCreatedAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
