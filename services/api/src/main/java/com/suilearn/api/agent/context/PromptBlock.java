package com.suilearn.api.agent.context;

public record PromptBlock(String name, String content, int estimatedTokens) {
    public PromptBlock {
        if (name == null || name.isBlank() || content == null || estimatedTokens < 1) {
            throw new IllegalArgumentException("name, content, and positive estimatedTokens are required");
        }
    }
}
