package com.suilearn.api.agent.context;

import com.suilearn.api.agent.infrastructure.turn.SessionMessageJpaRepository;
import com.suilearn.api.agent.llm.LlmMessage;
import java.util.ArrayList;
import java.util.List;

public final class SessionMessageHistory {
    private final SessionMessageJpaRepository messages;

    public SessionMessageHistory(SessionMessageJpaRepository messages) {
        this.messages = messages;
    }

    public List<LlmMessage> recent(String sessionId, String currentTurnId) {
        if (messages == null || sessionId == null || sessionId.isBlank()) {
            return List.of();
        }
        var entities = messages.findTop20BySessionIdAndTurnIdNotOrderByCreatedAtDesc(sessionId, currentTurnId);
        var reversed = new ArrayList<com.suilearn.api.agent.infrastructure.turn.SessionMessageEntity>(entities);
        java.util.Collections.reverse(reversed);
        return reversed.stream().map(entity -> switch (entity.getRole()) {
            case "USER" -> LlmMessage.user(entity.getContent());
            case "ASSISTANT" -> LlmMessage.assistant(entity.getContent(), List.of());
            case "TOOL" -> LlmMessage.tool(entity.getId(), entity.getContent());
            default -> LlmMessage.user(entity.getContent());
        }).toList();
    }
}
