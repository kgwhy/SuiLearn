package com.suilearn.api.task.application;

final class DurableMessageConsumer {
    private final InboundMessageStore messages;
    private final TransactionBoundary transactionBoundary;

    DurableMessageConsumer(InboundMessageStore messages, TransactionBoundary transactionBoundary) {
        this.messages = messages;
        this.transactionBoundary = transactionBoundary;
    }

    void consume(String messageId, TransactionBoundary.Work<?> handler, ManualAcknowledgment acknowledgment) {
        transactionBoundary.execute(() -> {
            if (messages.claim(messageId)) {
                handler.run();
                messages.complete(messageId);
            }
            return null;
        });
        acknowledgment.acknowledge(messageId);
    }
}
