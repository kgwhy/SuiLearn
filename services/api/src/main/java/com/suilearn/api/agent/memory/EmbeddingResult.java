package com.suilearn.api.agent.memory;

import java.util.List;

public record EmbeddingResult(boolean available, List<Double> vector, String reason) {
    public EmbeddingResult {
        vector = vector == null ? List.of() : List.copyOf(vector);
    }

    public static EmbeddingResult available(List<Double> vector) {
        if (vector == null || vector.isEmpty()) {
            throw new IllegalArgumentException("embedding vector is required");
        }
        return new EmbeddingResult(true, vector, null);
    }

    public static EmbeddingResult unavailable(String reason) {
        return new EmbeddingResult(false, List.of(), reason == null ? "unavailable" : reason);
    }
}
