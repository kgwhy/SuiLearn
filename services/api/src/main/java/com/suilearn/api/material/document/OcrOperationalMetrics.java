package com.suilearn.api.material.document;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/** Bounded, content-free observability for individual OCR page attempts. */
public final class OcrOperationalMetrics {
    private static final Set<String> OUTCOMES = Set.of("succeeded", "failed", "timed_out");
    private final MeterRegistry registry;

    public OcrOperationalMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public static OcrOperationalMetrics noop() {
        return new OcrOperationalMetrics(new SimpleMeterRegistry());
    }

    public void recordPageResult(String status, long elapsedNanos) {
        String outcome = normalizedOutcome(status);
        Counter.builder("suilearn.ocr.pages").tag("outcome", outcome).register(registry).increment();
        Timer.builder("suilearn.ocr.duration").tag("outcome", outcome).register(registry)
            .record(Math.max(0, elapsedNanos), TimeUnit.NANOSECONDS);
    }

    private String normalizedOutcome(String status) {
        String normalized = status == null ? "failed" : status.toLowerCase(Locale.ROOT);
        return OUTCOMES.contains(normalized) ? normalized : "failed";
    }
}
