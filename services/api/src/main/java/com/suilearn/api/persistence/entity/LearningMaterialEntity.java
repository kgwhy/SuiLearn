package com.suilearn.api.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "learning_materials")
public class LearningMaterialEntity {
    @Id
    private String id;
    private String knowledgeBaseId;
    private String title;
    private String sourceType;
    private String status;
    private String importTaskId;
    private String embeddingTaskId;
    private String errorMessage;
    @Column(columnDefinition = "text")
    private String content;
    private Instant createdAt;
    private Instant deletedAt;

    protected LearningMaterialEntity() {
    }

    public LearningMaterialEntity(
        String id,
        String knowledgeBaseId,
        String title,
        String sourceType,
        String status,
        String importTaskId,
        String embeddingTaskId,
        String errorMessage,
        String content,
        Instant createdAt,
        Instant deletedAt
    ) {
        this.id = id;
        this.knowledgeBaseId = knowledgeBaseId;
        this.title = title;
        this.sourceType = sourceType;
        this.status = status;
        this.importTaskId = importTaskId;
        this.embeddingTaskId = embeddingTaskId;
        this.errorMessage = errorMessage;
        this.content = content;
        this.createdAt = createdAt;
        this.deletedAt = deletedAt;
    }

    public String getId() {
        return id;
    }

    public String getKnowledgeBaseId() {
        return knowledgeBaseId;
    }

    public String getTitle() {
        return title;
    }

    public String getSourceType() {
        return sourceType;
    }

    public String getStatus() {
        return status;
    }

    public String getImportTaskId() {
        return importTaskId;
    }

    public String getEmbeddingTaskId() {
        return embeddingTaskId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getContent() {
        return content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }
}
