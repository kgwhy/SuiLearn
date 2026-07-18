package com.suilearn.api.agent.memory;

import java.util.List;

public record SemanticRecallResult(RecallStatus status, List<ScoredSemanticMemory> memories, String detail) {
    public SemanticRecallResult {
        memories = memories == null ? List.of() : List.copyOf(memories);
    }

    public static SemanticRecallResult available(List<ScoredSemanticMemory> memories) {
        return new SemanticRecallResult(RecallStatus.AVAILABLE, memories, null);
    }

    public static SemanticRecallResult degraded(String detail) {
        return new SemanticRecallResult(RecallStatus.LONG_TERM_MEMORY_DEGRADED, List.of(), detail);
    }
}
