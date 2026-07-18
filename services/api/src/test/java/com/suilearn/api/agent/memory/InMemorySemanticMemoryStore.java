package com.suilearn.api.agent.memory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Comparator;

final class InMemorySemanticMemoryStore implements SemanticMemoryStore {
    private final Map<String, AgentSemanticMemory> memories = new LinkedHashMap<>();
    private SemanticMemoryQuery lastQuery;
    private boolean deleteFailure;

    @Override
    public List<AgentSemanticMemory> findByLearnerAndTypes(String learnerId, Set<MemoryType> types) {
        return memories.values().stream()
            .filter(memory -> memory.learnerId().equals(learnerId))
            .filter(memory -> types.contains(memory.memoryType()))
            .toList();
    }

    @Override
    public List<ScoredSemanticMemory> recall(SemanticMemoryQuery query, List<Double> embedding, int topK) {
        lastQuery = query;
        return findByLearnerAndTypes(query.learnerId(), query.types()).stream()
            .map(memory -> new ScoredSemanticMemory(memory, cosine(embedding, memory.embedding())))
            .filter(scored -> Double.isFinite(scored.score()))
            .sorted(Comparator.comparingDouble(ScoredSemanticMemory::score).reversed()
                .thenComparing(scored -> scored.memory().id()))
            .limit(Math.min(topK, query.topK()))
            .toList();
    }

    @Override
    public AgentSemanticMemory save(AgentSemanticMemory memory) {
        memories.put(memory.id(), memory);
        return memory;
    }

    @Override
    public long deleteByLearner(String learnerId) {
        if (deleteFailure) {
            throw new IllegalStateException("injected semantic deletion failure");
        }
        List<String> ids = new ArrayList<>();
        memories.forEach((id, memory) -> {
            if (memory.learnerId().equals(learnerId)) {
                ids.add(id);
            }
        });
        ids.forEach(memories::remove);
        return ids.size();
    }

    SemanticMemoryQuery lastQuery() {
        return lastQuery;
    }

    void failDeletes() {
        deleteFailure = true;
    }

    private static double cosine(List<Double> left, List<Double> right) {
        if (left.isEmpty() || left.size() != right.size()) {
            return Double.NaN;
        }
        double dot = 0;
        double leftNorm = 0;
        double rightNorm = 0;
        for (int index = 0; index < left.size(); index++) {
            double l = left.get(index);
            double r = right.get(index);
            dot += l * r;
            leftNorm += l * l;
            rightNorm += r * r;
        }
        return leftNorm == 0 || rightNorm == 0 ? Double.NaN : dot / Math.sqrt(leftNorm * rightNorm);
    }
}
