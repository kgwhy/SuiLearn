package com.suilearn.api.task.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.suilearn.api.persistence.entity.InboundMessageEntity;
import com.suilearn.api.persistence.repository.InboundMessageJpaRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PersistentInboundMessageStoreTest {
    @Test
    void atomicallyClaimsNewMessageAndRejectsDuplicateClaim() {
        var messages = mock(InboundMessageJpaRepository.class);
        when(messages.insertClaimIfAbsent(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.eq("message_1"), org.mockito.ArgumentMatchers.any()))
            .thenReturn(1, 0);
        var store = new PersistentInboundMessageStore(messages, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));

        assertThat(store.claim("message_1")).isTrue();
        assertThat(store.claim("message_1")).isFalse();
    }

    @Test
    void treatsConcurrentUniqueConstraintConflictAsAnAlreadyClaimedMessage() {
        var messages = mock(InboundMessageJpaRepository.class);
        when(messages.insertClaimIfAbsent(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.eq("message_1"), org.mockito.ArgumentMatchers.any()))
            .thenReturn(0);
        var store = new PersistentInboundMessageStore(messages, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));

        assertThat(store.claim("message_1")).isFalse();
    }
}
