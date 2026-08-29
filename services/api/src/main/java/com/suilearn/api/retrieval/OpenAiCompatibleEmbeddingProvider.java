package com.suilearn.api.retrieval;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suilearn.api.ai.AiFailureClassifier;
import com.suilearn.api.ai.AiFailureKind;
import com.suilearn.api.ai.AiHttpStatusException;
import com.suilearn.api.ai.AiOperationalMetrics;
import com.suilearn.api.config.SuiLearnAiProperties;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "suilearn.ai", name = "provider", havingValue = "openai-compatible")
public class OpenAiCompatibleEmbeddingProvider implements EmbeddingProvider {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final SuiLearnAiProperties properties;
    private final Retry retry;
    private final CircuitBreaker circuitBreaker;
    private final AiOperationalMetrics metrics;
    private volatile int cachedDimensions;

    public OpenAiCompatibleEmbeddingProvider(SuiLearnAiProperties properties, ObjectMapper objectMapper) {
        this(properties, objectMapper, HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(Math.max(1000, properties.timeoutMs())))
            .build(), AiOperationalMetrics.noop());
    }

    @Autowired
    public OpenAiCompatibleEmbeddingProvider(
        SuiLearnAiProperties properties,
        ObjectMapper objectMapper,
        MeterRegistry meterRegistry
    ) {
        this(properties, objectMapper, HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(Math.max(1000, properties.timeoutMs())))
            .build(), new AiOperationalMetrics(meterRegistry));
    }

    OpenAiCompatibleEmbeddingProvider(
        SuiLearnAiProperties properties,
        ObjectMapper objectMapper,
        HttpClient httpClient
    ) {
        this(properties, objectMapper, httpClient, AiOperationalMetrics.noop());
    }

    private OpenAiCompatibleEmbeddingProvider(
        SuiLearnAiProperties properties,
        ObjectMapper objectMapper,
        HttpClient httpClient,
        AiOperationalMetrics metrics
    ) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
        this.retry = adapterRetry(properties.maxRetries());
        this.circuitBreaker = CircuitBreaker.of("openai-compatible-embedding", CircuitBreakerConfig.custom()
            .slidingWindowSize(properties.circuitBreakerWindow())
            .minimumNumberOfCalls(properties.circuitBreakerMinimumCalls())
            .failureRateThreshold(properties.circuitBreakerFailureRatePercent())
            .waitDurationInOpenState(Duration.ofMillis(properties.circuitBreakerOpenStateMs()))
            .permittedNumberOfCallsInHalfOpenState(properties.circuitBreakerHalfOpenCalls())
            .recordException(AiFailureClassifier::countsTowardCircuitBreaker)
            .build());
        this.metrics = metrics;
    }

    @Override
    public Embedding embed(String input) {
        ensureConfigured();
        long startedAt = System.nanoTime();
        var request = HttpRequest.newBuilder(URI.create(normalizeBaseUrl(properties.effectiveEmbeddingBaseUrl()) + "/embeddings"))
            .timeout(Duration.ofMillis(Math.max(1000, properties.timeoutMs())))
            .header("Authorization", "Bearer " + properties.effectiveEmbeddingApiKey())
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody(input)))
            .build();
        try {
            var response = circuitBreaker.executeSupplier(() -> {
                var candidate = retry.executeSupplier(() -> send(request));
                if (candidate.statusCode() < 200 || candidate.statusCode() >= 300) {
                    throw new AiHttpStatusException(candidate.statusCode());
                }
                return candidate;
            });
            var embedding = parseEmbedding(response.body());
            cachedDimensions = embedding.values().size();
            metrics.record("embedding", AiFailureKind.SUCCESS, System.nanoTime() - startedAt);
            return embedding;
        } catch (RuntimeException exception) {
            metrics.record("embedding", AiFailureClassifier.classify(exception), System.nanoTime() - startedAt);
            if (exception instanceof AiHttpStatusException) {
                throw new IllegalStateException(exception.getMessage(), exception);
            }
            if (exception instanceof UncheckedIOException) {
                throw new IllegalStateException("OpenAI-compatible embeddings request failed", exception);
            }
            if (exception instanceof RequestInterruptedException) {
                throw new IllegalStateException("OpenAI-compatible embeddings request interrupted", exception);
            }
            throw exception;
        }
    }

    private HttpResponse<String> send(HttpRequest request) {
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new RequestInterruptedException(exception);
        }
    }

    private Retry adapterRetry(int maxRetries) {
        if (maxRetries < 0 || maxRetries > 1) {
            throw new IllegalArgumentException("Adapter max retries must be between 0 and 1");
        }
        return Retry.of("openai-compatible-embedding", RetryConfig.<HttpResponse<String>>custom()
            .maxAttempts(maxRetries + 1)
            .waitDuration(Duration.ZERO)
            .retryOnResult(response -> isTransientStatus(response.statusCode()))
            .retryOnException(exception -> exception instanceof UncheckedIOException)
            .build());
    }

    private boolean isTransientStatus(int statusCode) {
        return statusCode == 408 || statusCode == 429 || statusCode >= 500;
    }

    @Override
    public boolean supportsEmbeddings() {
        return properties.hasOpenAiCompatibleEmbeddingConfiguration();
    }

    @Override
    public String model() {
        return properties.embeddingModel();
    }

    @Override
    public int dimensions() {
        return cachedDimensions;
    }

    @Override
    public String binding() {
        return "openai-compatible";
    }

    @Override
    public String baseUrl() {
        return properties.effectiveEmbeddingBaseUrl();
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
        if (!properties.hasOpenAiCompatibleEmbeddingConfiguration()) {
            throw new IllegalStateException("OpenAI-compatible provider is missing embedding baseUrl, embedding apiKey, or embeddingModel");
        }
    }

    private String normalizeBaseUrl(String baseUrl) {
        var normalized = baseUrl == null ? "" : baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static final class RequestInterruptedException extends RuntimeException {
        private RequestInterruptedException(InterruptedException cause) {
            super(cause);
        }
    }
}
