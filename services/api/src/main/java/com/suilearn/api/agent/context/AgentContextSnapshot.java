package com.suilearn.api.agent.context;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record AgentContextSnapshot(
    String systemContract,
    String currentTask,
    String scope,
    List<Entry> supplemental,
    int estimatedTokens,
    Map<ContextSource, Integer> estimatedTokensBySource,
    List<TrimEvent> trimming
) {
    public AgentContextSnapshot {
        systemContract = Objects.requireNonNull(systemContract, "systemContract");
        currentTask = Objects.requireNonNull(currentTask, "currentTask");
        scope = Objects.requireNonNull(scope, "scope");
        supplemental = List.copyOf(supplemental);
        estimatedTokensBySource = Map.copyOf(new EnumMap<>(estimatedTokensBySource));
        trimming = List.copyOf(trimming);
        if (estimatedTokens < 0) {
            throw new IllegalArgumentException("estimatedTokens must not be negative");
        }
    }

    public record Entry(
        ContextSource source,
        String stableId,
        String sourceRef,
        String content,
        double relevance,
        long sequence,
        Trust trust,
        int estimatedTokens
    ) {
        public Entry {
            Objects.requireNonNull(source, "source");
            Objects.requireNonNull(stableId, "stableId");
            Objects.requireNonNull(content, "content");
            Objects.requireNonNull(trust, "trust");
            if (estimatedTokens < 1) {
                throw new IllegalArgumentException("estimatedTokens must be positive");
            }
        }
    }

    public enum Trust {
        TRUSTED_SYSTEM,
        UNTRUSTED_DATA
    }

    public record TrimEvent(ContextSource source, TrimReason reason, int estimatedTokens) {
        public TrimEvent {
            Objects.requireNonNull(source, "source");
            Objects.requireNonNull(reason, "reason");
            if (estimatedTokens < 1) {
                throw new IllegalArgumentException("estimatedTokens must be positive");
            }
        }
    }

    public enum TrimReason {
        DUPLICATE_STABLE_ID,
        CONTEXT_BUDGET
    }
}
