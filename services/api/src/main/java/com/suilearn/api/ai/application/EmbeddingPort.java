package com.suilearn.api.ai.application;

import java.util.List;

public interface EmbeddingPort {
    EmbeddingResponse embed(EmbeddingRequest request);

    record EmbeddingRequest(String text) {
    }

    record EmbeddingResponse(List<Double> values, String model, int dimensions) {
    }
}
