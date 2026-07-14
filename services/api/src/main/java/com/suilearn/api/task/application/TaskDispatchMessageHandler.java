package com.suilearn.api.task.application;

import com.suilearn.api.model.TaskLifecycleStatus;
import org.springframework.amqp.core.Message;
import org.springframework.stereotype.Component;

/** Establishes the persisted task dispatch transition; stage-specific work is attached by Task 3.4. */
@Component
final class TaskDispatchMessageHandler implements ProcessingMessageHandler {
    static final String TASK_ID_HEADER = "x-suilearn-task-id";
    static final String STAGE_HEADER = "x-suilearn-stage";
    private final TaskService tasks;

    TaskDispatchMessageHandler(TaskService tasks) { this.tasks = tasks; }

    @Override
    public void handle(Message message) {
        String taskId = stringHeader(message, TASK_ID_HEADER);
        String stage = stringHeader(message, STAGE_HEADER);
        var task = tasks.getTaskStatus(taskId);
        if (task.status() == TaskLifecycleStatus.QUEUED) {
            tasks.startTask(task, stage);
        }
    }

    private String stringHeader(Message message, String name) {
        Object value = message.getMessageProperties().getHeaders().get(name);
        if (!(value instanceof String text) || text.isBlank()) {
            throw new IllegalArgumentException("Missing required processing message header: " + name);
        }
        return text;
    }
}
