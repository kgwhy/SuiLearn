package com.suilearn.api.task.application;

import com.suilearn.api.persistence.entity.OutboxEventEntity;
import com.suilearn.api.persistence.repository.OutboxEventJpaRepository;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PersistentTransactionalOutbox {
    private final OutboxEventJpaRepository events;
    private final Clock clock;

    public PersistentTransactionalOutbox(OutboxEventJpaRepository events, Clock clock) {
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public OutboxEventEntity submit(String taskId, String stage, String idempotencyKey, String payload) {
        return events.save(OutboxEventEntity.pending(
            "outbox_" + UUID.randomUUID().toString().replace("-", ""), taskId, stage, idempotencyKey, payload, clock.instant()
        ));
    }
}
