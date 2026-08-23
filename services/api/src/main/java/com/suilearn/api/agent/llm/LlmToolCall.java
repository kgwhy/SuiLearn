package com.suilearn.api.agent.llm;

public record LlmToolCall(String id, String name, String arguments) {
    public LlmToolCall {
        if (id == null || id.isBlank() || name == null || name.isBlank() || arguments == null) {
            throw new IllegalArgumentException("tool call id, name, and arguments are required");
        }
    }
}
