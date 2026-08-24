package com.suilearn.api.agent.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.suilearn.api.agent.config.AgentConfigurationProperties;
import com.suilearn.api.agent.runtime.AgentWebSocketProperties;
import com.suilearn.api.agent.runtime.EventType;
import com.suilearn.api.agent.runtime.InMemoryTurnStore;
import com.suilearn.api.agent.runtime.StartTurnCommand;
import com.suilearn.api.agent.runtime.StudyScope;
import com.suilearn.api.agent.runtime.TurnRuntimeService;
import com.suilearn.api.security.AgentAuthProperties;
import com.suilearn.api.security.LearnerTokenHandshakeInterceptor;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

class AgentTurnWebSocketHandlerTest {
    private static final Instant NOW = Instant.parse("2026-08-23T08:00:00Z");
    private final ObjectMapper mapper = JsonMapper.builder().findAndAddModules().build();

    @Test
    void startTurnAutoSubscribesAndStreamsEventThenAck() throws Exception {
        var runtime = runtime((context, events) -> {
            events.publish(EventType.PROGRESS, "test", null, "work", Map.of());
            events.publishTerminal(EventType.FAILED, com.suilearn.api.agent.runtime.TurnStatus.FAILED,
                "test", null, "failed", Map.of());
        });
        var fixture = sessionFixture();
        var session = fixture.session();
        var handler = new AgentTurnWebSocketHandler(runtime, properties(true), wsProperties(true), mapper);

        handler.handleTextMessage(session, new TextMessage(mapper.writeValueAsString(Map.of(
            "kind", "command",
            "command", "start_turn",
            "learnerId", "learner-1",
            "sessionId", "sess-ws",
            "message", "question",
            "capability", "study_agent",
            "scope", Map.of("knowledgeBaseId", "kb-1")
        ))));

        List<JsonNode> frames = awaitFrames(fixture, 3);
        assertThat(frames).anyMatch(frame -> frame.path("kind").asText().equals("event")
            && frame.path("type").asText().equals("turn_started"));
        assertThat(frames).anyMatch(frame -> frame.path("kind").asText().equals("event")
            && frame.path("type").asText().equals("failed"));
        assertThat(frames).anyMatch(frame -> frame.path("kind").asText().equals("ack")
            && frame.path("command").asText().equals("start_turn"));
    }

    @Test
    void disabledAgentOrWebSocketReturnsStableError() throws Exception {
        var runtime = runtime((context, events) -> events.publishTerminal(EventType.DONE,
            com.suilearn.api.agent.runtime.TurnStatus.COMPLETED, "test", null, "done", Map.of()));
        var fixture = sessionFixture();
        var session = fixture.session();
        var handler = new AgentTurnWebSocketHandler(runtime, properties(false), wsProperties(true), mapper);
        handler.handleTextMessage(session, new TextMessage(mapper.writeValueAsString(Map.of(
            "kind", "command", "command", "ping"))));
        assertThat(awaitFrames(fixture, 1).getFirst().path("code").asText())
            .isEqualTo("AGENT_FEATURE_DISABLED");

        var wsDisabled = new AgentTurnWebSocketHandler(runtime, properties(true), wsProperties(false), mapper);
        var fixture2 = sessionFixture();
        var session2 = fixture2.session();
        wsDisabled.handleTextMessage(session2, new TextMessage(mapper.writeValueAsString(Map.of(
            "kind", "command", "command", "ping"))));
        assertThat(awaitFrames(fixture2, 1).getFirst().path("code").asText())
            .isEqualTo("AGENT_WEBSOCKET_DISABLED");
    }

