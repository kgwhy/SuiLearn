package com.suilearn.api.agent.runtime;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record TurnContext(
    String turnId,
    String sessionId,
    String learnerId,
    String capability,
    StudyScope scope,
    List<SourceSelection> sources,
    String userMessage,
    List<ChatMessage> history,
    List<Attachment> attachments,
    Map<String, Object> metadata
) {
    public static final String DEFAULT_CAPABILITY = "study_agent";

    public TurnContext {
        turnId = requireText(turnId, "turnId");
        sessionId = requireText(sessionId, "sessionId");
        learnerId = requireText(learnerId, "learnerId");
        capability = capability == null || capability.isBlank() ? DEFAULT_CAPABILITY : capability.strip();
        Objects.requireNonNull(scope, "scope");
        sources = List.copyOf(sources == null ? List.of() : sources);
        userMessage = requireText(userMessage, "userMessage");
        history = List.copyOf(history == null ? List.of() : history);
        attachments = List.copyOf(attachments == null ? List.of() : attachments);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.strip();
    }
}
