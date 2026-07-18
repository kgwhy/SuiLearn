package com.suilearn.api.agent.application;

import com.suilearn.api.agent.memory.MemoryCandidate;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

@FunctionalInterface
public interface MemoryCandidateExtractor {
    Optional<MemoryCandidate> extract(Request request);

    record Request(String learnerId, String runId, String verifiedOutcome,
                   List<String> sourceReferences, Duration remaining) {
        public Request {
            sourceReferences = List.copyOf(sourceReferences == null ? List.of() : sourceReferences);
            if (learnerId == null || learnerId.isBlank() || runId == null || runId.isBlank()
                || verifiedOutcome == null || remaining == null || remaining.isNegative() || remaining.isZero()) {
                throw new IllegalArgumentException("valid extraction request is required");
            }
        }
    }
}
