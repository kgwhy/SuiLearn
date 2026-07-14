package com.suilearn.api.task.application;

import java.time.Clock;

final class OutboxDispatcher {
    private final OutboxEventStore store;
    private final OutboxBrokerPublisher publisher;
    private final RetryPolicy retryPolicy;
    private final Clock clock;

    OutboxDispatcher(OutboxEventStore store, OutboxBrokerPublisher publisher, RetryPolicy retryPolicy, Clock clock) {
        this.store = store;
        this.publisher = publisher;
        this.retryPolicy = retryPolicy;
        this.clock = clock;
    }

    void dispatchPending() {
        for (var event : store.pending()) {
            if (publisher.publish(event)) {
                store.save(event.publishedAt(clock.instant()));
            } else if (retryPolicy.next(event.attempts() + 1, FailureKind.TRANSIENT) == DeliveryDecision.RETRY) {
                store.save(event.retryAt(clock.instant().plusSeconds(1L << Math.min(event.attempts(), 6))));
            } else {
                store.save(event.deadLetter());
            }
        }
    }
}
