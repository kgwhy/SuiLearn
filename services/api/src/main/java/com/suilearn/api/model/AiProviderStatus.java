package com.suilearn.api.model;

public record AiProviderStatus(
    AiProviderType providerType,
    boolean configured,
    boolean available,
    String baseUrl,
    String chatModel,
    String embeddingModel,
    Integer embeddingDimensions,
    String apiKeyEnvName,
    Integer timeoutMs,
    Integer maxRetries,
    String message
) {
}
