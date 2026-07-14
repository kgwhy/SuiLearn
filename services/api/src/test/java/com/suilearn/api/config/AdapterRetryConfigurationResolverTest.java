package com.suilearn.api.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
