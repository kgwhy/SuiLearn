package com.suilearn.api.task.application;

import com.suilearn.api.model.AiProviderType;
import com.suilearn.api.model.TaskKind;
import com.suilearn.api.model.TaskStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reusable task-domain transaction boundary for async submissions; orchestration attaches stages in Task 3.4. */
@Service
public class TaskOutboxSubmissionService {
    private final TaskService tasks;
    private final PersistentTransactionalOutbox outbox;

    public TaskOutboxSubmissionService(TaskService tasks, PersistentTransactionalOutbox outbox) {
        this.tasks = tasks;
        this.outbox = outbox;
    }

    @Transactional
    public TaskStatus submit(
        TaskKind kind, String knowledgeBaseId, String materialId, AiProviderType providerType, String model,
        String currentStep, String stage, String idempotencyKey, String payload
    ) {
        TaskStatus task = tasks.createTask(kind, knowledgeBaseId, materialId, providerType, model, currentStep);
        outbox.submit(task.id(), stage, idempotencyKey, payload);
        return task;
    }
}
