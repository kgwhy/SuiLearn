package com.suilearn.api.task.application;

import com.suilearn.api.model.AiProviderType;
import com.suilearn.api.model.TaskKind;
import com.suilearn.api.model.TaskLifecycleStatus;
import com.suilearn.api.model.TaskResultRef;
import com.suilearn.api.model.TaskStatus;
import com.suilearn.api.task.infrastructure.TaskStore;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class TaskService {
    private final Clock clock;
    private final TaskStore taskStore;

    public TaskService(TaskStore taskStore, Clock clock) {
        this.clock = clock;
        this.taskStore = taskStore;
    }

    public TaskStatus getTaskStatus(String taskId) {
        return taskStore.find(taskId)
            .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));
    }

    public TaskStatus createTask(
        TaskKind kind,
        String knowledgeBaseId,
        String materialId,
        AiProviderType providerType,
        String model,
        String currentStep
    ) {
        var now = clock.instant();
        return taskStore.save(new TaskStatus(
            newId("task"),
            kind,
            TaskLifecycleStatus.QUEUED,
            knowledgeBaseId,
            materialId,
            null,
            providerType,
            model,
            0,
            currentStep,
            null,
            null,
            0,
            null,
            now,
            null,
            null,
            now
        ));
    }

    public TaskStatus startTask(TaskStatus task, String currentStep) {
        return updateTask(
            task,
            TaskLifecycleStatus.RUNNING,
            0,
            currentStep,
            null,
            null,
            null,
            task.materialId(),
            task.generatedContentId()
        );
    }

    /** Reopens a failed durable task only after the broker has accepted a retry delivery. */
    public TaskStatus scheduleRetry(TaskStatus task) {
        return scheduleRetry(task, task.retryCount() + 1);
    }

    /** Persists the broker retry header as the durable retry count. */
    public TaskStatus scheduleRetry(TaskStatus task, int retryCount) {
        if (retryCount < task.retryCount()) {
            throw new IllegalArgumentException("Retry count cannot move backwards");
        }
        var now = clock.instant();
        return taskStore.save(new TaskStatus(
            task.id(), task.kind(), TaskLifecycleStatus.QUEUED, task.knowledgeBaseId(), task.materialId(), task.generatedContentId(),
            task.providerType(), task.model(), 0, task.currentStep(), null, null, retryCount, null,
            task.createdAt(), null, null, now
        ));
    }

    public TaskStatus updateTask(
        TaskStatus existing,
        TaskLifecycleStatus status,
        Integer progressPercent,
        String currentStep,
        TaskResultRef resultRef,
        String errorCode,
        String errorMessage,
        String materialId,
        String generatedContentId
    ) {
        var now = clock.instant();
        return taskStore.save(new TaskStatus(
            existing.id(),
            existing.kind(),
            status,
            existing.knowledgeBaseId(),
            materialId == null ? existing.materialId() : materialId,
            generatedContentId == null ? existing.generatedContentId() : generatedContentId,
            existing.providerType(),
            existing.model(),
            progressPercent,
            currentStep,
            errorCode,
            errorMessage,
            existing.retryCount(),
            resultRef,
            existing.createdAt(),
            existing.startedAt() == null && status == TaskLifecycleStatus.RUNNING ? now : existing.startedAt(),
            isTerminal(status) ? now : existing.finishedAt(),
            now
        ));
    }

    private boolean isTerminal(TaskLifecycleStatus status) {
        return status == TaskLifecycleStatus.SUCCEEDED
            || status == TaskLifecycleStatus.FAILED
            || status == TaskLifecycleStatus.CANCELLED;
    }

    private String newId(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "");
    }
}
