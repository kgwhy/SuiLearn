package com.suilearn.api.task.application;

import java.time.Clock;
import java.util.UUID;

final class ProcessingOperationService {
    private final ProcessingOperationStore store;
    private final Clock clock;

    ProcessingOperationService(ProcessingOperationStore store, Clock clock) {
        this.store = store;
        this.clock = clock;
    }

    synchronized OperationClaim claim(String operationKey, String taskId, String stage, String adapterVersion) {
        var existing = store.findByOperationKey(operationKey);
        if (existing.isPresent() && existing.get().state() == ProcessingOperationState.SUCCEEDED) {
            return new OperationClaim(existing.get().id(), OperationClaimDisposition.REUSE_COMPLETED,
                existing.get().resultReference());
        }
        if (existing.isPresent() && existing.get().state() == ProcessingOperationState.STARTED) {
            return new OperationClaim(existing.get().id(), OperationClaimDisposition.ALREADY_RUNNING, null);
        }
        var operation = existing.orElseGet(() -> new ProcessingOperation(
            "operation_" + UUID.randomUUID().toString().replace("-", ""), operationKey, taskId, stage,
            adapterVersion, ProcessingOperationState.RETRYABLE, 0, null, clock.instant()
        ));
        operation = store.save(operation.started(clock.instant()));
        return new OperationClaim(operation.id(), OperationClaimDisposition.CLAIMED, null);
    }

    void complete(String operationId, String resultReference) {
        var operation = store.find(operationId).orElseThrow(() -> new IllegalArgumentException("Operation not found"));
        store.save(operation.succeeded(resultReference, clock.instant()));
    }

    void recoverInterrupted() {
        store.started().forEach(operation -> store.save(operation.retryable(clock.instant())));
    }
}
