package com.suilearn.api.agent.tool;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record ToolDefinition(
    String name,
    String description,
    Map<String, Object> parameters,
    boolean deferred,
    Set<String> requiredScopes
) {
    public ToolDefinition {
        name = requireText(name, "name");
        description = description == null ? "" : description.strip();
        parameters = immutableCopy(parameters);
        requiredScopes = Set.copyOf(requiredScopes == null ? Set.of() : requiredScopes);
    }

    private static Map<String, Object> immutableCopy(Map<String, Object> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        var copy = new LinkedHashMap<String, Object>(source);
        for (var entry : copy.entrySet()) {
            Objects.requireNonNull(entry.getValue(), "parameter values must not be null");
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
