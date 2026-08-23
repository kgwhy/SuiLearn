package com.suilearn.api.agent.runtime;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record StreamEvent(
    String turnId,
    String sessionId,
    long seq,
    EventType type,
    String source,
    String stage,
    String content,
    Map<String, Object> metadata,
    Instant ts
) {
    public StreamEvent {
        turnId = requireText(turnId, "turnId");
        sessionId = requireText(sessionId, "sessionId");
        if (seq < 1) {
            throw new IllegalArgumentException("seq must be >= 1");
        }
        Objects.requireNonNull(type, "type");
        source = source == null || source.isBlank() ? null : source.strip();
        stage = stage == null || stage.isBlank() ? null : stage.strip();
        content = content == null ? "" : content;
        metadata = immutableCopy(metadata);
        Objects.requireNonNull(ts, "ts");
    }

    public StreamEvent withSeqAndTs(long newSeq, Instant newTs) {
        return new StreamEvent(turnId, sessionId, newSeq, type, source, stage, content, metadata, newTs);
    }

    private static Map<String, Object> immutableCopy(Map<String, Object> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        var copy = new LinkedHashMap<String, Object>(source);
        for (var entry : copy.entrySet()) {
            Objects.requireNonNull(entry.getValue(), "metadata values must not be null");
        }
        return Collections.unmodifiableMap(copy);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.strip();
    }
}
