package com.suilearn.api.agent.memory;

import java.time.Instant;
import java.util.List;

public record AgentSemanticMemory(
    String id,
    String learnerId,
    MemoryType memoryType,
    String content,
    String contentFingerprint,
    List<Double> embedding,
    double confidence,
    String sourceRunId,
    String sourceRef,
    Instant createdAt,
    Instant updatedAt
) {
    public AgentSemanticMemory {
        embedding = embedding == null ? List.of() : List.copyOf(embedding);
    }
}
