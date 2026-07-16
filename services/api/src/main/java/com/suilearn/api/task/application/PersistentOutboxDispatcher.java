package com.suilearn.api.task.application;

import com.suilearn.api.persistence.entity.OutboxEventEntity;
import com.suilearn.api.persistence.repository.OutboxEventJpaRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

public class PersistentOutboxDispatcher {
    private final OutboxEventJpaRepository events;
    private final OutboxBrokerPublisher publisher;
    private final RetryPolicy retryPolicy;
    private final Clock clock;

    public PersistentOutboxDispatcher(
        OutboxEventJpaRepository events, OutboxBrokerPublisher publisher, RetryPolicy retryPolicy, Clock clock
    ) {
        this.events = events;
        this.publisher = publisher;
        this.retryPolicy = retryPolicy;
        this.clock = clock;
    }

    @Transactional
    public void dispatchDue() {
        List<OutboxEventEntity> due = new ArrayList<>(events.findByStateOrderByCreatedAtAsc("PENDING"));
        due.addAll(events.findByStateInAndNextRetryAtLessThanEqualOrderByCreatedAtAsc(List.of("RETRY_WAIT"), clock.instant()));
        for (var event : due) {
            var delivery = new DurableOutboxEvent(event.id(), event.taskId(), event.stage(), event.payload(),
                OutboxDeliveryState.valueOf(event.state()), event.attemptCount(), event.createdAt(), event.nextRetryAt(), event.publishedAt(),
                event.retryCount());
            if (publisher.publish(delivery)) {
                event.markPublished(clock.instant());
            } else if (retryPolicy.next(event.attemptCount() + 1, FailureKind.TRANSIENT) == DeliveryDecision.RETRY) {
                event.scheduleRetry(clock.instant().plusSeconds(1L << Math.min(event.attemptCount(), 6)));
            } else {
                event.markDeadLetter();
            }
            events.save(event);
        }
    }
}
