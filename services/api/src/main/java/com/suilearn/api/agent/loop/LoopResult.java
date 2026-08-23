package com.suilearn.api.agent.loop;

import com.suilearn.api.agent.llm.LlmUsage;

public record LoopResult(Status status, String content, int toolCalls, LlmUsage usage) {
    public LoopResult {
        content = content == null ? "" : content;
        usage = usage == null ? LlmUsage.none() : usage;
    }

    public enum Status {
        COMPLETED,
        BUDGET_EXHAUSTED,
        INVALID_MODEL_OUTPUT,
        FAILED
    }
}
