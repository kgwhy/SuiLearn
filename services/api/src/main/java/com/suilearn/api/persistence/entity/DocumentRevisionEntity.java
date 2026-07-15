package com.suilearn.api.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(name = "document_revisions", uniqueConstraints = @UniqueConstraint(columnNames = {"materialId", "revisionNumber"}))
public class DocumentRevisionEntity {
    @Id private String id;
    private String materialId;
    private Integer revisionNumber;
    private String sourceChecksum;
    private String origin;
    private String processingVersion;
    private Instant createdAt;

    protected DocumentRevisionEntity() { }

    public DocumentRevisionEntity(
        String id, String materialId, Integer revisionNumber, String sourceChecksum, String processingVersion, Instant createdAt
    ) {
        this(id, materialId, revisionNumber, sourceChecksum, processingVersion, processingVersion, createdAt);
    }

    public DocumentRevisionEntity(
        String id, String materialId, Integer revisionNumber, String sourceChecksum, String origin, String processingVersion, Instant createdAt
    ) {
        this.id = id;
        this.materialId = materialId;
        this.revisionNumber = revisionNumber;
        this.sourceChecksum = sourceChecksum;
        this.origin = origin;
        this.processingVersion = processingVersion;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public String getMaterialId() { return materialId; }
    public Integer getRevisionNumber() { return revisionNumber; }
    public String getSourceChecksum() { return sourceChecksum; }
    public String getOrigin() { return origin; }
    public String getProcessingVersion() { return processingVersion; }
    public Instant getCreatedAt() { return createdAt; }
}
