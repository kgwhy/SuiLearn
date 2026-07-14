package com.suilearn.api.task.application;

import java.time.Instant;

record ProcessingOperation(
    String id, String operationKey, String taskId, String stage, String adapterVersion,
    ProcessingOperationState state, int attempts, String resultReference, Instant updatedAt
) {
    ProcessingOperation started(Instant now) {
        return new ProcessingOperation(id, operationKey, taskId, stage, adapterVersion,
            ProcessingOperationState.STARTED, attempts + 1, null, now);
    }

    ProcessingOperation succeeded(String resultReference, Instant now) {
        return new ProcessingOperation(id, operationKey, taskId, stage, adapterVersion,
            ProcessingOperationState.SUCCEEDED, attempts, resultReference, now);
    }

    ProcessingOperation retryable(Instant now) {
        return new ProcessingOperation(id, operationKey, taskId, stage, adapterVersion,
            ProcessingOperationState.RETRYABLE, attempts, null, now);
    }
}
