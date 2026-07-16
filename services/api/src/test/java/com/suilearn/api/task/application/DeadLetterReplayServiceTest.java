package com.suilearn.api.task.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.suilearn.api.model.AiProviderType;
import com.suilearn.api.model.TaskKind;
import com.suilearn.api.model.TaskLifecycleStatus;
import com.suilearn.api.model.TaskStatus;
import com.suilearn.api.persistence.entity.DeadLetterMessageEntity;
import com.suilearn.api.persistence.entity.OutboxEventEntity;
import com.suilearn.api.persistence.repository.DeadLetterMessageJpaRepository;
import com.suilearn.api.persistence.repository.OutboxEventJpaRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

class DeadLetterReplayServiceTest {
    @Test
    void recordsDeadLetterMetadataWithoutPersistingTheMessageBody() {
        var deadLetters = mock(DeadLetterMessageJpaRepository.class);
        when(deadLetters.findById("message_1")).thenReturn(Optional.empty());
        var service = new DeadLetterReplayService(
            deadLetters, mock(OutboxEventJpaRepository.class), mock(TaskService.class), fixedClock()
        );

        service.record(message(), FailureKind.PERMANENT, new IllegalArgumentException("bad request body: secret"));

        var captured = ArgumentCaptor.forClass(DeadLetterMessageEntity.class);
        verify(deadLetters).save(captured.capture());
        assertThat(captured.getValue().originalMessageId()).isEqualTo("message_1");
        assertThat(captured.getValue().taskId()).isEqualTo("task_1");
        assertThat(captured.getValue().stage()).isEqualTo("PARSING");
        assertThat(captured.getValue().originalQueue()).isEqualTo("document.processing");
        assertThat(captured.getValue().retryCount()).isEqualTo(2);
        assertThat(captured.getValue().errorMessage()).doesNotContain("secret");
    }

    @Test
    void replayReopensTheFailedTaskAndPersistsAFreshDurableOutboxEvent() {
        var deadLetters = mock(DeadLetterMessageJpaRepository.class);
        var outbox = mock(OutboxEventJpaRepository.class);
        var tasks = mock(TaskService.class);
        var tracked = trackedMessage();
        stubReplay(deadLetters, outbox, tracked);
        var failedTask = failedTask();
        when(tasks.getTaskStatus("task_1")).thenReturn(failedTask);
        var service = new DeadLetterReplayService(deadLetters, outbox, tasks, fixedClock());

        service.replay("message_1");

        var event = ArgumentCaptor.forClass(OutboxEventEntity.class);
        verify(outbox).save(event.capture());
        assertThat(event.getValue().id()).isNotEqualTo("message_1");
        assertThat(event.getValue().taskId()).isEqualTo("task_1");
        assertThat(event.getValue().stage()).isEqualTo("PARSING");
        assertThat(event.getValue().payload()).isEqualTo("{\"safe\":true}");
        assertThat(event.getValue().retryCount()).isEqualTo(3);
        verify(tasks).scheduleRetry(failedTask, 3);
        verify(deadLetters).save(tracked);
        assertThat(Integer.valueOf(tracked.replayCount())).isEqualTo(1);
    }

    private DeadLetterMessageEntity trackedMessage() {
        return DeadLetterMessageEntity.recorded(
            "message_1", "task_1", "PARSING", "document.processing", 2, "PERMANENT", "IllegalArgumentException", fixedClock().instant()
        );
    }

    private void stubReplay(
        DeadLetterMessageJpaRepository deadLetters, OutboxEventJpaRepository outbox, DeadLetterMessageEntity tracked
    ) {
        when(deadLetters.findById("message_1")).thenReturn(Optional.of(tracked));
        when(outbox.findById("message_1")).thenReturn(Optional.of(OutboxEventEntity.pending(
            "message_1", "task_1", "PARSING", "task_1:PARSING", "{\"safe\":true}", fixedClock().instant()
        )));
    }

    private Message message() {
        var properties = new MessageProperties();
        properties.setMessageId("message_1");
        properties.setConsumerQueue("document.processing");
        properties.setHeader("x-suilearn-task-id", "task_1");
        properties.setHeader("x-suilearn-stage", "PARSING");
        properties.setHeader(ProcessingFailureRouter.RETRY_COUNT_HEADER, 2);
        return new Message("body that must never be persisted".getBytes(), properties);
    }

    private TaskStatus failedTask() {
        return new TaskStatus("task_1", TaskKind.MATERIAL_IMPORT, TaskLifecycleStatus.FAILED, "kb_1", "mat_1", null,
            AiProviderType.OPENAI_COMPATIBLE, "model", 0, "PARSING", "FAILED", "safe", 2, null,
            fixedClock().instant(), fixedClock().instant(), fixedClock().instant(), fixedClock().instant());
    }

    private Clock fixedClock() {
        return Clock.fixed(Instant.parse("2026-07-14T00:00:00Z"), ZoneOffset.UTC);
    }
}
