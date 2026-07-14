package com.suilearn.api.task.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.LinkedHashSet;
import org.junit.jupiter.api.Test;

class DurableOutboxInfrastructureTest {
    private final Clock clock = Clock.fixed(Instant.parse("2026-07-13T10:00:00Z"), ZoneOffset.UTC);

    @Test
    void marksOutboxEventPublishedOnlyAfterBrokerConfirmAndRecoversUnconfirmedEvent() {
        var store = new InMemoryOutboxEventStore();
        var event = store.add("outbox_1", "task_1", "PARSING", "payload");
        var publisher = new OutboxDispatcher(store, ignored -> false, new RetryPolicy(3), clock);

        publisher.dispatchPending();

        assertThat(store.find(event.id()).state()).isEqualTo(OutboxDeliveryState.RETRY_WAIT);
        assertThat(store.find(event.id()).publishedAt()).isNull();

        publisher = new OutboxDispatcher(store, ignored -> true, new RetryPolicy(3), clock);
        publisher.dispatchPending();

        assertThat(store.find(event.id()).state()).isEqualTo(OutboxDeliveryState.PUBLISHED);
        assertThat(store.find(event.id()).publishedAt()).isEqualTo(clock.instant());
    }

    @Test
    void acknowledgesOnlyAfterCommittedHandlingAndNeverRunsDuplicateMessageTwice() {
        var messages = new InMemoryInboundMessageStore();
        var acknowledger = new RecordingManualAcknowledgment();
        var consumer = new DurableMessageConsumer(messages, new ImmediateTransactionBoundary());

        consumer.consume("message_1", () -> "committed", acknowledger);
        consumer.consume("message_1", () -> { throw new AssertionError("duplicate handler must not run"); }, acknowledger);

        assertThat(messages.completedIds()).containsExactly("message_1");
        assertThat(acknowledger.acknowledgedIds()).containsExactly("message_1", "message_1");
    }

    @Test
    void reusesCompletedOperationResultAndRecoversInterruptedOperationForRetry() {
        var operations = new InMemoryProcessingOperationStore();
        var service = new ProcessingOperationService(operations, clock);

        var first = service.claim("ocr:revision_1:page_1:v1", "task_1", "OCR", "tesseract-v1");
        service.complete(first.operationId(), "block_1");
        var replay = service.claim("ocr:revision_1:page_1:v1", "task_1", "OCR", "tesseract-v1");
        var interrupted = service.claim("ocr:revision_1:page_2:v1", "task_1", "OCR", "tesseract-v1");

        service.recoverInterrupted();

        assertThat(replay.disposition()).isEqualTo(OperationClaimDisposition.REUSE_COMPLETED);
        assertThat(replay.resultReference()).isEqualTo("block_1");
        assertThat(operations.find(interrupted.operationId()).orElseThrow().state()).isEqualTo(ProcessingOperationState.RETRYABLE);
    }

    @Test
    void sendsTransientFailuresToDlqAfterBoundedAttemptsAndDoesNotRetryPermanentFailures() {
        var retry = new RetryPolicy(2);

        assertThat(retry.next(1, FailureKind.TRANSIENT)).isEqualTo(DeliveryDecision.RETRY);
        assertThat(retry.next(2, FailureKind.TRANSIENT)).isEqualTo(DeliveryDecision.DEAD_LETTER);
        assertThat(retry.next(1, FailureKind.PERMANENT)).isEqualTo(DeliveryDecision.DEAD_LETTER);
        assertThat(MessagingTopology.queueNames()).containsExactlyInAnyOrder(
            "document.processing", "knowledge-point.generation", "question.generation"
        );
        assertThat(MessagingTopology.deadLetterQueueNames()).allMatch(name -> name.endsWith(".dlq"));
    }

    private static final class InMemoryOutboxEventStore implements OutboxEventStore {
        private final Map<String, DurableOutboxEvent> events = new LinkedHashMap<>();

        DurableOutboxEvent add(String id, String taskId, String stage, String payload) {
            var event = new DurableOutboxEvent(id, taskId, stage, payload, OutboxDeliveryState.PENDING, 0,
                Instant.parse("2026-07-13T10:00:00Z"), null, null);
            events.put(id, event);
            return event;
        }

        DurableOutboxEvent find(String id) { return events.get(id); }

        @Override public List<DurableOutboxEvent> pending() {
            return events.values().stream()
                .filter(event -> event.state() == OutboxDeliveryState.PENDING || event.state() == OutboxDeliveryState.RETRY_WAIT)
                .toList();
        }

        @Override public DurableOutboxEvent save(DurableOutboxEvent event) {
            events.put(event.id(), event);
            return event;
        }
    }

    private static final class InMemoryInboundMessageStore implements InboundMessageStore {
        private final Set<String> claimed = new LinkedHashSet<>();
        private final Set<String> completed = new LinkedHashSet<>();

        @Override public boolean claim(String messageId) { return claimed.add(messageId); }
        @Override public void complete(String messageId) { completed.add(messageId); }
        Set<String> completedIds() { return completed; }
    }

    private static final class RecordingManualAcknowledgment implements ManualAcknowledgment {
        private final List<String> acknowledged = new ArrayList<>();
        @Override public void acknowledge(String messageId) { acknowledged.add(messageId); }
        List<String> acknowledgedIds() { return acknowledged; }
    }

    private static final class ImmediateTransactionBoundary implements TransactionBoundary {
        @Override public <T> T execute(Work<T> work) { return work.run(); }
    }

    private static final class InMemoryProcessingOperationStore implements ProcessingOperationStore {
        private final Map<String, ProcessingOperation> byId = new LinkedHashMap<>();

        @Override public Optional<ProcessingOperation> findByOperationKey(String key) {
            return byId.values().stream().filter(operation -> operation.operationKey().equals(key)).findFirst();
        }

        @Override public Optional<ProcessingOperation> find(String id) { return Optional.ofNullable(byId.get(id)); }
        @Override public List<ProcessingOperation> started() {
            return byId.values().stream().filter(operation -> operation.state() == ProcessingOperationState.STARTED).toList();
        }
        @Override public ProcessingOperation save(ProcessingOperation operation) {
            byId.put(operation.id(), operation);
            return operation;
        }
    }
}
