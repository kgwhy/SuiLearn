package com.suilearn.api.ai;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public final class AiOperationalMetrics {
    private static final Set<String> OPERATIONS = Set.of("chat", "embedding");
    private final MeterRegistry registry;

    public AiOperationalMetrics(MeterRegistry registry) { this.registry = registry; }

    public static AiOperationalMetrics noop() {
        return new AiOperationalMetrics(new io.micrometer.core.instrument.simple.SimpleMeterRegistry());
    }

    public void record(String operation, AiFailureKind outcome, long elapsedNanos) {
        String normalizedOperation = OPERATIONS.contains(operation) ? operation : "other";
        Counter.builder("suilearn.ai.requests").tags("operation", normalizedOperation, "outcome", outcome.tagValue())
            .register(registry).increment();
        Timer.builder("suilearn.ai.duration").tags("operation", normalizedOperation, "outcome", outcome.tagValue())
            .register(registry).record(Math.max(0, elapsedNanos), TimeUnit.NANOSECONDS);
    }
}
