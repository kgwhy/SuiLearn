package com.suilearn.api.agent.memory;

import java.util.Set;

public record SemanticMemoryQuery(String learnerId, Set<MemoryType> types, int topK) {
    public SemanticMemoryQuery {
        types = types == null ? Set.of() : Set.copyOf(types);
        if (learnerId == null || learnerId.isBlank() || types.isEmpty() || topK < 1) {
            throw new IllegalArgumentException("learnerId, memory types, and positive topK are required");
        }
    }
}
