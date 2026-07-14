package com.suilearn.api.task.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.suilearn.api.persistence.entity.OutboxEventEntity;
import com.suilearn.api.persistence.repository.OutboxEventJpaRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class PersistentOutboxDispatcherTest {
    @Test
    void marksPersistedEventPublishedOnlyAfterConfirm() {
        var events = mock(OutboxEventJpaRepository.class);
        var event = OutboxEventEntity.pending("outbox_1", "task_1", "PARSING", "key_1", "{}", Instant.EPOCH);
        when(events.findByStateOrderByCreatedAtAsc("PENDING")).thenReturn(List.of(event));
        when(events.findByStateInAndNextRetryAtLessThanEqualOrderByCreatedAtAsc(any(), any())).thenReturn(List.of());
        var dispatcher = new PersistentOutboxDispatcher(events, ignored -> true, new RetryPolicy(3),
            Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));

        dispatcher.dispatchDue();

        assertThat(event.state()).isEqualTo("PUBLISHED");
        verify(events).save(event);
    }
}
