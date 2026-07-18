package com.suilearn.api.agent.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("suilearn.agent")
public record AgentConfigurationProperties(
    boolean enabled,
    @Min(1) @Max(8) int maxSteps,
    @Min(1) @Max(6) int subagentMaxSteps,
    @Min(1) @Max(16) int maxToolCalls,
    @NotNull Duration runTimeout,
    @Min(2048) @Max(32768) int contextMaxTokens,
    @Min(1) @Max(5) int practiceDefaultCount,
    @NotNull @Valid Session session,
    @NotNull @Valid Memory memory
) {
    private static final Duration MIN_RUN_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration MAX_RUN_TIMEOUT = Duration.ofSeconds(180);

    public AgentConfigurationProperties {
        requireRange("maxSteps", maxSteps, 1, 8);
        requireRange("subagentMaxSteps", subagentMaxSteps, 1, 6);
        requireRange("maxToolCalls", maxToolCalls, 1, 16);
        requireRange("contextMaxTokens", contextMaxTokens, 2048, 32768);
        requireRange("practiceDefaultCount", practiceDefaultCount, 1, 5);
        requireDuration("runTimeout", runTimeout, MIN_RUN_TIMEOUT, MAX_RUN_TIMEOUT);
        if (session == null) {
            throw new IllegalArgumentException("session configuration is required");
        }
        if (memory == null) {
            throw new IllegalArgumentException("memory configuration is required");
        }
    }

    public record Session(@NotNull Duration ttl, @Min(1) @Max(50) int maxTurns) {
        private static final Duration MIN_TTL = Duration.ofHours(1);
        private static final Duration MAX_TTL = Duration.ofHours(168);

        public Session {
            requireDuration("session.ttl", ttl, MIN_TTL, MAX_TTL);
            requireRange("session.maxTurns", maxTurns, 1, 50);
        }
    }

    public record Memory(@Min(1) @Max(10) int topK, double minConfidence) {
        public Memory {
            requireRange("memory.topK", topK, 1, 10);
            if (!Double.isFinite(minConfidence) || minConfidence < 0.50d || minConfidence > 1.00d) {
                throw new IllegalArgumentException("memory.minConfidence must be between 0.50 and 1.00");
            }
        }
    }

    private static void requireRange(String property, int value, int minimum, int maximum) {
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException(property + " must be between " + minimum + " and " + maximum);
        }
    }

    private static void requireDuration(String property, Duration value, Duration minimum, Duration maximum) {
        if (value == null || value.compareTo(minimum) < 0 || value.compareTo(maximum) > 0) {
            throw new IllegalArgumentException(property + " must be between " + minimum + " and " + maximum);
        }
    }
}
