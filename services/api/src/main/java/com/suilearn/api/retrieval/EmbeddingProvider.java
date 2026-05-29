package com.suilearn.api.retrieval;

import java.util.List;

public interface EmbeddingProvider {
    Embedding embed(String input);

    record Embedding(List<Double> values) {
    }
}
