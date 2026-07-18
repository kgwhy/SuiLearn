package com.suilearn.api.agent.infrastructure.memory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(
    name = "agent_semantic_memories",
    indexes = @Index(name = "idx_agent_memory_learner_type", columnList = "learnerId,memoryType"),
    uniqueConstraints = @UniqueConstraint(
        name = "uk_agent_memory_learner_type_fingerprint",
        columnNames = {"learnerId", "memoryType", "contentFingerprint"})
)
public class AgentSemanticMemoryEntity {
    @Id
    private String id;
    private String learnerId;
    private String memoryType;
    @Column(columnDefinition = "text", nullable = false)
    private String content;
    @Column(nullable = false, length = 64)
    private String contentFingerprint;
    @Column(columnDefinition = "text", nullable = false)
    private String embeddingJson;
    private double confidence;
    private String sourceRunId;
    private String sourceRef;
    private Instant createdAt;
    private Instant updatedAt;

    protected AgentSemanticMemoryEntity() {
    }

    public AgentSemanticMemoryEntity(String id, String learnerId, String memoryType, String content,
                                     String contentFingerprint, String embeddingJson, double confidence,
                                     String sourceRunId, String sourceRef, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.learnerId = learnerId;
        this.memoryType = memoryType;
        this.content = content;
        this.contentFingerprint = contentFingerprint;
        this.embeddingJson = embeddingJson;
        this.confidence = confidence;
        this.sourceRunId = sourceRunId;
        this.sourceRef = sourceRef;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() { return id; }
    public String getLearnerId() { return learnerId; }
    public String getMemoryType() { return memoryType; }
    public String getContent() { return content; }
    public String getContentFingerprint() { return contentFingerprint; }
    public String getEmbeddingJson() { return embeddingJson; }
    public double getConfidence() { return confidence; }
    public String getSourceRunId() { return sourceRunId; }
    public String getSourceRef() { return sourceRef; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
