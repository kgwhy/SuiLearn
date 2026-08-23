package com.suilearn.api.agent.llm;

public record LlmUsage(long promptTokens, long completionTokens) {
    public LlmUsage {
        if (promptTokens < 0 || completionTokens < 0) {
            throw new IllegalArgumentException("usage tokens must not be negative");
        }
    }

    public static LlmUsage none() {
        return new LlmUsage(0, 0);
    }
}
