package com.suilearn.api.task.application;

import com.suilearn.api.persistence.repository.InboundMessageJpaRepository;
import java.time.Clock;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

public class PersistentInboundMessageStore implements InboundMessageStore {
    private final InboundMessageJpaRepository messages;
    private final Clock clock;

    public PersistentInboundMessageStore(InboundMessageJpaRepository messages, Clock clock) {
        this.messages = messages;
        this.clock = clock;
    }

    @Override
    @Transactional
    public boolean claim(String messageId) {
        return messages.insertClaimIfAbsent(
            "inbound_" + UUID.randomUUID().toString().replace("-", ""), messageId, clock.instant()
        ) == 1;
    }

    @Override
    @Transactional
    public void complete(String messageId) {
        messages.findByMessageId(messageId).ifPresent(message -> message.complete(clock.instant()));
    }
}
