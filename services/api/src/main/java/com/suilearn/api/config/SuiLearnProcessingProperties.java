package com.suilearn.api.config;

import org.springframework.core.env.Environment;

public record SuiLearnProcessingProperties(
    boolean asyncProcessingEnabled,
    boolean ocrEnabled,
    int maxFileSizeMb,
    int pdfMaxPages,
    int processingConcurrency,
    int ocrConcurrency,
    boolean retainOriginal,
    boolean knowledgePointAutoGenerationEnabled,
    int maxAttempts,
    long parserTimeoutMs,
    long ocrTimeoutMs,
    long libreOfficeTimeoutMs,
    long rabbitRetryShortDelayMs,
    long rabbitRetryLongDelayMs,
    String minioEndpoint,
    String minioAccessKey,
    String minioSecretKey,
    String minioBucket,
    int pdfOcrTextDensityThreshold
) {
    public static SuiLearnProcessingProperties from(Environment environment) {
        return new SuiLearnProcessingProperties(
            environment.getProperty("suilearn.async-processing.enabled", Boolean.class, true),
            environment.getProperty("suilearn.ocr.enabled", Boolean.class, true),
            environment.getProperty("suilearn.max-file-size-mb", Integer.class, 50),
            environment.getProperty("suilearn.pdf.max-pages", Integer.class, 500),
            environment.getProperty("suilearn.processing.concurrency", Integer.class, 2),
            environment.getProperty("suilearn.ocr.concurrency", Integer.class, 1),
            environment.getProperty("suilearn.retain-original", Boolean.class, true),
            environment.getProperty("suilearn.knowledge-point.auto-generation-enabled", Boolean.class, true),
            environment.getProperty("suilearn.processing.max-attempts", Integer.class, 3),
            environment.getProperty("suilearn.parser.timeout-ms", Long.class, 120000L),
            environment.getProperty("suilearn.ocr.timeout-ms", Long.class, 60000L),
            environment.getProperty("suilearn.libreoffice.timeout-ms", Long.class, 120000L),
            environment.getProperty("suilearn.rabbitmq.retry-short-delay-ms", Long.class, 30000L),
            environment.getProperty("suilearn.rabbitmq.retry-long-delay-ms", Long.class, 300000L),
            environment.getProperty("suilearn.minio.endpoint", "http://localhost:9000"),
            environment.getProperty("suilearn.minio.access-key", ""),
            environment.getProperty("suilearn.minio.secret-key", ""),
            environment.getProperty("suilearn.minio.bucket", "suilearn-assets"),
            environment.getProperty("suilearn.pdf.ocr-text-density-threshold", Integer.class, 16)
        );
    }

    public boolean allowsNewUploads() {
        return asyncProcessingEnabled;
    }
}
