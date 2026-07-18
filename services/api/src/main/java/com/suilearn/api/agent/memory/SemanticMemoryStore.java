package com.suilearn.api.agent.memory;

import java.util.List;
import java.util.Set;

public interface SemanticMemoryStore {
    List<AgentSemanticMemory> findByLearnerAndTypes(String learnerId, Set<MemoryType> types);

    List<ScoredSemanticMemory> recall(SemanticMemoryQuery query, List<Double> embedding, int topK);

    AgentSemanticMemory save(AgentSemanticMemory memory);

    long deleteByLearner(String learnerId);
}
