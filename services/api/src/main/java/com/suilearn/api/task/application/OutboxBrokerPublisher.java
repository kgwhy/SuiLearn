package com.suilearn.api.task.application;

@FunctionalInterface
public interface OutboxBrokerPublisher {
    boolean publish(DurableOutboxEvent event);
}
