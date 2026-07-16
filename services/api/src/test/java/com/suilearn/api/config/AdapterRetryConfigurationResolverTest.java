package com.suilearn.api.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suilearn.api.ai.OpenAiCompatibleAiProvider;
import com.suilearn.api.retrieval.OpenAiCompatibleEmbeddingProvider;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class AdapterRetryConfigurationResolverTest {
    @Test
    void defaultsToNoImmediateAdapterRetriesWhenBothKeysAreAbsent() {
        var configuration = AdapterRetryConfigurationResolver.resolve(null, null);

        assertThat(configuration.maxRetries()).isZero();
        assertThat(configuration.diagnosticCode()).isNull();
    }

    @Test
    void mapsPositiveLegacyValueToOneRetryAndReportsDiagnostic() {
        var configuration = AdapterRetryConfigurationResolver.resolve("", "2");

        assertThat(configuration.maxRetries()).isEqualTo(1);
        assertThat(configuration.diagnosticCode()).isEqualTo("SUILEARN_RETRY_CONFIG_LEGACY_MAPPED");
    }

    @Test
    void rejectsSimultaneousNonEmptyCanonicalAndLegacyValues() {
        assertThatThrownBy(() -> AdapterRetryConfigurationResolver.resolve("0", "0"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("SUILEARN_RETRY_CONFIG_CONFLICT");
    }

    @Test
    void applicationConfigurationUsesCanonicalRetryDefaultAndLegacyMapping() {
        var configuration = new AppConfig();

        assertThat(configuration.suiLearnAiProperties(new MockEnvironment()).maxRetries()).isZero();
        assertThat(configuration.suiLearnAiProperties(new MockEnvironment()
            .withProperty("suilearn.ai.max-retries", "5")).maxRetries()).isEqualTo(1);
    }

    @Test
    void configuresCircuitBreakerWindowWithDefaultAndBoundedOverride() {
        var configuration = new AppConfig();

        assertThat(configuration.suiLearnAiProperties(new MockEnvironment()).circuitBreakerWindow()).isEqualTo(10);
        assertThat(configuration.suiLearnAiProperties(new MockEnvironment()
            .withProperty("suilearn.circuit-breaker.sliding-window-size", "25")).circuitBreakerWindow()).isEqualTo(25);
        assertThatThrownBy(() -> new SuiLearnAiProperties(
            "openai-compatible", "", "", "", "", "", "", "", "", 1000, 0, 4))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("between 5 and 100");
    }

    @Test
    void appliesExplicitCircuitBreakerKnobsToChatAndEmbeddingProviders() {
        var configuration = new AppConfig();
        var properties = configuration.suiLearnAiProperties(new MockEnvironment()
            .withProperty("suilearn.circuit-breaker.failure-rate-percent", "70")
            .withProperty("suilearn.circuit-breaker.sliding-window-size", "12")
            .withProperty("suilearn.circuit-breaker.minimum-calls", "4")
            .withProperty("suilearn.circuit-breaker.open-state-ms", "45000")
            .withProperty("suilearn.circuit-breaker.half-open-calls", "3"));

        assertCircuitBreakerConfiguration(circuitBreakerOf(new OpenAiCompatibleAiProvider(properties, new ObjectMapper())));
        assertCircuitBreakerConfiguration(circuitBreakerOf(new OpenAiCompatibleEmbeddingProvider(properties, new ObjectMapper())));
    }

    @Test
    void usesSafeCircuitBreakerDefaultsAndRejectsOutOfRangeOverrides() {
        var configuration = new AppConfig();
        var defaults = configuration.suiLearnAiProperties(new MockEnvironment());

        assertCircuitBreakerConfiguration(circuitBreakerOf(new OpenAiCompatibleAiProvider(defaults, new ObjectMapper())),
            50, 10, 5, 60000, 2);
        assertThatThrownBy(() -> configuration.suiLearnAiProperties(new MockEnvironment()
            .withProperty("suilearn.circuit-breaker.minimum-calls", "11")
            .withProperty("suilearn.circuit-breaker.sliding-window-size", "10")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("minimum calls");
        assertThatThrownBy(() -> configuration.suiLearnAiProperties(new MockEnvironment()
            .withProperty("suilearn.circuit-breaker.failure-rate-percent", "9")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("failure rate");
    }

    private void assertCircuitBreakerConfiguration(CircuitBreaker circuitBreaker) {
        assertCircuitBreakerConfiguration(circuitBreaker, 70, 12, 4, 45000, 3);
    }

    private void assertCircuitBreakerConfiguration(CircuitBreaker circuitBreaker, int failureRate, int slidingWindow,
                                                   int minimumCalls, int openStateMs, int halfOpenCalls) {
        var configuration = circuitBreaker.getCircuitBreakerConfig();
        assertThat(configuration.getFailureRateThreshold()).isEqualTo(failureRate);
        assertThat(configuration.getSlidingWindowSize()).isEqualTo(slidingWindow);
        assertThat(configuration.getMinimumNumberOfCalls()).isEqualTo(minimumCalls);
        assertThat(configuration.getWaitIntervalFunctionInOpenState().apply(1)).isEqualTo(openStateMs);
        assertThat(configuration.getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(halfOpenCalls);
    }

    private CircuitBreaker circuitBreakerOf(Object provider) {
        try {
            Field field = provider.getClass().getDeclaredField("circuitBreaker");
            field.setAccessible(true);
            return (CircuitBreaker) field.get(provider);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Provider must expose one circuit breaker", exception);
        }
    }

    @Test
    void processingDefaultsEnableAsyncWorkButDisabledAsyncWorkRejectsNewUploads() {
        assertThat(SuiLearnProcessingProperties.from(new MockEnvironment()).asyncProcessingEnabled()).isTrue();
        assertThat(SuiLearnProcessingProperties.from(new MockEnvironment()).allowsNewUploads()).isTrue();

        var disabled = SuiLearnProcessingProperties.from(new MockEnvironment()
            .withProperty("suilearn.async-processing.enabled", "false"));
        assertThat(disabled.allowsNewUploads()).isFalse();
        assertThat(disabled.processingConcurrency()).isEqualTo(2);
        assertThat(disabled.ocrConcurrency()).isEqualTo(1);
    }

    @Test
    void createsSeparateBoundedExecutorsForConsumersAndOcr() {
        var properties = SuiLearnProcessingProperties.from(new MockEnvironment());
        var configuration = new ProcessingExecutorConfig();

        var consumer = configuration.processingConsumerTaskExecutor(properties);
        var ocr = configuration.ocrTaskExecutor(properties);
        try {
            assertThat(consumer).isNotSameAs(ocr);
            assertThat(consumer.getCorePoolSize()).isEqualTo(2);
            assertThat(ocr.getCorePoolSize()).isEqualTo(1);
        } finally {
            consumer.shutdown();
            ocr.shutdown();
        }
    }
}
