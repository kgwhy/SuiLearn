package com.suilearn.api.retrieval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suilearn.api.config.SuiLearnAiProperties;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OpenAiCompatibleEmbeddingProviderTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AtomicInteger responseStatus = new AtomicInteger(200);
    private final AtomicInteger responseDelayMillis = new AtomicInteger();
    private final AtomicInteger requestCount = new AtomicInteger();
    private HttpServer server;
    private SuiLearnAiProperties properties;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/embeddings", this::handleEmbedding);
        server.start();
        properties = new SuiLearnAiProperties(
            "openai-compatible",
            "http://127.0.0.1:" + server.getAddress().getPort(),
            "test-key",
            "test-chat",
            "test-embedding",
            1000,
            0
        );
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void recordsSuccessfulEmbeddingWithLowCardinalityTags() {
        var registry = new SimpleMeterRegistry();
        var provider = new OpenAiCompatibleEmbeddingProvider(properties, objectMapper, registry);

        assertThat(provider.embed("HashMap source").values()).containsExactly(0.1, 0.2, 0.3);

        assertThat(registry.find("suilearn.ai.requests")
            .tags("operation", "embedding", "outcome", "success").counter()).isNotNull();
        assertThat(registry.getMeters()).allSatisfy(meter -> assertThat(meter.getId().getTags())
            .allSatisfy(tag -> assertThat(tag.getKey()).isIn("operation", "outcome")));
    }

    @Test
    void recordsRateLimitedEmbeddingWithLowCardinalityTags() {
        responseStatus.set(429);
        var registry = new SimpleMeterRegistry();
        var provider = new OpenAiCompatibleEmbeddingProvider(properties, objectMapper, registry);

        assertThatThrownBy(() -> provider.embed("HashMap source"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("HTTP 429");

        assertThat(registry.find("suilearn.ai.requests")
            .tags("operation", "embedding", "outcome", "rate_limited").counter()).isNotNull();
    }

    @Test
    void recordsTimedOutEmbeddingWithLowCardinalityTags() {
        responseDelayMillis.set(1200);
        var registry = new SimpleMeterRegistry();
        var provider = new OpenAiCompatibleEmbeddingProvider(properties, objectMapper, registry);

        assertThatThrownBy(() -> provider.embed("HashMap source"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("request failed");

        assertThat(registry.find("suilearn.ai.requests")
            .tags("operation", "embedding", "outcome", "timeout").counter()).isNotNull();
    }

    @Test
    void doesNotOpenCircuitAfterPermanentEmbeddingFailures() {
        responseStatus.set(400);
        var registry = new SimpleMeterRegistry();
        var provider = new OpenAiCompatibleEmbeddingProvider(properties, objectMapper, registry);

        for (int attempt = 0; attempt < 12; attempt++) {
            assertThatThrownBy(() -> provider.embed("HashMap source")).isInstanceOf(IllegalStateException.class);
        }

        assertThat(requestCount.get()).isEqualTo(12);
        assertThat(registry.find("suilearn.ai.requests")
            .tags("operation", "embedding", "outcome", "permanent").counter()).isNotNull();
    }

    private void handleEmbedding(HttpExchange exchange) throws IOException {
        requestCount.incrementAndGet();
        try {
            Thread.sleep(responseDelayMillis.get());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Embedding response interrupted", exception);
        }
        var body = responseStatus.get() == 200
            ? "{\"data\":[{\"embedding\":[0.1,0.2,0.3]}]}"
            : "{\"error\":\"embedding failure\"}";
        var bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(responseStatus.get(), bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
