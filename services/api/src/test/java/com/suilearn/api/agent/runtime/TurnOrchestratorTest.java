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

class TurnOrchestratorTest {
    private static final Instant NOW = Instant.parse("2026-08-23T08:00:00Z");

    @Test
    void runtimeRoutesDefaultCapabilityAndKeepsUnavailableTerminal() throws Exception {
        var capabilities = CapabilityRegistry.builtin();
        var orchestrator = new TurnOrchestrator(capabilities);
        var service = new TurnRuntimeService(new InMemoryTurnStore(), orchestrator,
            JsonMapper.builder().findAndAddModules().build(), Clock.fixed(NOW, ZoneOffset.UTC),
            Set.of("study_agent", "rag_qa", "question_generation"),
            Executors.newVirtualThreadPerTaskExecutor());

        var outcome = service.start(new StartTurnCommand("learner-1", "sess-1", "question", null,
            new StudyScope("kb-1", null), List.of(), List.of()));
        TurnResult result = service.awaitResult(outcome.record().turnId(), Duration.ofSeconds(2));

        assertThat(result.status()).isEqualTo(TurnStatus.FAILED);
        assertThat(service.eventsAfter(result.turnId(), 0).events())
            .extracting(StreamEvent::type)
            .containsExactly(EventType.TURN_STARTED, EventType.STAGE_START, EventType.PROGRESS,
                EventType.ERROR, EventType.FAILED);
        assertThat(service.eventsAfter(result.turnId(), 0).events())
            .extracting(StreamEvent::source)
            .allMatch(source -> source == null || source.equals("study_agent"));
    }

    @Test
    void unknownCapabilityIsRejectedBeforeTurnCreation() {
        var capabilities = CapabilityRegistry.builtin();
        var orchestrator = new TurnOrchestrator(capabilities);
        var service = new TurnRuntimeService(new InMemoryTurnStore(), orchestrator,
            JsonMapper.builder().findAndAddModules().build(), Clock.fixed(NOW, ZoneOffset.UTC),
            Set.of("study_agent", "rag_qa", "question_generation"),
            Executors.newVirtualThreadPerTaskExecutor());

        assertThatThrownBy(() -> service.start(new StartTurnCommand("learner-1", "sess-1", "question",
            "missing", new StudyScope("kb-1", null), List.of(), List.of())))
            .isInstanceOfSatisfying(TurnApiException.class, error ->
                assertThat(error.code()).isEqualTo(TurnErrorCode.AGENT_CAPABILITY_UNKNOWN));
    }
}
