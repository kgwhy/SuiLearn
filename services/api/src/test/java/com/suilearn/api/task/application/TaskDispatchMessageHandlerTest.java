package com.suilearn.api.task.application;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.suilearn.api.knowledgepoint.application.KnowledgePointService;
import com.suilearn.api.material.application.MaterialImportService;
import com.suilearn.api.model.TaskKind;
import com.suilearn.api.model.TaskLifecycleStatus;
import com.suilearn.api.model.TaskStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

class TaskDispatchMessageHandlerTest {
    @Test
    void dispatchesQueuedKnowledgePointExtractionToTheConsumer() {
        var tasks = mock(TaskService.class);
        var knowledgePoints = mock(KnowledgePointService.class);
        var task = task();
        when(tasks.getTaskStatus(task.id())).thenReturn(task);
        var handler = new TaskDispatchMessageHandler(tasks, mock(MaterialImportService.class), knowledgePoints, null, new ObjectMapper());

        handler.handle(message(task.id(), "GENERATING_KNOWLEDGE_POINTS"));

        verify(knowledgePoints).consumeGeneration(task.id(), task.materialId());
    }

    private static Message message(String taskId, String stage) {
        var properties = new MessageProperties();
        properties.setHeader(TaskDispatchMessageHandler.TASK_ID_HEADER, taskId);
        properties.setHeader(TaskDispatchMessageHandler.STAGE_HEADER, stage);
        return new Message(new byte[0], properties);
    }

    private static TaskStatus task() {
        var now = Instant.parse("2026-07-01T00:00:00Z");
        return new TaskStatus("task_1", TaskKind.KNOWLEDGE_POINT_EXTRACTION, TaskLifecycleStatus.QUEUED, "kb_1", "mat_1", null,
            null, null, 0, "QUEUED", null, null, 0, null, now, null, null, now);
    }
}
