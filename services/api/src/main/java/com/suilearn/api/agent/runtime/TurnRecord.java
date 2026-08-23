package com.suilearn.api.agent.runtime;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record TurnRecord(
    String turnId,
    String sessionId,
    String learnerId,
    String capability,
    TurnStatus status,
    StudyScope scope,
    List<SourceSelection> sources,
    String inputMessageId,
    long lastSeq,
    Instant createdAt,
    Instant startedAt,
    Instant finishedAt
) {
    public TurnRecord {
        turnId = requireText(turnId, "turnId");
        sessionId = requireText(sessionId, "sessionId");
        learnerId = requireText(learnerId, "learnerId");
        capability = capability == null ? TurnContext.DEFAULT_CAPABILITY : capability.strip();
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(scope, "scope");
        sources = List.copyOf(sources == null ? List.of() : sources);
        inputMessageId = requireText(inputMessageId, "inputMessageId");
        if (lastSeq < 1) {
            throw new IllegalArgumentException("lastSeq must be >= 1");
        }
        Objects.requireNonNull(createdAt, "createdAt");
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.strip();
    }
}
