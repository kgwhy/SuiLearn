package com.suilearn.api.service;

import com.suilearn.api.config.SuiLearnAiProperties;
import com.suilearn.api.model.AiProviderStatus;
import com.suilearn.api.model.AiProviderType;
import com.suilearn.api.retrieval.EmbeddingProvider;
import org.springframework.stereotype.Service;

@Service
public class AiProviderStatusService {
    private static final String API_KEY_ENV_NAME = "SUILEARN_AI_API_KEY";

    private final EmbeddingProvider embeddingProvider;
    private final SuiLearnAiProperties properties;

    public AiProviderStatusService(EmbeddingProvider embeddingProvider, SuiLearnAiProperties properties) {
        this.embeddingProvider = embeddingProvider;
        this.properties = properties;
    }

    public AiProviderStatus getStatus() {
        var configured = properties.hasOpenAiCompatibleConfiguration();
        return new AiProviderStatus(
            AiProviderType.OPENAI_COMPATIBLE,
            configured,
            configured,
            emptyToNull(properties.baseUrl()),
            emptyToNull(properties.chatModel()),
            emptyToNull(properties.embeddingModel()),
            null,
            API_KEY_ENV_NAME,
            properties.timeoutMs(),
            properties.maxRetries(),
            configured
                ? "OpenAI-compatible adapter is configured and available for HTTP generation"
                : "OpenAI-compatible provider is missing baseUrl, apiKey, chatModel, or embeddingModel"
        );
    }

    private String emptyToNull(String value) {
        return SuiLearnAiProperties.hasText(value) ? value : null;
    }
}
