package com.suilearn.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suilearn.api.config.SuiLearnAiProperties;
import com.suilearn.api.model.AiProviderType;
import com.suilearn.api.retrieval.EmbeddingProvider;
import java.util.List;
import org.junit.jupiter.api.Test;

class AiProviderStatusServiceTest {
    @Test
    void openAiCompatibleStatusIsAvailableWhenConfiguredAndDoesNotExposeSecret() {
        var service = new AiProviderStatusService(
            new TestEmbeddingProvider(),
            new SuiLearnAiProperties(
                "openai-compatible",
                "https://ai.example.test/v1",
                "secret-key-value",
                "chat-model",
                "embedding-model",
                30000,
                2
            )
        );

        var status = service.getStatus();
        var json = new ObjectMapper().valueToTree(status);

        assertThat(status.providerType()).isEqualTo(AiProviderType.OPENAI_COMPATIBLE);
        assertThat(status.configured()).isTrue();
        assertThat(status.available()).isTrue();
        assertThat(status.apiKeyEnvName()).isEqualTo("SUILEARN_AI_API_KEY");
        assertThat(json.toString()).doesNotContain("secret-key-value");
    }

    private static class TestEmbeddingProvider implements EmbeddingProvider {
        @Override
        public Embedding embed(String input) {
            return new Embedding(List.of(0.0, 0.0, 0.0));
        }

        @Override
        public String model() {
            return "test-embedding-v1";
        }

        @Override
        public int dimensions() {
            return 3;
        }
    }
}
