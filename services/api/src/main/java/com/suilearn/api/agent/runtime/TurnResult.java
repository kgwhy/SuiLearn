package com.suilearn.api.agent.runtime;

import java.time.Instant;
import java.util.Objects;

/** Terminal REST snapshot returned by the synchronous turn endpoint. */
public record TurnResult(
    String turnId,
    String sessionId,
    TurnStatus status,
    long lastSeq,
    StreamEvent terminalEvent,
    Instant createdAt,
    Instant finishedAt
) {
    public TurnResult {
        turnId = requireText(turnId, "turnId");
        sessionId = requireText(sessionId, "sessionId");
        Objects.requireNonNull(status, "status");
        if (lastSeq < 1) {
            throw new IllegalArgumentException("lastSeq must be >= 1");
        }
        Objects.requireNonNull(terminalEvent, "terminalEvent");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(finishedAt, "finishedAt");
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.strip();
    }
}
