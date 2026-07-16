package com.suilearn.api.ai;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.net.http.HttpTimeoutException;
import org.junit.jupiter.api.Test;

class AiOperationalMetricsTest {
    @Test
    void recordsLowCardinalityAiOutcomesWithoutRequestIdentifiersOrContent() {
        var registry = new SimpleMeterRegistry();
        var metrics = new AiOperationalMetrics(registry);

        metrics.record("chat", AiFailureClassifier.classify(new HttpTimeoutException("body must not become a tag")), 12);

        var counter = registry.find("suilearn.ai.requests").tags("operation", "chat", "outcome", "timeout").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1);
        assertThat(registry.getMeters()).allSatisfy(meter -> assertThat(meter.getId().getTags())
            .extracting(tag -> tag.getKey()).doesNotContain("materialId", "taskId", "content", "error"));
    }

    @Test
    void classifiesRateLimitsAndCircuitOpenSeparatelyFromPermanentFailures() {
        assertThat(AiFailureClassifier.classifyHttpStatus(429)).isEqualTo(AiFailureKind.RATE_LIMITED);
        assertThat(AiFailureClassifier.classifyHttpStatus(400)).isEqualTo(AiFailureKind.PERMANENT);
        var circuitBreaker = io.github.resilience4j.circuitbreaker.CircuitBreaker.ofDefaults("test");
        assertThat(AiFailureClassifier.classify(
            io.github.resilience4j.circuitbreaker.CallNotPermittedException.createCallNotPermittedException(circuitBreaker)))
            .isEqualTo(AiFailureKind.CIRCUIT_OPEN);
    }
}
