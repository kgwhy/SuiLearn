package com.suilearn.api.task.application;

import java.util.List;

interface OutboxEventStore {
    List<DurableOutboxEvent> pending();
    DurableOutboxEvent save(DurableOutboxEvent event);
}
