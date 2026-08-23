package com.suilearn.api.agent.llm;

import java.util.List;
import java.util.Map;

public record LlmRequest(String model, List<LlmMessage> messages, List<Map<String, Object>> tools,
                         Double temperature, Integer maxTokens) {
    public LlmRequest {
        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException("model is required");
        }
        messages = List.copyOf(messages == null ? List.of() : messages);
        tools = List.copyOf(tools == null ? List.of() : tools);
        if (temperature != null && (!Double.isFinite(temperature) || temperature < 0 || temperature > 2)) {
            throw new IllegalArgumentException("temperature must be between 0 and 2");
        }
        if (maxTokens != null && maxTokens < 1) {
            throw new IllegalArgumentException("maxTokens must be positive");
        }
    }
}
