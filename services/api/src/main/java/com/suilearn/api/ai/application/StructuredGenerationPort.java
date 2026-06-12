package com.suilearn.api.ai.application;

public interface StructuredGenerationPort {
    <T> T generate(StructuredGenerationRequest<T> request);

    record StructuredGenerationRequest<T>(String prompt, Class<T> outputType) {
    }
}
