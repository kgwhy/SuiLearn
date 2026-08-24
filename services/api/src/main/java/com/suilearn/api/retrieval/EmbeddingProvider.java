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

    default String binding() {
        return "default";
    }

    default String baseUrl() {
        return "";
    }

    default String apiVersion() {
        return "";
    }

    record Embedding(List<Double> values) {
    }
}
