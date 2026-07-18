package com.suilearn.api.agent.prompt;

import java.util.List;

public record StructuredAgentOutput(Action action, String answer, List<String> citations) {
    public StructuredAgentOutput {
        citations = List.copyOf(citations == null ? List.of() : citations);
    }

    public enum Action {
        ANSWER,
        UNCERTAIN,
        PARTIAL
    }
}
