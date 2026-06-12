package com.suilearn.api.task.application;

import com.suilearn.api.model.TaskLifecycleStatus;
import com.suilearn.api.model.TaskResultRef;
import com.suilearn.api.model.TaskStatus;
import org.springframework.stereotype.Component;

@Component
public class TaskExecutor {
    private final TaskService taskService;

    public TaskExecutor(TaskService taskService) {
        this.taskService = taskService;
    }

    public <T> T runManagedTask(
        TaskStatus queuedTask,
        String startStep,
        TaskRun<T> taskRun,
        TaskFailureHandler<T> failureHandler
    ) {
        var execution = new TaskExecution(taskService.startTask(queuedTask, startStep));
        try {
            return taskRun.run(execution);
        } catch (RuntimeException exception) {
            return failureHandler.handle(execution, exception);
        }
    }

    @FunctionalInterface
    public interface TaskRun<T> {
        T run(TaskExecution execution);
    }

    @FunctionalInterface
    public interface TaskFailureHandler<T> {
        T handle(TaskExecution execution, RuntimeException exception);
    }

    public class TaskExecution {
        private TaskStatus current;

        private TaskExecution(TaskStatus current) {
            this.current = current;
        }

        public TaskStatus current() {
            return current;
        }

        public TaskStatus progress(int progressPercent, String currentStep, String materialId, String generatedContentId) {
            current = taskService.updateTask(
                current,
                TaskLifecycleStatus.RUNNING,
                progressPercent,
                currentStep,
                null,
                null,
                null,
                materialId,
                generatedContentId
            );
            return current;
        }

        public TaskStatus succeed(
            String currentStep,
            TaskResultRef resultRef,
            String materialId,
            String generatedContentId
        ) {
            current = taskService.updateTask(
                current,
                TaskLifecycleStatus.SUCCEEDED,
                100,
                currentStep,
                resultRef,
                null,
                null,
                materialId,
                generatedContentId
            );
            return current;
        }

        public TaskStatus fail(String errorCode, String errorMessage, String materialId, String generatedContentId) {
            current = taskService.updateTask(
                current,
                TaskLifecycleStatus.FAILED,
                100,
                "FAILED",
                null,
                errorCode,
                errorMessage,
                materialId,
                generatedContentId
            );
            return current;
        }
    }
}
