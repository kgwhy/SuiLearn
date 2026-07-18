package com.suilearn.api.agent.memory;

public record MemoryCandidate(
    String learnerId,
    MemoryType memoryType,
    String content,
    String contentFingerprint,
    double confidence,
    String sourceRunId,
    String sourceRef
) {
}
