package com.suilearn.api.retrieval;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suilearn.api.config.SuiLearnAiProperties;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "suilearn.ai", name = "provider", havingValue = "openai-compatible")
public class OpenAiCompatibleEmbeddingProvider implements EmbeddingProvider {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final SuiLearnAiProperties properties;

    public OpenAiCompatibleEmbeddingProvider(SuiLearnAiProperties properties, ObjectMapper objectMapper) {
        this(properties, objectMapper, HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(Math.max(1000, properties.timeoutMs())))
            .build());
    }

    OpenAiCompatibleEmbeddingProvider(
        SuiLearnAiProperties properties,
        ObjectMapper objectMapper,
        HttpClient httpClient
    ) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    @Override
    public Embedding embed(String input) {
        ensureConfigured();
        var request = HttpRequest.newBuilder(URI.create(normalizeBaseUrl(properties.baseUrl()) + "/embeddings"))
            .timeout(Duration.ofMillis(Math.max(1000, properties.timeoutMs())))
            .header("Authorization", "Bearer " + properties.apiKey())
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody(input)))
            .build();
        RuntimeException lastFailure = null;
        for (var attempt = 0; attempt <= Math.max(0, properties.maxRetries()); attempt++) {
            try {
                var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    return parseEmbedding(response.body());
                }
                lastFailure = new IllegalStateException("OpenAI-compatible embeddings returned HTTP " + response.statusCode());
            } catch (IOException exception) {
                lastFailure = new IllegalStateException("OpenAI-compatible embeddings request failed", exception);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("OpenAI-compatible embeddings request interrupted", exception);
            }
        }
        throw lastFailure == null ? new IllegalStateException("OpenAI-compatible embeddings request failed") : lastFailure;
    }

    @Override
    public String model() {
        return properties.embeddingModel();
    }

    @Override
    public int dimensions() {
        return 0;
    }

    private String requestBody(String input) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                "model", properties.embeddingModel(),
                "input", input == null ? "" : input
            ));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to serialize OpenAI-compatible embedding request", exception);
        }
    }

    private Embedding parseEmbedding(String responseBody) {
        try {
            var root = objectMapper.readTree(responseBody);
            var values = new ArrayList<Double>();
            root.path("data").path(0).path("embedding").forEach(value -> values.add(value.asDouble()));
            if (values.isEmpty()) {
                throw new IllegalStateException("OpenAI-compatible embeddings response did not include an embedding");
            }
            return new Embedding(values);
        } catch (IOException exception) {
            throw new IllegalStateException("OpenAI-compatible embeddings response was not valid JSON", exception);
        }
    }

    private void ensureConfigured() {
        if (!properties.hasOpenAiCompatibleConfiguration()) {
            throw new IllegalStateException("OpenAI-compatible provider is missing baseUrl, apiKey, chatModel, or embeddingModel");
        }
    }

    private String normalizeBaseUrl(String baseUrl) {
        var normalized = baseUrl == null ? "" : baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.endsWith("/v1")) {
            return normalized;
        }
        return normalized + "/v1";
    }
}
