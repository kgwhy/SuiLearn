package com.suilearn.api.agent.memory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class WorkingMemory implements AutoCloseable {
    private final Map<String, Object> state = new HashMap<>();
    private boolean released;

    public void put(String key, Object value) {
        ensureOpen();
        state.put(requireText(key, "key"), value);
    }

    public Optional<Object> get(String key) {
        ensureOpen();
        return Optional.ofNullable(state.get(requireText(key, "key")));
    }

    public boolean isReleased() {
        return released;
    }

    @Override
    public void close() {
        state.clear();
        released = true;
    }

    private void ensureOpen() {
        if (released) {
            throw new IllegalStateException("working memory has been released");
        }
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }
}
