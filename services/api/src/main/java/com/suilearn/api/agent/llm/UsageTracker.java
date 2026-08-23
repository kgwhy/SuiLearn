package com.suilearn.api.agent.llm;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class UsageTracker {
    public static final double DEFAULT_PROMPT_PRICE_PER_MILLION_USD = 0.15;
    public static final double DEFAULT_COMPLETION_PRICE_PER_MILLION_USD = 0.60;

    private final Map<String, ModelPrice> priceTable;
    private final Map<String, TrackedUsage> totals = new ConcurrentHashMap<>();

    public UsageTracker(Map<String, ModelPrice> priceTable) {
        this.priceTable = Map.copyOf(priceTable == null ? Map.of() : priceTable);
    }

    public static UsageTracker defaults() { return new UsageTracker(Map.of()); }

    public TrackedUsage track(String key, String model, LlmUsage usage) {
        ModelPrice price = priceTable.getOrDefault(model,
            new ModelPrice(DEFAULT_PROMPT_PRICE_PER_MILLION_USD, DEFAULT_COMPLETION_PRICE_PER_MILLION_USD));
        double cost = BigDecimal.valueOf(usage.promptTokens()).multiply(BigDecimal.valueOf(price.promptPerMillionUsd()))
            .add(BigDecimal.valueOf(usage.completionTokens()).multiply(BigDecimal.valueOf(price.completionPerMillionUsd())))
            .divide(BigDecimal.valueOf(1_000_000), 8, RoundingMode.HALF_UP).doubleValue();
        return totals.merge(key, new TrackedUsage(usage.promptTokens(), usage.completionTokens(), cost),
            (left, right) -> new TrackedUsage(left.promptTokens() + right.promptTokens(),
                left.completionTokens() + right.completionTokens(), left.costUsd() + right.costUsd()));
    }

    public TrackedUsage total(String key) { return totals.getOrDefault(key, new TrackedUsage(0, 0, 0.0)); }

    public record ModelPrice(double promptPerMillionUsd, double completionPerMillionUsd) {
        public ModelPrice {
            if (!Double.isFinite(promptPerMillionUsd) || promptPerMillionUsd < 0
                || !Double.isFinite(completionPerMillionUsd) || completionPerMillionUsd < 0) {
                throw new IllegalArgumentException("model prices must be finite and non-negative");
            }
        }
    }

    public record TrackedUsage(long promptTokens, long completionTokens, double costUsd) {
        public TrackedUsage {
            if (promptTokens < 0 || completionTokens < 0 || !Double.isFinite(costUsd) || costUsd < 0) {
                throw new IllegalArgumentException("invalid tracked usage");
            }
        }
    }
}
