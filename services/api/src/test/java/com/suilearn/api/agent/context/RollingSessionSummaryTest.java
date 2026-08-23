package com.suilearn.api.agent.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.suilearn.api.agent.infrastructure.turn.SessionMessageEntity;
import com.suilearn.api.agent.infrastructure.turn.SessionMessageJpaRepository;
import com.suilearn.api.agent.infrastructure.turn.SessionSummaryEntity;
import com.suilearn.api.agent.infrastructure.turn.SessionSummaryJpaRepository;
import com.suilearn.api.agent.llm.LlmClient;
import com.suilearn.api.agent.llm.LlmMessage;
import com.suilearn.api.agent.llm.LlmRequest;
import com.suilearn.api.agent.llm.LlmResponse;
import com.suilearn.api.agent.llm.LlmUsage;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RollingSessionSummaryTest {
    private static final Instant NOW = Instant.parse("2026-08-23T08:00:00Z");

    @Test
    void existingWatermarkIsIdempotentWithoutNewMessages() {
        var messages = mock(SessionMessageJpaRepository.class);
        var summaries = mock(SessionSummaryJpaRepository.class);
        var client = mock(LlmClient.class);
        when(messages.findBySessionIdAndTurnIdNotOrderByCreatedAtAsc("sess", "turn"))
            .thenReturn(List.of());
        when(summaries.findById("sess")).thenReturn(Optional.of(
            new SessionSummaryEntity("sess", "old summary", "msg-1", NOW, NOW)));
        var service = new RollingSessionSummary(messages, summaries, client,
            Clock.fixed(NOW, ZoneOffset.UTC), "fake-model", 4);

        assertThat(service.ensure("sess", "turn")).contains("old summary");
        verify(client, never()).chat(any());
    }

    @Test
    void antiDriftRebuildsFromOriginalMessages() {
        var messages = mock(SessionMessageJpaRepository.class);
        var summaries = mock(SessionSummaryJpaRepository.class);
        var client = mock(LlmClient.class);
        var old = new SessionSummaryEntity("sess", "stale", "m0", NOW.minusSeconds(60), NOW.minusSeconds(60));
        var m1 = new SessionMessageEntity("m1", "sess", "learner", "t1", "USER", "question 1", NOW.minusSeconds(10));
        var m2 = new SessionMessageEntity("m2", "sess", "learner", "t2", "ASSISTANT", "answer 1", NOW.minusSeconds(5));
        when(messages.findBySessionIdAndTurnIdNotOrderByCreatedAtAsc("sess", "turn")).thenReturn(List.of(m1, m2));
        when(summaries.findById("sess")).thenReturn(Optional.of(old));
        when(client.chat(any())).thenReturn(new LlmResponse("rebuilt", List.of(), new LlmUsage(2, 1), "stop"));
        var service = new RollingSessionSummary(messages, summaries, client,
            Clock.fixed(NOW, ZoneOffset.UTC), "fake-model", 4);

        assertThat(service.ensure("sess", "turn")).contains("rebuilt");
        ArgumentCaptor<SessionSummaryEntity> captor = ArgumentCaptor.forClass(SessionSummaryEntity.class);
        verify(summaries).save(captor.capture());
        assertThat(captor.getValue().getSessionId()).isEqualTo("sess");
        assertThat(captor.getValue().getSummary()).isEqualTo("rebuilt");
        assertThat(captor.getValue().getSummaryUpToMessageId()).isEqualTo("m2");
        assertThat(captor.getValue().getSummaryUpToCreatedAt()).isEqualTo(m2.getCreatedAt());
    }

    @Test
    void summarizerFailureReturnsExistingSummary() {
        var messages = mock(SessionMessageJpaRepository.class);
        var summaries = mock(SessionSummaryJpaRepository.class);
        var client = mock(LlmClient.class);
        var old = new SessionSummaryEntity("sess", "old", "m0", NOW.minusSeconds(60), NOW.minusSeconds(60));
        var m1 = new SessionMessageEntity("m1", "sess", "learner", "t1", "USER", "question", NOW.minusSeconds(10));
        when(messages.findBySessionIdAndTurnIdNotOrderByCreatedAtAsc("sess", "turn")).thenReturn(List.of(m1));
        when(summaries.findById("sess")).thenReturn(Optional.of(old));
        when(client.chat(any())).thenThrow(new IllegalStateException("AI down"));
        var service = new RollingSessionSummary(messages, summaries, client,
            Clock.fixed(NOW, ZoneOffset.UTC), "fake-model", 4);

        assertThat(service.ensure("sess", "turn")).contains("old");
    }
}
