package com.suilearn.api.agent.llm;

import java.util.List;

public record LlmChunk(String contentDelta, List<LlmToolCallDelta> toolCallDeltas,
                       LlmUsage usage, String finishReason) {
    public LlmChunk {
        contentDelta = contentDelta == null ? "" : contentDelta;
        toolCallDeltas = List.copyOf(toolCallDeltas == null ? List.of() : toolCallDeltas);
    }
}
