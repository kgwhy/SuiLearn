package com.suilearn.api.task.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.suilearn.api.material.application.MaterialImportService;
import com.suilearn.api.model.TaskKind;
import com.suilearn.api.model.TaskLifecycleStatus;
import com.suilearn.api.model.TaskStatus;
import java.util.Arrays;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

class TaskDispatchMessageHandlerContractTest {
    @Test
    void keepsTheMaterialWorkerAsADurableMessageHandlerDependency() {
        assertThat(Arrays.stream(TaskDispatchMessageHandler.class.getDeclaredFields())
            .map(field -> field.getType()))
            .anyMatch(type -> type == MaterialImportService.class);
    }

    @Test
    void dispatchesMaterialImportsToTheWorkerBeforeTheDurableConsumerCanAck() {
        var tasks = mock(TaskService.class);
        var imports = mock(MaterialImportService.class);
        var task = new TaskStatus("task_1", TaskKind.MATERIAL_IMPORT, TaskLifecycleStatus.QUEUED, "kb_1", "mat_1", null,
            null, null, 0, "UPLOADED", null, null, 0, null, Instant.EPOCH, null, null, Instant.EPOCH);
        when(tasks.getTaskStatus("task_1")).thenReturn(task);
        var properties = new MessageProperties();
        properties.setHeader(TaskDispatchMessageHandler.TASK_ID_HEADER, "task_1");
        properties.setHeader(TaskDispatchMessageHandler.STAGE_HEADER, "UPLOADED");

        new TaskDispatchMessageHandler(tasks, imports).handle(new Message(new byte[0], properties));

        verify(imports).consumeQueuedMaterialImport("mat_1", "task_1");
        verify(tasks, never()).startTask(task, "UPLOADED");
    }

    @Test
    void dispatchesARetryThatTheConfirmedBrokerRouteAlreadyQueuedWithoutChangingItsKind() {
        var tasks = mock(TaskService.class);
        var imports = mock(MaterialImportService.class);
        var queuedRetry = new TaskStatus("task_reprocess", TaskKind.MATERIAL_REPROCESS, TaskLifecycleStatus.QUEUED, "kb_1", "mat_1", null,
            null, null, 0, "REPROCESS", null, null, 1, null, Instant.EPOCH, null, null, Instant.EPOCH);
        when(tasks.getTaskStatus("task_reprocess")).thenReturn(queuedRetry);
        var properties = new MessageProperties();
        properties.setHeader(TaskDispatchMessageHandler.TASK_ID_HEADER, "task_reprocess");
        properties.setHeader(TaskDispatchMessageHandler.STAGE_HEADER, "REPROCESS");

        new TaskDispatchMessageHandler(tasks, imports).handle(new Message(new byte[0], properties));

        verify(tasks, never()).scheduleRetry(org.mockito.ArgumentMatchers.any());
        verify(imports).consumeQueuedMaterialImport("mat_1", "task_reprocess");
        assertThat(queuedRetry.kind()).isEqualTo(TaskKind.MATERIAL_REPROCESS);
    }

    @Test
    void doesNotReprocessAnAlreadyTerminalMaterialTaskOnDuplicateDelivery() {
        var tasks = mock(TaskService.class);
        var imports = mock(MaterialImportService.class);
        var task = new TaskStatus("task_1", TaskKind.MATERIAL_IMPORT, TaskLifecycleStatus.SUCCEEDED, "kb_1", "mat_1", null,
            null, null, 100, "READY", null, null, 0, null, Instant.EPOCH, Instant.EPOCH, Instant.EPOCH, Instant.EPOCH);
        when(tasks.getTaskStatus("task_1")).thenReturn(task);
        var properties = new MessageProperties();
        properties.setHeader(TaskDispatchMessageHandler.TASK_ID_HEADER, "task_1");
        properties.setHeader(TaskDispatchMessageHandler.STAGE_HEADER, "UPLOADED");

        new TaskDispatchMessageHandler(tasks, imports).handle(new Message(new byte[0], properties));

        verify(imports, never()).consumeQueuedMaterialImport("mat_1", "task_1");
    }

    @Test
    void doesNotRunAFailedMaterialImportWithoutAConfirmedRetryRoute() {
        var tasks = mock(TaskService.class);
        var imports = mock(MaterialImportService.class);
        var failed = new TaskStatus("task_1", TaskKind.MATERIAL_IMPORT, TaskLifecycleStatus.FAILED, "kb_1", "mat_1", null,
            null, null, 100, "FAILED", "MATERIAL_IMPORT_FAILED", "OCR failed", 0, null,
            Instant.EPOCH, Instant.EPOCH, Instant.EPOCH, Instant.EPOCH);
        when(tasks.getTaskStatus("task_1")).thenReturn(failed);
        var properties = new MessageProperties();
        properties.setHeader(TaskDispatchMessageHandler.TASK_ID_HEADER, "task_1");
        properties.setHeader(TaskDispatchMessageHandler.STAGE_HEADER, "UPLOADED");

        new TaskDispatchMessageHandler(tasks, imports).handle(new Message(new byte[0], properties));

        verify(tasks, never()).scheduleRetry(org.mockito.ArgumentMatchers.any());
        verify(imports, never()).consumeQueuedMaterialImport("mat_1", "task_1");
    }
}
