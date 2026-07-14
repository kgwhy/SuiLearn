package com.suilearn.api.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.Locale;

@Entity
@Table(name = "processing_operations", uniqueConstraints = @UniqueConstraint(columnNames = "operationKey"))
public class ProcessingOperationEntity {
    private static final int MAX_ERROR_SUMMARY_LENGTH = 120;

    @Id private String id;
    private String operationKey;
    private String taskId;
    private String stage;
    private String state;
    private Integer attemptCount;
    private String resultReference;
    private String adapterVersion;
    private String errorCode;
    private String errorMessage;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant startedAt;
    private Instant finishedAt;

    protected ProcessingOperationEntity() { }

    public static ProcessingOperationEntity completed(
        String id, String operationKey, String taskId, String stage, String adapterVersion, String resultReference, Instant now
    ) {
        var operation = started(id, operationKey, taskId, stage, adapterVersion, now);
        operation.state = "SUCCEEDED";
        operation.resultReference = resultReference;
        operation.finishedAt = now;
        return operation;
    }

    public static ProcessingOperationEntity started(
        String id, String operationKey, String taskId, String stage, String adapterVersion, Instant now
    ) {
        var operation = new ProcessingOperationEntity();
        operation.id = id;
        operation.operationKey = operationKey;
        operation.taskId = taskId;
        operation.stage = stage;
        operation.adapterVersion = adapterVersion;
        operation.state = "STARTED";
        operation.attemptCount = 1;
        operation.createdAt = now;
        operation.updatedAt = now;
        operation.startedAt = now;
        return operation;
    }

    public String id() { return id; }
    public String state() { return state; }
    public String resultReference() { return resultReference; }
    public String errorCode() { return errorCode; }
    public String errorMessage() { return errorMessage; }

    public void restart(Instant now) {
        state = "STARTED";
        attemptCount = (attemptCount == null ? 0 : attemptCount) + 1;
        startedAt = now;
        updatedAt = now;
    }

    public void complete(String resultReference, Instant now) {
        state = "SUCCEEDED";
        this.resultReference = resultReference;
        finishedAt = now;
        updatedAt = now;
    }

    public void markRetryable(Instant now) { state = "RETRYABLE"; updatedAt = now; }

    public void fail(boolean permanent, String message, Instant now) {
        state = permanent ? "PERMANENT_FAILURE" : "RETRYABLE";
        errorCode = permanent ? "PERMANENT_FAILURE" : "RETRYABLE_FAILURE";
        errorMessage = sanitize(message);
        finishedAt = now;
        updatedAt = now;
    }

    private String sanitize(String message) {
        if (message == null || message.isBlank()) return "operation failure";
        String normalized = message.replaceAll("[\\r\\n\\t]+", " ").replaceAll("\\s{2,}", " ").trim()
            .toLowerCase(Locale.ROOT);
        String summary;
        if (normalized.contains("timeout") || normalized.contains("timed out")) {
            summary = "adapter request timed out";
        } else if (normalized.contains("unavailable") || normalized.contains("connection refused")
            || normalized.contains("connection reset") || normalized.contains("connect exception")) {
            summary = "adapter unavailable";
        } else if (normalized.contains("unsupported")) {
            summary = "unsupported adapter input";
        } else if (normalized.contains("invalid") || normalized.contains("malformed")) {
            summary = "invalid adapter request";
        } else {
            summary = "operation failure";
        }
        return summary.length() <= MAX_ERROR_SUMMARY_LENGTH ? summary : summary.substring(0, MAX_ERROR_SUMMARY_LENGTH);
    }
}
