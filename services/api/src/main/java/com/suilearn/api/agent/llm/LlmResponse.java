package com.suilearn.api.agent.llm;

import java.util.List;

public record LlmResponse(String content, List<LlmToolCall> toolCalls,
                          LlmUsage usage, String finishReason) {
    public LlmResponse {
        content = content == null ? "" : content;
        toolCalls = List.copyOf(toolCalls == null ? List.of() : toolCalls);
        usage = usage == null ? LlmUsage.none() : usage;
        finishReason = finishReason == null ? "" : finishReason;
    }
}
