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
    int maxRetries
) {
    public SuiLearnAiProperties(
        String provider,
        String baseUrl,
        String apiKey,
        String chatModel,
        String embeddingModel,
        int timeoutMs,
        int maxRetries
    ) {
        this(provider, baseUrl, apiKey, "", "", "", "", chatModel, embeddingModel, timeoutMs, maxRetries);
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
