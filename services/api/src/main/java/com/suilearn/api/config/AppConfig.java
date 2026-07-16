package com.suilearn.api.config;

import com.suilearn.api.material.document.ExternalProcessRunner;
import com.suilearn.api.material.document.LibreOfficePreviewAdapter;
import com.suilearn.api.material.document.OcrOperationalMetrics;
import com.suilearn.api.material.document.TesseractOcrAdapter;
import com.suilearn.api.runtimefixture.RuntimeFixtureProcessRunner;
import java.time.Clock;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import io.micrometer.core.instrument.MeterRegistry;

@Configuration
public class AppConfig {
    private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    SuiLearnAiProperties suiLearnAiProperties(Environment environment) {
        var retryConfiguration = AdapterRetryConfigurationResolver.resolve(
            environment.getProperty("suilearn.adapter.max-retries"),
            environment.getProperty("suilearn.ai.max-retries")
        );
        if (retryConfiguration.diagnosticCode() != null) {
            logger.warn("{}: mapped deprecated SUILEARN_AI_MAX_RETRIES to SUILEARN_ADAPTER_MAX_RETRIES",
                retryConfiguration.diagnosticCode());
        }
        return new SuiLearnAiProperties(
            environment.getProperty("suilearn.ai.provider", "openai-compatible"),
            environment.getProperty("suilearn.ai.base-url", ""),
            environment.getProperty("suilearn.ai.api-key", ""),
            environment.getProperty("suilearn.ai.chat-base-url", ""),
            environment.getProperty("suilearn.ai.chat-api-key", ""),
            environment.getProperty("suilearn.ai.embedding-base-url", ""),
            environment.getProperty("suilearn.ai.embedding-api-key", ""),
            environment.getProperty("suilearn.ai.chat-model", ""),
            environment.getProperty("suilearn.ai.embedding-model", ""),
            environment.getProperty("suilearn.ai.timeout-ms", Integer.class, 30000),
            retryConfiguration.maxRetries(),
            environment.getProperty("suilearn.circuit-breaker.sliding-window-size", Integer.class, 10),
            environment.getProperty("suilearn.circuit-breaker.failure-rate-percent", Integer.class, 50),
            environment.getProperty("suilearn.circuit-breaker.minimum-calls", Integer.class, 5),
            environment.getProperty("suilearn.circuit-breaker.open-state-ms", Integer.class, 60000),
            environment.getProperty("suilearn.circuit-breaker.half-open-calls", Integer.class, 2)
        );
    }

    @Bean
    SuiLearnProcessingProperties suiLearnProcessingProperties(Environment environment) {
        return SuiLearnProcessingProperties.from(environment);
    }

    @Bean
    AsyncProcessingAdmissionGuard asyncProcessingAdmissionGuard(SuiLearnProcessingProperties properties) {
        return new AsyncProcessingAdmissionGuard(properties.asyncProcessingEnabled());
    }

    @Bean
    LibreOfficePreviewAdapter libreOfficePreviewAdapter(SuiLearnProcessingProperties properties) {
        return new LibreOfficePreviewAdapter("soffice", ExternalProcessRunner.processBuilder(),
            Duration.ofMillis(properties.libreOfficeTimeoutMs()), "libreoffice-v1");
    }

    @Bean
    TesseractOcrAdapter tesseractOcrAdapter(SuiLearnProcessingProperties properties,
                                             ObjectProvider<RuntimeFixtureProcessRunner> runtimeFixtureRunner,
                                             MeterRegistry meterRegistry) {
        ExternalProcessRunner runner = runtimeFixtureRunner.getIfAvailable();
        if (runner == null) {
            runner = ExternalProcessRunner.processBuilder();
        }
        return new TesseractOcrAdapter("tesseract", runner, properties.ocrConcurrency(),
            Duration.ofMillis(properties.ocrTimeoutMs()), "tesseract-v1", new OcrOperationalMetrics(meterRegistry));
    }
}
