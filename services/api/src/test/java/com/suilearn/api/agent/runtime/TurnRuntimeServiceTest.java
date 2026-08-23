package com.suilearn.api.agent.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class TurnRuntimeServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-23T08:00:00Z");
    private final ObjectMapper mapper = JsonMapper.builder().findAndAddModules().build();
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void startReplayAndTerminalResultFollowContractSequence() throws Exception {
        var service = service((context, events) -> {
            events.publish(EventType.RESULT, "test", "report", "answer", Map.of());
            events.publishTerminal(EventType.DONE, TurnStatus.COMPLETED, "test", "report", "done", Map.of());
        });

        var outcome = service.start(command("sess-1", "study_agent"));
        TurnResult result = service.awaitResult(outcome.record().turnId(), Duration.ofSeconds(2));

        assertThat(result.status()).isEqualTo(TurnStatus.COMPLETED);
        assertThat(result.lastSeq()).isEqualTo(3);
        assertThat(result.terminalEvent().type()).isEqualTo(EventType.DONE);
        assertThat(service.eventsAfter(result.turnId(), 0).events())
            .extracting(StreamEvent::seq).containsExactly(1L, 2L, 3L);
        assertThat(service.eventsAfter(result.turnId(), 1).events())
            .extracting(StreamEvent::seq).containsExactly(2L, 3L);
        var beyond = service.eventsAfter(result.turnId(), 99);
        assertThat(beyond.lastSeq()).isEqualTo(3);
        assertThat(beyond.events()).isEmpty();
    }

    @Test
    void startRejectsUnknownCapabilityAndActiveSessionConflict() throws Exception {
        var started = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var service = service((context, events) -> {
            started.countDown();
            try {
                release.await(2, TimeUnit.SECONDS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        });

        assertThatThrownBy(() -> service.start(command("sess-unknown", "rag_qa")))
            .isInstanceOfSatisfying(TurnApiException.class, error ->
                assertThat(error.code()).isEqualTo(TurnErrorCode.AGENT_CAPABILITY_UNKNOWN));

        var first = service.start(command("sess-active", "study_agent"));
        assertThat(started.await(1, TimeUnit.SECONDS)).isTrue();
        assertThatThrownBy(() -> service.start(command("sess-active", "study_agent")))
            .isInstanceOfSatisfying(TurnApiException.class, error ->
                assertThat(error.code()).isEqualTo(TurnErrorCode.AGENT_TURN_ACTIVE_CONFLICT));
        service.cancel(first.record().turnId());
        release.countDown();
    }

    @Test
    void cancelStopsExecutorAndSubmitReplyRejectsNonWaitingTurn() throws Exception {
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var service = service((context, events) -> {
            events.publish(EventType.PROGRESS, "test", "work", "working", Map.of());
            entered.countDown();
            try {
                release.await(2, TimeUnit.SECONDS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
            events.publish(EventType.CONTENT, "test", "late", "late", Map.of());
        });

        var outcome = service.start(command("sess-cancel", "study_agent"));
        assertThat(entered.await(1, TimeUnit.SECONDS)).isTrue();
        var record = service.cancel(outcome.record().turnId());
        assertThat(record.status()).isEqualTo(TurnStatus.CANCELLED);
        release.countDown();

        assertThatThrownBy(() -> service.submitReply(outcome.record().turnId(), "reply", Map.of()))
            .isInstanceOfSatisfying(TurnApiException.class, error ->
                assertThat(error.code()).isEqualTo(TurnErrorCode.AGENT_TURN_NOT_WAITING_FOR_INPUT));
    }

    @Test
    void orphanRecoveryAppendsUniqueTerminalEvent() {
        var store = new InMemoryTurnStore();
        var context = context("turn-orphan", "sess-orphan");
        var first = new StreamEvent("turn-orphan", "sess-orphan", 1, EventType.TURN_STARTED,
            "study_agent", null, "", Map.of(), NOW);
        store.createTurn(context, "msg-orphan", first);
        var service = new TurnRuntimeService(store, new UnavailableTurnExecutor(), mapper, clock,
            Set.of("study_agent"), Executors.newVirtualThreadPerTaskExecutor());

        var recovered = service.recoverOrphans();

        assertThat(recovered).singleElement().satisfies(record -> {
            assertThat(record.status()).isEqualTo(TurnStatus.FAILED_ORPHANED);
            assertThat(record.lastSeq()).isEqualTo(2);
        });
        assertThat(store.findEventsAfter("turn-orphan", 0)).extracting(StreamEvent::seq).containsExactly(1L, 2L);
        assertThat(service.recoverOrphans()).isEmpty();
    }

    @Test
    void oversizedEventPayloadBecomesSanitizedTerminalFailure() throws Exception {
        var service = service((context, events) -> events.publish(EventType.CONTENT, "test", "bad",
            "x".repeat(TurnEventSink.MAX_EVENT_PAYLOAD_BYTES + 1), Map.of()));

        var outcome = service.start(command("sess-big", "study_agent"));
        TurnResult result = service.awaitResult(outcome.record().turnId(), Duration.ofSeconds(2));

        assertThat(result.status()).isEqualTo(TurnStatus.FAILED);
        assertThat(result.terminalEvent().type()).isEqualTo(EventType.FAILED);
        assertThat(service.eventsAfter(result.turnId(), 0).events().stream().map(StreamEvent::content))
            .doesNotContain("x".repeat(TurnEventSink.MAX_EVENT_PAYLOAD_BYTES + 1));
    }

    private TurnRuntimeService service(TurnExecutor executor) {
        return new TurnRuntimeService(new InMemoryTurnStore(), executor, mapper, clock,
            Set.of("study_agent"), Executors.newVirtualThreadPerTaskExecutor());
    }

    private StartTurnCommand command(String sessionId, String capability) {
        return new StartTurnCommand("learner-1", sessionId, "question", capability,
            new StudyScope("kb-1", null), List.of(), List.of());
    }

    private TurnContext context(String turnId, String sessionId) {
        return new TurnContext(turnId, sessionId, "learner-1", "study_agent",
            new StudyScope("kb-1", null), List.of(), "question", List.of(), List.of(), Map.of());
    }
}
