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
        var embeddingsConfigured = embeddingProvider.supportsEmbeddings();
        return new AiProviderStatus(
            AiProviderType.OPENAI_COMPATIBLE,
            configured,
            configured,
            emptyToNull(properties.effectiveChatBaseUrl()),
            emptyToNull(properties.chatModel()),
            embeddingsConfigured ? emptyToNull(properties.embeddingModel()) : null,
            embeddingsConfigured ? embeddingProvider.dimensions() : 0,
            API_KEY_ENV_NAME,
            properties.timeoutMs(),
            properties.maxRetries(),
            statusMessage(configured, embeddingsConfigured)
        );
    }

    private String statusMessage(boolean configured, boolean embeddingsConfigured) {
        if (!configured) {
            return "OpenAI-compatible provider is missing chat baseUrl, apiKey, or chatModel";
        }
        if (embeddingsConfigured) {
            return "OpenAI-compatible adapter is configured for HTTP generation and vector retrieval";
        }
        return "OpenAI-compatible adapter is configured for HTTP generation; RAG retrieval will use text-only search";
    }

    private String emptyToNull(String value) {
        return SuiLearnAiProperties.hasText(value) ? value : null;
    }
}
