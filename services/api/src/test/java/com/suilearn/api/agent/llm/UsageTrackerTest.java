package com.suilearn.api.agent.llm;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class UsageTrackerTest {
    @Test
    void computesCostFromConfiguredPriceTable() {
        var tracker = new UsageTracker(Map.of("model-x",
            new UsageTracker.ModelPrice(2.0, 4.0)));

        var usage = tracker.track("turn-1", "model-x", new LlmUsage(500, 250));

        assertThat(usage.promptTokens()).isEqualTo(500);
        assertThat(usage.completionTokens()).isEqualTo(250);
        assertThat(usage.costUsd()).isEqualTo(0.002);
        assertThat(tracker.total("turn-1").costUsd()).isEqualTo(0.002);
    }

    @Test
    void fallsBackToDefaultPrices() {
        var tracker = UsageTracker.defaults();
        var usage = tracker.track("turn-1", "unknown", new LlmUsage(1_000_000, 1_000_000));
        assertThat(usage.costUsd()).isEqualTo(0.75);
    }
}
