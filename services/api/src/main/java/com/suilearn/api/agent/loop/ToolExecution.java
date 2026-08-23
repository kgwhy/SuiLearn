package com.suilearn.api.agent.loop;

import com.suilearn.api.agent.llm.LlmToolCall;
import com.suilearn.api.agent.tool.ToolResult;

public record ToolExecution(LlmToolCall call, ToolResult result) {
    public ToolExecution {
        if (call == null || result == null) {
            throw new IllegalArgumentException("call and result are required");
        }
    }
}
