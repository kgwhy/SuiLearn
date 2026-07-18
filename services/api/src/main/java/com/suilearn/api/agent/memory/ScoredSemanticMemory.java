package com.suilearn.api.agent.memory;

public record ScoredSemanticMemory(AgentSemanticMemory memory, double score) {
}
