package com.suilearn.api.task.application;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public final class OutboxRecoveryScheduler {
    private final PersistentOutboxDispatcher dispatcher;
    private final PersistentProcessingOperationClaims operations;

    public OutboxRecoveryScheduler(PersistentOutboxDispatcher dispatcher, PersistentProcessingOperationClaims operations) {
        this.dispatcher = dispatcher;
        this.operations = operations;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recoverAfterRestart() {
        operations.recoverInterrupted();
        dispatcher.dispatchDue();
    }

    @Scheduled(fixedDelayString = "${suilearn.outbox.dispatch-interval-ms:1000}")
    public void dispatchDue() {
        dispatcher.dispatchDue();
    }
}