    @Test
    void authenticatedWsUsesPrincipalLearnerAndRejectsMissingPrincipal() throws Exception {
        var captured = new AtomicReference<String>();
        var runtime = runtime((context, events) -> {
            captured.set(context.learnerId());
            events.publishTerminal(EventType.DONE, com.suilearn.api.agent.runtime.TurnStatus.COMPLETED,
                "test", null, "done", Map.of());
        });
        var auth = new AgentAuthProperties();
        auth.setEnabled(true);
        var fixture = sessionFixture(java.util.Map.of(
            LearnerTokenHandshakeInterceptor.LEARNER_ID_ATTRIBUTE, "learner-a",
            LearnerTokenHandshakeInterceptor.AUTH_FAILED_ATTRIBUTE, false));
        var handler = new AgentTurnWebSocketHandler(runtime, properties(true), wsProperties(true), mapper, auth);
        handler.handleTextMessage(fixture.session(), new TextMessage(mapper.writeValueAsString(Map.of(
            "kind", "command", "command", "start_turn",
            "learnerId", "learner-b",
            "message", "question",
            "scope", Map.of("knowledgeBaseId", "kb-1")
        ))));
        awaitFrames(fixture, 2);
        assertThat(captured.get()).isEqualTo("learner-a");

        var missingFixture = sessionFixture(java.util.Map.of(
            LearnerTokenHandshakeInterceptor.AUTH_FAILED_ATTRIBUTE, true));
        var missingHandler = new AgentTurnWebSocketHandler(runtime, properties(true), wsProperties(true), mapper, auth);
        missingHandler.handleTextMessage(missingFixture.session(), new TextMessage(mapper.writeValueAsString(Map.of(
            "kind", "command", "command", "ping"))));
        assertThat(awaitFrames(missingFixture, 1).getFirst().path("code").asText())
            .isEqualTo("AGENT_AUTH_REQUIRED");
    }

    @Test
    void pingReturnsPongWithoutPersistedEvent() throws Exception {
        var runtime = runtime((context, events) -> events.publishTerminal(EventType.DONE,
            com.suilearn.api.agent.runtime.TurnStatus.COMPLETED, "test", null, "done", Map.of()));
        var fixture = sessionFixture();
        var session = fixture.session();
        var handler = new AgentTurnWebSocketHandler(runtime, properties(true), wsProperties(true), mapper);
        handler.handleTextMessage(session, new TextMessage(mapper.writeValueAsString(Map.of(
            "kind", "command", "command", "ping"))));
        assertThat(awaitFrames(fixture, 1).getFirst().path("kind").asText()).isEqualTo("pong");
    }

    private TurnRuntimeService runtime(com.suilearn.api.agent.runtime.TurnExecutor executor) {
        return new TurnRuntimeService(new InMemoryTurnStore(), executor, mapper,
            Clock.fixed(NOW, ZoneOffset.UTC), Set.of("study_agent"), Executors.newVirtualThreadPerTaskExecutor());
    }

    private record SessionFixture(WebSocketSession session, List<TextMessage> sent) {}

    private SessionFixture sessionFixture() throws Exception {
        return sessionFixture(new ConcurrentHashMap<>());
    }

    private SessionFixture sessionFixture(java.util.Map<String, Object> attributes) throws Exception {
        var session = mock(WebSocketSession.class);
        var sent = new CopyOnWriteArrayList<TextMessage>();
        when(session.isOpen()).thenReturn(true);
        when(session.getId()).thenReturn("ws-1");
        when(session.getAttributes()).thenReturn(attributes);
        org.mockito.Mockito.doAnswer(invocation -> {
            sent.add(invocation.getArgument(0, TextMessage.class));
            return null;
        }).when(session).sendMessage(ArgumentMatchers.any(TextMessage.class));
        return new SessionFixture(session, sent);
    }

    private AgentConfigurationProperties properties(boolean enabled) {
        return new AgentConfigurationProperties(enabled, 4, 3, 8, Duration.ofSeconds(90), 12000, 3,
            new AgentConfigurationProperties.Session(Duration.ofHours(24), 20),
            new AgentConfigurationProperties.Memory(5, 0.8));
    }

    private AgentWebSocketProperties wsProperties(boolean enabled) {
        var properties = new AgentWebSocketProperties();
        properties.setEnabled(enabled);
        return properties;
    }

    private List<JsonNode> awaitFrames(SessionFixture fixture, int expected) throws Exception {
        var deadline = System.nanoTime() + Duration.ofSeconds(2).toNanos();
        while (fixture.sent().size() < expected && System.nanoTime() < deadline) {
            Thread.sleep(20);
        }
        assertThat(fixture.sent().size()).isGreaterThanOrEqualTo(expected);
        var frames = new ArrayList<JsonNode>();
        for (var message : fixture.sent()) {
            frames.add(mapper.readTree(message.getPayload()));
        }
        return frames;
    }
}
