package com.suilearn.api.ai.infrastructure.springai;

import com.suilearn.api.ai.application.StructuredGenerationPort;

public class SpringAiStructuredGenerationAdapter implements StructuredGenerationPort {
    @Override
    public <T> T generate(StructuredGenerationRequest<T> request) {
        throw new UnsupportedOperationException("Spring AI structured generation adapter boundary is defined but not enabled yet");
    }
}
