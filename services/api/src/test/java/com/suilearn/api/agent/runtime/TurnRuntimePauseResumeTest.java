package com.suilearn.api.agent.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.json.JsonMapper;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;

class TurnRuntimePauseResumeTest {
    private static final Instant NOW = Instant.parse("2026-08-23T08:00:00Z");

    @Test
    void submitReplyDeliversToWaitingExecutorAndResumes() throws Exception {
        var store = new InMemoryTurnStore();
        var service = new TurnRuntimeService(store, (context, events) -> {
            try {
                var reply = events.pauseForUser("q1", "Which level?", false, Duration.ofSeconds(2));
                events.publish(EventType.RESULT, context.capability(), "resumed", "answer:" + reply.text(), Map.of());
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(interrupted);
            } catch (java.util.concurrent.TimeoutException timeout) {
                throw new IllegalStateException(timeout);
            }
            events.publishTerminal(EventType.DONE, TurnStatus.COMPLETED, context.capability(), "resumed",
                "done", Map.of());
        }, JsonMapper.builder().findAndAddModules().build(), Clock.fixed(NOW, ZoneOffset.UTC),
            Set.of("study_agent"), Executors.newVirtualThreadPerTaskExecutor());

        var outcome = service.start(new StartTurnCommand("learner", "sess", "question", "study_agent",
            new StudyScope("kb", null), List.of(), List.of()));
        var deadline = System.nanoTime() + Duration.ofSeconds(2).toNanos();
        while (store.findTurn(outcome.record().turnId()).orElseThrow().status() != TurnStatus.WAITING_INPUT
            && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
        assertThat(store.findTurn(outcome.record().turnId()).orElseThrow().status())
            .isEqualTo(TurnStatus.WAITING_INPUT);

        assertThatThrownBy(() -> service.submitReply("missing", "x", Map.of()))
            .isInstanceOfSatisfying(TurnApiException.class, error ->
                assertThat(error.code()).isEqualTo(TurnErrorCode.AGENT_TURN_NOT_FOUND));

        service.submitReply(outcome.record().turnId(), "EASY", Map.of());
        TurnResult result = service.awaitResult(outcome.record().turnId(), Duration.ofSeconds(2));

        assertThat(result.status()).isEqualTo(TurnStatus.COMPLETED);
        assertThat(service.eventsAfter(result.turnId(), 0).events()).extracting(StreamEvent::type)
            .contains(EventType.WAIT_FOR_INPUT, EventType.RESULT, EventType.DONE);
    }

    @Test
    void submitReplyRejectsNonWaitingTurn() throws Exception {
        var service = new TurnRuntimeService(new InMemoryTurnStore(), (context, events) ->
            events.publishTerminal(EventType.DONE, TurnStatus.COMPLETED, "test", null, "done", Map.of()),
            JsonMapper.builder().findAndAddModules().build(), Clock.fixed(NOW, ZoneOffset.UTC),
            Set.of("study_agent"), Executors.newVirtualThreadPerTaskExecutor());
        var outcome = service.start(new StartTurnCommand("learner", "sess", "question", "study_agent",
            new StudyScope("kb", null), List.of(), List.of()));
        service.awaitResult(outcome.record().turnId(), Duration.ofSeconds(2));

        assertThatThrownBy(() -> service.submitReply(outcome.record().turnId(), "x", Map.of()))
            .isInstanceOfSatisfying(TurnApiException.class, error ->
                assertThat(error.code()).isEqualTo(TurnErrorCode.AGENT_TURN_NOT_WAITING_FOR_INPUT));
    }
}
