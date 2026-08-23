package com.suilearn.api.agent.runtime;

import java.time.Instant;
import java.util.Objects;

public record ChatMessage(String messageId, ChatRole role, String content, Instant createdAt) {
    public ChatMessage {
        messageId = requireText(messageId, "messageId");
        Objects.requireNonNull(role, "role");
        content = content == null ? "" : content;
        Objects.requireNonNull(createdAt, "createdAt");
    }

    public enum ChatRole {
        USER,
        ASSISTANT,
        TOOL
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.strip();
    }
}
