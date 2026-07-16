package com.suilearn.api.config;

import com.suilearn.api.model.AiProviderType;

public record SuiLearnAiProperties(
    String provider,
    String baseUrl,
    String apiKey,
    String chatBaseUrl,
    String chatApiKey,
    String embeddingBaseUrl,
    String embeddingApiKey,
    String chatModel,
    String embeddingModel,
    int timeoutMs,
    int maxRetries,
    int circuitBreakerWindow,
    int circuitBreakerFailureRatePercent,
    int circuitBreakerMinimumCalls,
    int circuitBreakerOpenStateMs,
    int circuitBreakerHalfOpenCalls
) {
    public SuiLearnAiProperties {
        if (circuitBreakerWindow < 5 || circuitBreakerWindow > 100) {
            throw new IllegalArgumentException("AI circuit breaker window must be between 5 and 100");
        }
        if (circuitBreakerFailureRatePercent < 10 || circuitBreakerFailureRatePercent > 100) {
            throw new IllegalArgumentException("AI circuit breaker failure rate must be between 10 and 100");
        }
        if (circuitBreakerMinimumCalls < 1 || circuitBreakerMinimumCalls > circuitBreakerWindow) {
            throw new IllegalArgumentException("AI circuit breaker minimum calls must be between 1 and the sliding window size");
        }
        if (circuitBreakerOpenStateMs < 1000 || circuitBreakerOpenStateMs > 600000) {
            throw new IllegalArgumentException("AI circuit breaker open state must be between 1000 and 600000 ms");
        }
        if (circuitBreakerHalfOpenCalls < 1 || circuitBreakerHalfOpenCalls > 10) {
            throw new IllegalArgumentException("AI circuit breaker half-open calls must be between 1 and 10");
        }
    }

    public SuiLearnAiProperties(
        String provider, String baseUrl, String apiKey, String chatBaseUrl, String chatApiKey, String embeddingBaseUrl,
        String embeddingApiKey, String chatModel, String embeddingModel, int timeoutMs, int maxRetries
    ) {
        this(provider, baseUrl, apiKey, chatBaseUrl, chatApiKey, embeddingBaseUrl, embeddingApiKey, chatModel, embeddingModel,
            timeoutMs, maxRetries, 10, 50, 5, 60000, 2);
    }

    public SuiLearnAiProperties(
        String provider, String baseUrl, String apiKey, String chatBaseUrl, String chatApiKey, String embeddingBaseUrl,
        String embeddingApiKey, String chatModel, String embeddingModel, int timeoutMs, int maxRetries, int circuitBreakerWindow
    ) {
        this(provider, baseUrl, apiKey, chatBaseUrl, chatApiKey, embeddingBaseUrl, embeddingApiKey, chatModel, embeddingModel,
            timeoutMs, maxRetries, circuitBreakerWindow, 50, Math.min(5, circuitBreakerWindow), 60000, 2);
    }

    public SuiLearnAiProperties(
        String provider,
        String baseUrl,
        String apiKey,
        String chatModel,
        String embeddingModel,
        int timeoutMs,
        int maxRetries
    ) {
        this(provider, baseUrl, apiKey, "", "", "", "", chatModel, embeddingModel, timeoutMs, maxRetries, 10, 50, 5, 60000, 2);
    }

    public AiProviderType providerType() {
        return AiProviderType.OPENAI_COMPATIBLE;
    }

    public boolean hasOpenAiCompatibleConfiguration() {
        return hasOpenAiCompatibleChatConfiguration();
    }

    public boolean hasOpenAiCompatibleChatConfiguration() {
        return hasText(effectiveChatBaseUrl()) && hasText(effectiveChatApiKey()) && hasText(chatModel);
    }

    public boolean hasOpenAiCompatibleEmbeddingConfiguration() {
        return hasText(effectiveEmbeddingBaseUrl()) && hasText(effectiveEmbeddingApiKey()) && hasText(embeddingModel);
    }

    public String effectiveChatBaseUrl() {
        return hasText(chatBaseUrl) ? chatBaseUrl : baseUrl;
    }

    public String effectiveChatApiKey() {
        return hasText(chatApiKey) ? chatApiKey : apiKey;
    }

    public String effectiveEmbeddingBaseUrl() {
        return hasText(embeddingBaseUrl) ? embeddingBaseUrl : baseUrl;
    }

    public String effectiveEmbeddingApiKey() {
        return hasText(embeddingApiKey) ? embeddingApiKey : apiKey;
    }

    public static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
