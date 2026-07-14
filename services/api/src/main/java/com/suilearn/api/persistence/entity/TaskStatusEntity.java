package com.suilearn.api.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "generation_tasks")
public class TaskStatusEntity {
    @Id
    private String id;
    private String kind;
    private String status;
    private String knowledgeBaseId;
    private String materialId;
    private String generatedContentId;
    private String providerType;
    private String model;
    private Integer progressPercent;
    private String currentStep;
    private String errorCode;
    private String errorMessage;
    private Integer retryCount;
    private Integer attemptCount;
    private Instant nextRetryAt;
    private String correlationId;
    private String processingVersion;
    private String idempotencyKey;
    @Column(columnDefinition = "text")
    private String resultRefJson;
    private Instant createdAt;
    private Instant startedAt;
    private Instant finishedAt;
    private Instant updatedAt;

    protected TaskStatusEntity() {
    }

    public TaskStatusEntity(
        String id,
        String kind,
        String status,
        String knowledgeBaseId,
        String materialId,
        String generatedContentId,
        String providerType,
        String model,
        Integer progressPercent,
        String currentStep,
        String errorCode,
        String errorMessage,
        Integer retryCount,
        String resultRefJson,
        Instant createdAt,
        Instant startedAt,
        Instant finishedAt,
        Instant updatedAt
    ) {
        this.id = id;
        this.kind = kind;
        this.status = status;
        this.knowledgeBaseId = knowledgeBaseId;
        this.materialId = materialId;
        this.generatedContentId = generatedContentId;
        this.providerType = providerType;
        this.model = model;
        this.progressPercent = progressPercent;
        this.currentStep = currentStep;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.retryCount = retryCount;
        this.attemptCount = 0;
        this.nextRetryAt = null;
        this.correlationId = id;
        this.processingVersion = "v1";
        this.idempotencyKey = id;
        this.resultRefJson = resultRefJson;
        this.createdAt = createdAt;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public String getKind() {
        return kind;
    }

    public String getStatus() {
        return status;
    }

    public String getKnowledgeBaseId() {
        return knowledgeBaseId;
    }

    public String getMaterialId() {
        return materialId;
    }

    public String getGeneratedContentId() {
        return generatedContentId;
    }

    public String getProviderType() {
        return providerType;
    }

    public String getModel() {
        return model;
    }

    public Integer getProgressPercent() {
        return progressPercent;
    }

    public String getCurrentStep() {
        return currentStep;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public Integer getAttemptCount() { return attemptCount; }
    public Instant getNextRetryAt() { return nextRetryAt; }
    public String getCorrelationId() { return correlationId; }
    public String getProcessingVersion() { return processingVersion; }
    public String getIdempotencyKey() { return idempotencyKey; }

    public String getResultRefJson() {
        return resultRefJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
