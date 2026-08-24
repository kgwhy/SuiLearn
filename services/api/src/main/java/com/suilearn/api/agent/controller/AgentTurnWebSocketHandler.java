package com.suilearn.api.agent.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suilearn.api.agent.config.AgentConfigurationProperties;
import com.suilearn.api.agent.runtime.AgentWebSocketProperties;
import com.suilearn.api.agent.runtime.Attachment;
import com.suilearn.api.agent.runtime.StartTurnCommand;
import com.suilearn.api.agent.runtime.StreamEvent;
import com.suilearn.api.agent.runtime.StudyScope;
import com.suilearn.api.agent.runtime.TurnApiException;
import com.suilearn.api.agent.runtime.TurnErrorCode;
import com.suilearn.api.agent.runtime.TurnEventSubscription;
import com.suilearn.api.agent.runtime.TurnEventListener;
import com.suilearn.api.agent.runtime.TurnRuntimeService;
import com.suilearn.api.security.AgentAuthProperties;
import com.suilearn.api.security.LearnerTokenHandshakeInterceptor;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

public class AgentTurnWebSocketHandler extends TextWebSocketHandler {
    private static final Logger LOG = LoggerFactory.getLogger(AgentTurnWebSocketHandler.class);

    private final TurnRuntimeService runtime;
    private final AgentConfigurationProperties agentProperties;
    private final AgentWebSocketProperties websocketProperties;
    private final ObjectMapper objectMapper;
    private final AgentAuthProperties authProperties;
    private final Map<WebSocketSession, Map<String, TurnEventSubscription>> subscriptions = new ConcurrentHashMap<>();

    public AgentTurnWebSocketHandler(TurnRuntimeService runtime,
                                     AgentConfigurationProperties agentProperties,
                                     AgentWebSocketProperties websocketProperties,
                                     ObjectMapper objectMapper) {
        this(runtime, agentProperties, websocketProperties, objectMapper, new AgentAuthProperties());
    }

