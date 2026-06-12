package com.suilearn.api.ai.infrastructure.springai;

import com.suilearn.api.ai.application.EmbeddingPort;

public class SpringAiEmbeddingAdapter implements EmbeddingPort {
    @Override
    public EmbeddingResponse embed(EmbeddingRequest request) {
        throw new UnsupportedOperationException("Spring AI embedding adapter boundary is defined but not enabled yet");
    }
}
