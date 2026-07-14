package com.suilearn.api.task.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

class DeadLetterReplayServiceTest {
    @Test
    void recordsDeadLetterMetadataWithoutPersistingTheMessageBody() {
        var deadLetters = mock(DeadLetterMessageJpaRepository.class);
        when(deadLetters.findById("message_1")).thenReturn(Optional.empty());
        var service = new DeadLetterReplayService(
            deadLetters, mock(OutboxEventJpaRepository.class), mock(RabbitTemplate.class), fixedClock()
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
    void manuallyReplaysTrackedOutboxMessageWithItsOriginalMessageIdentity() {
        var deadLetters = mock(DeadLetterMessageJpaRepository.class);
        var outbox = mock(OutboxEventJpaRepository.class);
        var template = mock(RabbitTemplate.class);
        var tracked = trackedMessage();
        stubReplay(deadLetters, outbox, tracked);
        var service = new DeadLetterReplayService(deadLetters, outbox, template, fixedClock(),
            () -> new CorrelationData.Confirm(true, null));

        service.replay("message_1");

        var message = ArgumentCaptor.forClass(Message.class);
        verify(template).send(eq("suilearn.processing"), eq("document.processing"), message.capture(), any(CorrelationData.class));
        assertThat(message.getValue().getMessageProperties().getMessageId()).isEqualTo("message_1");
        assertThat((String) message.getValue().getMessageProperties().getHeader("x-suilearn-task-id")).isEqualTo("task_1");
        verify(deadLetters).save(tracked);
        assertThat(Integer.valueOf(tracked.replayCount())).isEqualTo(1);
    }

    @Test
    void keepsDeadLetterPendingWhenBrokerNegativelyConfirmsReplay() {
        var deadLetters = mock(DeadLetterMessageJpaRepository.class);
        var outbox = mock(OutboxEventJpaRepository.class);
        var template = mock(RabbitTemplate.class);
        var tracked = trackedMessage();
        stubReplay(deadLetters, outbox, tracked);
        var service = new DeadLetterReplayService(deadLetters, outbox, template, fixedClock(),
            () -> new CorrelationData.Confirm(false, "broker rejected replay"));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.replay("message_1"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("not confirmed");

        verify(deadLetters, never()).save(tracked);
        assertThat(tracked.replayCount()).isZero();
    }

    @Test
    void keepsDeadLetterPendingWhenReplayConfirmationFails() {
        var deadLetters = mock(DeadLetterMessageJpaRepository.class);
        var outbox = mock(OutboxEventJpaRepository.class);
        var template = mock(RabbitTemplate.class);
        var tracked = trackedMessage();
        stubReplay(deadLetters, outbox, tracked);
        var service = new DeadLetterReplayService(deadLetters, outbox, template, fixedClock(),
            () -> { throw new java.util.concurrent.TimeoutException("confirm timed out"); });

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.replay("message_1"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("not confirmed");

        verify(deadLetters, never()).save(tracked);
        assertThat(tracked.replayCount()).isZero();
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

    private Clock fixedClock() {
        return Clock.fixed(Instant.parse("2026-07-14T00:00:00Z"), ZoneOffset.UTC);
    }
}
