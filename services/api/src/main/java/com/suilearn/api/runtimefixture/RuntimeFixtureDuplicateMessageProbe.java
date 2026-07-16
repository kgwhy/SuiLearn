package com.suilearn.api.runtimefixture;

import com.suilearn.api.task.application.PersistentInboundMessageStore;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/** Exercises durable inbound-message idempotence using an internal identity that is never exposed. */
@Service
@Profile("runtime-fixture")
public final class RuntimeFixtureDuplicateMessageProbe {
    private final PersistentInboundMessageStore messages;

    public RuntimeFixtureDuplicateMessageProbe(PersistentInboundMessageStore messages) {
        this.messages = messages;
    }

    public DuplicateMessageProbeResponse trigger() {
        String messageId = "runtime-fixture-" + UUID.randomUUID();
        boolean firstDeliveryClaimed = messages.claim(messageId);
        boolean duplicateDeliveryRejected = !messages.claim(messageId);
        if (firstDeliveryClaimed) {
            messages.complete(messageId);
        }
        return new DuplicateMessageProbeResponse(firstDeliveryClaimed, duplicateDeliveryRejected);
    }

    public record DuplicateMessageProbeResponse(boolean firstDeliveryClaimed, boolean duplicateDeliveryRejected) { }
}
