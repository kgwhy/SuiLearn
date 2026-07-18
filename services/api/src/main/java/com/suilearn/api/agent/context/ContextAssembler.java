package com.suilearn.api.agent.context;

import com.suilearn.api.agent.context.AgentContextRequest.Candidate;
import com.suilearn.api.agent.context.AgentContextSnapshot.Entry;
import com.suilearn.api.agent.context.AgentContextSnapshot.TrimEvent;
import com.suilearn.api.agent.context.AgentContextSnapshot.TrimReason;
import com.suilearn.api.agent.context.AgentContextSnapshot.Trust;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.suilearn.api.agent.metrics.AgentMetrics;

public final class ContextAssembler {
    private final TokenEstimator tokenEstimator;
    private final ContextBudgetPolicy budgetPolicy;
    private final AgentMetrics metrics;

    public ContextAssembler(TokenEstimator tokenEstimator, ContextBudgetPolicy budgetPolicy) {
        this(tokenEstimator, budgetPolicy, AgentMetrics.noop());
    }

    public ContextAssembler(TokenEstimator tokenEstimator, ContextBudgetPolicy budgetPolicy, AgentMetrics metrics) {
        this.tokenEstimator = tokenEstimator;
        this.budgetPolicy = budgetPolicy;
        this.metrics = metrics;
    }

    public AgentContextSnapshot assemble(AgentContextRequest request, int maximumTokens) {
        List<Entry> candidates = new ArrayList<>();
        request.evidence().stream().filter(EvidenceItem::verified).forEach(item -> candidates.add(new Entry(
            ContextSource.EVIDENCE, item.stableId(), item.sourceRef(), item.content(), item.relevance(), 0,
            Trust.UNTRUSTED_DATA, tokenEstimator.estimate(item.content()))));
        addCandidates(candidates, request.sessionSummaries(), ContextSource.SESSION_SUMMARY);
        addCandidates(candidates, request.semanticMemories(), ContextSource.SEMANTIC_MEMORY);
        addCandidates(candidates, request.observations(), ContextSource.OBSERVATION);

        DedupeResult deduped = deduplicate(candidates);
        int mandatoryTokens = tokenEstimator.estimate(request.systemContract())
            + tokenEstimator.estimate(request.currentTask())
            + tokenEstimator.estimate(request.scope());
        ContextBudgetPolicy.Result budgeted = budgetPolicy.apply(mandatoryTokens, deduped.entries(), maximumTokens);

        List<TrimEvent> trimming = new ArrayList<>(deduped.trimming());
        trimming.addAll(budgeted.trimming());
        Map<ContextSource, Integer> bySource = tokenBreakdown(request, budgeted.retained());
        AgentContextSnapshot snapshot = new AgentContextSnapshot(
            request.systemContract(), request.currentTask(), request.scope(), budgeted.retained(),
            budgeted.estimatedTokens(), bySource, trimming);
        metrics.recordContextTokens(snapshot.estimatedTokens());
        return snapshot;
    }

    private void addCandidates(List<Entry> target, List<Candidate> candidates, ContextSource source) {
        for (Candidate candidate : candidates) {
            target.add(new Entry(source, candidate.stableId(), null, candidate.content(), candidate.relevance(),
                candidate.sequence(), Trust.UNTRUSTED_DATA, tokenEstimator.estimate(candidate.content())));
        }
    }

    private DedupeResult deduplicate(List<Entry> candidates) {
        Map<String, Entry> retained = new LinkedHashMap<>();
        List<TrimEvent> trimming = new ArrayList<>();
        for (Entry candidate : candidates) {
            String key = candidate.source().name() + "\u0000" + candidate.stableId();
            Entry existing = retained.get(key);
            if (existing == null) {
                retained.put(key, candidate);
            } else if (isBetter(candidate, existing)) {
                retained.put(key, candidate);
                trimming.add(new TrimEvent(existing.source(), TrimReason.DUPLICATE_STABLE_ID,
                    existing.estimatedTokens()));
            } else {
                trimming.add(new TrimEvent(candidate.source(), TrimReason.DUPLICATE_STABLE_ID,
                    candidate.estimatedTokens()));
            }
        }
        return new DedupeResult(List.copyOf(retained.values()), trimming);
    }

    private boolean isBetter(Entry candidate, Entry existing) {
        if (candidate.relevance() != existing.relevance()) {
            return candidate.relevance() > existing.relevance();
        }
        return candidate.sequence() > existing.sequence();
    }

    private Map<ContextSource, Integer> tokenBreakdown(AgentContextRequest request, List<Entry> retained) {
        var result = new EnumMap<ContextSource, Integer>(ContextSource.class);
        result.put(ContextSource.SYSTEM_CONTRACT, tokenEstimator.estimate(request.systemContract()));
        result.put(ContextSource.CURRENT_TASK, tokenEstimator.estimate(request.currentTask()));
        result.put(ContextSource.SCOPE, tokenEstimator.estimate(request.scope()));
        for (Entry entry : retained) {
            result.merge(entry.source(), entry.estimatedTokens(), Integer::sum);
        }
        return result;
    }

    private record DedupeResult(List<Entry> entries, List<TrimEvent> trimming) {
    }
}
