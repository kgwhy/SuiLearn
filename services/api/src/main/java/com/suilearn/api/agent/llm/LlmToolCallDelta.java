package com.suilearn.api.agent.llm;

public record LlmToolCallDelta(int index, String id, String name, String argumentsDelta) {
    public LlmToolCallDelta {
        if (index < 0) {
            throw new IllegalArgumentException("tool call index must not be negative");
        }
        argumentsDelta = argumentsDelta == null ? "" : argumentsDelta;
    }
}
