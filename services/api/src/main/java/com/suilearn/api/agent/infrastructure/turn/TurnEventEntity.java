package com.suilearn.api.agent.infrastructure.turn;

import com.suilearn.api.agent.runtime.EventType;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(
    name = "turn_events",
    uniqueConstraints = @UniqueConstraint(name = "uk_turn_events_turn_seq", columnNames = {"turn_id", "seq"}),
    indexes = @Index(name = "idx_turn_events_session_created", columnList = "session_id, created_at")
)
public class TurnEventEntity {
    @EmbeddedId
    private TurnEventId id;
    private String sessionId;
    @Enumerated(EnumType.STRING)
    private EventType type;
    @Column(columnDefinition = "text")
    private String payload;
    private Instant createdAt;

    protected TurnEventEntity() {
    }

    public TurnEventEntity(TurnEventId id, String sessionId, EventType type, String payload, Instant createdAt) {
        this.id = id;
        this.sessionId = sessionId;
        this.type = type;
        this.payload = payload;
        this.createdAt = createdAt;
    }

    public TurnEventId getId() { return id; }
    public String getSessionId() { return sessionId; }
    public EventType getType() { return type; }
    public String getPayload() { return payload; }
    public Instant getCreatedAt() { return createdAt; }
}
