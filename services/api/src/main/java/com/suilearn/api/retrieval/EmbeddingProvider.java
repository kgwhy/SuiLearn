package com.suilearn.api.retrieval;

import java.util.List;

public interface EmbeddingProvider {
    Embedding embed(String input);

    default String model() {
        return "unknown-embedding-model";
    }

    default int dimensions() {
        return embed("").values().size();
    }

    record Embedding(List<Double> values) {
    }
}
