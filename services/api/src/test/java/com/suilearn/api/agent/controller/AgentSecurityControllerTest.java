package com.suilearn.api.agent.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.suilearn.api.agent.config.AgentConfigurationProperties;
import com.suilearn.api.agent.controller.TurnDtos.ScopeRequest;
import com.suilearn.api.agent.controller.TurnDtos.StartTurnRequest;
import com.suilearn.api.agent.runtime.EventType;
import com.suilearn.api.agent.runtime.InMemoryTurnStore;
import com.suilearn.api.agent.runtime.StudyScope;
import com.suilearn.api.agent.runtime.TurnApiException;
import com.suilearn.api.agent.runtime.TurnErrorCode;
import com.suilearn.api.agent.runtime.TurnRuntimeService;
import com.suilearn.api.security.AgentAuthProperties;
import com.suilearn.api.security.LearnerAuthenticationToken;
import com.suilearn.api.security.LearnerPrincipal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class AgentSecurityControllerTest {
    private static final Instant NOW = Instant.parse("2026-08-23T08:00:00Z");

    @Test
    void authenticatedStartUsesPrincipalLearnerInsteadOfRequestBody() throws Exception {
        var captured = new AtomicReference<String>();
        var runtime = runtime((context, events) -> {
            captured.set(context.learnerId());
            events.publishTerminal(EventType.DONE, com.suilearn.api.agent.runtime.TurnStatus.COMPLETED,
                "test", null, "done", Map.of());
        });
        var controller = new AgentTurnController(runtime, properties(true), Duration.ofSeconds(2), auth(true));

        controller.start(new StartTurnRequest("learner-requested", "sess-1", "question",
            "study_agent", new ScopeRequest("kb-1", null), List.of()),
            new LearnerAuthenticationToken(new LearnerPrincipal("learner-a")));

        assertThat(captured.get()).isEqualTo("learner-a");
    }

    @Test
    void authenticatedTurnAccessIsLearnerScoped() throws Exception {
        var runtime = runtime((context, events) -> events.publishTerminal(EventType.DONE,
            com.suilearn.api.agent.runtime.TurnStatus.COMPLETED, "test", null, "done", Map.of()));
        var controller = new AgentTurnController(runtime, properties(true), Duration.ofSeconds(2), auth(true));
        var response = controller.start(new StartTurnRequest("learner-a", "sess-1", "question",
            "study_agent", new ScopeRequest("kb-1", null), List.of()),
            new LearnerAuthenticationToken(new LearnerPrincipal("learner-a")));

        assertThatThrownBy(() -> controller.events(response.turnId(), 0,
            new LearnerAuthenticationToken(new LearnerPrincipal("learner-b"))))
            .isInstanceOfSatisfying(TurnApiException.class, error ->
                assertThat(error.code()).isEqualTo(TurnErrorCode.AGENT_TURN_NOT_FOUND));
    }

    @Test
    void authEnabledWithoutPrincipalFailsClosed() {
        var runtime = runtime((context, events) -> { });
        var controller = new AgentTurnController(runtime, properties(true), Duration.ofSeconds(2), auth(true));

        assertThatThrownBy(() -> controller.start(new StartTurnRequest("learner-a", "sess-1", "question",
            "study_agent", new ScopeRequest("kb-1", null), List.of()), null))
            .isInstanceOfSatisfying(TurnApiException.class, error ->
                assertThat(error.code()).isEqualTo(TurnErrorCode.AGENT_AUTH_REQUIRED));
    }

    private TurnRuntimeService runtime(com.suilearn.api.agent.runtime.TurnExecutor executor) {
        return new TurnRuntimeService(new InMemoryTurnStore(), executor,
            JsonMapper.builder().findAndAddModules().build(), Clock.fixed(NOW, ZoneOffset.UTC),
            Set.of("study_agent"), Executors.newVirtualThreadPerTaskExecutor());
    }

    private AgentConfigurationProperties properties(boolean enabled) {
        return new AgentConfigurationProperties(enabled, 4, 3, 8, Duration.ofSeconds(90), 12000, 3,
            new AgentConfigurationProperties.Session(Duration.ofHours(24), 20),
            new AgentConfigurationProperties.Memory(5, 0.8));
    }

    private AgentAuthProperties auth(boolean enabled) {
        var auth = new AgentAuthProperties();
        auth.setEnabled(enabled);
        return auth;
    }
}
