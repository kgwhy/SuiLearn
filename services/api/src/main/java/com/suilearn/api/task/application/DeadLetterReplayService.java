package com.suilearn.api.task.application;

import com.suilearn.api.persistence.entity.DeadLetterMessageEntity;
import com.suilearn.api.persistence.entity.OutboxEventEntity;
import com.suilearn.api.persistence.repository.DeadLetterMessageJpaRepository;
import com.suilearn.api.persistence.repository.OutboxEventJpaRepository;
import java.time.Clock;
import java.util.UUID;
import org.springframework.amqp.core.Message;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Internal-only dead-letter audit and replay service; no public HTTP contract is exposed. */
@Service
public class DeadLetterReplayService {
    private final DeadLetterMessageJpaRepository deadLetters;
    private final OutboxEventJpaRepository outbox;
    private final TaskService tasks;
    private final Clock clock;

    public DeadLetterReplayService(
        DeadLetterMessageJpaRepository deadLetters,
        OutboxEventJpaRepository outbox,
        TaskService tasks,
        Clock clock
    ) {
        this.deadLetters = deadLetters;
        this.outbox = outbox;
        this.tasks = tasks;
        this.clock = clock;
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
        var task = tasks.getTaskStatus(tracked.taskId());
        if (task.status() != com.suilearn.api.model.TaskLifecycleStatus.FAILED) {
            throw new IllegalStateException("Dead-letter replay requires a failed task");
        }
        int nextRetryCount = tracked.retryCount() + 1;
        outbox.save(OutboxEventEntity.pending(
            newOutboxId(), event.taskId(), event.stage(), newIdempotencyKey(), event.payload(), clock.instant(), nextRetryCount
        ));
        tasks.scheduleRetry(task, nextRetryCount);
        tracked.markReplayed(clock.instant());
        deadLetters.save(tracked);
    }

    private String newOutboxId() {
        return "outbox_" + UUID.randomUUID().toString().replace("-", "");
    }

    private String newIdempotencyKey() {
        return "dlq-replay:" + UUID.randomUUID();
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
