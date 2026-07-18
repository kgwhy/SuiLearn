package com.suilearn.api.agent.memory;

@FunctionalInterface
public interface EmbeddingProvider {
    EmbeddingResult embed(String content);
}