    public AgentTurnWebSocketHandler(TurnRuntimeService runtime,
                                     AgentConfigurationProperties agentProperties,
                                     AgentWebSocketProperties websocketProperties,
                                     ObjectMapper objectMapper,
                                     AgentAuthProperties authProperties) {
        this.runtime = runtime;
        this.agentProperties = agentProperties;
        this.websocketProperties = websocketProperties;
        this.objectMapper = objectMapper;
        this.authProperties = authProperties;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        if (authProperties.isEnabled() && !hasAuthenticatedLearner(session)) {
            sendError(session, TurnErrorCode.AGENT_AUTH_REQUIRED, null);
            try {
                session.close(CloseStatus.POLICY_VIOLATION);
            } catch (IOException ignored) {
                // The sanitized error frame has already been sent.
            }
            return;
        }
        sendAck(session, "connect", null, null);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JsonNode root;
        try {
            root = objectMapper.readTree(message.getPayload());
        } catch (JsonProcessingException exception) {
            sendError(session, TurnErrorCode.INVALID_AGENT_REQUEST, null);
            return;
        }
        if (root == null || !root.isObject()) {
            sendError(session, TurnErrorCode.INVALID_AGENT_REQUEST, null);
            return;
        }
        try {
            dispatch(session, root);
        } catch (TurnApiException exception) {
            sendError(session, exception.code(), null);
        } catch (IllegalArgumentException exception) {
            sendError(session, TurnErrorCode.INVALID_AGENT_REQUEST, null);
        } catch (RuntimeException exception) {
            LOG.warn("Unexpected WS command failure", exception);
            sendError(session, TurnErrorCode.AGENT_DEPENDENCY_UNAVAILABLE, null);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        var sessionSubscriptions = subscriptions.remove(session);
        if (sessionSubscriptions != null) {
            sessionSubscriptions.values().forEach(TurnEventSubscription::close);
        }
    }

    private void dispatch(WebSocketSession session, JsonNode root) throws IOException {
        if (!"command".equals(root.path("kind").asText())) {
            sendError(session, TurnErrorCode.INVALID_AGENT_REQUEST, null);
            return;
        }
        if (!agentProperties.enabled()) {
            sendError(session, TurnErrorCode.AGENT_FEATURE_DISABLED, null);
            return;
        }
        if (!websocketProperties.isEnabled()) {
            sendError(session, TurnErrorCode.AGENT_WEBSOCKET_DISABLED, null);
            return;
        }
        if (authProperties.isEnabled() && !hasAuthenticatedLearner(session)) {
            sendError(session, TurnErrorCode.AGENT_AUTH_REQUIRED, null);
            return;
        }

        String command = root.path("command").asText("");
        switch (command) {
            case "ping" -> send(session, Map.of("kind", "pong", "ts", java.time.Instant.now().toString()));
            case "start_turn" -> startTurn(session, root);
            case "subscribe_turn" -> subscribe(session, root, false);
            case "resume_from" -> subscribe(session, root, true);
            case "cancel_turn" -> {
                var record = runtime.cancel(requiredText(root, "turnId"), learnerId(session, root, null));
                sendAck(session, "cancel_turn", record.turnId(), record.status().name());
            }
            case "submit_user_reply" -> {
                var record = runtime.submitReply(requiredText(root, "turnId"),
                    root.hasNonNull("text") ? root.get("text").asText() : null,
                    root.hasNonNull("answers") ? objectMapper.convertValue(root.get("answers"), Map.class) : null,
                    learnerId(session, root, null));
                sendAck(session, "submit_user_reply", record.turnId(), record.status().name());
            }
            case "check_active_turn" -> {
                var info = runtime.checkActiveTurn(requiredText(root, "sessionId"), learnerId(session, root, null));
                var payload = new LinkedHashMap<String, Object>();
                payload.put("kind", "ack");
                payload.put("command", "check_active_turn");
                payload.put("sessionId", info.sessionId());
                payload.put("turnId", info.turnId());
                payload.put("status", info.status() == null ? null : info.status().name());
                send(session, payload);
            }
            default -> sendError(session, TurnErrorCode.INVALID_AGENT_REQUEST, null);
        }
    }

    private void startTurn(WebSocketSession session, JsonNode root) {
        var request = root.path("scope");
        String knowledgeBaseId = textOrNull(request, "knowledgeBaseId");
        String materialId = textOrNull(request, "materialId");
        if (knowledgeBaseId == null && materialId == null) {
            throw new TurnApiException(TurnErrorCode.AGENT_SCOPE_REQUIRED);
        }
        var scope = new StudyScope(knowledgeBaseId, materialId);
        String requestedLearnerId = textOrNull(root, "learnerId");
        if (!authProperties.isEnabled() && requestedLearnerId == null) {
            throw new TurnApiException(TurnErrorCode.INVALID_AGENT_REQUEST);
        }
        var command = new StartTurnCommand(learnerId(session, root, requestedLearnerId),
            textOrNull(root, "sessionId"), requiredText(root, "message"), textOrNull(root, "capability"), scope,
            List.of(), attachments(root));
        var outcome = runtime.start(command);
        subscribe(session, outcome.record().turnId(), 0, command.learnerId());
        sendAck(session, "start_turn", outcome.record().turnId(), outcome.record().status().name());
    }

    private void subscribe(WebSocketSession session, JsonNode root, boolean resume) {
        String turnId = requiredText(root, "turnId");
        long afterSeq = root.path("afterSeq").asLong(0);
        String learnerId = learnerId(session, root, null);
        subscribe(session, turnId, afterSeq, learnerId);
        sendAck(session, resume ? "resume_from" : "subscribe_turn", turnId, null);
    }

    private void subscribe(WebSocketSession session, String turnId, long afterSeq, String learnerId) {
        var sessionSubscriptions = subscriptions.computeIfAbsent(session, ignored -> new ConcurrentHashMap<>());
        var previous = sessionSubscriptions.remove(turnId);
        if (previous != null) {
            previous.close();
        }
        TurnEventListener listener = event -> sendEvent(session, event);
        var subscription = runtime.subscribeReplaying(turnId, afterSeq, listener, learnerId);
        sessionSubscriptions.put(turnId, subscription);
    }

    private boolean hasAuthenticatedLearner(WebSocketSession session) {
        Object value = session.getAttributes().get(LearnerTokenHandshakeInterceptor.LEARNER_ID_ATTRIBUTE);
        Object failed = session.getAttributes().get(LearnerTokenHandshakeInterceptor.AUTH_FAILED_ATTRIBUTE);
        return value != null && !Boolean.TRUE.equals(failed);
    }

    private String learnerId(WebSocketSession session, JsonNode root, String requested) {
        if (!authProperties.isEnabled()) {
            return requested;
        }
        Object value = session.getAttributes().get(LearnerTokenHandshakeInterceptor.LEARNER_ID_ATTRIBUTE);
        Boolean failed = (Boolean) session.getAttributes().get(LearnerTokenHandshakeInterceptor.AUTH_FAILED_ATTRIBUTE);
        if (value == null || Boolean.TRUE.equals(failed)) {
            throw new TurnApiException(TurnErrorCode.AGENT_AUTH_REQUIRED);
        }
        return value.toString();
    }

    private void sendEvent(WebSocketSession session, StreamEvent event) {
        if (!session.isOpen()) {
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("kind", "event");
        payload.put("turnId", event.turnId());
        payload.put("sessionId", event.sessionId());
        payload.put("seq", event.seq());
        payload.put("type", event.type().wireName());
        payload.put("source", event.source());
        payload.put("stage", event.stage());
        payload.put("content", event.content());
        payload.put("metadata", event.metadata());
        payload.put("ts", event.ts().toString());
        send(session, payload);
    }

    private void sendAck(WebSocketSession session, String command, String turnId, String status) {
        var payload = new LinkedHashMap<String, Object>();
        payload.put("kind", "ack");
        payload.put("command", command);
        payload.put("turnId", turnId);
        payload.put("status", status);
        send(session, payload);
    }

    private void sendError(WebSocketSession session, TurnErrorCode code, String turnId) {
        var payload = new LinkedHashMap<String, Object>();
        payload.put("kind", "error");
        payload.put("code", code.name());
        payload.put("message", code.safeMessage());
        payload.put("turnId", turnId);
        send(session, payload);
    }

    private void send(WebSocketSession session, Map<String, Object> payload) {
        if (!session.isOpen()) {
            return;
        }
        try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
        } catch (IOException exception) {
            LOG.warn("Failed to send WebSocket message to session {}", session.getId(), exception);
            try {
                session.close(CloseStatus.SESSION_NOT_RELIABLE);
            } catch (IOException closeFailure) {
                LOG.debug("Failed to close broken WebSocket session", closeFailure);
            }
        }
    }

    private static List<Attachment> attachments(JsonNode root) {
        JsonNode attachments = root.path("attachments");
        if (attachments.isMissingNode() || !attachments.isArray()) {
            return List.of();
        }
        return java.util.stream.StreamSupport.stream(attachments.spliterator(), false)
            .filter(JsonNode::isObject)
            .map(item -> new Attachment(item.path("attachmentId").asText(""),
                item.path("mediaType").asText(null), item.path("reference").asText("")))
            .toList();
    }

    private static String requiredText(JsonNode root, String field) {
        String value = textOrNull(root, field);
        if (value == null) {
            throw new TurnApiException(TurnErrorCode.INVALID_AGENT_REQUEST);
        }
        return value;
    }

    private static String textOrNull(JsonNode root, String field) {
        JsonNode node = root.path(field);
        return node.isMissingNode() || node.isNull() || node.asText("").isBlank() ? null : node.asText().strip();
    }
}
