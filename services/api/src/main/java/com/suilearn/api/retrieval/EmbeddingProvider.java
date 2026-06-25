package com.suilearn.api.retrieval;

import java.util.List;

public interface EmbeddingProvider {
    Embedding embed(String input);

    default boolean supportsEmbeddings() {
        return true;
    }

    default String model() {
        return "unknown-embedding-model";
    }

    default int dimensions() {
        return supportsEmbeddings() ? embed("").values().size() : 0;
    }

    record Embedding(List<Double> values) {
    }
}
