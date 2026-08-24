package com.suilearn.api.agent.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.suilearn.api.agent.config.AgentConfigurationProperties;
import com.suilearn.api.agent.controller.TurnDtos.ReplyRequest;
import com.suilearn.api.agent.controller.TurnDtos.StartTurnRequest;
import com.suilearn.api.agent.controller.TurnDtos.ScopeRequest;
import com.suilearn.api.agent.runtime.EventType;
import com.suilearn.api.agent.runtime.InMemoryTurnStore;
import com.suilearn.api.agent.runtime.StartTurnCommand;
import com.suilearn.api.agent.runtime.StudyScope;
import com.suilearn.api.agent.runtime.TurnErrorCode;
import com.suilearn.api.agent.runtime.TurnRuntimeService;
import com.suilearn.api.agent.runtime.TurnApiException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

class AgentTurnControllerTest {
    private static final Instant NOW = Instant.parse("2026-08-23T08:00:00Z");

    @Test
    void controllerPathsMatchOpenApiResourceSurface() throws Exception {
        assertThat(AgentTurnController.class.getMethod("start", StartTurnRequest.class,
            org.springframework.security.core.Authentication.class)
            .getAnnotation(PostMapping.class).value()).containsExactly("/api/v2/agent/turns");
        assertThat(AgentTurnController.class.getMethod("events", String.class, long.class,
            org.springframework.security.core.Authentication.class)
            .getAnnotation(GetMapping.class).value()).containsExactly("/api/v2/agent/turns/{turnId}/events");
        assertThat(AgentTurnController.class.getMethod("activeTurn", String.class,
            org.springframework.security.core.Authentication.class)
            .getAnnotation(GetMapping.class).value()).containsExactly("/api/v2/agent/sessions/{sessionId}/active-turn");
    }

    @Test
    void synchronousStartReturnsTerminalResultAndReplayWorks() throws Exception {
        var runtime = runtime((context, events) -> {
            events.publish(EventType.RESULT, "test", "report", "answer", Map.of());
            events.publishTerminal(EventType.DONE, com.suilearn.api.agent.runtime.TurnStatus.COMPLETED,
                "test", "report", "done", Map.of());
        });
        var controller = new AgentTurnController(runtime, properties(true), Duration.ofSeconds(1));

        var response = controller.start(new StartTurnRequest("learner-1", "sess-1", "question",
            "study_agent", new ScopeRequest("kb-1", null), List.of()), null);

        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.lastSeq()).isEqualTo(3);
        assertThat(response.terminalEvent().type()).isEqualTo("done");

