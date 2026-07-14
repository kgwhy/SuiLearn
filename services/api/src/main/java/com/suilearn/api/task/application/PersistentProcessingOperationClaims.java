package com.suilearn.api.task.application;

import com.suilearn.api.persistence.entity.ProcessingOperationEntity;
import com.suilearn.api.persistence.repository.ProcessingOperationJpaRepository;
import java.time.Clock;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

public class PersistentProcessingOperationClaims {
    private final ProcessingOperationJpaRepository operations;
    private final Clock clock;

    public PersistentProcessingOperationClaims(ProcessingOperationJpaRepository operations, Clock clock) {
        this.operations = operations;
        this.clock = clock;
    }

    @Transactional
    public OperationClaim claim(String operationKey, String taskId, String stage, String adapterVersion) {
        var now = clock.instant();
        var id = "operation_" + UUID.randomUUID().toString().replace("-", "");
        if (operations.insertStartedIfAbsent(id, operationKey, taskId, stage, adapterVersion, now) == 1) {
            return new OperationClaim(id, OperationClaimDisposition.CLAIMED, null);
        }
        return resolveExistingClaim(operationKey, now);
    }

    private OperationClaim resolveExistingClaim(String operationKey, java.time.Instant now) {
        ProcessingOperationEntity operation = operations.findByOperationKey(operationKey)
            .orElseThrow(() -> new IllegalStateException("Operation claim conflict was not persisted"));
        if (operation.state().equals("SUCCEEDED")) {
            return new OperationClaim(operation.id(), OperationClaimDisposition.REUSE_COMPLETED, operation.resultReference());
        }
        if (operation.state().equals("RETRYABLE") && operations.restartRetryable(operationKey, now) == 1) {
            return new OperationClaim(operation.id(), OperationClaimDisposition.CLAIMED, null);
        }
        return new OperationClaim(operation.id(), OperationClaimDisposition.ALREADY_RUNNING, null);
    }

    @Transactional
    public void complete(String operationId, String resultReference) {
        var operation = operations.findById(operationId).orElseThrow(() -> new IllegalArgumentException("Operation not found"));
        operation.complete(resultReference, clock.instant());
    }

    @Transactional
    public void fail(String operationId, FailureKind failureKind, String message) {
        var operation = operations.findById(operationId).orElseThrow(() -> new IllegalArgumentException("Operation not found"));
        operation.fail(failureKind == FailureKind.PERMANENT, message, clock.instant());
    }

    @Transactional
    public void recoverInterrupted() {
        operations.findByState("STARTED").forEach(operation -> operation.markRetryable(clock.instant()));
    }
}
