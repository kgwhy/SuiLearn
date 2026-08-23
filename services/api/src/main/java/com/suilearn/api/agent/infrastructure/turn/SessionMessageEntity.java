package com.suilearn.api.agent.infrastructure.turn;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(
    name = "session_message",
    indexes = @Index(name = "idx_session_message_session_created", columnList = "session_id, created_at")
)
public class SessionMessageEntity {
    @Id
    private String id;
    private String sessionId;
    private String learnerId;
    private String turnId;
    private String role;
    @Column(columnDefinition = "text")
    private String content;
    private Instant createdAt;

    protected SessionMessageEntity() {
    }

    public SessionMessageEntity(String id, String sessionId, String learnerId, String turnId,
                                String role, String content, Instant createdAt) {
        this.id = id;
        this.sessionId = sessionId;
        this.learnerId = learnerId;
        this.turnId = turnId;
        this.role = role;
        this.content = content;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public String getSessionId() { return sessionId; }
    public String getLearnerId() { return learnerId; }
    public String getTurnId() { return turnId; }
    public String getRole() { return role; }
    public String getContent() { return content; }
    public Instant getCreatedAt() { return createdAt; }
}