        var page = controller.events(response.turnId(), 1, null);
        assertThat(page.events()).extracting(TurnDtos.TurnEventResponse::seq).containsExactly(2L, 3L);
    }

    @Test
    void synchronousStartCarriesUsageSummaryFromLatestResultEvent() throws Exception {
        var runtime = runtime((context, events) -> {
            events.publish(EventType.RESULT, "study_agent", "loop", "answer",
                Map.of("toolCalls", 3, "promptTokens", 120L, "completionTokens", 30L,
                    "usageCostUsd", 0.0004d, "estimatedContextTokens", 2400));
            events.publishTerminal(EventType.DONE, com.suilearn.api.agent.runtime.TurnStatus.COMPLETED,
                "study_agent", "loop", "done", Map.of("toolCalls", 3));
        });
        var controller = new AgentTurnController(runtime, properties(true), Duration.ofSeconds(1));

        var response = controller.start(new StartTurnRequest("learner-1", "sess-1", "question",
            "study_agent", new ScopeRequest("kb-1", null), List.of()), null);

        assertThat(response.terminalEvent().type()).isEqualTo("done");
        assertThat(response.promptTokens()).isEqualTo(120L);
        assertThat(response.completionTokens()).isEqualTo(30L);
        assertThat(response.usageCostUsd()).isEqualTo(0.0004d);
        assertThat(response.actionTraceCount()).isEqualTo(3);
        assertThat(response.estimatedContextTokens()).isEqualTo(2400);
    }

    @Test
    void usageSummaryDefaultsToZeroWithoutResultMetadata() throws Exception {
        var runtime = runtime((context, events) -> events.publishTerminal(EventType.FAILED,
            com.suilearn.api.agent.runtime.TurnStatus.FAILED, "test", null, "failed", Map.of()));
        var controller = new AgentTurnController(runtime, properties(true), Duration.ofSeconds(1));

        var response = controller.start(new StartTurnRequest("learner-1", "sess-1", "question",
            null, new ScopeRequest("kb-1", null), List.of()), null);

        assertThat(response.promptTokens()).isZero();
        assertThat(response.completionTokens()).isZero();
        assertThat(response.usageCostUsd()).isZero();
        assertThat(response.actionTraceCount()).isZero();
        assertThat(response.estimatedContextTokens()).isZero();
    }

    @Test
    void disabledAgentAndMissingScopeAreRejected() {
        var runtime = runtime((context, events) -> events.publishTerminal(EventType.DONE,
            com.suilearn.api.agent.runtime.TurnStatus.COMPLETED, "test", null, "done", Map.of()));
        var disabled = new AgentTurnController(runtime, properties(false), Duration.ofSeconds(1));

        assertThatThrownBy(() -> disabled.start(new StartTurnRequest("learner-1", "sess-1", "question",
            null, new ScopeRequest("kb-1", null), List.of()), null))
            .isInstanceOfSatisfying(TurnApiException.class, error ->
                assertThat(error.code()).isEqualTo(TurnErrorCode.AGENT_FEATURE_DISABLED));

        var enabled = new AgentTurnController(runtime, properties(true), Duration.ofSeconds(1));
        assertThatThrownBy(() -> enabled.start(new StartTurnRequest("learner-1", "sess-1", "question",
            null, new ScopeRequest(null, null), List.of()), null))
            .isInstanceOfSatisfying(TurnApiException.class, error ->
                assertThat(error.code()).isEqualTo(TurnErrorCode.AGENT_SCOPE_REQUIRED));
    }

    @Test
    void replyCommandRejectsNonWaitingTurn() throws Exception {
        var runtime = runtime((context, events) -> {
            events.publish(EventType.PROGRESS, "test", null, "work", Map.of());
            events.publishTerminal(EventType.FAILED, com.suilearn.api.agent.runtime.TurnStatus.FAILED,
                "test", null, "failed", Map.of());
        });
        var controller = new AgentTurnController(runtime, properties(true), Duration.ofSeconds(1));
        var response = controller.start(new StartTurnRequest("learner-1", "sess-1", "question",
            null, new ScopeRequest("kb-1", null), List.of()), null);

        assertThatThrownBy(() -> controller.reply(response.turnId(), new ReplyRequest("reply", Map.of()), null))
            .isInstanceOfSatisfying(TurnApiException.class, error ->
                assertThat(error.code()).isEqualTo(TurnErrorCode.AGENT_TURN_NOT_WAITING_FOR_INPUT));
    }

    private TurnRuntimeService runtime(com.suilearn.api.agent.runtime.TurnExecutor executor) {
        var mapper = JsonMapper.builder().findAndAddModules().build();
        return new TurnRuntimeService(new InMemoryTurnStore(), executor, mapper,
            Clock.fixed(NOW, ZoneOffset.UTC), Set.of("study_agent"), Executors.newVirtualThreadPerTaskExecutor());
    }

    private AgentConfigurationProperties properties(boolean enabled) {
        return new AgentConfigurationProperties(enabled, 4, 3, 8, Duration.ofSeconds(90), 12000, 3,
            new AgentConfigurationProperties.Session(Duration.ofHours(24), 20),
            new AgentConfigurationProperties.Memory(5, 0.8));
    }
}
