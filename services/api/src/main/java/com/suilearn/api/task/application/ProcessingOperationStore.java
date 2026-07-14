package com.suilearn.api.task.application;

import java.util.List;
import java.util.Optional;

interface ProcessingOperationStore {
    Optional<ProcessingOperation> findByOperationKey(String operationKey);
    Optional<ProcessingOperation> find(String id);
    List<ProcessingOperation> started();
    ProcessingOperation save(ProcessingOperation operation);
}
