package com.suilearn.api.agent.runtime;

import java.util.List;

public record StartTurnCommand(
    String learnerId,
    String sessionId,
    String message,
    String capability,
    StudyScope scope,
    List<SourceSelection> sources,
    List<Attachment> attachments
) {
    public StartTurnCommand {
        learnerId = requireText(learnerId, "learnerId");
        if (sessionId != null && sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId must be absent or non-blank");
        }
        message = requireText(message, "message");
        capability = capability == null || capability.isBlank() ? TurnContext.DEFAULT_CAPABILITY : capability.strip();
        if (scope == null) {
            throw new IllegalArgumentException("scope is required");
        }
        sources = List.copyOf(sources == null ? List.of() : sources);
        attachments = List.copyOf(attachments == null ? List.of() : attachments);
    }

    public static StartTurnCommand of(String learnerId, String sessionId, String message,
                                      String capability, StudyScope scope) {
        return new StartTurnCommand(learnerId, sessionId, message, capability, scope, List.of(), List.of());
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.strip();
    }
}
