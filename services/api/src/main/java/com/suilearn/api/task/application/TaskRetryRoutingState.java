package com.suilearn.api.task.application;

import com.suilearn.api.model.TaskKind;
import com.suilearn.api.model.TaskLifecycleStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Records an accepted broker retry before its delayed delivery can be consumed. */
@Service
public class TaskRetryRoutingState {
    private final TaskService tasks;

    public TaskRetryRoutingState(TaskService tasks) {
        this.tasks = tasks;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void retryAccepted(String taskId, int retryCount) {
        if (taskId == null || taskId.isBlank()) {
            return;
        }
        var task = tasks.getTaskStatus(taskId);
        if (!isMaterialProcessing(task.kind()) || task.status() != TaskLifecycleStatus.FAILED) {
            return;
        }
        tasks.scheduleRetry(task, retryCount);
    }

    private boolean isMaterialProcessing(TaskKind kind) {
        return kind == TaskKind.MATERIAL_IMPORT || kind == TaskKind.MATERIAL_REPROCESS;
    }
}
