package com.suilearn.api.agent.context;

import java.util.List;
import java.util.Objects;

public record AgentContextRequest(
    String systemContract,
    String currentTask,
    String scope,
    List<EvidenceItem> evidence,
    List<Candidate> sessionSummaries,
    List<Candidate> semanticMemories,
    List<Candidate> observations
) {
    public AgentContextRequest {
        systemContract = requireText(systemContract, "systemContract");
        currentTask = requireText(currentTask, "currentTask");
        scope = requireText(scope, "scope");
        evidence = List.copyOf(evidence == null ? List.of() : evidence);
        sessionSummaries = List.copyOf(sessionSummaries == null ? List.of() : sessionSummaries);
        semanticMemories = List.copyOf(semanticMemories == null ? List.of() : semanticMemories);
        observations = List.copyOf(observations == null ? List.of() : observations);
    }

    public record Candidate(String stableId, String content, double relevance, long sequence) {
        public Candidate {
            stableId = requireText(stableId, "stableId");
            content = requireText(content, "content");
            if (!Double.isFinite(relevance) || relevance < 0.0d || relevance > 1.0d) {
                throw new IllegalArgumentException("relevance must be between 0 and 1");
            }
            if (sequence < 0) {
                throw new IllegalArgumentException("sequence must not be negative");
            }
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return Objects.requireNonNull(value).strip();
    }
}
