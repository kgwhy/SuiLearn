package com.suilearn.api.agent.tool;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record ToolResult(
    String content,
    List<ToolCitation> sources,
    Map<String, Object> metadata,
    boolean success,
    AskUserPayload pauseForUser
) {
    public ToolResult {
        content = content == null ? "" : content;
        sources = List.copyOf(sources == null ? List.of() : sources);
        metadata = immutableCopy(metadata);
        if (pauseForUser != null && success) {
            throw new IllegalArgumentException("pauseForUser requires success=false");
        }
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
}
