package com.suilearn.api.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.suilearn.api.persistence.entity.InboundMessageEntity;
import com.suilearn.api.persistence.entity.OutboxEventEntity;
import com.suilearn.api.task.application.PersistentTransactionalOutbox;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

class TransactionalOutboxPersistenceTest {
    @Test
    void persistsOutboxInTheSubmissionTransactionAndDeduplicatesInboundMessageIds() throws Exception {
        assertThat(PersistentTransactionalOutbox.class.getMethod("submit", String.class, String.class, String.class, String.class)
            .getAnnotation(Transactional.class)).isNotNull();
        assertThat(OutboxEventEntity.class.getAnnotation(Table.class).name()).isEqualTo("outbox_events");
        var inbound = InboundMessageEntity.class.getAnnotation(Table.class);
        assertThat(inbound.name()).isEqualTo("inbound_messages");
        assertThat(inbound.uniqueConstraints()).anySatisfy(c -> assertThat(c.columnNames()).containsExactly("messageId"));
    }
}
