package com.suilearn.api.config;

import com.suilearn.api.model.AiProviderType;

public record SuiLearnAiProperties(
    String provider,
    String baseUrl,
    String apiKey,
    String chatModel,
    String embeddingModel,
    int timeoutMs,
    int maxRetries
) {
    public AiProviderType providerType() {
        return "openai-compatible".equalsIgnoreCase(provider)
            ? AiProviderType.OPENAI_COMPATIBLE
            : AiProviderType.FAKE;
    }

    public boolean hasOpenAiCompatibleConfiguration() {
        return hasText(baseUrl) && hasText(apiKey) && hasText(chatModel) && hasText(embeddingModel);
    }

    public static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
