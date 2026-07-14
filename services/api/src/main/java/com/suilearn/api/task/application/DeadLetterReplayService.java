package com.suilearn.api.task.application;

import com.suilearn.api.persistence.entity.DeadLetterMessageEntity;
import com.suilearn.api.persistence.repository.DeadLetterMessageJpaRepository;
import com.suilearn.api.persistence.repository.OutboxEventJpaRepository;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.concurrent.TimeUnit;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Internal-only dead-letter audit and replay service; no public HTTP contract is exposed. */
@Service
public class DeadLetterReplayService {
    private static final String PROCESSING_EXCHANGE = "suilearn.processing";
    private final DeadLetterMessageJpaRepository deadLetters;
    private final OutboxEventJpaRepository outbox;
    private final RabbitTemplate rabbitTemplate;
    private final Clock clock;
    private final RabbitOutboxBrokerPublisher.ConfirmationAwaiter confirmationAwaiter;

    @Autowired
    public DeadLetterReplayService(
        DeadLetterMessageJpaRepository deadLetters,
        OutboxEventJpaRepository outbox,
        RabbitTemplate rabbitTemplate,
        Clock clock
    ) {
        this(deadLetters, outbox, rabbitTemplate, clock, null);
    }

    DeadLetterReplayService(
        DeadLetterMessageJpaRepository deadLetters,
        OutboxEventJpaRepository outbox,
        RabbitTemplate rabbitTemplate,
        Clock clock,
        RabbitOutboxBrokerPublisher.ConfirmationAwaiter confirmationAwaiter
    ) {
        this.deadLetters = deadLetters;
        this.outbox = outbox;
        this.rabbitTemplate = rabbitTemplate;
        this.clock = clock;
        this.confirmationAwaiter = confirmationAwaiter;
    }

    @Transactional
    public void record(Message message, FailureKind failureKind, RuntimeException failure) {
        String messageId = requiredMessageId(message);
        String taskId = header(message, TaskDispatchMessageHandler.TASK_ID_HEADER);
        String stage = header(message, TaskDispatchMessageHandler.STAGE_HEADER);
        String queue = message.getMessageProperties().getConsumerQueue();
        if (queue == null || queue.isBlank()) {
            throw new IllegalArgumentException("Processing message has no consumer queue", failure);
        }
        int retryCount = retryCount(message);
        String errorCode = failure.getClass().getSimpleName();
        var entry = deadLetters.findById(messageId).orElseGet(() -> DeadLetterMessageEntity.recorded(
            messageId, taskId, stage, queue, retryCount, failureKind.name(), errorCode, clock.instant()
        ));
        entry.refresh(retryCount, failureKind.name(), errorCode, clock.instant());
        deadLetters.save(entry);
    }

    @Transactional
    public void replay(String originalMessageId) {
        var tracked = deadLetters.findById(originalMessageId)
            .orElseThrow(() -> new IllegalArgumentException("Dead-letter message not found: " + originalMessageId));
        var event = outbox.findById(originalMessageId)
            .orElseThrow(() -> new IllegalStateException("Dead-letter replay requires its original local Outbox event"));
        if (!event.taskId().equals(tracked.taskId()) || !event.stage().equals(tracked.stage())) {
            throw new IllegalStateException("Dead-letter replay metadata does not match its original Outbox event");
        }
        publishConfirmed(event);
        tracked.markReplayed(clock.instant());
        deadLetters.save(tracked);
    }

    private void publishConfirmed(com.suilearn.api.persistence.entity.OutboxEventEntity event) {
        var correlation = new CorrelationData(event.id());
        rabbitTemplate.send(PROCESSING_EXCHANGE, routingKey(event.stage()), replayMessage(event), correlation);
        try {
            var confirmation = confirmationAwaiter == null
                ? correlation.getFuture().get(10, TimeUnit.SECONDS)
                : confirmationAwaiter.await();
            if (confirmation == null || !confirmation.isAck()) {
                throw new IllegalStateException("Dead-letter replay was not confirmed by RabbitMQ");
            }
        } catch (IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("Dead-letter replay was not confirmed by RabbitMQ", exception);
        }
    }

    private Message replayMessage(com.suilearn.api.persistence.entity.OutboxEventEntity event) {
        return MessageBuilder.withBody(event.payload().getBytes(StandardCharsets.UTF_8))
            .setMessageId(event.id())
            .setHeader(TaskDispatchMessageHandler.TASK_ID_HEADER, event.taskId())
            .setHeader(TaskDispatchMessageHandler.STAGE_HEADER, event.stage())
            .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
            .build();
    }

    private String routingKey(String stage) {
        return switch (stage) {
            case "GENERATING_KNOWLEDGE_POINTS" -> "knowledge-point.generation";
            case "GENERATING_QUESTIONS" -> "question.generation";
            default -> "document.processing";
        };
    }

    private String requiredMessageId(Message message) {
        String messageId = message.getMessageProperties().getMessageId();
        if (messageId == null || messageId.isBlank()) {
            throw new IllegalArgumentException("Processing message is missing messageId");
        }
        return messageId;
    }

    private String header(Message message, String name) {
        Object value = message.getMessageProperties().getHeaders().get(name);
        return value instanceof String text && !text.isBlank() ? text : null;
    }

    private int retryCount(Message message) {
        Object value = message.getMessageProperties().getHeaders().get(ProcessingFailureRouter.RETRY_COUNT_HEADER);
        if (value instanceof Number number) return Math.max(number.intValue(), 0);
        if (value instanceof String text) {
            try { return Math.max(Integer.parseInt(text), 0); }
            catch (NumberFormatException ignored) { return 0; }
        }
        return 0;
    }
}
