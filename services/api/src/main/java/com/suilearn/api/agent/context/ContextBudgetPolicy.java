package com.suilearn.api.agent.context;

import com.suilearn.api.agent.context.AgentContextSnapshot.Entry;
import com.suilearn.api.agent.context.AgentContextSnapshot.TrimEvent;
import com.suilearn.api.agent.context.AgentContextSnapshot.TrimReason;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ContextBudgetPolicy {
    private static final List<ContextSource> TRIM_ORDER = List.of(
        ContextSource.OBSERVATION,
        ContextSource.SEMANTIC_MEMORY,
        ContextSource.SESSION_SUMMARY,
        ContextSource.EVIDENCE
    );

    public Result apply(int mandatoryTokens, List<Entry> entries, int maximumTokens) {
        if (maximumTokens < 1) {
            throw new IllegalArgumentException("maximumTokens must be positive");
        }
        if (mandatoryTokens > maximumTokens) {
            throw new ContextBudgetExceededException(mandatoryTokens, maximumTokens);
        }

        var retained = new ArrayList<>(entries);
        var trimmed = new ArrayList<TrimEvent>();
        int total = mandatoryTokens + retained.stream().mapToInt(Entry::estimatedTokens).sum();
        for (ContextSource source : TRIM_ORDER) {
            if (total <= maximumTokens) {
                break;
            }
            List<Entry> candidates = retained.stream()
                .filter(entry -> entry.source() == source)
                .sorted(removalComparator(source))
                .toList();
            for (Entry candidate : candidates) {
                if (total <= maximumTokens) {
                    break;
                }
                retained.remove(candidate);
                total -= candidate.estimatedTokens();
                trimmed.add(new TrimEvent(source, TrimReason.CONTEXT_BUDGET, candidate.estimatedTokens()));
            }
        }
        return new Result(retained, trimmed, total);
    }

    private Comparator<Entry> removalComparator(ContextSource source) {
        Comparator<Entry> stableTieBreak = Comparator.comparing(Entry::stableId);
        if (source == ContextSource.SESSION_SUMMARY) {
            return Comparator.comparingLong(Entry::sequence).thenComparing(stableTieBreak);
        }
        return Comparator.comparingDouble(Entry::relevance)
            .thenComparingLong(Entry::sequence)
            .thenComparing(stableTieBreak);
    }

    public record Result(List<Entry> retained, List<TrimEvent> trimming, int estimatedTokens) {
        public Result {
            retained = List.copyOf(retained);
            trimming = List.copyOf(trimming);
        }
    }

    public static final class ContextBudgetExceededException extends IllegalArgumentException {
        public ContextBudgetExceededException(int mandatoryTokens, int maximumTokens) {
            super("immutable context requires " + mandatoryTokens + " estimated tokens, budget is " + maximumTokens);
        }
    }
}
