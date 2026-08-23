package com.suilearn.api.agent.llm;

import java.util.List;

public record LlmMessage(String role, String content, List<LlmToolCall> toolCalls, String toolCallId) {
    public LlmMessage {
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("role is required");
        }
        content = content == null ? "" : content;
        toolCalls = List.copyOf(toolCalls == null ? List.of() : toolCalls);
    }

    public static LlmMessage system(String content) { return new LlmMessage("system", content, List.of(), null); }
    public static LlmMessage user(String content) { return new LlmMessage("user", content, List.of(), null); }
    public static LlmMessage assistant(String content, List<LlmToolCall> toolCalls) {
        return new LlmMessage("assistant", content, toolCalls, null);
    }
    public static LlmMessage tool(String toolCallId, String content) {
        return new LlmMessage("tool", content, List.of(), toolCallId);
    }
}
