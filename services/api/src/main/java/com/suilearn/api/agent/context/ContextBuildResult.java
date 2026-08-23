package com.suilearn.api.agent.context;

import com.suilearn.api.agent.llm.LlmMessage;
import java.util.List;

public record ContextBuildResult(String systemPrompt, List<LlmMessage> messages,
                                 int estimatedContextTokens, int trimmedMessages) {
    public ContextBuildResult {
        if (systemPrompt == null || systemPrompt.isBlank()) {
            throw new IllegalArgumentException("systemPrompt is required");
        }
        messages = List.copyOf(messages == null ? List.of() : messages);
        if (estimatedContextTokens < 1 || trimmedMessages < 0) {
            throw new IllegalArgumentException("invalid context budget report");
        }
    }
}
